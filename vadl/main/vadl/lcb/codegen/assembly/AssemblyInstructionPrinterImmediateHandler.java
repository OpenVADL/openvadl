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
import vadl.gcb.passes.IdentifyFieldUsagePass;
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
import vadl.utils.SourceLocation;
import vadl.viam.Constant;
import vadl.viam.Definition;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.PrintableInstruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.HasRegisterTensor;
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
import vadl.viam.graph.dependency.OperationExistsNode;
import vadl.viam.graph.dependency.OperationForAllNode;
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
  private final IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages;

  /**
   * Constructor.
   */
  public AssemblyInstructionPrinterImmediateHandler(PassResults passResults,
                                                    PrintableInstruction instruction,
                                                    TableGenInstruction tableGenInstruction) {
    super(instruction, tableGenInstruction);
    this.passResults = passResults;
    this.fieldUsages =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
            IdentifyFieldUsagePass.class);
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
      ctx.ln("std::string(\"" + str.value() + "\")");
    } else if (node.constant() instanceof Constant.Value val) {
      ctx.ln(val.intValue() + "");
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
      ctx.ln("std::string(\"" + instruction.identifier().simpleName() + "\")");
    } else if (node.builtIn() == BuiltInTable.CONCATENATE_STRINGS) {
      for (int i = 0; i < node.arguments().size(); i++) {
        if (i != 0) {
          ctx.wr(" + ");
        }

        AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(this,
            (CNodeContext) ctx, node.arguments().get(i));
      }
    } else if (node.builtIn() == BuiltInTable.REGISTER) {
      var argument = node.arguments().getFirst();
      if (argument instanceof FieldRefNode fieldRefNode) {
        ctx.wr(printRegister(fieldRefNode));
      } else if (argument instanceof FuncParamNode funcParamNode) {
        ctx.wr(printRegister(funcParamNode));
      } else {
        throw Diagnostic.error("not supported argument for register builtin",
                argument.location())
            .build();
      }
    } else if (node.builtIn() == BuiltInTable.UDEC) {
      writeImmediateWithRadix(node, ctx, 10, false);
    } else if (node.builtIn() == BuiltInTable.SDEC) {
      writeImmediateWithRadix(node, ctx, 10, true);
    } else if (node.builtIn() == BuiltInTable.HEX) {
      writeImmediateWithRadix(node, ctx, 16, false);
    } else if (node.builtIn() == BuiltInTable.INTEGRAL) {
      ctx.wr("AsmUtils::unwrapToIntegral(");
      AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(this, (CNodeContext) ctx,
          node.arg(0));
      ctx.wr(")");
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
    var index = indexInInputsOrOutputs(node).orElseThrow(
        () -> Diagnostic.error("Cannot find operand", node.location()).build()
    );
    var operands = Stream.concat(tableGenInstruction.getOutOperands().stream(),
        tableGenInstruction.getInOperands().stream()).toList();
    var operand = operands.get(index);

    if (operand instanceof GcbInstructionRegisterFileOperand
        || operand instanceof GcbInstructionIndexedRegisterFileOperand) {
      ctx.wr("MI->getOperand(" + index + ").getReg()");
    } else {
      ctx.wr("MI->getOperand(" + index + ").getImm()");
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, FieldRefNode node) {
    if (instruction instanceof Instruction machineInstruction) {
      var usages = fieldUsages.getFieldUsages(machineInstruction, node.formatField());

      if (usages.isEmpty()) {
        throw Diagnostic.error(
            "The generator does not know whether this is an immediate or register access.",
            node.location()).build();
      } else if (usages.size() > 1) {
        throw Diagnostic.error(
            "The generator registered multiple register usages for this field.",
            node.location()).build();
      }

      var usage = usages.getFirst();
      var indexInOperands = ensurePresent(indexInInputsOrOutputs(node.formatField()),
          () -> Diagnostic.error("Cannot find operand: " + node.formatField().simpleName(),
              node.location()));
      if (usage == IdentifyFieldUsagePass.FieldUsage.IMMEDIATE) {
        ctx.wr("MI->getOperand(%s).getImm()", indexInOperands);
      } else if (usage == IdentifyFieldUsagePass.FieldUsage.REGISTER) {
        ctx.wr("MI->getOperand(%s).getReg()", indexInOperands);
      }
    } else {
      throw Diagnostic.error("not supported", node.location()).build();
    }
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

    ctx.wr("MI->getOperand(%s).getImm()", indexInOperands);
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

  private void handleConditional(BuiltInCall node, CGenContext<Node> ctx, String operation) {
    node.ensure(node.arguments().size() == 2, "Expected two arguments");

    AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(this, (CNodeContext) ctx,
        node.arguments().get(0));

    ctx.wr(" " + operation + " ");

    AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(this, (CNodeContext) ctx,
        node.arguments().get(1));
  }

  protected void handleArbitraryExpression(BuiltInCall node, int radix, CGenContext<Node> ctx) {
    ctx.wr("AsmUtils::formatImm(");
    AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(this,
        (CNodeContext) ctx, node.arguments().getFirst());
    ctx.wr(", %d, &MAI)", radix);
  }

  private void writeImmediateWithRadix(BuiltInCall node, CGenContext<Node> ctx, int radix,
                                       boolean isSigned) {
    if (node.arguments().getFirst() instanceof BuiltInCall bc
        && bc.builtIn() == BuiltInTable.INTEGRAL) {
      ctx.wr("AsmUtils::unwrapToIntegralStr(");
      writeImmediateWithRadix(bc, ctx, radix, isSigned);
      ctx.wr(")");
    } else if (node.arguments().getFirst() instanceof FieldRefNode fieldRefNode) {
      writeImmediateWithRadix(fieldRefNode.formatField(),
          ctx,
          radix,
          fieldRefNode.location(),
          isSigned);
    } else if (node.arguments().getFirst() instanceof FieldAccessRefNode fieldAccessRefNode) {
      writeImmediateWithRadix(fieldAccessRefNode.fieldAccess(),
          ctx,
          radix,
          fieldAccessRefNode.location(),
          isSigned);
    } else if (node.arguments().getFirst() instanceof FuncParamNode funcParamNode) {
      // This case is for pseudo instructions because they arguments are not fields,
      // but function parameter nodes.
      writeImmediateWithRadix(funcParamNode, ctx, radix, funcParamNode.location(), isSigned);
    } else if (node.arguments().getFirst() instanceof ConstantNode constantNode) {
      writeImmediateWithRadix(constantNode, ctx, radix);
    } else {
      handleArbitraryExpression(node, radix, ctx);
    }
  }

  private void writeImmediateWithRadix(ConstantNode constantNode,
                                       CGenContext<Node> ctx,
                                       int radix) {
    ctx.wr("AsmUtils::formatImm(%d, %d, &MAI)", constantNode.constant().asVal().intValue(), radix);
  }

  protected void writeImmediateWithRadix(Format.Field field,
                                         CGenContext<Node> ctx,
                                         int radix,
                                         SourceLocation sourceLocation,
                                         boolean isSigned) {
    // If this instruction is a machine instruction then ...
    if (instruction instanceof Instruction machineInstruction) {
      var originalGraph = getUnmodifiedOriginalGraph(machineInstruction);
      var fieldEncodings = machineInstruction.fieldEncodingsByUsedFieldAccesses(originalGraph);
      var fieldEncoding =
          fieldEncodings.stream().filter(x -> x.targetField().equals(field)).findFirst();

      // If this field has an encoding, we are applying it.
      if (fieldEncoding.isPresent()) {
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
                  var operand = tableGenInstruction.getOperand(index);
                  var isUsedAsImmediate = operand instanceof GcbInstructionImmediateOperand;
                  // You can also use a register as immediate, but then you need to call ".getReg".
                  if (isUsedAsImmediate) {
                    return String.format("MI->getOperand(%s).getImm()", index);
                  } else {
                    return String.format("MI->getOperand(%s).getReg()", index);
                  }
                })
                .collect(Collectors.joining(", "));

        if (isSigned) {
          // The sign extension function needs to know which bit it should consider.
          int width = fieldEncoding.get().targetField().size();
          ctx.wr("AsmUtils::formatImm(VADL_sextract(%s(%s), %d), %d, &MAI)",
              encodingFunction.header().functionName().lower(),
              argumentsForEncodingFunction,
              width,
              radix);
        } else {
          ctx.wr("AsmUtils::formatImm(%s(%s), %d, &MAI)",
              encodingFunction.header().functionName().lower(),
              argumentsForEncodingFunction,
              radix);
        }
      } else {
        var indexInOperands = ensurePresent(indexInInputsOrOutputs(field), () ->
            Diagnostic.error("Immediate must be part of an tablegen input or output.",
                sourceLocation)
        );

        // Otherwise print raw field value.
        writeFieldWithRawImmediateWithRadix(indexInOperands, radix);
      }
    } else {
      var indexInOperands = ensurePresent(indexInInputsOrOutputs(field), () ->
          Diagnostic.error("Immediate must be part of an tablegen input or output.",
              sourceLocation)
      );

      // Otherwise print raw field value.
      writeFieldWithRawImmediateWithRadix(indexInOperands, radix);
    }
  }

  protected void writeImmediateWithRadix(Format.FieldAccess fieldAccess,
                                         CGenContext<Node> ctx,
                                         int radix,
                                         SourceLocation sourceLocation,
                                         boolean isSigned) {
    var indexInOperands = ensurePresent(indexInInputsOrOutputs(fieldAccess), () ->
        Diagnostic.error("Immediate must be part of an tablegen input or output.",
            sourceLocation)
    );

    formatImm(ctx, radix, indexInOperands, isSigned);
  }

  protected void writeImmediateWithRadix(FuncParamNode node,
                                         CGenContext<Node> ctx,
                                         int radix,
                                         SourceLocation sourceLocation,
                                         boolean isSigned) {
    var indexInOperands = ensurePresent(indexInInputsOrOutputs(node), () ->
        Diagnostic.error("Immediate must be part of an tablegen input or output.",
            sourceLocation)
    );

    formatImm(ctx, radix, indexInOperands, isSigned);
  }

  private void writeFieldWithRawImmediateWithRadix(int indexInOperands,
                                                   int radix) {
    // Operand must not be necessarily an immediate, but can also be a register.
    if (tableGenInstruction.getOperand(indexInOperands) instanceof GcbInstructionImmediateOperand) {
      ctx.wr("AsmUtils::formatImm(MCOperandWrapper(MI->getOperand(%s)), %d, &MAI)",
          indexInOperands, radix);
    } else {
      ctx.wr("std::to_string(MI->getOperand(%s).getReg())", indexInOperands);
    }
  }

  private void formatImm(CGenContext<Node> ctx,
                         int radix,
                         Integer indexInOperands,
                         boolean isSigned) {
    if (isSigned) {
      // default is always signed
      ctx.wr("AsmUtils::formatImm(MI->getOperand(%s).getImm(), %d, &MAI)",
          indexInOperands, radix);
    } else {
      ctx.wr("AsmUtils::formatImm(MCOperandWrapper(MI->getOperand(%s)), %d, &MAI)",
          indexInOperands, radix);
    }
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

  /**
   * The assembly string contains a {@code register(rd) }, but the given {@code fieldRefNode}
   * comes from a {@link Format} and is therefore a {@link FieldRefNode}.
   */
  private String printRegister(FieldRefNode fieldRefNode) {
    var index = indexInInputsOrOutputs(fieldRefNode.formatField())
        .orElseThrow(() -> Diagnostic.error(
            "Field is not part of an input or output operand in tablegen",
            fieldRefNode.location()).build());

    // We need this helper function "getRegisterName..." because
    // we might need to support multiple register files.
    var registerFileName = getRegisterFile(instruction.behavior(), fieldRefNode);
    return
        String.format(
            "getRegisterNameFrom%sByIndex("
                + "MCOperandWrapper(MI->getOperand(%d)).unwrapToIntegral())" + " // "
                + fieldRefNode.formatField().identifier.simpleName() + "\n",
            registerFileName, index);
  }

  /**
   * The assembly string contains a {@code register(rd) }, but it is not part of a {@link Format}
   * because it might come from a {@link PseudoInstruction}.
   * For example the {@code MV} pseudo instruction in RISCV.
   *
   * <pre>
   *   pseudo instruction MV( rd : Index, rs1 : Index ) =
   *   {
   *       ADDI{ rd = rd, rs1 = rs1, imm = 0 as Bits<12> }
   *   }
   *   assembly MV = (mnemonic, " ", register( rd ), ",", register( rs1 ))
   * </pre>
   * Note that the emitted {@code rd} is given as a pseudo instruction operand and does not belong
   * to any {@link Format} and instruction behavior. Therefore, we must use the {@code ADDI}
   * register usages to determine what register file the "field" {@code rd} belongs to. When
   * multiple machine instructions with multiple register files exist, then we cannot determine it
   * automatically.
   */
  private String printRegister(FuncParamNode funcParamNode) {
    funcParamNode.ensure(instruction.behavior().isPseudoInstruction(),
        "instruction must be pseudo instruction");
    var instrCallNodes = instruction.behavior().getNodes(InstrCallNode.class).toList();

    /*
     * Stores all the uses of a register operand of an instruction to determine later
     * what the pseudo instruction needs as a register file.
     */
    record RegisterFileCandidates(Instruction instruction, Format.Field field) {

    }

    var candidates = new ArrayList<RegisterFileCandidates>();

    // First get all fields which use this funcParamNode
    for (var instrCallNode : instrCallNodes) {
      for (var pair : instrCallNode.getZippedArgumentsWithParameters().toList()) {
        // ADDI{ rd = rd, rs1 = rs1, imm = 0 as Bits<12> }
        //            ^___ argument
        //       ^___ param
        var parameter = pair.left();
        var argument = pair.right();

        if (parameter.isLeft() && argument instanceof FuncParamNode funcParam
            && funcParam.equals(funcParamNode)) {
          // is a field and not a field access function.
          // and it is exactly a node which we need to check
          candidates.add(new RegisterFileCandidates(instrCallNode.target(), parameter.left()));
        }
      }
    }

    // Go over all the candidates and check all the usages of the fields.
    // When the usage references a register file then we collect it.
    var registerFiles = candidates
        .stream()
        .flatMap(candidate ->
            candidate.instruction().behavior().getNodes(FieldRefNode.class)
                .filter(x -> x.formatField().equals(candidate.field))
                .flatMap(x -> x.usages()
                    .filter(y -> y instanceof HasRegisterTensor z && z.hasRegisterFile()))
                .map(x -> ((HasRegisterTensor) x).registerTensor()))
        .collect(Collectors.toSet());

    // We have set of register files derived by the machine instructions.
    // When we have only one element then it is clear what the pseudo instruction has to print
    // for a register file.
    // When we have multiple then we cannot say automatically what to choose.
    if (registerFiles.isEmpty()) {
      throw Diagnostic.error(
          "No machine instruction uses this parameter. It is not known which register "
              + "file to use.",
          funcParamNode.location()).build();
    } else if (registerFiles.size() > 1) {
      throw Diagnostic.error(
          "Multiple machine instructions use different register files for this index. "
              + "It is not clear what register file the pseudo instruction must use.",
          funcParamNode.location()).build();
    }

    var registerFileName =
        registerFiles.stream().findFirst().orElseThrow().identifier.simpleName();
    var index = indexInInputsOrOutputs(funcParamNode)
        .orElseThrow(() -> Diagnostic.error(
            "Parameter is not part of an input or output operand in tablegen",
            funcParamNode.location()).build());

    return
        String.format(
            "getRegisterNameFrom%sByIndex("
                + "MCOperandWrapper(MI->getOperand(%d)).unwrapToIntegral())" + " // "
                + funcParamNode.parameter().simpleName() + "\n",
            registerFileName, index);
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

  private String getRegisterFile(Graph behavior, FieldRefNode fieldRefNode) {
    var candidates = behavior.getNodes(FieldRefNode.class)
        .filter(x -> x.formatField().equals(fieldRefNode.formatField()))
        .flatMap(Node::usages)
        .filter(x -> x instanceof HasRegisterTensor y && y.hasRegisterFile())
        .map(x -> ((HasRegisterTensor) x).registerTensor())
        .collect(Collectors.toSet());

    if (candidates.isEmpty()) {
      throw Diagnostic.error(
          "Field is not used as register. Therefore the compiler generator cannot "
              +
              "detect the register file.",
          fieldRefNode.location()).build();
    } else if (candidates.size() > 1) {
      throw Diagnostic.error(
          "Field is by multiple register file. Therefore the compiler generator cannot "
              +
              "detect the register file for the assembly.",
          fieldRefNode.location()).build();
    } else {
      return candidates.iterator().next().identifier.simpleName();
    }
  }

  public CNodeContext ctx() {
    return ctx;
  }

  public StringBuilder builder() {
    return builder;
  }
}
