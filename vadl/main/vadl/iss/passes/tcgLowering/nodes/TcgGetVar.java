package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.function.Function;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Abstract sealed class representing a variable retrieval operation in the TCG.
 */
public abstract sealed class TcgGetVar extends TcgOpNode
    permits TcgGetVar.TcgGetRegFile, TcgGetVar.TcgGetReg, TcgGetVar.TcgGetTemp {

  public TcgGetVar(TcgV dest) {
    super(dest, dest.width());
  }

  /**
   * Represents an operation in the TCG for retrieving a value from a temporary variable.
   */
  public static final class TcgGetTemp extends TcgGetVar {

    public TcgGetTemp(TcgV res) {
      super(res);
    }

    @Override
    public String cCode(Function<Node, String> nodeToCCode) {
      return "TCGv_" + dest.width() + " " + dest.varName() + " = "
          + "tcg_tmp_new_" + width + "();";
    }

    @Override
    public Node copy() {
      return new TcgGetTemp(dest);
    }

    @Override
    public Node shallowCopy() {
      return new TcgGetTemp(dest);
    }
  }

  /**
   * Represents an operation in the TCG for retrieving a value from a register.
   */
  public static final class TcgGetReg extends TcgGetVar {

    @DataValue
    Register register;

    public TcgGetReg(Register reg, TcgV res) {
      super(res);
      register = reg;
    }

    public Register register() {
      return register;
    }

    @Override
    public String cCode(Function<Node, String> nodeToCCode) {
      return "TCGv_" + dest.width() + " " + dest.varName() + " = "
          + "cpu_" + register.simpleName().toLowerCase() + ";";
    }


    @Override
    public Node copy() {
      return new TcgGetTemp(dest);
    }

    @Override
    public Node shallowCopy() {
      return new TcgGetTemp(dest);
    }

    @Override
    protected void collectData(List<Object> collection) {
      super.collectData(collection);
      collection.add(register);
    }
  }

  /**
   * Represents an operation in the TCG for retrieving a value from a register file.
   * This is emitted as e.g. {@code get_x(ctx, a->rs1)} in the instruction translation.
   */
  public static final class TcgGetRegFile extends TcgGetVar {

    @DataValue
    RegisterFile registerFile;
    @Input
    ExpressionNode index;

    /**
     * The kind of the register file retrieval.
     * If the result variable is used as destination, the kind is DEST.
     * Otherwise, it is SRC.
     */
    public enum Kind {
      SRC,
      DEST,
    }

    @DataValue
    Kind kind;

    /**
     * Constructs a TcgGetRegFile object representing an operation in the TCG for retrieving a value
     * from a register file.
     *
     * @param registerFile The register file from which the variable is to be retrieved.
     * @param index        The index expression node specifying
     *                     the address within the register file.
     * @param kind         The kind of the register file retrieval, either SRC or DEST.
     * @param res          The result variable representing the output of this operation.
     */
    public TcgGetRegFile(RegisterFile registerFile, ExpressionNode index, Kind kind, TcgV res) {
      super(res);
      this.registerFile = registerFile;
      this.index = index;
      this.kind = kind;
    }

    @Override
    public void verifyState() {
      super.verifyState();

      var cppType = registerFile.resultType().fittingCppType();
      ensure(cppType != null, "Couldn't fit cpp type");
      ensure(dest.width().width <= cppType.bitWidth(),
          "register file result width does not fit in node's result var width");
    }

    public RegisterFile registerFile() {
      return registerFile;
    }

    public Kind kind() {
      return kind;
    }

    public ExpressionNode index() {
      return index;
    }

    @Override
    public String cCode(Function<Node, String> nodeToCCode) {
      var prefix = kind() == TcgGetVar.TcgGetRegFile.Kind.DEST ? "dest" : "get";
      return "TCGv_" + dest.width() + " " + dest.varName() + " = "
          + prefix + "_" + registerFile.simpleName().toLowerCase()
          + "(ctx, " + nodeToCCode.apply(index)
          + ");";
    }

    @Override
    public Node copy() {
      return new TcgGetRegFile(registerFile, index.copy(ExpressionNode.class), kind, dest);
    }

    @Override
    public Node shallowCopy() {
      return new TcgGetRegFile(registerFile, index, kind, dest);
    }

    @Override
    protected void collectData(List<Object> collection) {
      super.collectData(collection);
      collection.add(registerFile);
      collection.add(kind);
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
}
