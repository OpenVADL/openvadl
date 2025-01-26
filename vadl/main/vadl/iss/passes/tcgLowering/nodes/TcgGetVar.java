package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

// TODO: This should extend TcgVarnode instead

/**
 * Abstract sealed class representing a variable retrieval operation in the TCG.
 */
public abstract sealed class TcgGetVar extends TcgOpNode
    permits TcgGetVar.TcgGetConst, TcgGetVar.TcgGetReg, TcgGetVar.TcgGetRegFile,
    TcgGetVar.TcgGetTemp {

  public TcgGetVar(TcgVRefNode dest) {
    super(dest, dest.var().width());
  }

  /**
   * Constructs a {@link TcgGetVar} node from the given {@link TcgV}.
   */
  public static TcgGetVar from(TcgVRefNode varRef) {
    return switch (varRef.var().kind()) {
      case TMP -> new TcgGetTemp(varRef);
      case CONST -> new TcgGetConst(varRef, varRef.var().constValue());
      case REG -> new TcgGetReg((Register) varRef.var().registerOrFile(), varRef);
      case REG_FILE -> {
        var kind =
            varRef.var().isDest() ? TcgGetRegFile.Kind.DEST : TcgGetRegFile.Kind.SRC;
        yield new TcgGetRegFile((RegisterFile) varRef.var().registerOrFile(),
            varRef.var().regFileIndex(), kind, varRef);
      }
    };
  }

  /**
   * Represents an operation in the TCG for retrieving a value from a temporary variable.
   */
  public static final class TcgGetTemp extends TcgGetVar {

    public TcgGetTemp(TcgVRefNode dest) {
      super(dest);
    }

    @Override
    public String cCode(Function<Node, String> nodeToCCode) {
      return "TCGv_" + firstDest().var().width() + " " + firstDest().var().varName() + " = "
          + "tcg_temp_new_" + width() + "();";
    }

    @Override
    public Node copy() {
      return new TcgGetTemp(firstDest().copy(TcgVRefNode.class));
    }

    @Override
    public Node shallowCopy() {
      return new TcgGetTemp(firstDest());
    }
  }

  /**
   * Represents an operation in the TCG for retrieving a value from a constant variable.
   */
  public static final class TcgGetConst extends TcgGetVar {

    @Input
    private ExpressionNode constValue;

    public TcgGetConst(TcgVRefNode dest, ExpressionNode constValue) {
      super(dest);
      this.constValue = constValue;
    }

    @Override
    public String cCode(Function<Node, String> nodeToCCode) {
      return "TCGv_" + firstDest().var().width() + " " + firstDest().cCode() + " = "
          + "tcg_constant_" + width() + "(" + nodeToCCode.apply(constValue) + ");";
    }

    public ExpressionNode constValue() {
      return constValue;
    }

    @Override
    public Set<TcgVRefNode> usedVars() {
      // by defining the constant var it self to be used, we prevent that the variable
      // can be potentially be written before
      var sup = super.usedVars();
      sup.add(firstDest());
      return sup;
    }

    @Override
    public Node copy() {
      return new TcgGetTemp(firstDest());
    }

    @Override
    public Node shallowCopy() {
      return new TcgGetTemp(firstDest());
    }

    @Override
    protected void collectInputs(List<Node> collection) {
      super.collectInputs(collection);
      collection.add(constValue);
    }

    @Override
    protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
      super.applyOnInputsUnsafe(visitor);
      constValue = visitor.apply(this, constValue, ExpressionNode.class);
    }
  }

  /**
   * Represents an operation in the TCG for retrieving a value from a register.
   */
  public static final class TcgGetReg extends TcgGetVar {

    @DataValue
    Register register;

    public TcgGetReg(Register reg, TcgVRefNode dest) {
      super(dest);
      register = reg;
    }

    public Register register() {
      return register;
    }

    @Override
    public String cCode(Function<Node, String> nodeToCCode) {
      return "TCGv_" + firstDest().var().width() + " " + firstDest().cCode() + " = "
          + "cpu_" + register.simpleName().toLowerCase() + ";";
    }

    @Override
    public Set<TcgVRefNode> usedVars() {
      // tcg get regs are also reads, so it can't be shared
      var sup = super.usedVars();
      sup.add(firstDest());
      return sup;
    }

    @Override
    public Node copy() {
      return new TcgGetTemp(firstDest());
    }

    @Override
    public Node shallowCopy() {
      return new TcgGetTemp(firstDest());
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
     * If the result variable is used as destination, the kind is dest()
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
     * @param kind         The kind of the register file retrieval, either SRC or dest()
     * @param res          The result variable representing the output of this operation.
     */
    public TcgGetRegFile(RegisterFile registerFile, ExpressionNode index, Kind kind,
                         TcgVRefNode res) {
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
      ensure(firstDest().var().width().width <= cppType.bitWidth(),
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
    public Set<TcgVRefNode> usedVars() {
      // tcg get regs are also reads, so it can't be shared
      var sup = super.usedVars();
      sup.add(firstDest());
      return sup;
    }

    @Override
    public String cCode(Function<Node, String> nodeToCCode) {
      var prefix = kind() == Kind.DEST ? "dest" : "get";
      return "TCGv_" + firstDest().var().width() + " " + firstDest().var().varName() + " = "
          + prefix + "_" + registerFile.simpleName().toLowerCase()
          + "(ctx, " + nodeToCCode.apply(index)
          + ");";
    }

    @Override
    public Node copy() {
      return new TcgGetRegFile(registerFile, index.copy(ExpressionNode.class), kind, firstDest());
    }

    @Override
    public Node shallowCopy() {
      return new TcgGetRegFile(registerFile, index, kind, firstDest());
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
