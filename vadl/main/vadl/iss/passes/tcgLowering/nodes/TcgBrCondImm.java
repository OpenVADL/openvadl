package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgCondition;
import vadl.iss.passes.tcgLowering.TcgLabel;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

public class TcgBrCondImm extends TcgOpNode {

  @DataValue
  private TcgCondition condition;

  @DataValue
  private TcgLabel label;

  @DataValue
  private TcgV cmpArg1;

  @Input
  private ExpressionNode cmpArg2;


  public TcgBrCondImm(TcgV cmpArg1, ExpressionNode cmpArg2, TcgCondition condition,
                      TcgLabel label) {
    // TODO: This super constructor is useless. We need to create a TcgGenNode super type
    super(TcgV.gen(TcgWidth.i64), TcgWidth.i64);
    this.condition = condition;
    this.label = label;
    this.cmpArg1 = cmpArg1;
    this.cmpArg2 = cmpArg2;
  }

  public TcgCondition condition() {
    return condition;
  }

  public TcgLabel label() {
    return label;
  }

  public TcgV cmpArg1() {
    return cmpArg1;
  }

  public ExpressionNode cmpArg2() {
    return cmpArg2;
  }

  @Override
  public Node copy() {
    return new TcgBrCondImm(cmpArg1, cmpArg2.copy(ExpressionNode.class), condition, label);
  }

  @Override
  public Node shallowCopy() {
    return new TcgBrCondImm(cmpArg1, cmpArg2, condition, label);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(condition);
    collection.add(label);
    collection.add(cmpArg1);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(cmpArg2);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    cmpArg2 = visitor.apply(this, cmpArg2, ExpressionNode.class);
  }
}
