package vadl.viam.graph;

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
 * Interface for visiting multiple {@link Node} and its subtypes.
 */
public interface GraphNodeVisitor {
  /**
   * Catchall method when overloading did not work.
   */
  default void visit(Node node) {
    throw new RuntimeException("Node type is not implemented: " + node.getClass());
  }

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
   * Visit {@link ZeroExtendNode}.
   */
  void visit(ZeroExtendNode node);

  /**
   * Visit {@link SignExtendNode}.
   */
  void visit(SignExtendNode node);

  /**
   * Visit {@link TruncateNode}.
   */
  void visit(TruncateNode node);

  /**
   * Visit {@link ExpressionNode}.
   */
  void visit(ExpressionNode expressionNode);

  /**
   * Visit {@link SideEffectNode}.
   */
  void visit(SideEffectNode sideEffectNode);
}