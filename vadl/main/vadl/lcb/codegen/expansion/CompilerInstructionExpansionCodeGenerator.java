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
import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import vadl.cppCodeGen.FunctionCodeGenerator;
import vadl.cppCodeGen.SymbolTable;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.context.CNodeWithBaggageContext;
import vadl.cppCodeGen.model.GcbCppAccessFunction;
import vadl.cppCodeGen.model.GcbCppFunctionWithBody;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.relocation.model.HasRelocationComputationAndUpdate;
import vadl.gcb.valuetypes.TargetName;
import vadl.gcb.valuetypes.VariantKind;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.utils.Pair;
import vadl.viam.CompilerInstruction;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.PrintableInstruction;
import vadl.viam.PseudoInstruction;
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
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.passes.CfgTraverser;

/**
 * A {@link PseudoInstruction} contains one or multiple {@link Instruction}. This generator
 * creates the CPP code which creates the code to expand the pseudo instruction.
 */
public class CompilerInstructionExpansionCodeGenerator extends FunctionCodeGenerator {
  private static final String FIELD = "field";
  private static final String INSTRUCTION_CALL_NODE = "instructionCallNode";
  private static final String INSTRUCTION = "instruction";
  private static final String INSTRUCTION_SYMBOL = "instructionSymbol";

  private final TargetName targetName;
  private final IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages;
  private final Map<FieldInInstruction, GcbCppFunctionWithBody> immediateDecodingsByField;
  private final List<HasRelocationComputationAndUpdate> relocations;
  private final CompilerInstruction compilerInstruction;
  private final SymbolTable symbolTable;
  private final GenerateLinkerComponentsPass.VariantKindStore variantKindStore;
  private final IdentityHashMap<Instruction, LlvmLoweringRecord.Machine> machineInstructionRecords;
  private final IdentityHashMap<NewLabelNode, String> labelSymbolNameLookup;

  record FieldInInstruction(PrintableInstruction instruction, Format.Field field) {

  }

  /**
   * Constructor.
   */
  public CompilerInstructionExpansionCodeGenerator(
      TargetName targetName,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      Map<TableGenImmediateRecord, GcbCppAccessFunction> immediateDecodings,
      List<HasRelocationComputationAndUpdate> relocations,
      GenerateLinkerComponentsPass.VariantKindStore variantKindStore,
      CompilerInstruction compilerInstruction,
      Function function,
      IdentityHashMap<Instruction, LlvmLoweringRecord.Machine> machineInstructionRecords) {
    super(function);
    this.targetName = targetName;
    this.fieldUsages = fieldUsages;
    this.relocations = relocations;
    this.compilerInstruction = compilerInstruction;
    this.symbolTable = new SymbolTable();
    this.variantKindStore = variantKindStore;
    this.machineInstructionRecords = machineInstructionRecords;
    this.labelSymbolNameLookup = new IdentityHashMap<>();
    this.immediateDecodingsByField = immediateDecodings
        .entrySet()
        .stream().map(x -> new Pair<>(
            new FieldInInstruction(x.getKey().instructionRef(),
                x.getKey().fieldAccessRef().fieldRef()),
            x.getValue()))
        .collect(Collectors.toMap(Pair::left, Pair::right));
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
  public void handle(CGenContext<Node> ctx, FuncParamNode toHandle) {
    var field = ((CNodeWithBaggageContext) ctx).get(FIELD, Format.Field.class);
    var instructionSymbol = ((CNodeWithBaggageContext) ctx).getString(INSTRUCTION_SYMBOL);
    var instruction = ((CNodeWithBaggageContext) ctx).get(INSTRUCTION, Instruction.class);
    var instructionCallNode =
        ((CNodeWithBaggageContext) ctx).get(INSTRUCTION_CALL_NODE, InstrCallNode.class);

    var pseudoInstructionIndex =
        getOperandIndexFromCompilerInstruction(field, toHandle, toHandle.parameter().identifier);

    var usage = fieldUsages.getFieldUsages(instruction).get(field);
    ensure(usage != null, "usage must not be null");
    ensure(usage.size() == 1, () -> {
      throw Diagnostic.error(
          "Cannot expand pseudo instruction because the usage of the field is unclear",
          field.location()).build();
    });

    switch (usage.get(0)) {
      case IMMEDIATE -> {
        var argumentSymbol = symbolTable.getNextVariable();
        ctx.ln("const MCExpr* %s = MCOperandToMCExpr(instruction.getOperand(%d));", argumentSymbol,
            pseudoInstructionIndex);

        var argumentImmSymbol = symbolTable.getNextVariable();

        String variant = "VK_None";

        if (!instructionCallNode.isParameterFieldAccess(field)) {
          var variants = variantKindStore.decodeVariantKindsByField(instruction, field);

          ensure(variants.size() == 1, () -> Diagnostic.error(
              "There are unexpectedly multiple variant kinds for the pseudo expansion available.",
              toHandle.location()));

          variant = ensurePresent(
              requireNonNull(variants).stream().filter(VariantKind::isImmediate).findFirst(),
              () -> Diagnostic.error(
                  "Expected a variant for an immediate. But haven't " + "found any",
                  toHandle.location())).value();
        }

        ctx.ln(
            "MCOperand %s = MCOperand::createExpr(%sMCExpr::create(%s, "
                + "%sMCExpr::VariantKind::%s, " + "Ctx));", argumentImmSymbol, targetName.value(),
            argumentSymbol, targetName.value(),
            requireNonNull(variant));
        ctx.ln(String.format("%s.addOperand(%s);", instructionSymbol, argumentImmSymbol));
      }
      case REGISTER -> ctx.ln("%s.addOperand(instruction.getOperand(%d));", instructionSymbol,
          pseudoInstructionIndex);
      default -> throw Diagnostic.error("Cannot detect the usage of this field",
          instructionCallNode.location().join(field.location())).build();
    }
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
  public void handle(CGenContext<Node> ctx, ConstantNode toHandle) {
    var field = ((CNodeWithBaggageContext) ctx).get(FIELD, Format.Field.class);
    var instruction = ((CNodeWithBaggageContext) ctx).get(INSTRUCTION, Instruction.class);
    var instructionSymbol = ((CNodeWithBaggageContext) ctx).getString(INSTRUCTION_SYMBOL);
    var instructionCallNode =
        ((CNodeWithBaggageContext) ctx).get(INSTRUCTION_CALL_NODE, InstrCallNode.class);

    var usage = fieldUsages.getFieldUsages(instruction).get(field);
    ensure(usage != null, "usage must not be null");

    ensure(usage.size() == 1, () -> {
      throw Diagnostic.error(
          "Cannot expand pseudo instruction because the usage of the field is unclear",
          field.location()).build();
    });

    switch (usage.getFirst()) {
      case IMMEDIATE -> {
        var immediateValueString = String.valueOf(toHandle.constant().asVal().intValue());

        if (!instructionCallNode.isParameterFieldAccess(field)) {
          var decodingFunction =
              immediateDecodingsByField.get(new FieldInInstruction(instruction, field));
          ensure(decodingFunction != null, "decodingFunction must not be null");
          var decodingFunctionName = decodingFunction.header().functionName().lower();
          immediateValueString = decodingFunctionName + "(" + immediateValueString + ")";
        }

        context.ln("%s.addOperand(MCOperand::createImm(%s));", instructionSymbol,
            immediateValueString);
      }
      case REGISTER -> {
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

        var registerFile =
            ensurePresent(registerFiles.stream().findFirst(), "Expected one register file");

        context.ln("%s.addOperand(MCOperand::createReg(%s::%s%s));", instructionSymbol,
            targetName.value(), registerFile.identifier.simpleName(),
            toHandle.constant().asVal().intValue());
      }
      default -> throw Diagnostic.error("Cannot generate cpp code for this argument",
          field.location()).build();
    }
  }

  @Override
  public void handle(CGenContext<Node> ctx, FuncCallNode toHandle) {
    var field = ((CNodeWithBaggageContext) ctx).get(FIELD, Format.Field.class);
    var relocation = (Relocation) toHandle.function();


    if (toHandle.arguments().get(0) instanceof FuncParamNode funcParamNode) {
      /*
        Here is the argument to `pcrel_lo` the `FuncParamNode`.

        pseudo instruction XXX ( rd: Index, symbol: Bits<32> ) =
        {
            LD { rd = rd, rs1 = rd, imm = pcrel_lo( symbol ) }
        }
      */

      var parameterName = funcParamNode.parameter().identifier;
      var pseudoInstructionIndex =
          getOperandIndexFromCompilerInstruction(field, toHandle, parameterName);

      var argumentSymbol = symbolTable.getNextVariable();

      ctx.ln("const MCExpr* %s = MCOperandToMCExpr(instruction.getOperand(%d));", argumentSymbol,
          pseudoInstructionIndex);

      handleRelocationOperand(ctx, argumentSymbol, relocation);
    } else if (toHandle.arguments().get(0) instanceof LabelNode labelNode) {
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

      var argumentSymbol = symbolTable.getNextVariable();
      ctx.ln(
          "const MCExpr* %s = MCOperandToMCExpr(MCOperand::createExpr("
              + "MCSymbolRefExpr::create(%s, Ctx)));",
          argumentSymbol, labelSymbolName);

      handleRelocationOperand(ctx, argumentSymbol, relocation);
    } else {
      throw Diagnostic.error("Cannot handle node", toHandle.location()).build();
    }
  }

  private void handleRelocationOperand(CGenContext<Node> ctx, String argumentSymbol,
                                       Relocation relocation) {
    final var instruction = ((CNodeWithBaggageContext) ctx).get(INSTRUCTION, Instruction.class);
    final var instructionSymbol = ((CNodeWithBaggageContext) ctx).getString(INSTRUCTION_SYMBOL);
    final var field = ((CNodeWithBaggageContext) ctx).get(FIELD, Format.Field.class);
    final var instructionCallNode =
        ((CNodeWithBaggageContext) ctx).get(INSTRUCTION_CALL_NODE, InstrCallNode.class);

    var argumentRelocationSymbol = symbolTable.getNextVariable();
    var elfRelocation = relocations.stream().filter(x -> x.relocation() == relocation).findFirst();
    ensure(elfRelocation.isPresent(), "elfRelocation must exist");
    var variant = elfRelocation.get().variantKind().value();

    ctx.ln("const MCExpr* %s = %sMCExpr::create(%s, %sMCExpr::VariantKind::%s, Ctx);",
        argumentRelocationSymbol, targetName.value(), argumentSymbol,
        targetName.value(), variant);

    String mcExprSymbol = argumentRelocationSymbol;

    // build a nested MCExpr with the decode variant
    // to call the decoding function
    if (!instructionCallNode.isParameterFieldAccess(field)) {
      var decodeVariants = variantKindStore.decodeVariantKindsByField(instruction, field);
      ensure(decodeVariants.size() == 1, () -> Diagnostic.error(
          "There are unexpectedly multiple variant kinds for the pseudo expansion available.",
          field.location()));

      var decodeVariant = ensurePresent(
          requireNonNull(decodeVariants).stream().filter(VariantKind::isImmediate).findFirst(),
          () -> Diagnostic.error(
              "Expected a variant for an immediate. But haven't " + "found any",
              field.location())).value();

      mcExprSymbol = symbolTable.getNextVariable();

      ctx.ln(
          "const MCExpr* %s = %sMCExpr::create(%s, %sMCExpr::VariantKind::%s, Ctx);",
          mcExprSymbol, targetName.value(), argumentRelocationSymbol,
          targetName.value(), decodeVariant);
    }

    var operandSymbol = symbolTable.getNextVariable();
    ctx.ln("MCOperand %s = MCOperand::createExpr(%s);", operandSymbol, mcExprSymbol);
    ctx.ln(String.format("%s.addOperand(%s);", instructionSymbol, operandSymbol));
  }

  /**
   * Trying to get the index from {@code pseudoInstruction.parameters} based on the given
   * {@code parameter}. This index is important because it is the index of the pseudo instruction's
   * operands which will be used for the pseudo instruction's expansion.
   * For example, you have pseudo instruction {@code X(arg1, arg2) } which has two arguments. You
   * have to know that {@code arg2} has index {@code 1} to correctly map it to a machine
   * instruction later.
   */
  private int getOperandIndexFromCompilerInstruction(Format.Field field, ExpressionNode argument,
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

  @Override
  public String genFunctionDefinition() {
    context.ln(genFunctionSignature()).ln("{").spacedIn().ln("std::vector< MCInst > result;");

    var cfgTraversal = new CfgTraverser() {
      @Override
      public ControlNode onDirectional(DirectionalNode dir) {
        if (dir instanceof InstrCallNode instrCallNode) {
          var sym = symbolTable.getNextVariable();
          context.ln("MCInst %s = MCInst();", sym)
              .ln("%s.setOpcode(%s::%s);", sym, targetName.value(),
                  instrCallNode.target().identifier.simpleName());
          writeInstructionCall(context, instrCallNode, sym);
          context.ln("result.push_back(%s);", sym);
          context.ln("callback(%s);", sym);
        } else if (dir instanceof NewLabelNode newLabelNode) {
          var sym = symbolTable.getNextVariable();
          context.ln("MCSymbol *%s = Ctx.createTempSymbol();", sym);
          context.ln("callbackSymbol(%s);", sym);
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

  private void writeInstructionCall(CNodeContext context, InstrCallNode instrCallNode,
                                    String instructionSymbol) {
    var pairs =
        Streams.zip(instrCallNode.getParamFields().stream(), instrCallNode.arguments().stream(),
            Pair::of).toList();

    var reorderedPairs = reorderParameters(instrCallNode.target(), pairs);

    reorderedPairs.forEach(pair -> {
      /*
        pseudo instruction CALL( symbol : Bits<32> ) =
         {
              LUI{ rd = 1 as Bits5, imm = hi20( symbol ) }
              ...
         }

         E.g. rd is the field and 1 is the argument.
      */

      var field = pair.left();
      var argument = pair.right();

      var newContext = new CNodeWithBaggageContext(context).put(FIELD, field)
          .put(INSTRUCTION_CALL_NODE, instrCallNode).put(INSTRUCTION, instrCallNode.target())
          .put(INSTRUCTION_SYMBOL, instructionSymbol);

      if (argument instanceof ConstantNode cn) {
        handle(newContext, cn);
      } else if (argument instanceof FuncCallNode fn) {
        ensure(fn.function() instanceof Relocation,
            () -> Diagnostic.error("Function must be a relocation", fn.location()));
        handle(newContext, fn);
      } else if (argument instanceof FuncParamNode fn) {
        handle(newContext, fn);
      } else if (argument instanceof ZeroExtendNode zn) {
        handle(newContext, zn);
      } else {
        throw Diagnostic.error("Not implemented for this node type.", argument.location())
            .build();
      }
    });
  }

  /**
   * The order of the parameters is not necessarily the order in which the expansion should happen.
   * This function looks at the {@link LlvmLoweringRecord} of the corresponding instruction
   * and reorders the list according to the order of outputs and inputs.
   */
  private List<Pair<Format.Field, ExpressionNode>> reorderParameters(
      Instruction instruction,
      List<Pair<Format.Field, ExpressionNode>> pairs) {
    var result = new ArrayList<Pair<Format.Field, ExpressionNode>>();
    var lookup = pairs.stream().collect(Collectors.toMap(Pair::left, Pair::right));

    var llvmRecord = ensureNonNull(machineInstructionRecords.get(instruction),
        () -> Diagnostic.error("Cannot find llvmRecord for instruction used in pseudo instruction",
            instruction.location()));

    var order = llvmRecord.info().outputInputOperandsFormatFields();

    for (var item : order) {
      var l = ensureNonNull(lookup.get(item),
          () -> Diagnostic.error("Cannot find format's field in pseudo instruction",
              item.location()));
      result.add(Pair.of(item, l));
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
