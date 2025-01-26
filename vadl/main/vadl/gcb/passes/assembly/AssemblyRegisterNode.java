package vadl.gcb.passes.assembly;

import vadl.gcb.passes.assembly.visitors.AssemblyVisitor;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * Register Node in assembly.
 */
public class AssemblyRegisterNode extends BuiltInCall {


  public AssemblyRegisterNode(NodeList<ExpressionNode> args,
                              Type type) {
    super(BuiltInTable.REGISTER, args, type);
  }


  public AssemblyRegisterNode(FieldRefNode fieldRefNode,
                              Type type) {
    super(BuiltInTable.REGISTER, new NodeList<>(fieldRefNode), type);
  }

  public FieldRefNode field() {
    return (FieldRefNode) args.get(0);
  }

  @Override
  public ExpressionNode copy() {
    return new AssemblyRegisterNode(
        new NodeList<>(this.arguments().stream().map(x -> (ExpressionNode) x.copy()).toList()),
        this.type());
  }

  @Override
  public Node shallowCopy() {
    return new AssemblyRegisterNode(args, type());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof AssemblyVisitor v) {
      v.visit(this);
    } else {
      visitor.visit(this);
    }
  }
}
