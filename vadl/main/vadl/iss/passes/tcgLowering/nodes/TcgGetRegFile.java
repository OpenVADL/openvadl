package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.RegisterFile;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

public class TcgGetRegFile extends TcgOpNode {

  @DataValue
  RegisterFile registerFile;
  @Input
  ExpressionNode index;

  public TcgGetRegFile(RegisterFile registerFile, ExpressionNode index,
                       TcgV res,
                       TcgWidth width) {
    super(res, width);
    this.registerFile = registerFile;
    this.index = index;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    var cType = registerFile.resultType().fittingCppType();
    ensure(cType != null, "Couldn't fit cpp type");
    ensure(res.width().width <= cType.bitWidth(),
        "register file result width does not fit in node's result var width");
  }

  public RegisterFile registerFile() {
    return registerFile;
  }

  public ExpressionNode index() {
    return index;
  }

  @Override
  public Node copy() {
    return new TcgGetRegFile(registerFile, index.copy(ExpressionNode.class), res, width);
  }

  @Override
  public Node shallowCopy() {
    return new TcgGetRegFile(registerFile, index, res, width);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(registerFile);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(index);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    index = visitor.apply(this, index, ExpressionNode.class);
  }
}
