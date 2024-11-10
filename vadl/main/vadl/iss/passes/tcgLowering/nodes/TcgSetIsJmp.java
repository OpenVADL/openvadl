package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

public class TcgSetIsJmp extends TcgOpNode {

  public enum Type {
    NORETURN,
    NEXT,
    CHAIN;

    public String cCode() {
      return "DISAS_" + this.name();
    }
  }

  @DataValue
  private Type type;

  public TcgSetIsJmp(Type type) {
    // TODO: This super constructor is useless. We need to create a TcgGenNode super type
    super(TcgV.gen(TcgWidth.i64), TcgWidth.i64);
    this.type = type;
  }

  public Type type() {
    return type;
  }

  @Override
  public Node copy() {
    return new TcgSetIsJmp(type);
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(type);
  }
}
