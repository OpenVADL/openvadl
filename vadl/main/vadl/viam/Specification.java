package vadl.viam;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Specification {

  private final Identifier identifier;
  private final List<Definition> definitions = new ArrayList<>();


  public Specification(Identifier identifier) {
    this.identifier = identifier;
  }

  public Stream<Format> formats() {
    return definitions.stream()
        .filter(Format.class::isInstance)
        .map(Format.class::cast);
  }

  public void add(Definition definition) {
    definitions.add(definition);
  }

  public Identifier identifier() {
    return identifier;
  }
}
