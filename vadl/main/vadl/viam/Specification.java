package vadl.viam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Represents a VIAM VADL specification.
 */
public class Specification {

  private final Identifier identifier;
  private final List<Definition> definitions = new ArrayList<>();


  public Specification(Identifier identifier) {
    this.identifier = identifier;
  }

  /**
   * Returns all global format definitions as stream.
   */
  public Stream<Format> formats() {
    return definitions.stream()
        .filter(Format.class::isInstance)
        .map(Format.class::cast);
  }

  public void add(Definition definition) {
    definitions.add(definition);
  }

  public void addAll(Collection<Definition> definition) {
    definitions.addAll(definition);
  }

  public Identifier identifier() {
    return identifier;
  }
}
