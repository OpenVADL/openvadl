package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Not really a TCG operation, but a context setter, that assigns some value to the
 * {@code ctx->base.is_jmp} variable.
 * This is required if the TCG translation should be stopped because the translation block
 * ends (e.g. because of some branching).
 */
public class TcgSetIsJmp extends TcgOpNode {

  /**
   * Defines the behavior done by the translator.
   * NEXT is the default case and tells the translator that the TB has not ended.
   * NORETURN is tells the translator to stop the translation block.
   * CHAIN tells the tb_stop function to end the TB but also emit a jump to the next PC address,
   * as it is possible that the next instruction should be executed.
   */
  public enum Type {
    NORETURN,
    NEXT,
    CHAIN;

    @SuppressWarnings("MethodName")
    public String cCode() {
      return "DISAS_" + this.name();
    }
  }

  @DataValue
  private Type type;


  /**
   * Constructor for TcgSetIsJmp.
   *
   * @param type Defines the behavior done by the translator. It can be NORETURN, NEXT, or CHAIN.
   */
  public TcgSetIsJmp(Type type) {
    // TODO: This super constructor is useless. We need to create a TcgGenNode super type
    super(TcgV.gen(TcgWidth.i64), TcgWidth.i64);
    this.type = type;
  }

  public Type type() {
    return type;
  }

  @Override
  public Node copy() {
    return new TcgSetIsJmp(type);
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(type);
  }
}
