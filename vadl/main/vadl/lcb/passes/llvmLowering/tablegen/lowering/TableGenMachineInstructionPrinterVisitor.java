package vadl.lcb.passes.llvmLowering.tablegen.lowering;

import java.io.StringWriter;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionValueNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbPseudoInstructionNode;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.viam.Constant;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Visitor for machine instructions.
 */
public class TableGenMachineInstructionPrinterVisitor implements TableGenMachineInstructionVisitor {
  protected final StringWriter writer = new StringWriter();

  public String getResult() {
    return writer.toString();
  }

  protected void joinArgumentsWithComma(NodeList<ExpressionNode> args) {
    for (int i = 0; i < args.size(); i++) {
      visit(args.get(i));

      if (i < args.size() - 1) {
        writer.write(", ");
      }
    }
  }

  @Override
  public void visit(LcbPseudoInstructionNode node) {
    writer.write("(");
    writer.write(node.instruction().identifier.simpleName() + " ");

    joinArgumentsWithComma(node.arguments());

    writer.write(")");
  }

  @Override
  public void visit(LcbMachineInstructionNode node) {
    writer.write("(");
    writer.write(node.instruction().identifier.simpleName() + " ");

    joinArgumentsWithComma(node.arguments());

    writer.write(")");
  }

  @Override
  public void visit(LcbMachineInstructionParameterNode machineInstructionParameterNode) {
    writer.write(machineInstructionParameterNode.instructionOperand().render());
  }

  @Override
  public void visit(LcbMachineInstructionValueNode machineInstructionValueNode) {
    writer.write("(" + machineInstructionValueNode.valueType().getLlvmType() + " "
        + machineInstructionValueNode.constant().asVal().intValue() + ")");
  }

  @Override
  public void visit(ConstantNode node) {
    if (node.constant() instanceof Constant.Str str) {
      writer.write(str.value());
    } else if (node.constant() instanceof Constant.Value v) {
      writer.write(v.intValue());
    } else {
      throw new RuntimeException("not implemented");
    }
  }

  @Override
  public void visit(BuiltInCall node) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(WriteRegNode writeRegNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(WriteRegFileNode writeRegFileNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(WriteMemNode writeMemNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(SliceNode sliceNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(SelectNode selectNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ReadRegNode readRegNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ReadRegFileNode readRegFileNode) {
    var operand = LlvmInstructionLoweringStrategy.generateTableGenInputOutput(readRegFileNode);
    writer.write(operand.render());
  }

  @Override
  public void visit(ReadMemNode readMemNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(LetNode letNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(FuncParamNode funcParamNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(FuncCallNode funcCallNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(FieldRefNode fieldRefNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(AbstractBeginNode abstractBeginNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(InstrEndNode instrEndNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ReturnNode returnNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(BranchEndNode branchEndNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(InstrCallNode instrCallNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(IfNode ifNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ZeroExtendNode node) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(SignExtendNode node) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(TruncateNode node) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ExpressionNode expressionNode) {
    expressionNode.accept(this);
  }

  @Override
  public void visit(SideEffectNode sideEffectNode) {
    sideEffectNode.accept(this);
  }
}
