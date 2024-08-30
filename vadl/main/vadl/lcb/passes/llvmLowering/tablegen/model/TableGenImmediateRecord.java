package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.Objects;
import vadl.lcb.codegen.encoding.DecodingCodeGenerator;
import vadl.lcb.codegen.encoding.EncodingCodeGenerator;
import vadl.lcb.codegen.encoding.PredicateCodeGenerator;
import vadl.lcb.codegen.model.llvm.ValueType;

/**
 * Represents an immediate record in TableGen.
 */
public class TableGenImmediateRecord {
  private final String name;
  private final String encoderMethod;
  private final String decoderMethod;
  private final String predicateMethod;
  private final ValueType type;

  /**
   * Constructor for an immediate operand.
   */
  public TableGenImmediateRecord(String rawName,
                                 ValueType type) {
    this.name = rawName;
    this.encoderMethod = EncodingCodeGenerator.generateFunctionName(rawName);
    this.decoderMethod = DecodingCodeGenerator.generateFunctionName(rawName);
    this.predicateMethod = PredicateCodeGenerator.generateFunctionName(rawName);
    this.type = type;
  }

  public String rawName() {
    return name;
  }

  public String encoderMethod() {
    return encoderMethod;
  }

  public String decoderMethod() {
    return decoderMethod;
  }

  public ValueType type() {
    return type;
  }

  public String fullname() {
    return String.format("%sAs%s", this.name, type.getFancyName());
  }


  public String predicateMethod() {
    return predicateMethod;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TableGenImmediateRecord that = (TableGenImmediateRecord) o;
    return Objects.equals(name, that.name)
        && Objects.equals(encoderMethod, that.encoderMethod)
        && Objects.equals(decoderMethod, that.decoderMethod)
        && Objects.equals(predicateMethod, that.predicateMethod)
        && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, encoderMethod, decoderMethod, predicateMethod, type);
  }
}