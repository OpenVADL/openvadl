package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.function.Function;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Represents an abstract unary operation node within the Tiny Code Generator (TCG) framework.
 * This class serves as a base for specific unary operations by handling
 * common functionality such as argument management and data collection.
 */
public abstract class TcgUnaryOpNode extends TcgOpNode {

  @DataValue
  TcgV arg;

  public TcgUnaryOpNode(TcgV dest, TcgV arg) {
    super(dest, dest.width());
    this.arg = arg;
  }

  public TcgV arg() {
    return arg;
  }

  public abstract String tcgFunctionName();

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return tcgFunctionName() + "(" + dest.varName() + ", " + arg.varName() + ");";
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(arg);
  }
}
