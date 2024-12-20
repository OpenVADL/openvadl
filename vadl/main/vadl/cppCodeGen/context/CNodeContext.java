package vadl.cppCodeGen.context;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import vadl.viam.graph.Node;

public class CNodeContext extends CGenContext<Node> {

  private BiConsumer<CNodeContext, Node> dispatch;

  public CNodeContext(Consumer<String> writer,
                      BiConsumer<CNodeContext, Node> dispatch) {
    super(writer);
    this.dispatch = dispatch;
  }


  @Override
  public CGenContext<Node> gen(Node node) {
    dispatch.accept(this, node);
    return this;
  }

  @Override
  public String genToString(Node node) {
    var builder = new StringBuilder();
    var subContext = new CNodeContext(builder::append, dispatch);
    subContext.gen(node);
    return builder.toString();
  }
}
