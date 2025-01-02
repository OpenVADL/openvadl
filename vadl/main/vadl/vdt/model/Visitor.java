package vadl.vdt.model;

public interface Visitor<T> {

  default T visit(InnerNode node) {
    // Do nothing by default
    return null;
  }

  default T visit(LeafNode node) {
    // Do nothing by default
    return null;
  }

}
