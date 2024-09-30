package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.Objects;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

public abstract class TcgBinaryImmOpNode extends TcgOpNode {

  @DataValue
  TcgV arg1;

  @Input
  ExpressionNode arg2;

  public TcgBinaryImmOpNode(TcgV res, TcgV arg1, ExpressionNode arg2, TcgWidth width) {
    super(res, width);
    this.arg1 = arg1;
    this.arg2 = arg2;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(arg1.width() == width, "argument 1 width does not match");
    ensure(arg2.type().isData(), "argument 2 is not a data type");
    ensure(
        Objects.requireNonNull(((DataType) arg2.type()).fittingCppType()).bitWidth() == width.width,
        "argument 2 width does not match");
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(arg1);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(arg2);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    arg2 = visitor.apply(this, arg2, ExpressionNode.class);
  }
}
