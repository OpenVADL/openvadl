package vadl.viam;

import vadl.types.Type;

public class Immediate extends Definition {

  private final Type type;
  private final Function decoding;
  private final Function encoding;
  private final Function predicate;

  public Immediate(Identifier identifier,
                   Function decoding,
                   Function encoding,
                   Function predicate) {
    super(identifier);
    this.type = decoding.returnType();
    this.decoding = decoding;
    this.encoding = encoding;
    this.predicate = predicate;
  }

  public Type type() {
    return type;
  }

  public Function decoding() {
    return decoding;
  }

  public Function encoding() {
    return encoding;
  }

  public Function predicate() {
    return predicate;
  }
}
