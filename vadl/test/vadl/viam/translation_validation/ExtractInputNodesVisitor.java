package vadl.viam.translation_validation;

import java.util.ArrayList;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
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
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;

/**
 * Helper visitor to extract {@link Node} which are inputs
 * for the {@link Graph}.
 */
public class ExtractInputNodesVisitor implements GraphNodeVisitor {
  private final ArrayList<Node> inputs = new ArrayList<>();

  public ArrayList<Node> getInputs() {
    return inputs;
  }

  @Override
  public void visit(ConstantNode node) {

  }

  @Override
  public void visit(BuiltInCall node) {
    node.arguments().forEach(this::visit);
  }

  @Override
  public void visit(WriteRegNode writeRegNode) {
    visit(writeRegNode.value());
  }

  @Override
  public void visit(WriteRegFileNode writeRegFileNode) {
    visit(writeRegFileNode.value());
  }

  @Override
  public void visit(WriteMemNode writeMemNode) {
    visit(writeMemNode.value());
  }

  @Override
  public void visit(TypeCastNode typeCastNode) {
    visit(typeCastNode.value());
  }

  @Override
  public void visit(SliceNode sliceNode) {
    visit(sliceNode.value());
  }

  @Override
  public void visit(SelectNode selectNode) {
    visit(selectNode.condition());
    visit(selectNode.trueCase());
    visit(selectNode.falseCase());
  }

  @Override
  public void visit(ReadRegNode readRegNode) {
    inputs.add(readRegNode);
  }

  @Override
  public void visit(ReadRegFileNode readRegFileNode) {
    inputs.add(readRegFileNode);
  }

  @Override
  public void visit(ReadMemNode readMemNode) {
    inputs.add(readMemNode);
  }

  @Override
  public void visit(LetNode letNode) {
    visit(letNode.expression());
  }

  @Override
  public void visit(FuncParamNode funcParamNode) {
    inputs.add(funcParamNode);
  }

  @Override
  public void visit(FuncCallNode funcCallNode) {
    inputs.add(funcCallNode);
  }

  @Override
  public void visit(FieldRefNode fieldRefNode) {
    inputs.add(fieldRefNode);
  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {
    inputs.add(fieldAccessRefNode);
  }

  @Override
  public void visit(AbstractBeginNode abstractBeginNode) {
    abstractBeginNode.inputs().forEach(this::visit);
  }

  @Override
  public void visit(InstrEndNode instrEndNode) {
    instrEndNode.sideEffects.forEach(this::visit);
  }

  @Override
  public void visit(ReturnNode returnNode) {
    visit(returnNode.value);
  }

  @Override
  public void visit(EndNode endNode) {
    endNode.sideEffects.forEach(this::visit);
  }

  @Override
  public void visit(InstrCallNode instrCallNode) {
    instrCallNode.arguments().forEach(this::visit);
  }

  @Override
  public void visit(IfNode ifNode) {
    visit(ifNode.condition);
    visit(ifNode.trueBranch());
    visit(ifNode.falseBranch());
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
