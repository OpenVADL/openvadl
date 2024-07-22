package vadl.viam.graph;

import vadl.gcb.passes.encoding.nodes.NegatedNode;
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
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;

/**
 * Interface for visiting multiple {@link Node} and its subtypes.
 */
public interface GraphNodeVisitor {
  /**
   * Visit {@link ConstantNode}.
   */
  void visit(ConstantNode node);

  /**
   * Visit {@link BuiltInCall}.
   */
  void visit(BuiltInCall node);

  /**
   * Visit {@link WriteRegNode}.
   */
  void visit(WriteRegNode writeRegNode);

  /**
   * Visit {@link WriteRegFileNode}.
   */
  void visit(WriteRegFileNode writeRegFileNode);

  /**
   * Visit {@link WriteMemNode}.
   */
  void visit(WriteMemNode writeMemNode);

  /**
   * Visit {@link TypeCastNode}.
   */
  void visit(TypeCastNode typeCastNode);

  /**
   * Visit {@link SliceNode}.
   */
  void visit(SliceNode sliceNode);

  /**
   * Visit {@link SelectNode}.
   */
  void visit(SelectNode selectNode);

  /**
   * Visit {@link ReadRegNode}.
   */
  void visit(ReadRegNode readRegNode);

  /**
   * Visit {@link ReadRegFileNode}.
   */
  void visit(ReadRegFileNode readRegFileNode);

  /**
   * Visit {@link ReadMemNode}.
   */
  void visit(ReadMemNode readMemNode);

  /**
   * Visit {@link LetNode}.
   */
  void visit(LetNode letNode);

  /**
   * Visit {@link FuncParamNode}.
   */
  void visit(FuncParamNode funcParamNode);

  /**
   * Visit {@link FuncCallNode}.
   */
  void visit(FuncCallNode funcCallNode);

  /**
   * Visit {@link FieldRefNode}.
   */
  void visit(FieldRefNode fieldRefNode);

  /**
   * Visit {@link FieldAccessRefNode}.
   */
  void visit(FieldAccessRefNode fieldAccessRefNode);

  /**
   * Visit {@link AbstractBeginNode}.
   */
  void visit(AbstractBeginNode abstractBeginNode);

  /**
   * Visit {@link InstrEndNode}.
   */
  void visit(InstrEndNode instrEndNode);

  /**
   * Visit {@link ReturnNode}.
   */
  void visit(ReturnNode returnNode);

  /**
   * Visit {@link EndNode}.
   */
  void visit(EndNode endNode);

  /**
   * Visit {@link InstrCallNode}.
   */
  void visit(InstrCallNode instrCallNode);

  /**
   * Visit {@link IfNode}.
   */
  void visit(IfNode ifNode);

  /**
   * Visit {@link ExpressionNode}.
   */
  void visit(ExpressionNode expressionNode);

  /**
   * Visit {@link NegatedNode}.
   */
  void visit(NegatedNode negatedNode);
}
