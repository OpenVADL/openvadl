package vadl.vdt.model;

import javax.annotation.Nullable;

public interface Visitor<T> {

  default @Nullable T visit(InnerNode node) {
    // Do nothing by default
    return null;
  }

  default @Nullable T visit(LeafNode node) {
    // Do nothing by default
    return null;
  }

}
