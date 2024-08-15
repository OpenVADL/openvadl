package vadl.lcb.tablegen.model;

import vadl.lcb.codegen.DecodingCodeGenerator;
import vadl.lcb.codegen.EncodingCodeGenerator;
import vadl.lcb.codegen.model.llvm.ValueType;

/**
 * Represents an immediate in TableGen.
 */
public class TableGenImmediateOperand extends TableGenClass {
  private final String name;
  private final String encoderMethod;
  private final String decoderMethod;
  private final ValueType type;

  /**
   * Constructor for an immediate operand.
   */
  public TableGenImmediateOperand(String rawName,
                                  ValueType type) {
    this.name = rawName;
    this.encoderMethod = EncodingCodeGenerator.getFunctionPrefix() + rawName;
    this.decoderMethod = DecodingCodeGenerator.getFunctionPrefix() + rawName;
    this.type = type;
  }

  public String getRawName() {
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

  public String getFullName() {
    return String.format("%sAs%s", this.name, type.getFancyName());
  }
}