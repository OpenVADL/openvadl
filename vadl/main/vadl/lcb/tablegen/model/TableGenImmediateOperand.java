package vadl.lcb.tablegen.model;

/**
 * Represents an immediate in TableGen.
 */
public class TableGenImmediateOperand extends TableGenClass {
  private final String name;
  private final String encoderMethod;
  private final String decoderMethod;

  public TableGenImmediateOperand(String name, String encoderMethod, String decoderMethod) {
    this.name = name;
    this.encoderMethod = encoderMethod;
    this.decoderMethod = decoderMethod;
  }

  public String getName() {
    return name;
  }

  public String getEncoderMethod() {
    return encoderMethod;
  }

  public String getDecoderMethod() {
    return decoderMethod;
  }
}