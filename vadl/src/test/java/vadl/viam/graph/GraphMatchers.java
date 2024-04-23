package vadl.viam.graph;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class GraphMatchers {

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
