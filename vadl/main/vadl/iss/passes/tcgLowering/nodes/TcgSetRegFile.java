package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.RegisterFile;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents an operation in the TCG for setting a value to a register file.
 * This is emitted as e.g. {@code set_x(ctx, a->rs1, res_var)} in the instruction translation.
 */
public final class TcgSetRegFile extends TcgOpNode {

  @DataValue
  RegisterFile registerFile;
  @Input
  ExpressionNode index;

  /**
   * Constructs a TcgSetRegFile object representing an operation in the TCG for sets a value
   * to a register file.
   *
   * @param registerFile The register file to which the variable is to be written.
   * @param index        The index expression node specifying
   *                     the address within the register file.
   * @param res          The result variable representing the value to write.
   */
  public TcgSetRegFile(RegisterFile registerFile, ExpressionNode index,
                       TcgV res) {
    super(res, res.width());
    this.registerFile = registerFile;
    this.index = index;
  }


  @Override
  public void verifyState() {
    super.verifyState();

    var cppType = registerFile.resultType().fittingCppType();
    ensure(cppType != null, "Couldn't fit cpp type");
    ensure(res.width().width <= cppType.bitWidth(),
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
    return new TcgSetRegFile(registerFile, index.copy(ExpressionNode.class), res);
  }

  @Override
  public Node shallowCopy() {
    return new TcgSetRegFile(registerFile, index, res);
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

