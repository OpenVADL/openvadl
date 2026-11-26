// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.lcb.codegen.assembly;


import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.error.Diagnostic;
import vadl.gcb.passes.operands.ReferencesFormatField;
import vadl.gcb.passes.operands.model.GcbDefaultInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionBareSymbolOperand;
import vadl.gcb.passes.operands.model.GcbInstructionImmediateOperand;
import vadl.gcb.passes.operands.model.GcbInstructionIndexedRegisterFileOperand;
import vadl.gcb.passes.operands.model.GcbInstructionRegisterFileOperand;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.lcb.passes.llvmLowering.CreateFunctionsFromImmediatesPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.ReferencesImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionLabelOperand;
import vadl.lcb.template.utils.ImmediateEncodingFunctionProvider;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.Definition;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.PrintableInstruction;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.BranchBeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.ForallEndNode;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.NewLabelNode;
import vadl.viam.graph.control.ProcEndNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StageEndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.GroupRef;
import vadl.viam.graph.dependency.LogicRef;
import vadl.viam.graph.dependency.OperationExistsNode;
import vadl.viam.graph.dependency.OperationForAllNode;
import vadl.viam.graph.dependency.OperationRef;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.StageEffectNode;
import vadl.viam.graph.dependency.TensorNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteSignalNode;
import vadl.viam.graph.dependency.WriteStageOutputNode;
import vadl.viam.passes.SnapshotInstructionBehaviorPass;

/**
 * Generates the code blocks to construct an assembly string. This handler creates the code path
 * where the operand is an immediate.
 */
@DispatchFor(
    value = Node.class,
    context = CNodeContext.class,
    include = {"vadl.viam", "vadl.lcb.graph"}
)
public class AssemblyInstructionPrinterImmediateHandler
    extends AbstractAssemblyInstructionPrinterHandler {

  private final PassResults passResults;

  /**
   * Constructor.
   */
  public AssemblyInstructionPrinterImmediateHandler(PassResults passResults,
                                                    PrintableInstruction instruction,
                                                    TableGenInstruction tableGenInstruction) {
    super(instruction, tableGenInstruction);
    this.passResults = passResults;
    this.ctx = new CNodeContext(
        builder::append,
        (ctx, node)
            -> AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(this, ctx, node));
  }

  @Handler
  @Override
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, ConstantNode node) {
    if (node.constant() instanceof Constant.Str str) {
      ctx.wr("std::string(\"" + str.value() + "\")");
    } else if (node.constant() instanceof Constant.Value val) {
      ctx.wr(val.intValue() + "");
    } else {
      throw Diagnostic.error(
          String.format("Not supported constant type: %s", node.constant().getClass()),
          node.location()).build();
    }
  }

  @Handler
  @Override
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, SelectNode node) {
    ctx.wr("(");
    AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(this, (CNodeContext) ctx,
        node.condition());
    ctx.wr(" ? ");
    AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(this, (CNodeContext) ctx,
        node.trueCase());
    ctx.wr(" : ");
    AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(this, (CNodeContext) ctx,
        node.falseCase());
    ctx.wr(")");
  }

  @Handler
  @Override
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, BuiltInCall node) {
    if (node.builtIn() == BuiltInTable.MNEMONIC) {
      ctx.wr("std::string(\"" + instruction.identifier().simpleName() + "\")");
    } else if (node.builtIn() == BuiltInTable.CONCATENATE_STRINGS) {
      for (int i = 0; i < node.arguments().size(); i++) {
        if (i != 0) {
          ctx.ln();
          ctx.wr("+ ");
        }

        AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(this,
            (CNodeContext) ctx, node.arguments().get(i));
      }
    } else if (node.builtIn() == BuiltInTable.REGISTER) {
      handleRegisterBuiltin(node, ctx);
    } else if (node.builtIn() == BuiltInTable.UDEC) {
      writeImmediateWithRadix(node, ctx, 10, false);
    } else if (node.builtIn() == BuiltInTable.SDEC) {
      writeImmediateWithRadix(node, ctx, 10, true);
    } else if (node.builtIn() == BuiltInTable.HEX) {
      writeImmediateWithRadix(node, ctx, 16, false);
    } else if (node.builtIn() == BuiltInTable.EQU) {
      handleConditional(node, ctx, "==");
    } else if (node.builtIn() == BuiltInTable.NEQ) {
      handleConditional(node, ctx, "!=");
    } else if (node.builtIn() == BuiltInTable.SGTH || node.builtIn() == BuiltInTable.UGTH) {
      handleConditional(node, ctx, ">");
    } else if (node.builtIn() == BuiltInTable.SGEQ || node.builtIn() == BuiltInTable.UGEQ) {
      handleConditional(node, ctx, ">=");
    } else if (node.builtIn() == BuiltInTable.SLTH || node.builtIn() == BuiltInTable.ULTH) {
      handleConditional(node, ctx, "<");
    } else if (node.builtIn() == BuiltInTable.SLEQ || node.builtIn() == BuiltInTable.ULEQ) {
      handleConditional(node, ctx, "<=");
    } else {
      super.handle(ctx, node);
    }
  }

  @Override
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, FuncParamNode node) {
    var indexInOperands = indexInInputsOrOutputs(node).orElseThrow(
        () -> Diagnostic.error("Cannot find operand", node.location()).build()
    );
    ctx.wr("MCOperandWrapper(MI->getOperand(%s))", indexInOperands);
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, FieldRefNode node) {
    // Since the operand value is in semantic view, if a field is used in a printing
    // function the semantic value needs to be encoded to derive the field value.
    // See: https://openvadl.github.io/openvadl/md_lcb.html#autotoc_md81
    if (instruction instanceof Instruction machineInstruction
        && fieldEncoding(machineInstruction, node.formatField()).isPresent()) {
      writeEncodedField(machineInstruction, node.formatField(), ctx);
      return;
    }

    var indexInOperands = ensurePresent(indexInInputsOrOutputs(node.formatField()),
        () -> Diagnostic.error("Cannot find operand: " + node.formatField().simpleName(),
            node.location()));
    ctx.wr("MCOperandWrapper(MI->getOperand(%s))", indexInOperands);
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, TensorNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }


  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, ReadArtificialResNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, ReadStageOutputNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, FoldNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, OperationForAllNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, OperationExistsNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, AsmBuiltInCall node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, StartNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, ReturnNode node) {
    ctx.wr("return ");
    AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(this, (CNodeContext) ctx,
        node.value());
    ctx.ln(";");
  }

  @Handler
  @Override
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, ForIdxNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, StageEffectNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, WriteStageOutputNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, InstrCallNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, ReadMemNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, FieldAccessRefNode node) {
    var indexInOperands = ensurePresent(indexInInputsOrOutputs(node.fieldAccess()), () ->
        Diagnostic.error("Immediate must be part of an tablegen input or output.",
            node.location())
    );

    ctx.wr("MCOperandWrapper(MI->getOperand(%s))", indexInOperands);
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, WriteRegTensorNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, WriteMemNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, WriteArtificialResNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, ReadRegTensorNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, ProcCallNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, ProcEndNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, ScheduledNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, MergeNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, IfNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, NewLabelNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, InstrEndNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, StageEndNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, BranchBeginNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, ForallEndNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, BranchEndNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, ForallNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, WriteSignalNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, ReadSignalNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, GroupRef node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, OperationRef node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, LogicRef node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  /**
   * Argument for the "register()" builtin is always an index into a register file.
   * Argument to the builtin can be just a Field / FieldAccess / FuncParam node
   * like "register(rd)" or an expression containing such a node like register(rd + 3).
   * It is required that the argument expression contains exactly one such node,
   * in order to be able to determine the concrete register file which should be indexed.
   */
  private void handleRegisterBuiltin(BuiltInCall node, CGenContext<Node> ctx) {
    var operandIndex = operandRefIndexInOperands(node);
    var operand = tableGenInstruction.getOperand(operandIndex);

    var argument = node.arguments().getFirst();
    if (operand instanceof GcbInstructionRegisterFileOperand
        || operand instanceof GcbInstructionIndexedRegisterFileOperand) {
      var regOperand = (GcbDefaultInstructionOperand) operand;
      ctx.wr("AsmUtils::getRegisterNameFrom%sByIndex(", regOperand.type());
      AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(this, (CNodeContext) ctx,
          argument);
      ctx.wr(")");
    } else {
      throw Diagnostic.error("Not supported argument for register builtin, argument expression "
          + "has to include exactly one register operand.", argument.location()).build();
    }
  }

  protected int operandRefIndexInOperands(BuiltInCall node) {
    var children = new ArrayList<Node>();
    node.collectInputsWithChildren(children);

    // Operands can be FieldRef / FieldAccessRef (machine instructions)
    // or FuncParam (pseudo instructions)
    var operandReferences = children.stream()
        .filter(child -> child instanceof FieldAccessRefNode
            || child instanceof FieldRefNode
            || child instanceof FuncParamNode)
        .toList();

    ViamError.ensure(operandReferences.size() == 1,
        () -> Diagnostic.error("Only one operand reference is allowed.", node.location()));

    var operandIndex = switch (operandReferences.getFirst()) {
      case FieldAccessRefNode fieldAccessNode ->
          indexInInputsOrOutputs(fieldAccessNode.fieldAccess());
      case FieldRefNode fieldRefNode -> indexInInputsOrOutputs(fieldRefNode.formatField());
      case FuncParamNode funcParamNode -> indexInInputsOrOutputs(funcParamNode);
      default -> throw Diagnostic.error(
          "Unsupported operand reference type: " + operandReferences.getFirst().nodeName(),
          operandReferences.getFirst().location()).build();
    };
    return ensurePresent(operandIndex, () ->
        Diagnostic.error("Immediate must be part of an tablegen input or output.",
            operandReferences.getFirst().location()));
  }

  private void handleConditional(BuiltInCall node, CGenContext<Node> ctx, String operation) {
    node.ensure(node.arguments().size() == 2, "Expected two arguments");

    AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(this, (CNodeContext) ctx,
        node.arguments().get(0));

    ctx.wr(" " + operation + " ");

    AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(this, (CNodeContext) ctx,
        node.arguments().get(1));
  }

  protected void writeImmediateWithRadix(BuiltInCall node, CGenContext<Node> ctx, int radix,
                                         boolean isSigned) {
    var argument = node.arguments().getFirst();
    ctx.wr("AsmUtils::formatImm(");
    AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(this, (CNodeContext) ctx,
        argument);
    if (isSigned && argument.type() instanceof DataType dataType && dataType.bitWidth() < 64) {
      ctx.wr(", %d, &MAI, %d)", radix, dataType.bitWidth());
    } else {
      ctx.wr(", %d, &MAI)", radix);
    }
  }

  private Optional<Format.FieldEncoding> fieldEncoding(Instruction machineInstruction,
                                                       Format.Field field) {
    return machineInstruction
        .fieldEncodingsByUsedFieldAccesses(getUnmodifiedOriginalGraph(machineInstruction))
        .stream().filter(x -> x.targetField().equals(field)).findFirst();
  }

  private void writeEncodedField(Instruction machineInstruction, Format.Field field,
                                 CGenContext<Node> ctx) {
    var uncheckedEncodingFunctions =
        ImmediateEncodingFunctionProvider.generateEncodeFunctions(passResults)
            .get(instruction);
    var encodingFunctions = ensureNonNull(uncheckedEncodingFunctions,
        () -> Diagnostic.error("Cannot find encoding functions for the instruction.",
            machineInstruction.location()));

    var encodingFunction = ensurePresent(
        encodingFunctions.stream().filter(x -> x.field().equals(field)).findFirst(),
        () -> Diagnostic.error("Cannot find encoding function for field",
            machineInstruction.location().join(field.location()))
    );

    var argumentsForEncodingFunction =
        CreateFunctionsFromImmediatesPass.createParametersForEncodingFunctionFromInputOperands(
                tableGenInstruction)
            .stream()
            .map(x -> {
              var index = indexInInputsOrOutputs(
                  x.operand().immediateOperand().fieldAccessRef()).get();
              return String.format("MCOperandWrapper(MI->getOperand(%s))", index);
            })
            .collect(Collectors.joining(", "));

    ctx.wr("%s(%s)", encodingFunction.header().functionName().lower(),
        argumentsForEncodingFunction);
  }

  /**
   * Return the unmodified version of the instruction's behavior from the
   * {@link SnapshotInstructionBehaviorPass}.
   */
  private Graph getUnmodifiedOriginalGraph(Instruction machineInstruction) {
    var graphs =
        (Map<Instruction, Graph>) passResults.lastResultOf(SnapshotInstructionBehaviorPass.class);

    return ensureNonNull(graphs.get(machineInstruction),
        () -> Diagnostic.error("Cannot find unmodified instruction's behavior.",
            machineInstruction.location()));
  }

  protected Optional<Integer> indexInInputsOrOutputs(Format.Field needle) {
    var operands = Stream.concat(tableGenInstruction.getOutOperands().stream(),
        tableGenInstruction.getInOperands().stream()).toList();

    for (int i = 0; i < operands.size(); i++) {
      var operand = operands.get(i);
      if (operand instanceof ReferencesFormatField x
          && x.referencesField(needle)) {
        return Optional.of(i);
      } else if (operand instanceof GcbInstructionImmediateOperand immediateOperand
          && immediateOperand.fieldAccess().fieldRefs().contains(needle)) {
        return Optional.of(i);
      }
    }

    return Optional.empty();
  }

  protected Optional<Integer> indexInInputsOrOutputs(Format.FieldAccess needle) {
    var operands = Stream.concat(tableGenInstruction.getOutOperands().stream(),
        tableGenInstruction.getInOperands().stream()).toList();

    for (int i = 0; i < operands.size(); i++) {
      var operand = operands.get(i);
      if (operand instanceof ReferencesImmediateOperand x
          && x.immediateOperand().fieldAccessRef().equals(needle)) {
        return Optional.of(i);
      } else if (operand instanceof GcbInstructionImmediateOperand immediateOperand
          && immediateOperand.fieldAccess().equals(needle)) {
        return Optional.of(i);
      }
    }

    return Optional.empty();
  }

  protected Optional<Integer> indexInInputsOrOutputs(FuncParamNode needle) {
    var operands = Stream.concat(tableGenInstruction.getOutOperands().stream(),
        tableGenInstruction.getInOperands().stream()).toList();

    var paramName = needle.parameter().simpleName();
    for (int i = 0; i < operands.size(); i++) {
      var operand = operands.get(i);
      if (operand instanceof GcbInstructionBareSymbolOperand bareSymbolOperand
          && bareSymbolOperand.origin() instanceof FuncParamNode funcParamNode
          && needle.parameter().equals(funcParamNode.parameter())) {
        return Optional.of(i);
      } else if (operand instanceof TableGenInstructionLabelOperand) {
        return Optional.of(i);
      } else if (operand instanceof ReferencesFormatField x) {
        var names =
            x.formatFields().stream().map(Definition::simpleName).collect(Collectors.toSet());
        if (names.contains(paramName)) {
          return Optional.of(i);
        }
      } else if (operand instanceof GcbDefaultInstructionOperand defaultOperand
          && defaultOperand.name().equals(paramName)) {
        return Optional.of(i);
      }
    }

    return Optional.empty();
  }

  public CNodeContext ctx() {
    return ctx;
  }

  public StringBuilder builder() {
    return builder;
  }
}
