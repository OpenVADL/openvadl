package vadl.viam.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vadl.types.Type;
import vadl.types.TypeList;
import vadl.viam.graph.dependency.ExpressionNode;

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

  public @Nullable TypeList<Type> toTypeList() {
    var result = new TypeList<Type>(this.size());
    for (var node : this) {
      if (!(node instanceof ExpressionNode)) {
        return null;
      }
      result.add(((ExpressionNode) node).type());
    }
    return result;
  }
}
