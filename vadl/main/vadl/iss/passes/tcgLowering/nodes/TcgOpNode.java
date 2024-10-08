package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.control.DirectionalNode;

/**
 * Represents an abstract TCG (Tiny Code Generation) operation node.
 * It extends the DirectionalNode class and contains a result variable and a width specification.
 * This class is designed to be the base class for specific TCG operator nodes.
 */
public abstract class TcgOpNode extends DirectionalNode {

  @DataValue
  TcgV res;
  @DataValue
  TcgWidth width;

  public TcgOpNode(TcgV res, TcgWidth width) {
    this.res = res;
    this.width = width;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(res.width() == width, "result variable width does not match");
  }

  public TcgWidth width() {
    return width;
  }

  public TcgV res() {
    return res;
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // Do not visit by graph visitor
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(res);
    collection.add(width);
  }
}
