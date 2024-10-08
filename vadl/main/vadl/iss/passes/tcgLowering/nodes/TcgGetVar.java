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

public abstract sealed class TcgGetVar extends TcgOpNode
    permits TcgGetVar.TcgGetRegFile, TcgGetVar.TcgGetTemp {

  public TcgGetVar(TcgV res) {
    super(res, res.width());
  }

  public static final class TcgGetTemp extends TcgGetVar {

    public TcgGetTemp(TcgV res) {
      super(res);
    }

    @Override
    public Node copy() {
      return new TcgGetTemp(res);
    }

    @Override
    public Node shallowCopy() {
      return new TcgGetTemp(res);
    }
  }

  public static final class TcgGetRegFile extends TcgGetVar {

    @DataValue
    RegisterFile registerFile;
    @Input
    ExpressionNode index;

    public enum Kind {
      SRC,
      DEST,
    }

    @DataValue
    Kind kind;

    public TcgGetRegFile(RegisterFile registerFile, ExpressionNode index, Kind kind, TcgV res) {
      super(res);
      this.registerFile = registerFile;
      this.index = index;
      this.kind = kind;
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

    public Kind kind() {
      return kind;
    }

    public ExpressionNode index() {
      return index;
    }

    @Override
    public Node copy() {
      return new TcgGetRegFile(registerFile, index.copy(ExpressionNode.class), kind, res);
    }

    @Override
    public Node shallowCopy() {
      return new TcgGetRegFile(registerFile, index, kind, res);
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
