package vadl.viam.graph.visualize;

import vadl.viam.graph.Graph;

/**
 * The GraphVisualizer interface defines methods for visualizing a graph.
 *
 * @param <R> the type of the visualization result
 * @param <G> the type of the graph to visualize
 */
public interface GraphVisualizer<R, G extends Graph> {

  /**
   * Loads a graph into the GraphVisualizer.
   *
   * @param graph the graph to load into the GraphVisualizer
   * @return the GraphVisualizer instance
   */
  GraphVisualizer<R, G> load(G graph);

  /**
   * Generates a visualization of the graph.
   *
   * @return the visualization result
   */
  R visualize();
}
