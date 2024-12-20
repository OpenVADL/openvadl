package vadl.cppCodeGen.context;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import vadl.viam.graph.Node;

/**
 * A code generation context for {@link Node}s.
 * It will handle pass all generation calls to the dispatch
 * function passed to the constructor.
 */
public class CNodeContext extends CGenContext<Node> {

  private BiConsumer<CNodeContext, Node> dispatch;

  /**
   * Construct a new code generation context for {@link Node}s.
   *
   * @param writer   The writer that handles string passed by handlers.
   * @param dispatch The dispatch method used when requesting generation of some node.
   */
  public CNodeContext(Consumer<String> writer,
                      BiConsumer<CNodeContext, Node> dispatch) {
    super(writer);
    this.dispatch = dispatch;
  }


  @Override
  public CGenContext<Node> gen(Node entity) {
    dispatch.accept(this, entity);
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
