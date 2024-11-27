package vadl.iss.passes.tcgLowering.nodes;

import java.util.function.Function;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.DirectionalNode;

public abstract class TcgNode extends DirectionalNode {

  @SuppressWarnings("MethodName")
  abstract public String cCode(Function<Node, String> nodeToCCode);

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // Do not visit by graph visitor
  }

}
