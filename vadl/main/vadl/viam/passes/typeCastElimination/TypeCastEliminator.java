package vadl.viam.passes.typeCastElimination;

import java.util.HashMap;
import org.jetbrains.annotations.Nullable;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.passes.GraphProcessor;

/**
 * The type cast eliminator handles the complete elimination of all
 * {@link TypeCastNode}s in a behavior graph.
 * Currently, it will fail for type casts to and from types other than {@link vadl.types.DataType}
 * types.
 *
 * <p>The following rules define the replacement of such a type cast.
 * The rules are checked in the following order, and the first matching rule is
 * applied, while no further rule will be checked.
 * So the list can be seen as {@code else if} statements.
 * If the target type ...
 * <ol>
 * <li>has the <b>same bit representation</b> as the source type,
 * the type cast is removed without a replacement. However, the result type of the source node
 * is changed to the target type. See {@link DataType#isTrivialCastTo(Type)}
 * for an more concrete definition of nodes with <i>same bit representations</i>.</li>
 * <li>has a <b>smaller bit-width</b> than the source type,
 * the type cast is replaced by a {@link TruncateNode}. The result type of the new node
 * will have the same type as the target type.</li>
 * <li>is a signed integer ({@code SInt}) the node will be replaced by
 * a {@link SignExtendNode}.</li>
 * <li>is a unsigned integer, signed integer or bits ({@code SInt, Bits}),
 * the node will be replaced by a {@link ZeroExtendNode}.</li>
 * </ol>
 * A complete discussion can be found on
 * <a href="https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/93">open-vadl#93</a>.
 *
 * <p>If non of the rules matches, an error is thrown.</p>
 */
// TODO: @jzottele revisit when https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/93 is resolved
public class TypeCastEliminator extends GraphProcessor {

  /**
   * Runs the type cast eliminator on the whole graph.
   * The given graph instance will be manipulated.
   */
  public static void runOnGraph(Graph graph) {
    new TypeCastEliminator().processGraph(graph,
        // only get nodes that are not used (root nodes)
        node -> node.usageCount() == 0
    );
  }

  /**
   * Runs the eliminator on a subgraph, where root defines the start of the subgraph.
   * It will process all dependencies/inputs of the root.
   * The graph of root will be manipulated.
   */
  public static Node runOnSubgraph(Node root) {
    return new TypeCastEliminator().processNode(root);
  }

  /**
   * Eliminates the single type cast node.
   * It will return the replacement (if there is any).
   * This operation will manipulate the graph of node.
   */
  @Nullable
  public static ExpressionNode eliminate(TypeCastNode node) {
    return new TypeCastEliminator().eliminateTypeCast(node);
  }

  /**
   * This is the logic method that actually eliminates the type cast.
   * Documentation of the rules can be found at the class documentation {@link TypeCastEliminator}.
   *
   * @return the replacement of the type case node. May be null if the cast node was deleted
   *     without a replacement.
   */
  @Nullable
  private ExpressionNode eliminateTypeCast(TypeCastNode castNode) {
    castNode.ensure(castNode.isActive(), "internal: Type cast node is not an active node.");
    castNode.ensure(castNode.castType() instanceof DataType,
        "Currently only casts of data types are supported.");

    ExpressionNode source = castNode.value();

    source.ensure(source.usages().toList().contains(castNode),
        "internal: Cast node is not a usage.");
    source.ensure(source.isActiveIn(castNode.graph()),
        "internal: Input node is not an active node.");
    source.ensure(source.type() instanceof DataType,
        "Currently only casts from data types are supported");

    var graph = castNode.graph();
    var castType = (DataType) castNode.type();
    var inputType = (DataType) source.type();

    ExpressionNode replacement = null;

    // check the different rules and apply them accordingly
    if (inputType.isTrivialCastTo(castType)) {
      // match 1. rule: same bit representation
      // -> set the cast type as type of the source node
      source.setType(castType);
      // remove the node and remap edges
      castNode.replaceByNothingAndDelete();
      // no new node was created
      replacement = null;

    } else if (castType.bitWidth() < inputType.bitWidth()) {
      // match 2. rule: cast type bit-width is smaller than source type
      // -> create TruncateNode and add it
      var truncateNode = new TruncateNode(source, castType);
      replacement = (TruncateNode) castNode.replaceAndDelete(truncateNode);

    } else if (castType.getClass() == SIntType.class) {
      // match 3.
      // rule: cast type is a signed integer and input type is either sint or bits
      // -> create sign extend node
      var signExtendNode = new SignExtendNode(source, castType);
      replacement = (SignExtendNode) castNode.replaceAndDelete(signExtendNode);

    } else if (castType.getClass() == UIntType.class
        || castType.getClass() == BitsType.class) {
      // match 4. rule: cast type is one of sint, uint, or bits
      var zeroExtendNode = new ZeroExtendNode(source, castType);
      replacement = (ZeroExtendNode) castNode.replaceAndDelete(zeroExtendNode);

    } else {
      throw new ViamGraphError("Could not handle type cast, as non of the rules apply.")
          .addLocation(castNode.sourceLocation())
          .addContext(castNode)
          .addContext(graph)
          .addContext("input", source);
    }

    return replacement;
  }

  @Override
  protected Node processUnprocessedNode(Node toProcess) {
    // visit all inputs first, so type casts near to leaves
    // are eliminated earlier
    toProcess.visitInputs(this);

    if (toProcess instanceof TypeCastNode typeCastNode) {
      // backup the input
      var input = typeCastNode.value();

      // now we eliminate this type cast node
      var resultNode = eliminateTypeCast(typeCastNode);

      // if the type cast resulted in a deletion without a replacement
      // we use the input node as result
      return resultNode != null ? resultNode : input;
    }

    return toProcess;
  }

}
