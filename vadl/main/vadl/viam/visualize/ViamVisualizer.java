package vadl.viam.visualize;

/**
 * The ViamVisualizer interface represents a visualizer that generates a visualization of
 * a certain type.
 * The visualization can be used to represent an instance of the VIAM.
 *
 * @param <R> The type of the visualization result.
 */
public interface ViamVisualizer<R> {

  R visualize();

}
