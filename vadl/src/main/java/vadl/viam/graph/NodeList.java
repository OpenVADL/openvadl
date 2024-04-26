package vadl.viam.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * A list of nodes in the VIAM graph.
 */
public class NodeList<T extends Node> extends ArrayList<T> {
  
  public NodeList() {
  }

  public NodeList(@NotNull Collection<? extends T> c) {
    super(c);
  }

  @SafeVarargs
  public NodeList(@NotNull T... c) {
    this(Arrays.stream(c).toList());
  }
}
