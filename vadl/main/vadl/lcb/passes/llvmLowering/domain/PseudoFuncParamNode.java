package vadl.lcb.passes.llvmLowering.domain;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Parameter;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * This class is used when instruction operands have to be replaced by {@link FuncParamNode}.
 * In the example below we replace {@code rs1} by {@code rs}. The pseudo expander will also require
 * the index of {@code rs} when expanding the pseudo instruction.
 * <code>
 * pseudo instruction BGEZ( rs : Index, offset : Bits<12> ) =
 * {
 * BGE{ rs1 = rs, rs2 = 0 as Bits5, imm = offset }
 * }
 * </code>
 */
public class PseudoFuncParamNode extends FuncParamNode {
  /**
   * Index of the operand in the pseudo instruction.
   */
  @DataValue
  protected int index;

  /**
   * Constructs a FuncParamNode instance with a given parameter and type.
   * The node type and parameter type must be equal.
   */
  public PseudoFuncParamNode(Parameter parameter, int index) {
    super(parameter);
    this.index = index;
  }


  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(index);
  }


  @Override
  public Node copy() {
    return new PseudoFuncParamNode(parameter, index);
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }
}
