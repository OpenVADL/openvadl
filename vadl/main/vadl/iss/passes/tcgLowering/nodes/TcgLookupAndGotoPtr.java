package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.viam.graph.Node;

/**
 * TcgLookupAndGotoPtr is representing a TCG (Tiny Code Generation)
 * operation node that jumps to the location of the current PC address.
 * If the address is not yet translated to native machine code, it will trigger
 * the translation loop.
 *
 * <p>It translates to {@code lookup_and_goto_ptr();}
 */
public class TcgLookupAndGotoPtr extends TcgNode {

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "tcg_gen_lookup_and_goto_ptr();";
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return Set.of();
  }

  @Override
  public List<TcgVRefNode> definedVars() {
    return List.of();
  }

  @Override
  public Node copy() {
    return new TcgLookupAndGotoPtr();
  }

  @Override
  public Node shallowCopy() {
    return new TcgLookupAndGotoPtr();
  }

}
