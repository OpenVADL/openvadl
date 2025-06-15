// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.lcb.codegen.expansion;

import static java.util.Objects.requireNonNull;
import static vadl.error.DiagUtils.throwNotAllowed;
import static vadl.lcb.codegen.expansion.CompilerInstructionExpansionCodeGenerator.COMPILER_INSTRUCTION;
import static vadl.lcb.codegen.expansion.CompilerInstructionExpansionCodeGenerator.INSTRUCTION_SYMBOL;
import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.cppCodeGen.FunctionCodeGenerator;
import vadl.cppCodeGen.SymbolTable;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.context.CNodeWithBaggageContext;
import vadl.cppCodeGen.model.GcbCppAccessFunction;
import vadl.cppCodeGen.model.GcbCppEncodeFunction;
import vadl.cppCodeGen.model.GcbCppFunctionWithBody;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.relocation.model.HasRelocationComputationAndUpdate;
import vadl.gcb.valuetypes.TargetName;
import vadl.gcb.valuetypes.VariantKind;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesFormatField;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.utils.ImmediateEncodingFunctionProvider;
import vadl.pass.PassResults;
import vadl.utils.Either;
import vadl.utils.Pair;
import vadl.viam.CompilerInstruction;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.PrintableInstruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.RegisterTensor;
import vadl.viam.Relocation;
import vadl.viam.graph.HasRegisterTensor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.NewLabelNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LabelNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.passes.CfgTraverser;

/**
 * A {@link PseudoInstruction} contains one or multiple {@link Instruction}. This generator
 * creates the CPP code which creates the code to expand the pseudo instruction.
 */
public class CompilerInstructionExpansionCodeGenerator extends FunctionCodeGenerator {
  static final String FIELD = "field";
  static final String INSTRUCTION_CALL_NODE = "instructionCallNode";
  static final String INSTRUCTION = "instruction";
  static final String INSTRUCTION_SYMBOL = "instructionSymbol";
  static final String FIELD_VALUES = "fieldValues";
  static final String COMPILER_INSTRUCTION = "compilerInstruction";

  private final PassResults passResults;
  private final TargetName targetName;
  private final IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages;
  private final List<HasRelocationComputationAndUpdate> relocations;
  private final CompilerInstruction compilerInstruction;
  private final SymbolTable symbolTable;
  private final GenerateLinkerComponentsPass.VariantKindStore variantKindStore;
  private final IdentityHashMap<Instruction, LlvmLoweringRecord.Machine> machineInstructionRecords;
  private final IdentityHashMap<NewLabelNode, String> labelSymbolNameLookup;
  private final Map<Pair<PrintableInstruction, Format.FieldAccess>, GcbCppFunctionWithBody>
      decodingFunctions;
  private final Map<TableGenImmediateRecord, GcbCppAccessFunction> immediateDecodings;

  record FieldInInstruction(PrintableInstruction instruction, Format.Field field) {

  }

  /**
   * Constructor.
   */
  public CompilerInstructionExpansionCodeGenerator(
      PassResults passResults,
      TargetName targetName,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      Map<TableGenImmediateRecord, GcbCppAccessFunction> immediateDecodings,
      List<HasRelocationComputationAndUpdate> relocations,
      GenerateLinkerComponentsPass.VariantKindStore variantKindStore,
      CompilerInstruction compilerInstruction,
      Function function,
      IdentityHashMap<Instruction, LlvmLoweringRecord.Machine> machineInstructionRecords) {
    super(function);
    this.passResults = passResults;
    this.targetName = targetName;
    this.fieldUsages = fieldUsages;
    this.relocations = relocations;
    this.compilerInstruction = compilerInstruction;
    this.symbolTable = new SymbolTable();
    this.variantKindStore = variantKindStore;
    this.machineInstructionRecords = machineInstructionRecords;
    this.labelSymbolNameLookup = new IdentityHashMap<>();
    this.immediateDecodings = immediateDecodings;
    this.decodingFunctions = immediateDecodings
        .entrySet()
        .stream()
        .collect(Collectors.toMap(x -> {
          var key = x.getKey();
          return Pair.of(key.instructionRef(), key.fieldAccessRef());
        }, Map.Entry::getValue));
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadRegTensorNode toHandle) {
    throwNotAllowed(toHandle, "Register reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadMemNode toHandle) {
    throwNotAllowed(toHandle, "Memory reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadArtificialResNode toHandle) {
    throwNotAllowed(toHandle, "Artificial resource reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, FieldAccessRefNode toHandle) {
    throwNotAllowed(toHandle, "Format field accesses");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, FieldRefNode toHandle) {
    throwNotAllowed(toHandle, "field ref accesses");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, AsmBuiltInCall toHandle) {
    throwNotAllowed(toHandle, "Asm builtin calls");
  }

  @Override
  public String genFunctionDefinition() {
    context.ln(genFunctionSignature()).ln("{").spacedIn().ln("std::vector< MCInst > result;");

    var cfgTraversal = new CfgTraverser() {
      @Override
      public ControlNode onDirectional(DirectionalNode dir) {
        if (dir instanceof InstrCallNode instrCallNode) {
          // We compute the values for the fields, and we would like to store them.
          var symbolTableFieldValues = new HashMap<Format.Field, String>();

          var sym = symbolTable.getNextVariable();
          context.ln("MCInst %s = MCInst();", sym)
              .ln("%s.setOpcode(%s::%s);", sym, targetName.value(),
                  instrCallNode.target().identifier.simpleName())
              .ln("{")
              .spacedIn()
              .ln("// " + instrCallNode.target().simpleName());

          instrCallNode.getParamFields().forEach(field -> {
            context.ln("auto %s = 0;", field.identifier.simpleName());
          });

          writeInstructionCall(context,
              compilerInstruction,
              symbolTableFieldValues,
              instrCallNode,
              sym);
          context.spaceOut()
              .ln("}")
              .ln("result.push_back(%s);", sym)
              .ln("callback(%s);", sym);
        } else if (dir instanceof NewLabelNode newLabelNode) {
          var sym = symbolTable.getNextVariable();
          context.ln("MCSymbol *%s = Ctx.createTempSymbol();", sym)
              .ln("callbackSymbol(%s);", sym);
          labelSymbolNameLookup.put(newLabelNode, sym);
        }

        return dir;
      }
    };

    cfgTraversal.traverseBranch(
        function.behavior().getNodes(StartNode.class).findFirst().orElseThrow());

    context.ln("return result;").spaceOut().ln("}");

    return builder.toString();
  }

  private void writeInstructionCall(CNodeContext context,
                                    CompilerInstruction compilerInstruction,
                                    HashMap<Format.Field, String> symbolTableFieldValues,
                                    InstrCallNode instrCallNode,
                                    String instructionSymbol) {
    var fieldAccessesAndArgumentPair =
        Streams.zip(instrCallNode.getParamFieldsOrAccesses().stream(),
            instrCallNode.arguments().stream(),
            Pair::of).toList();

    var reorderParameters =
        reorderParameters(instrCallNode.target(), fieldAccessesAndArgumentPair);

    // Before we add the operands, we have to compute the fields
    // because field access functions might use multiple fields.
    context.ln("// GenerateRawFieldsHandler");
    reorderParameters.forEach(pair -> {
      var fieldOrAccessFunction = pair.left();
      var expr = pair.right();

      var newContext = new CNodeWithBaggageContext(context)
          .put(COMPILER_INSTRUCTION, compilerInstruction)
          .put(INSTRUCTION_CALL_NODE, instrCallNode).put(INSTRUCTION, instrCallNode.target())
          .put(INSTRUCTION_SYMBOL, instructionSymbol)
          .put(FIELD_VALUES, symbolTableFieldValues);

      var firstPass = new GenerateRawFieldsHandler(fieldUsages, targetName,
          this.compilerInstruction);
      firstPass.handle(newContext,
          instrCallNode,
          fieldOrAccessFunction.isRight() ? fieldOrAccessFunction.right() : null,
          fieldOrAccessFunction.isLeft() ? fieldOrAccessFunction.left() : null,
          expr);
    });

    context.ln("// DecodeFieldAccessesHandler");

    reorderParameters.forEach(pair -> {
      var fieldOrAccessFunction = pair.left();
      var expr = pair.right();

      var newContext = new CNodeWithBaggageContext(context)
          .put(COMPILER_INSTRUCTION, compilerInstruction)
          .put(INSTRUCTION_CALL_NODE, instrCallNode).put(INSTRUCTION, instrCallNode.target())
          .put(INSTRUCTION_SYMBOL, instructionSymbol)
          .put(FIELD_VALUES, symbolTableFieldValues);

      var pass = new DecodeFieldAccessesHandler(passResults,
          fieldUsages,
          targetName,
          this.compilerInstruction,
          relocations,
          decodingFunctions,
          machineInstructionRecords);
      pass.handle(newContext,
          instrCallNode,
          fieldOrAccessFunction.isRight() ? fieldOrAccessFunction.right() : null,
          fieldOrAccessFunction.isLeft() ? fieldOrAccessFunction.left() : null,
          expr);
    });

    context.ln("// AddingOperands");

    int addedOperands = 0;
    for (var pair : reorderParameters) {
      var fieldOrAccessFunction = pair.left();
      var expr = pair.right();

      var newContext = new CNodeWithBaggageContext(context)
          .put(COMPILER_INSTRUCTION, compilerInstruction)
          .put(INSTRUCTION_CALL_NODE, instrCallNode).put(INSTRUCTION, instrCallNode.target())
          .put(INSTRUCTION_SYMBOL, instructionSymbol)
          .put(FIELD_VALUES, symbolTableFieldValues);

      var pass =
          new AddingOperands(targetName,
              fieldUsages,
              symbolTable,
              relocations,
              variantKindStore,
              decodingFunctions,
              machineInstructionRecords,
              immediateDecodings,
              labelSymbolNameLookup,
              addedOperands);
      pass.handle(newContext,
          instrCallNode,
          fieldOrAccessFunction.isRight() ? fieldOrAccessFunction.right() : null,
          fieldOrAccessFunction.isLeft() ? fieldOrAccessFunction.left() : null,
          expr);

      addedOperands = pass.getAddedOperand();
    }
  }

  /**
   * Generate an expression for the given {@code field} based on the {@code cn}.
   *
   * @param instrCallNode
   * @param field         is the field (or the field behind a field access function) which is
   *                      assigned in the instruction call in the pseudo instruction.
   * @param cn            is the constant node which is assigned.
   * @return an expression.
   */
  private String generateFieldValue(InstrCallNode instrCallNode,
                                    Format.Field field,
                                    ConstantNode cn) {
    // Based on the usage, we know that we have to generate an immediate or a register.
    var usages = fieldUsages.getFieldUsages(instrCallNode.target()).get(field);
    ensure(usages != null && usages.size() == 1, () -> {
      throw Diagnostic.error(
          "Cannot expand pseudo instruction because the usage of the field is unclear",
          field.location()).build();
    });
    var usage = Objects.requireNonNull(usages).getFirst();

    // The cn might be
    // rd = 1 or imm = 1 or immS = 1
    // By switching over the usage, we know what it is.
    switch (usage) {
      case IMMEDIATE -> {
        // We have a case distinction.
        // We either have an assignment to a field or an assignment to field access function.

        if (instrCallNode.isParameterField(field)) {
          // It's an assigment to a field, therefore we have to decode the value since
          // the MCInst only stores decoded values.
          // We store the raw the value of the field, and then we decode the value.
          // But we decode it later because the first pass only creates the raw values.

          context.ln("%s = %s;", field.identifier.simpleName(), cn.constant().asVal().intValue());
        } else {

        }
      }
    }

    return "";
  }

  /**
   * The order of the parameters is not necessarily the order in which the expansion should happen.
   * This function looks at the {@link LlvmLoweringRecord} of the corresponding instruction
   * and reorders the list according to the order of outputs and inputs.
   */
  private List<Pair<Either<Format.Field, Format.FieldAccess>, ExpressionNode>> reorderParameters(
      Instruction instruction,
      List<Pair<Either<Format.Field, Format.FieldAccess>, ExpressionNode>> pairs) {
    var result = new ArrayList<Pair<Either<Format.Field, Format.FieldAccess>, ExpressionNode>>();
    var lookupFields = pairs.stream().filter(x -> x.left().isLeft())
        .collect(Collectors.toMap(x -> x.left().left(), Pair::right));
    var lookupFieldAccesses = pairs.stream().filter(x -> x.left().isRight())
        .collect(Collectors.toMap(x -> x.left().right(), Pair::right));

    var llvmRecord = ensureNonNull(machineInstructionRecords.get(instruction),
        () -> Diagnostic.error("Cannot find llvmRecord for instruction used in pseudo instruction",
            instruction.location()));

    var order = llvmRecord.info().outputInputOperands();

    for (var item : order) {
      if (item instanceof ReferencesImmediateOperand referencesImmediateOperand) {
        var fieldAccess = referencesImmediateOperand.immediateOperand().fieldAccessRef();

        // Does the InstrCallNode have a reference to a field access or to a field?
        if (lookupFieldAccesses.containsKey(fieldAccess)) {
          var value = lookupFieldAccesses.get(fieldAccess);
          result.add(Pair.of(new Either<>(null, fieldAccess), value));
        } else {
          fieldAccess.fieldRefs().forEach(field -> {
            if (lookupFields.containsKey(field)) {
              var value = lookupFields.get(field);
              result.add(Pair.of(new Either<>(field, null), value));
            }
          });
        }
      } else if (item instanceof ReferencesFormatField referencesFormatField) {
        if (lookupFields.containsKey(referencesFormatField.formatField())) {
          var value = lookupFields.get(referencesFormatField.formatField());
          result.add(Pair.of(new Either<>(referencesFormatField.formatField(), null), value));
        }
      }
    }

    return result;
  }

  @Override
  public String genFunctionSignature() {
    return "std::vector<MCInst> %sMCInstExpander::%s_%s".formatted(targetName.value(),
        compilerInstruction.identifier.lower(), function.simpleName())
        + "(const MCInst& instruction, std::function<void(const MCInst &)> callback, "
        + "std::function<void(MCSymbol* )> callbackSymbol ) const";
  }
}

interface CaseHandler {
  /*
    The expansion generator has two necessary passes.
    First, it generates the fields with the raw values.
    Second, it applies the decoding functions and adds the raw or decoded values as operand to
    the instruction.

    Every pseudo instruction contains multiple instructions.
    Each instruction is a InstrCallNode with arguments.
    The methods here handle different cases under which condition code should be emitted.

    -> rd = 0        : Register assignment and expression is a constant
    -> rd = symbol   : Register assignment and expression is a FuncParamNode
    -> rd = func(XXX): Register assignment and expression is a FuncCallNode

    For all these cases, we need to decode the value for the field access functions.
    -> imm = 0        : Field assignment and expression is a constant
    -> imm = symbol   : Field assignment and expression is a FuncParamNode
    -> imm = func(XXX): Field assignment and expression is a FuncCallNode

    -> immS = 0        : Field access assignment and expression is a constant
    -> immS = symbol   : Field access assignment and expression is a FuncParamNode
    -> immS = func(XXX): Field access assignment and expression is a FuncCallNode
   */

  void fieldUsedAsRegisterAndExpressionConstant(CNodeWithBaggageContext newContext,
                                                InstrCallNode instrCallNode,
                                                Format.Field field,
                                                ConstantNode cn);

  void fieldUsedAsRegisterAndExpressionFuncParamNode(CNodeWithBaggageContext newContext,
                                                     InstrCallNode instrCallNode,
                                                     Format.Field field,
                                                     FuncParamNode funcParamNode);

  void fieldUsedAsRegisterAndExpressionFuncCallNode(CNodeWithBaggageContext newContext,
                                                    InstrCallNode instrCallNode,
                                                    Format.Field field,
                                                    FuncCallNode funcCallNode);

  void fieldUsedAsImmediateAndFieldAssignmentAndExpressionConstant(
      CNodeWithBaggageContext newContext, InstrCallNode instrCallNode, Format.Field field,
      ConstantNode cn);

  void fieldUsedAsImmediateAndFieldAssignmentAndExpressionFuncParamNode(
      CNodeWithBaggageContext newContext, InstrCallNode instrCallNode, Format.Field field,
      FuncParamNode funcParamNode);

  void fieldUsedAsImmediateAndFieldAssignmentAndExpressionFuncCallNode(
      CNodeWithBaggageContext newContext, InstrCallNode instrCallNode, Format.Field field,
      FuncCallNode funcCallNode);

  void fieldUsedAsImmediateAndFieldAccessAssignmentAndExpressionConstant(
      CNodeWithBaggageContext newContext, InstrCallNode instrCallNode,
      Format.FieldAccess fieldAccess,
      ConstantNode cn);

  void fieldUsedAsImmediateAndFieldAccessAssignmentAndExpressionFuncParamNode(
      CNodeWithBaggageContext newContext, InstrCallNode instrCallNode,
      Format.FieldAccess fieldAccess,
      FuncParamNode funcParamNode);

  void fieldUsedAsImmediateAndFieldAccessAssignmentAndExpressionFuncCallNode(
      CNodeWithBaggageContext newContext, InstrCallNode instrCallNode,
      Format.FieldAccess fieldAccess,
      FuncCallNode funcCallNode);

  default void handle(CNodeWithBaggageContext newContext,
                      InstrCallNode instrCallNode,
                      @Nullable Format.FieldAccess fieldAccess,
                      @Nullable Format.Field field,
                      ExpressionNode expr) {
    // The field argument is a field or a field behind a field access function.
    var isAssignmentToFieldAccessFunction = fieldAccess != null;
    var isAssignmentToField = field != null;
    var isFieldUsedAsImmediate = isImmediate(instrCallNode, field);
    var isFieldAccessUsedAsImmediate = isImmediate(instrCallNode, fieldAccess);
    var isFieldUsedAsRegister = !isFieldUsedAsImmediate && !isFieldAccessUsedAsImmediate;

    if (isFieldUsedAsRegister && expr instanceof ConstantNode cn) {
      fieldUsedAsRegisterAndExpressionConstant(newContext, instrCallNode,
          Objects.requireNonNull(field), cn);
    } else if (isFieldUsedAsRegister && expr instanceof FuncParamNode funcParamNode) {
      fieldUsedAsRegisterAndExpressionFuncParamNode(
          newContext,
          instrCallNode,
          Objects.requireNonNull(field),
          funcParamNode);
    } else if (isFieldUsedAsRegister && isAssignmentToField
        && expr instanceof FuncCallNode funcCallNode) {
      fieldUsedAsRegisterAndExpressionFuncCallNode(
          newContext,
          instrCallNode,
          Objects.requireNonNull(field),
          funcCallNode);
    } else if (isFieldUsedAsImmediate && expr instanceof ConstantNode cn) {
      fieldUsedAsImmediateAndFieldAssignmentAndExpressionConstant(
          newContext,
          instrCallNode,
          Objects.requireNonNull(field),
          cn);
    } else if (isFieldUsedAsImmediate && expr instanceof FuncParamNode funcParamNode) {
      fieldUsedAsImmediateAndFieldAssignmentAndExpressionFuncParamNode(
          newContext,
          instrCallNode,
          Objects.requireNonNull(field),
          funcParamNode);
    } else if (isFieldUsedAsImmediate && expr instanceof FuncCallNode funcCallNode) {
      fieldUsedAsImmediateAndFieldAssignmentAndExpressionFuncCallNode(
          newContext,
          instrCallNode,
          Objects.requireNonNull(field),
          funcCallNode);
    } else if (
        isAssignmentToFieldAccessFunction
            && expr instanceof ConstantNode cn) {
      fieldUsedAsImmediateAndFieldAccessAssignmentAndExpressionConstant(
          newContext,
          instrCallNode,
          Objects.requireNonNull(fieldAccess),
          cn);
    } else if (isAssignmentToFieldAccessFunction
        && expr instanceof FuncParamNode funcParamNode) {
      fieldUsedAsImmediateAndFieldAccessAssignmentAndExpressionFuncParamNode(
          newContext,
          instrCallNode,
          Objects.requireNonNull(fieldAccess),
          funcParamNode);
    } else if (isAssignmentToFieldAccessFunction
        && expr instanceof FuncCallNode funcCallNode) {
      fieldUsedAsImmediateAndFieldAccessAssignmentAndExpressionFuncCallNode(
          newContext,
          instrCallNode,
          Objects.requireNonNull(fieldAccess),
          funcCallNode);
    }
  }

  private boolean isImmediate(InstrCallNode instrCallNode, @Nullable Format.Field field) {
    if (field == null) {
      return false;
    }

    var usage = fieldUsages().getFieldUsages(instrCallNode.target()).get(field);
    ensure(usage != null, "usage must not be null");
    ensure(usage.size() == 1, () -> {
      throw Diagnostic.error(
          "Cannot expand pseudo instruction because the usage of the field is unclear",
          field.location()).build();
    });
    return usage.getFirst() == IdentifyFieldUsagePass.FieldUsage.IMMEDIATE;
  }

  private boolean isImmediate(InstrCallNode instrCallNode,
                              @Nullable Format.FieldAccess fieldAccess) {
    if (fieldAccess == null) {
      return false;
    }

    // At the moment, all field accesses are immediates.
    return true;
  }

  default RegisterTensor getRegisterFile(
      Instruction instruction,
      Format.Field field
  ) {
    // We know that `field` is used as a register index.
    // But we don't know which register file.
    // We look for the `field` in the machine instruction's behavior and return the usages.
    var registerFiles = instruction.behavior().getNodes(FieldRefNode.class)
        .filter(x -> x.formatField().equals(field)).flatMap(
            fieldRefNode -> fieldRefNode.usages()
                .filter(y -> y instanceof HasRegisterTensor z && z.hasRegisterFile()))
        .map(x -> ((HasRegisterTensor) x).registerTensor()).distinct().toList();

    ensure(registerFiles.size() == 1,
        () -> Diagnostic.error("Found multiple or none register files for this field.",
            field.location()).note(
            "The pseudo instruction expansion requires one register file to detect "
                + "the register file name. In this particular case is the field used by "
                + "multiple register files or none and we don't know which name to use."));

    return
        ensurePresent(registerFiles.stream().findFirst(), "Expected one register file");
  }

  /**
   * Trying to get the index from {@code pseudoInstruction.parameters} based on the given
   * {@code parameter}. This index is important because it is the index of the pseudo instruction's
   * operands which will be used for the pseudo instruction's expansion.
   * For example, you have pseudo instruction {@code X(arg1, arg2) } which has two arguments. You
   * have to know that {@code arg2} has index {@code 1} to correctly map it to a machine
   * instruction later.
   */
  default int getOperandIndexFromCompilerInstruction(
      CompilerInstruction compilerInstruction,
      Format.Field field,
      ExpressionNode argument,
      Identifier parameter) {
    for (int i = 0; i < compilerInstruction.parameters().length; i++) {
      if (parameter.simpleName()
          .equals(compilerInstruction.parameters()[i].identifier.simpleName())) {
        return i;
      }
    }

    throw Diagnostic.error(
            String.format("Cannot assign field '%s' because the field is not a field.",
                field.identifier.simpleName()), parameter.location())
        .locationDescription(argument.location(), "Trying to match this argument.")
        .locationDescription(field.location(), "Trying to assign this field.")
        .locationDescription(compilerInstruction.location(),
            "This pseudo instruction is affected.")
        .help("The parameter '%s' must match any pseudo instruction's parameter names",
            parameter.simpleName()).build();
  }

  /**
   * Trying to get the index from {@code pseudoInstruction.parameters} based on the given
   * {@code parameter}. This index is important because it is the index of the pseudo instruction's
   * operands which will be used for the pseudo instruction's expansion.
   * For example, you have pseudo instruction {@code X(arg1, arg2) } which has two arguments. You
   * have to know that {@code arg2} has index {@code 1} to correctly map it to a machine
   * instruction later.
   */
  default int getOperandIndexFromCompilerInstruction(
      CompilerInstruction compilerInstruction,
      Format.FieldAccess fieldAccess,
      ExpressionNode argument,
      Identifier parameter) {
    for (int i = 0; i < compilerInstruction.parameters().length; i++) {
      if (parameter.simpleName()
          .equals(compilerInstruction.parameters()[i].identifier.simpleName())) {
        return i;
      }
    }

    throw Diagnostic.error(
            String.format("Cannot assign field '%s' because the field access is not a field access.",
                fieldAccess.identifier.simpleName()), parameter.location())
        .locationDescription(argument.location(), "Trying to match this argument.")
        .locationDescription(fieldAccess.location(), "Trying to assign this field access function.")
        .locationDescription(compilerInstruction.location(),
            "This pseudo instruction is affected.")
        .help("The parameter '%s' must match any pseudo instruction's parameter names",
            parameter.simpleName()).build();
  }

  IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages();
}

class GenerateRawFieldsHandler implements CaseHandler {

  private final IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages;
  private final TargetName targetName;
  private final CompilerInstruction compilerInstruction;

  GenerateRawFieldsHandler(
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      TargetName targetName,
      CompilerInstruction compilerInstruction) {
    this.fieldUsages = fieldUsages;
    this.targetName = targetName;
    this.compilerInstruction = compilerInstruction;
  }

  @Override
  public void fieldUsedAsRegisterAndExpressionConstant(CNodeWithBaggageContext ctx,
                                                       InstrCallNode instrCallNode,
                                                       Format.Field field, ConstantNode cn) {
    var registerFile = getRegisterFile(instrCallNode.target(), field);
    var register =
        String.format("%s::%s%s",
            targetName.value(),
            registerFile.identifier.simpleName(),
            cn.constant().asVal().intValue());
    ctx.ln("%s = %s;", field.identifier.simpleName(), register);
  }

  @Override
  public void fieldUsedAsRegisterAndExpressionFuncParamNode(CNodeWithBaggageContext ctx,
                                                            InstrCallNode instrCallNode,
                                                            Format.Field field,
                                                            FuncParamNode funcParamNode) {
    var pseudoInstructionIndex =
        getOperandIndexFromCompilerInstruction(compilerInstruction, field, funcParamNode,
            funcParamNode.parameter().identifier);
    ctx.ln("%s = instruction.getOperand(%d).getReg();", field.identifier.simpleName(),
        pseudoInstructionIndex);
  }

  @Override
  public void fieldUsedAsRegisterAndExpressionFuncCallNode(CNodeWithBaggageContext ctx,
                                                           InstrCallNode instrCallNode,
                                                           Format.Field field,
                                                           FuncCallNode funcCallNode) {
    throw Diagnostic.error("Not implemented", field.location()).build();
  }

  @Override
  public void fieldUsedAsImmediateAndFieldAssignmentAndExpressionConstant(
      CNodeWithBaggageContext ctx, InstrCallNode instrCallNode, Format.Field field,
      ConstantNode cn) {
    var value = cn.constant().asVal().intValue();
    ctx.ln("%s = %s;", field.identifier.simpleName(), value);
  }

  @Override
  public void fieldUsedAsImmediateAndFieldAssignmentAndExpressionFuncParamNode(
      CNodeWithBaggageContext ctx, InstrCallNode instrCallNode, Format.Field field,
      FuncParamNode funcParamNode) {
    var pseudoInstructionIndex =
        getOperandIndexFromCompilerInstruction(compilerInstruction, field, funcParamNode,
            funcParamNode.parameter().identifier);
    ctx.ln("if(instruction.getOperand(%d).isImm()) {", pseudoInstructionIndex)
        .spacedIn()
        .ln("%s = instruction.getOperand(%d).getImm();", field.identifier.simpleName(),
            pseudoInstructionIndex)
        .spaceOut()
        .ln("}");
  }

  @Override
  public void fieldUsedAsImmediateAndFieldAssignmentAndExpressionFuncCallNode(
      CNodeWithBaggageContext ctx, InstrCallNode instrCallNode, Format.Field field,
      FuncCallNode funcCallNode) {
    if (funcCallNode.arguments().getFirst() instanceof FuncParamNode funcParamNode) {
      var pseudoInstructionIndex =
          getOperandIndexFromCompilerInstruction(compilerInstruction, field,
              funcParamNode,
              funcParamNode.parameter().identifier);

      ctx.ln("if(instruction.getOperand(%d).isImm()) {", pseudoInstructionIndex)
          .spacedIn()
          .ln("%s = instruction.getOperand(%d).getImm();", field.identifier.simpleName(),
              pseudoInstructionIndex)
          .spaceOut()
          .ln("}");
    }
  }

  @Override
  public void fieldUsedAsImmediateAndFieldAccessAssignmentAndExpressionConstant(
      CNodeWithBaggageContext ctx,
      InstrCallNode instrCallNode,
      Format.FieldAccess fieldAccess,
      ConstantNode cn) {
    // We don't assign the field in this pass, but the field access function.
    var value = cn.constant().asVal().intValue();
    ctx.ln("auto %s = %s;", fieldAccess.identifier.simpleName(), value);
  }

  @Override
  public void fieldUsedAsImmediateAndFieldAccessAssignmentAndExpressionFuncParamNode(
      CNodeWithBaggageContext ctx,
      InstrCallNode instrCallNode,
      Format.FieldAccess fieldAccess,
      FuncParamNode funcParamNode) {
    var pseudoInstructionIndex =
        getOperandIndexFromCompilerInstruction(compilerInstruction, fieldAccess.fieldRef(),
            funcParamNode,
            funcParamNode.parameter().identifier);
    ctx.ln("auto %s = instruction.getOperand(%d).getImm();", fieldAccess.identifier.simpleName(),
        pseudoInstructionIndex);
  }

  @Override
  public void fieldUsedAsImmediateAndFieldAccessAssignmentAndExpressionFuncCallNode(
      CNodeWithBaggageContext ctx, InstrCallNode instrCallNode, Format.FieldAccess fieldAccess,
      FuncCallNode funcCallNode) {

    if (funcCallNode.arguments().getFirst() instanceof FuncParamNode funcParamNode) {
      var pseudoInstructionIndex =
          getOperandIndexFromCompilerInstruction(compilerInstruction, fieldAccess.fieldRef(),
              funcParamNode,
              funcParamNode.parameter().identifier);
      ctx.ln("auto %s = instruction.getOperand(%d).getImm();", fieldAccess.identifier.simpleName(),
          pseudoInstructionIndex);
    }
  }

  @Override
  public IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages() {
    return fieldUsages;
  }
}

class DecodeFieldAccessesHandler implements CaseHandler {

  private final PassResults passResults;
  private final IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages;
  private final TargetName targetName;
  private final CompilerInstruction compilerInstruction;
  private final List<HasRelocationComputationAndUpdate> relocations;
  private final Map<Pair<PrintableInstruction, Format.FieldAccess>, GcbCppFunctionWithBody>
      decodingFunctions;
  private final IdentityHashMap<Instruction, LlvmLoweringRecord.Machine> machineInstructionRecords;

  DecodeFieldAccessesHandler(
      PassResults passResult,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      TargetName targetName,
      CompilerInstruction compilerInstruction,
      List<HasRelocationComputationAndUpdate> relocations,
      Map<Pair<PrintableInstruction, Format.FieldAccess>, GcbCppFunctionWithBody>
          decodingFunctions,

      IdentityHashMap<Instruction, LlvmLoweringRecord.Machine> machineInstructionRecords) {
    this.passResults = passResult;
    this.fieldUsages = fieldUsages;
    this.targetName = targetName;
    this.compilerInstruction = compilerInstruction;
    this.decodingFunctions = decodingFunctions;
    this.relocations = relocations;
    this.machineInstructionRecords = machineInstructionRecords;
  }

  @Override
  public void fieldUsedAsRegisterAndExpressionConstant(CNodeWithBaggageContext ctx,
                                                       InstrCallNode instrCallNode,
                                                       Format.Field field, ConstantNode cn) {

  }

  @Override
  public void fieldUsedAsRegisterAndExpressionFuncParamNode(CNodeWithBaggageContext ctx,
                                                            InstrCallNode instrCallNode,
                                                            Format.Field field,
                                                            FuncParamNode funcParamNode) {

  }

  @Override
  public void fieldUsedAsRegisterAndExpressionFuncCallNode(CNodeWithBaggageContext ctx,
                                                           InstrCallNode instrCallNode,
                                                           Format.Field field,
                                                           FuncCallNode funcCallNode) {
  }

  @Override
  public void fieldUsedAsImmediateAndFieldAssignmentAndExpressionConstant(
      CNodeWithBaggageContext ctx, InstrCallNode instrCallNode, Format.Field field,
      ConstantNode cn) {

  }

  @Override
  public void fieldUsedAsImmediateAndFieldAssignmentAndExpressionFuncParamNode(
      CNodeWithBaggageContext ctx, InstrCallNode instrCallNode, Format.Field field,
      FuncParamNode funcParamNode) {

  }

  @Override
  public void fieldUsedAsImmediateAndFieldAssignmentAndExpressionFuncCallNode(
      CNodeWithBaggageContext ctx, InstrCallNode instrCallNode, Format.Field field,
      FuncCallNode funcCallNode) {
  }

  @Override
  public void fieldUsedAsImmediateAndFieldAccessAssignmentAndExpressionConstant(
      CNodeWithBaggageContext ctx,
      InstrCallNode instrCallNode,
      Format.FieldAccess fieldAccess,
      ConstantNode cn) {
  }

  @Override
  public void fieldUsedAsImmediateAndFieldAccessAssignmentAndExpressionFuncParamNode(
      CNodeWithBaggageContext ctx,
      InstrCallNode instrCallNode,
      Format.FieldAccess fieldAccess,
      FuncParamNode funcParamNode) {

  }

  @Override
  public void fieldUsedAsImmediateAndFieldAccessAssignmentAndExpressionFuncCallNode(
      CNodeWithBaggageContext ctx, InstrCallNode instrCallNode, Format.FieldAccess fieldAccess,
      FuncCallNode funcCallNode) {
    // immS = lo(symbol)
    ensure(funcCallNode.function() instanceof Relocation, () ->
        Diagnostic.error("Only supporting relocation functions at the moment",
            funcCallNode.location()));
    ensure(funcCallNode.function().parameters().length == 1, () ->
        Diagnostic.error("Only supporting functions with one argument at the moment",
            funcCallNode.location()));
    var relocation = (Relocation) funcCallNode.function();

    // First apply relocation function then decode the value.
    var relocationRef =
        ensurePresent(relocations.stream().filter(x -> x.relocation() == relocation).findFirst(),
            () -> Diagnostic.error("Cannot find relocation", relocation.location()));

    if (funcCallNode.arguments().getFirst() instanceof FuncParamNode funcParamNode) {
      // There are two cases:
      // (1) the `symbol` in `immS = lo(symbol)` is actually an immediate
      // (2) the `symbol` in `immS = lo(symbol)` is an unknown address
      // The (1) case is easy because we cannot handle that.
      // For (2) case, we need to create an MCExpr with the variant kinds.
      // Since both types don't match, we assign for (2) the value `0`.

      var pseudoInstructionIndex =
          getOperandIndexFromCompilerInstruction(compilerInstruction, fieldAccess, funcCallNode,
              funcParamNode.parameter().identifier);
      ctx.ln("if(instruction.getOperand(%d).isImm()) {", pseudoInstructionIndex)
          .spacedIn();

      // Assign the field access
      ctx.ln("%s = %sBaseInfo::%s(instruction.getOperand(%d).getImm());",
          fieldAccess.identifier.simpleName(),
          targetName.value(),
          relocationRef.valueRelocation().functionName().lower(),
          pseudoInstructionIndex);

      var encodingFunctions =
          Objects.requireNonNull(
              ImmediateEncodingFunctionProvider.generateEncodeFunctions(passResults)
                  .get(instrCallNode.target()));

      fieldAccess.format().fieldEncodingsOf(Set.of(fieldAccess))
          .forEach(fieldEncoding -> {
            for (var encodingFunction : encodingFunctions) {
              if (encodingFunction.field() == fieldEncoding.targetField()) {
                encodeField(ctx, fieldEncoding, encodingFunction);
              }
            }
          });

      ctx
          .spaceOut()
          .ln("}");
    }
  }

  private void encodeField(
      CNodeWithBaggageContext ctx,
      Format.FieldEncoding fieldEncoding,
      GcbCppEncodeFunction encodingFunction) {
    var fieldToEncode = fieldEncoding.targetField();
    var encodingFunctionName = encodingFunction.header().functionName().lower();

    var params = Arrays.stream(encodingFunction.header().parameters())
        .map(param -> param.identifier.simpleName()).collect(
            Collectors.joining(", "));

    ctx.ln("%s = %s(%s)", fieldToEncode.identifier.simpleName(), encodingFunctionName, params);
  }

  @Override
  public IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages() {
    return fieldUsages;
  }
}

class AddingOperands implements CaseHandler {
  private final TargetName targetName;
  private final IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages;
  private final SymbolTable symbolTable;
  private final List<HasRelocationComputationAndUpdate> relocations;
  private final GenerateLinkerComponentsPass.VariantKindStore variantKindStore;
  private final Map<Pair<PrintableInstruction, Format.FieldAccess>, GcbCppFunctionWithBody>
      decodingFunctions;
  private final Map<Instruction, LlvmLoweringRecord.Machine> machineInstructionRecords;
  private final Map<Pair<PrintableInstruction, Format.FieldAccess>, GcbCppAccessFunction>
      immediateDecodings;
  private final Map<NewLabelNode, String> labelSymbolNameLookup;
  // Tracks how many operands were already added.
  private int addedOperand;

  public int getAddedOperand() {
    return addedOperand;
  }

  public AddingOperands(
      TargetName targetName,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      SymbolTable symbolTable,
      List<HasRelocationComputationAndUpdate> relocations,
      GenerateLinkerComponentsPass.VariantKindStore variantKindStore,
      Map<Pair<PrintableInstruction, Format.FieldAccess>, GcbCppFunctionWithBody> decodingFunctions,
      Map<Instruction, LlvmLoweringRecord.Machine> machineInstructionRecords,
      Map<TableGenImmediateRecord, GcbCppAccessFunction> immediateDecodings,
      Map<NewLabelNode, String> labelSymbolNameLookup,
      int addedOperands) {
    this.targetName = targetName;
    this.fieldUsages = fieldUsages;
    this.symbolTable = symbolTable;
    this.relocations = relocations;
    this.variantKindStore = variantKindStore;
    this.decodingFunctions = decodingFunctions;
    this.machineInstructionRecords = machineInstructionRecords;
    this.immediateDecodings = immediateDecodings
        .entrySet()
        .stream()
        .collect(Collectors.toMap(x -> {
          var key = x.getKey();
          return Pair.of(key.instructionRef(), key.fieldAccessRef());
        }, Map.Entry::getValue));
    this.labelSymbolNameLookup = labelSymbolNameLookup;
    this.addedOperand = addedOperands;
  }

  @Override
  public void fieldUsedAsRegisterAndExpressionConstant(CNodeWithBaggageContext ctx,
                                                       InstrCallNode instrCallNode,
                                                       Format.Field field,
                                                       ConstantNode cn) {
    var instructionSymbol = ctx.getString(INSTRUCTION_SYMBOL);
    ctx.ln("%s.addOperand(MCOperand::createReg(%s));",
        instructionSymbol,
        field.identifier.simpleName());
    addedOperand++;
  }

  @Override
  public void fieldUsedAsRegisterAndExpressionFuncParamNode(CNodeWithBaggageContext ctx,
                                                            InstrCallNode instrCallNode,
                                                            Format.Field field,
                                                            FuncParamNode funcParamNode) {
    var compilerInstruction =
        ctx.get(COMPILER_INSTRUCTION, CompilerInstruction.class);
    var instructionSymbol = ctx.getString(INSTRUCTION_SYMBOL);

    var pseudoInstructionIndex =
        getOperandIndexFromCompilerInstruction(compilerInstruction,
            field,
            funcParamNode,
            funcParamNode.parameter().identifier);
    ctx.ln("%s.addOperand(instruction.getOperand(%d)); // %s",
        instructionSymbol,
        pseudoInstructionIndex,
        field.identifier.simpleName());
    addedOperand++;
  }

  @Override
  public void fieldUsedAsRegisterAndExpressionFuncCallNode(CNodeWithBaggageContext ctx,
                                                           InstrCallNode instrCallNode,
                                                           Format.Field field,
                                                           FuncCallNode funcCallNode) {
    throw Diagnostic.error("not supported", funcCallNode.location()).build();
  }

  @Override
  public void fieldUsedAsImmediateAndFieldAssignmentAndExpressionConstant(
      CNodeWithBaggageContext ctx, InstrCallNode instrCallNode, Format.Field field,
      ConstantNode cn) {
    var instructionSymbol = ctx.getString(INSTRUCTION_SYMBOL);
    ctx.ln("%s.addOperand(MCOperand::createImm(%s));", instructionSymbol,
        field.identifier.simpleName());
    addedOperand++;
  }

  @Override
  public void fieldUsedAsImmediateAndFieldAssignmentAndExpressionFuncParamNode(
      CNodeWithBaggageContext ctx, InstrCallNode instrCallNode, Format.Field field,
      FuncParamNode funcParamNode) {
    var compilerInstruction =
        ctx.get(COMPILER_INSTRUCTION, CompilerInstruction.class);
    var instructionSymbol = ctx.getString(INSTRUCTION_SYMBOL);
    var pseudoInstructionIndex =
        getOperandIndexFromCompilerInstruction(compilerInstruction,
            field,
            funcParamNode,
            funcParamNode.parameter().identifier);

    // There are two cases:
    // (1) in one case, the operand is an immediate then we must create an immediate operand.
    ctx.ln("// We don't know whether the operand is an immediate or label?")
        .ln("// We find it out during parsing.")
        .ln("if(instruction.getOperand(%d).isImm()) {", pseudoInstructionIndex)
        .spacedIn()
        .ln("%s = instruction.getOperand(%d).getImm();", field.identifier.simpleName(),
            pseudoInstructionIndex)
        .ln(
            String.format(
                "%s.addOperand(MCOperand::createImm(instruction.getOperand(%d).getImm()));"
                    + "// %s",
                instructionSymbol,
                pseudoInstructionIndex,
                field.identifier.simpleName()))
        .spaceOut()
        .ln("}")
        .ln("else {")
        .spacedIn();

    // (2) the other case is when it is a label or an immediate with a modifier.
    var argumentSymbol = symbolTable.getNextVariable();
    ctx.ln("const MCExpr* %s = MCOperandToMCExpr(instruction.getOperand(%d));", argumentSymbol,
        pseudoInstructionIndex);

    var variants = variantKindStore.decodeVariantKindsByField(instrCallNode.target(), field);
    ensure(variants.size() == 1, () -> Diagnostic.error(
        "There are unexpectedly multiple variant kinds for the pseudo expansion available.",
        funcParamNode.location()));

    var variant = ensurePresent(
        requireNonNull(variants).stream().filter(VariantKind::isImmediate).findFirst(),
        () -> Diagnostic.error(
            "Expected a variant for an immediate. But haven't " + "found any",
            funcParamNode.location())).value();

    var argumentImmSymbol = symbolTable.getNextVariable();
    ctx.ln(
        "MCOperand %s = MCOperand::createExpr(%sMCExpr::create(%s, "
            + "%sMCExpr::VariantKind::%s, " + "Ctx));", argumentImmSymbol,
        targetName.value(),
        argumentSymbol,
        targetName.value(),
        variant);
    ctx.ln(String.format("%s.addOperand(%s); // %s", instructionSymbol, argumentImmSymbol,
        field.identifier.simpleName()));

    ctx
        .spaceOut()
        .ln("}");
    addedOperand++;
  }

  @Override
  public void fieldUsedAsImmediateAndFieldAssignmentAndExpressionFuncCallNode(
      CNodeWithBaggageContext ctx,
      InstrCallNode instrCallNode,
      Format.Field field,
      FuncCallNode funcCallNode) {
    // imm = lo(symbol)

    var compilerInstruction =
        ctx.get(COMPILER_INSTRUCTION, CompilerInstruction.class);
    var instructionSymbol = ctx.getString(INSTRUCTION_SYMBOL);

    ensure(funcCallNode.function() instanceof Relocation, () ->
        Diagnostic.error("Only supporting relocation functions at the moment",
            funcCallNode.location()));
    ensure(funcCallNode.function().parameters().length == 1, () ->
        Diagnostic.error("Only supporting functions with one argument at the moment",
            funcCallNode.location()));
    var relocation = (Relocation) funcCallNode.function();
    var relocationRef =
        ensurePresent(relocations.stream().filter(x -> x.relocation() == relocation).findFirst(),
            () -> Diagnostic.error("Cannot find relocation", relocation.location()));
    var elfRelocation = relocations.stream().filter(x -> x.relocation() == relocation).findFirst();
    ensure(elfRelocation.isPresent(), "elfRelocation must exist");
    var variant = elfRelocation.get().variantKind().value();

    if (funcCallNode.arguments().getFirst() instanceof FuncParamNode funcParamNode) {
      // There are two cases:
      // (1) the `symbol` in `imm = lo(symbol)` is actually an immediate
      // (2) the `symbol` in `imm = lo(symbol)` is an unknown address
      // The (1) case is easy because we cannot handle that.
      // For (2) case, we need to create an MCExpr with the variant kinds.
      // Since both types don't match, we assign for (2) the value `0`.

      var pseudoInstructionIndex =
          getOperandIndexFromCompilerInstruction(compilerInstruction, field, funcCallNode,
              funcParamNode.parameter().identifier);

      ctx.ln("if(instruction.getOperand(%d).isImm()) {", pseudoInstructionIndex)
          .spacedIn()
          .ln("%s = instruction.getOperand(%d).getImm();", field.identifier.simpleName(),
              pseudoInstructionIndex)
          .ln("%s = %sBaseInfo::%s(%s);",
              field.identifier.simpleName(),
              targetName.value(),
              relocationRef.valueRelocation().functionName().lower(),
              field.identifier.simpleName());

      // Then decode
      var record = Objects.requireNonNull(machineInstructionRecords.get(instrCallNode.target()));
      var operands = Stream.concat(record.info().outputs().stream(),
          record.info().inputs().stream()).toList();
      var operand = operands.get(addedOperand);
      ensure(operand instanceof ReferencesImmediateOperand,
          () -> Diagnostic.error("Expected that operand references immediate",
              funcParamNode.location()));
      var immediateOperand = ((ReferencesImmediateOperand) operand).immediateOperand();
      var decodingMethod = immediateOperand.rawDecoderMethod().lower();
      var parameters =
          Arrays.stream(Objects.requireNonNull(immediateDecodings.get(
                      Pair.of(immediateOperand.instructionRef(), immediateOperand.fieldAccessRef())))
                  .header().parameters())
              .map(x -> x.identifier.simpleName()).collect(Collectors.joining(", "));

      ctx.ln(String.format("%s.addOperand(MCOperand::createImm(%s(%s)));", instructionSymbol,
          decodingMethod,
          parameters));

      ctx.spaceOut()
          .ln("}")
          .ln("else {")
          .spacedIn();

      String expressionStr =
          String.format("MCOperandToMCExpr(instruction.getOperand(%d))", pseudoInstructionIndex);

      // Apply relocation / variant kind
      expressionStr = String.format("%sMCExpr::create(%s, %sMCExpr::VariantKind::%s, Ctx)",
          targetName.value(),
          expressionStr,
          targetName.value(),
          variant
      );

      // Apply decode
      var variants = variantKindStore.decodeVariantKindsByField(instrCallNode.target(), field);

      ensure(variants.size() == 1, () -> Diagnostic.error(
          "There are unexpectedly multiple variant kinds for the pseudo expansion available.",
          funcCallNode.location()));

      variant = ensurePresent(
          requireNonNull(variants).stream().filter(VariantKind::isImmediate).findFirst(),
          () -> Diagnostic.error(
              "Expected a variant for an immediate. But haven't " + "found any",
              funcCallNode.location())).value();

      // Decoding
      expressionStr = String.format("%sMCExpr::create(%s, %sMCExpr::VariantKind::%s, Ctx)",
          targetName.value(),
          expressionStr,
          targetName.value(),
          variant
      );

      ctx.ln(String.format("%s.addOperand(MCOperand::createExpr(%s)); // %s", instructionSymbol,
          expressionStr,
          field.identifier.simpleName()));

      ctx.spaceOut()
          .ln("}");
    } else if (funcCallNode.arguments().getFirst() instanceof LabelNode labelNode) {
       /*
      Here is the argument to `pcrel_lo` the `LabelNode`.

      pseudo instruction XXX ( rd: Index, symbol: Bits<32> ) =
      {
          new_label ( label )
          LD { rd = rd, rs1 = rd, imm = pcrel_lo( label ) }
      }
      */

      var newLabelNode =
          labelNode.usages().filter(x -> x instanceof NewLabelNode).findFirst().orElseThrow();
      var labelSymbolName =
          ensureNonNull(labelSymbolNameLookup.get(newLabelNode), "must not be null");

      var expressionStr = String.format("MCSymbolRefExpr::create(%s, Ctx)", labelSymbolName);

      // Apply relocation / variant kind
      expressionStr = String.format("%sMCExpr::create(%s, %sMCExpr::VariantKind::%s, Ctx)",
          targetName.value(),
          expressionStr,
          targetName.value(),
          variant
      );

      // Apply decode
      var variants = variantKindStore.decodeVariantKindsByField(instrCallNode.target(), field);

      ensure(variants.size() == 1, () -> Diagnostic.error(
          "There are unexpectedly multiple variant kinds for the pseudo expansion available.",
          funcCallNode.location()));

      variant = ensurePresent(
          requireNonNull(variants).stream().filter(VariantKind::isImmediate).findFirst(),
          () -> Diagnostic.error(
              "Expected a variant for an immediate. But haven't " + "found any",
              funcCallNode.location())).value();

      // Decoding
      expressionStr = String.format("%sMCExpr::create(%s, %sMCExpr::VariantKind::%s, Ctx)",
          targetName.value(),
          expressionStr,
          targetName.value(),
          variant
      );

      ctx.ln(String.format("%s.addOperand(MCOperand::createExpr(%s)); // %s", instructionSymbol,
          expressionStr,
          field.identifier.simpleName()));
    } else {
      throw Diagnostic.error("not supported", funcCallNode.location()).build();
    }

    addedOperand++;
  }

  @Override
  public void fieldUsedAsImmediateAndFieldAccessAssignmentAndExpressionConstant(
      CNodeWithBaggageContext ctx,
      InstrCallNode instrCallNode,
      Format.FieldAccess fieldAccess,
      ConstantNode cn) {
    var instructionSymbol = ctx.getString(INSTRUCTION_SYMBOL);
    ctx.ln(String.format("%s.addOperand(MCOperand::createExpr(%s));",
        instructionSymbol,
        fieldAccess.identifier.simpleName()));
    addedOperand++;
  }

  @Override
  public void fieldUsedAsImmediateAndFieldAccessAssignmentAndExpressionFuncParamNode(
      CNodeWithBaggageContext ctx, InstrCallNode instrCallNode,
      Format.FieldAccess fieldAccess, FuncParamNode funcParamNode) {
    var compilerInstruction = ctx.get(COMPILER_INSTRUCTION, CompilerInstruction.class);
    var instructionSymbol = ctx.getString(INSTRUCTION_SYMBOL);
    var pseudoInstructionIndex =
        getOperandIndexFromCompilerInstruction(compilerInstruction, fieldAccess, funcParamNode,
            funcParamNode.parameter().identifier);

    ctx.ln(String.format("%s.addOperand(instruction.getOperand(%d));",
        instructionSymbol,
        pseudoInstructionIndex));
    addedOperand++;
  }

  @Override
  public void fieldUsedAsImmediateAndFieldAccessAssignmentAndExpressionFuncCallNode(
      CNodeWithBaggageContext ctx, InstrCallNode instrCallNode,
      Format.FieldAccess fieldAccess, FuncCallNode funcCallNode) {
    // immS = lo(symbol)

    var compilerInstruction =
        ctx.get(COMPILER_INSTRUCTION, CompilerInstruction.class);
    var instructionSymbol = ctx.getString(INSTRUCTION_SYMBOL);

    ensure(funcCallNode.function() instanceof Relocation, () ->
        Diagnostic.error("Only supporting relocation functions at the moment",
            funcCallNode.location()));
    ensure(funcCallNode.function().parameters().length == 1, () ->
        Diagnostic.error("Only supporting functions with one argument at the moment",
            funcCallNode.location()));
    var relocation = (Relocation) funcCallNode.function();
    var relocationRef =
        ensurePresent(relocations.stream().filter(x -> x.relocation() == relocation).findFirst(),
            () -> Diagnostic.error("Cannot find relocation", relocation.location()));
    var elfRelocation = relocations.stream().filter(x -> x.relocation() == relocation).findFirst();
    ensure(elfRelocation.isPresent(), "elfRelocation must exist");
    var variant = elfRelocation.get().variantKind().value();

    if (funcCallNode.arguments().getFirst() instanceof FuncParamNode funcParamNode) {
      // There are two cases:
      // (1) the `symbol` in `imm = lo(symbol)` is actually an immediate
      // (2) the `symbol` in `imm = lo(symbol)` is an unknown address
      // The (1) case is easy because we cannot handle that.
      // For (2) case, we need to create an MCExpr with the variant kinds.
      // Since both types don't match, we assign for (2) the value `0`.

      var pseudoInstructionIndex =
          getOperandIndexFromCompilerInstruction(compilerInstruction, fieldAccess, funcCallNode,
              funcParamNode.parameter().identifier);

      ctx.ln("if(instruction.getOperand(%d).isImm()) {", pseudoInstructionIndex)
          .spacedIn();

      String appliedFunction = String.format("%sBaseInfo::%s(%s)",
          targetName.value(),
          relocationRef.valueRelocation().functionName().lower(),
          fieldAccess.identifier.simpleName()
      );

      ctx.ln(String.format("%s.addOperand(MCOperand::createImm(%s));", instructionSymbol,
              appliedFunction))
          .spaceOut()
          .ln("}")
          .ln("else {")
          .spacedIn();

      String expressionStr =
          String.format("MCOperandToMCExpr(instruction.getOperand(%d))", pseudoInstructionIndex);

      // Apply relocation / variant kind
      expressionStr = String.format("%sMCExpr::create(%s, %sMCExpr::VariantKind::%s, Ctx)",
          targetName.value(),
          expressionStr,
          targetName.value(),
          variant
      );

      ctx.ln(String.format("%s.addOperand(MCOperand::createExpr(%s)); // %s", instructionSymbol,
          expressionStr,
          fieldAccess.identifier.simpleName()));

      ctx.spaceOut()
          .ln("}");
    } else {
      throw Diagnostic.error("not supported", funcCallNode.location()).build();
    }
    addedOperand++;
  }

  @Override
  public IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages() {
    return fieldUsages;
  }
}