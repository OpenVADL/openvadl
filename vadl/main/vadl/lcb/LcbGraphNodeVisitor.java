package vadl.lcb;

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
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Visitor for nodes of the lcb layer.
 */
public interface LcbGraphNodeVisitor extends GraphNodeVisitor {
  /**
   * Default method for this visitor. Note that this is a fallback
   * and must not be reached.
   */
  default void visit(Node node) {
    if (node instanceof ConstantNode constantNode) {
      visit(constantNode);
    } else if (node instanceof BuiltInCall built) {
      visit(built);
    } else if (node instanceof WriteRegNode writeRegNode) {
      visit(writeRegNode);
    } else if (node instanceof WriteRegFileNode writeRegFileNode) {
      visit(writeRegFileNode);
    } else if (node instanceof WriteMemNode writeMemNode) {
      visit(writeMemNode);
    } else if (node instanceof SliceNode sliceNode) {
      visit(sliceNode);
    } else if (node instanceof SelectNode selectNode) {
      visit(selectNode);
    } else if (node instanceof ReadRegNode readRegNode) {
      visit(readRegNode);
    } else if (node instanceof ReadRegFileNode readRegFileNode) {
      visit(readRegFileNode);
    } else if (node instanceof ReadMemNode readMemNode) {
      visit(readMemNode);
    } else if (node instanceof LetNode letNode) {
      visit(letNode);
    } else if (node instanceof FuncParamNode funcParamNode) {
      visit(funcParamNode);
    } else if (node instanceof FuncCallNode funcCallNode) {
      visit(funcCallNode);
    } else if (node instanceof FieldRefNode fieldRefNode) {
      visit(fieldRefNode);
    } else if (node instanceof FieldAccessRefNode fieldAccessRefNode) {
      visit(fieldAccessRefNode);
    } else if (node instanceof AbstractBeginNode abstractBeginNode) {
      visit(abstractBeginNode);
    } else if (node instanceof InstrEndNode instrEndNode) {
      visit(instrEndNode);
    } else if (node instanceof ReturnNode returnNode) {
      visit(returnNode);
    } else if (node instanceof EndNode endNode) {
      visit(endNode);
    } else if (node instanceof InstrCallNode instrCallNode) {
      visit(instrCallNode);
    } else if (node instanceof IfNode ifNode) {
      visit(ifNode);
    } else if (node instanceof ZeroExtendNode zeroExtendNode) {
      visit(zeroExtendNode);
    } else if (node instanceof SignExtendNode signExtendNode) {
      visit(signExtendNode);
    } else if (node instanceof TruncateNode truncateNode) {
      visit(truncateNode);
    } else if (node instanceof ExpressionNode expressionNode) {
      visit(expressionNode);
    } else if (node instanceof SideEffectNode sideEffectNode) {
      visit(sideEffectNode);
    } else {
      visit(node);
    }
  }
}
