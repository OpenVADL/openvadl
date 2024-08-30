package vadl.viam.graph;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A iterator that is used to iterate over nodes in a graph.
 */
public interface NodeIter<T> extends Iterator<T> {

  /**
   * This iterator iterates over a snapshot of the graph.
   * This means it will only iterate over nodes that are in the graph at the time of
   * creating the iterator.
   * So if nodes are added during iteration, those nodes are not getting iterated.
   * However, if nodes are getting deleted during the iteration and were not yet iterated,
   * they will never be iterated.
   */
  class SnapshotIter implements NodeIter<Node> {

    private final int sizeAtCreation;
    protected int currentIndex;
    protected final Graph graph;

    public SnapshotIter(Graph graph) {
      this.graph = graph;
      sizeAtCreation = graph.nodes.size();
    }

    @Override
    public boolean hasNext() {
      while (currentIndex < sizeAtCreation && graph.nodes.get(currentIndex) == null) {
        currentIndex++;  // Skip null entries
      }
      return currentIndex < sizeAtCreation;
    }

    @Override
    public Node next() {
      if (currentIndex >= sizeAtCreation) {
        throw new NoSuchElementException("No more nodes available");
      }
      Node node = graph.nodes.get(currentIndex);
      currentIndex++;  // Move to the next index for future calls
      return node;
    }
  }
}
