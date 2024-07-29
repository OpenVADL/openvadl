package vadl.viam.graph;

/**
 * Marks nodes that provide a canonical form via the {@link Canonicalizable#canonical()} method.
 */
public interface Canonicalizable {

  /**
   * Returns the canonical form of itself.
   *
   * <p>The method implementation...
   * <ul>
   * <li>must have no side effects (e.g. mutation of inputs, data and successors)</li>
   * <li>must not modify the graph in any way (add or delete nodes)</li>
   * <li>may return a new uninitialized node that must be added to the graph by the
   * {@link Canonicalizer}</li>
   * <li>may return {@code this} or already existing nodes (active in same graph)</li>
   * </ul>
   *
   * <p>The canonical form includes constant evaluation, constant folding, operand reordering,...
   */
  Node canonical();

}
