package vadl.vdt.model;

public interface Node {

  <T> T accept(Visitor<T> visitor);

}
