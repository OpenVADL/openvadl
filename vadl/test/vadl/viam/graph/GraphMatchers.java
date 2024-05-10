package vadl.viam.graph;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;


/**
 * The GraphMatchers class is a collection of matcher methods for nodes in a graph.
 */
public class GraphMatchers {

  /**
   * Returns a Matcher object that matches nodes that are active in the given graph.
   *
   * @param graph the graph to check for node activity
   * @return a Matcher object that matches active nodes in the given graph
   */
  public static Matcher<Node> activeIn(Graph graph) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("active in graph ");
      }

      @Override
      protected boolean matchesSafely(Node node) {
        return node.isActiveIn(graph);
      }
    };
  }
}
