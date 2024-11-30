package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.GraphNodeVisitor;

/**
 * Represents an abstract TCG (Tiny Code Generation) operation node.
 * It extends the DirectionalNode class and contains a result variable and a width specification.
 * This class is designed to be the base class for specific TCG operator nodes.
 */
public abstract class TcgOpNode extends TcgNode {

  @DataValue
  TcgV dest;
  @DataValue
  Tcg_32_64 width;

  public TcgOpNode(TcgV dest, Tcg_32_64 width) {
    this.dest = dest;
    this.width = width;
  }

  public TcgOpNode(TcgV dest) {
    this.dest = dest;
    this.width = dest.width();
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(dest.width() == width, "result variable width does not match");
  }

  public Tcg_32_64 width() {
    return width;
  }

  public TcgV dest() {
    return dest;
  }

  public void setDest(TcgV res) {
    this.dest = res;
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // Do not visit by graph visitor
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(dest);
    collection.add(width);
  }
}
