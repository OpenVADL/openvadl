package vadl.iss.passes.nodes;

import java.util.List;
import javax.annotation.Nullable;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * A dependency node that holds the TcgV variable.
 * This is a node, as it makes it easier to optimize variable usages during allocation.
 */
public class TcgVRefNode extends DependencyNode {

  @DataValue
  private TcgV var;

  @Input
  @Nullable
  // the immediate(constant) value or register file index.
  private ExpressionNode dependency;

  public TcgVRefNode(TcgV var, @Nullable ExpressionNode dependency) {
    this.var = var;
    this.dependency = dependency;
  }

  public TcgV var() {
    return var;
  }

  @Nullable
  public ExpressionNode dependency() {
    return dependency;
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

  @SuppressWarnings("MethodName")
  public String cCode() {
    return var.varName();
  }

  @Override
  public TcgVRefNode copy() {
    return new TcgVRefNode(var, dependency == null ? null : dependency.copy());
  }

  @Override
  public Node shallowCopy() {
    return new TcgVRefNode(var, dependency);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(var);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    if (this.dependency != null) {
      collection.add(dependency);
    }
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    dependency = visitor.applyNullable(this, dependency, ExpressionNode.class);
  }
}
