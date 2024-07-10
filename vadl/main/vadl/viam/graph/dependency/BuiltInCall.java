package vadl.viam.graph.dependency;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;
import vadl.javaannotations.viam.DataValue;
import vadl.types.BuiltInTable;
import vadl.types.BuiltInTable.BuiltIn;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * Represents a function call to a VADL built-in.
 * It holds a {@link BuiltIn} function from the {@link vadl.types.BuiltInTable}.
 *
 * @see vadl.types.BuiltInTable
 * @see AbstractFunctionCallNode
 */
public class BuiltInCall extends AbstractFunctionCallNode {

  @DataValue
  protected BuiltIn builtIn;

  public BuiltInCall(BuiltIn builtIn, NodeList<ExpressionNode> args, Type type) {
    super(args, type);
    this.builtIn = builtIn;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(builtIn);
  }

  @Override
  public Node copy() {
    return new BuiltInCall(builtIn,
        new NodeList<>(this.arguments().stream().map(x -> (ExpressionNode) x.copy()).toList()),
        this.type());
  }

  @Override
  public Node shallowCopy() {
    return new BuiltInCall(builtIn, args, type());
  }

  /**
   * Gets the {@link BuiltIn}.
   */
  public BuiltIn builtIn() {
    return this.builtIn;
  }

  /**
   * Checks whether all the inputs of the node are constant.
   *
   * @return {@code true} if all the inputs are {@link ConstantNode} and {@code false}
   *     if any is not {@link ConstantNode}.
   */
  private boolean hasConstantInputs() {
    return inputs().allMatch(x -> x instanceof ConstantNode);
  }

  @Override
  public Optional<Node> normalize() {
    if (hasConstantInputs()) {
      if (this.builtIn == BuiltInTable.ADD) {
        return reduce(BigInteger::add);
      } else if (this.builtIn == BuiltInTable.SUB) {
        return reduce(BigInteger::subtract);
      } else if (this.builtIn == BuiltInTable.MUL) {
        return reduce(BigInteger::multiply);
      } else if (this.builtIn == BuiltInTable.MULS) {
        return reduce(BigInteger::multiply);
      } else if (this.builtIn == BuiltInTable.LSL) {
        return reduceWithInt(BigInteger::shiftLeft);
      } else if (this.builtIn == BuiltInTable.LSR) {
        return reduceWithInt(BigInteger::shiftRight);
      }
    }

    return Optional.empty();
  }

  @NotNull
  private Optional<Node> reduce(BiFunction<BigInteger, BigInteger, BigInteger> function) {
    ensure(this.arguments().size() == 2, "Expecting only two inputs");
    // Cast is safe because already checked that is constant.
    var x = (Constant.Value) ((ConstantNode) this.arguments().get(0)).constant;
    var y = (Constant.Value) ((ConstantNode) this.arguments().get(1)).constant;
    ensure(x.type().equals(y.type()), "Types must match");
    return Optional.of(
        new ConstantNode(new Constant.Value(function.apply(x.value(), y.value()), x.type())));
  }

  @NotNull
  private Optional<Node> reduceWithInt(BiFunction<BigInteger, Integer, BigInteger> function) {
    ensure(this.arguments().size() == 2, "Expecting only two inputs");
    // Cast is safe because already checked that is constant.
    var x = (Constant.Value) ((ConstantNode) this.arguments().get(0)).constant;
    var y = (Constant.Value) ((ConstantNode) this.arguments().get(1)).constant;
    return Optional.of(
        new ConstantNode(
            new Constant.Value(function.apply(x.value(), y.value().intValue()), x.type())));
  }
}
