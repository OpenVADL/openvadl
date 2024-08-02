package vadl.lcb.tablegen.model;

import vadl.lcb.codegen.model.llvm.ValueType;

/**
 * Represents an immediate in TableGen.
 */
public class TableGenImmediateOperand extends TableGenClass {
  private final String name;
  private final String encoderMethod;
  private final String decoderMethod;
  private final ValueType type;

  public TableGenImmediateOperand(String name, String encoderMethod, String decoderMethod,
                                  ValueType type) {
    this.name = name;
    this.encoderMethod = encoderMethod;
    this.decoderMethod = decoderMethod;
    this.type = type;
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

  public ValueType getType() {
    return type;
  }
}