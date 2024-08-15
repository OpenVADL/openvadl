package vadl.lcb.tablegen.lowering;

import java.io.StringWriter;
import vadl.lcb.LcbGraphNodeVisitor;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.viam.Constant;
import vadl.viam.Parameter;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.EndNode;
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
 * Visitor for generating TableGen patterns.
 * The goal is to generate S-expressions with the same the
 * variable naming.
 */
public class TableGenPatternVisitor implements LcbGraphNodeVisitor {
  private final StringWriter writer = new StringWriter();

  public String getResult() {
    return writer.toString();
  }

  @Override
  public void visit(ConstantNode node) {
    node.ensure(node.constant() instanceof Constant.Value, "constant must be value");
    if (node.constant() instanceof Constant.Value constant) {
      writer.write(constant.intValue());
    }
  }

  @Override
  public void visit(BuiltInCall node) {
    writer.write("(");
    if (node instanceof LlvmNodeLowerable lowerable) {
      writer.write(lowerable.lower() + " ");
    } else {
      writer.write(node.builtIn().operator() + " ");
    }

    for (int i = 0; i < node.arguments().size(); i++) {
      visit(node.arguments().get(i));

      if (i < node.arguments().size() - 1) {
        writer.write(", ");
      }
    }

    writer.write(")");
  }

  @Override
  public void visit(WriteRegNode writeRegNode) {

  }

  @Override
  public void visit(WriteRegFileNode writeRegFileNode) {

  }

  @Override
  public void visit(WriteMemNode writeMemNode) {

  }

  @Override
  public void visit(SliceNode sliceNode) {

  }

  @Override
  public void visit(SelectNode selectNode) {

  }

  @Override
  public void visit(ReadRegNode readRegNode) {
    // same as in LlvmLoweringStrategy#getTableGenInputOperands
    writer.write(readRegNode.register().name() + ":$" +
        readRegNode.nodeName());
  }

  @Override
  public void visit(ReadRegFileNode readRegFileNode) {
    // same as in LlvmLoweringStrategy#getTableGenInputOperands
    var address = (FieldRefNode) readRegFileNode.address();
    writer.write(readRegFileNode.registerFile().name() + ":$" +
        address.formatField().identifier.simpleName());
  }

  @Override
  public void visit(ReadMemNode readMemNode) {

  }

  @Override
  public void visit(LetNode letNode) {

  }

  @Override
  public void visit(FuncParamNode funcParamNode) {

  }

  @Override
  public void visit(FuncCallNode funcCallNode) {
    // same as in LlvmLoweringStrategy#getTableGenInputOperands
    writer.write(funcCallNode.function().identifier.lower() + ":$" +
        funcCallNode.function().identifier.simpleName());
  }

  @Override
  public void visit(FieldRefNode fieldRefNode) {

  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {
    // same as in LlvmLoweringStrategy#getTableGenInputOperands
    writer.write(fieldAccessRefNode.fieldAccess().accessFunction().name() + ":$" +
        fieldAccessRefNode.nodeName());
  }

  @Override
  public void visit(AbstractBeginNode abstractBeginNode) {

  }

  @Override
  public void visit(InstrEndNode instrEndNode) {

  }

  @Override
  public void visit(ReturnNode returnNode) {

  }

  @Override
  public void visit(EndNode endNode) {

  }

  @Override
  public void visit(InstrCallNode instrCallNode) {

  }

  @Override
  public void visit(IfNode ifNode) {

  }

  @Override
  public void visit(ZeroExtendNode node) {

  }

  @Override
  public void visit(SignExtendNode node) {

  }

  @Override
  public void visit(TruncateNode node) {

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
