package vadl.viam.graph;

import org.junit.jupiter.api.Test;
import vadl.viam.graph.helper.TestNodes;
import vadl.viam.graph.visualize.DotGraphVisualizer;

public class VisualizationTest {

  @Test
  void demoVisualization() {
    var graph = new Graph("demoGraph");

    var i1 = graph.add(new TestNodes.WithData(1));
    var i2 = graph.add(new TestNodes.WithData(2));
    var a = graph.add(new TestNodes.WithTwoInputs(i1, i2));

    var visualizer = new DotGraphVisualizer()
        .load(graph);

    System.out.println(visualizer.visualize());

  }
}
