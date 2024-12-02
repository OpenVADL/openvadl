package vadl.iss.passes.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.DependencyNode;

public class TcgVRefNode extends DependencyNode {

  @DataValue
  private TcgV var;

  public TcgVRefNode(TcgV var) {
    this.var = var;
  }

  public TcgV var() {
    return var;
  }

  public void setVar(TcgV var) {
    this.var = var;
  }

  public Tcg_32_64 width() {
    return var.width();
  }

  public String varName() {
    return var.varName();
  }

  public String cCode() {
    return var.varName();
  }

  @Override
  public Node copy() {
    return new TcgVRefNode(var);
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(var);
  }
}
