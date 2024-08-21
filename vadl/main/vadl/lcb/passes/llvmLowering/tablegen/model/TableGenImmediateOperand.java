package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.Objects;
import vadl.lcb.codegen.DecodingCodeGenerator;
import vadl.lcb.codegen.EncodingCodeGenerator;
import vadl.lcb.codegen.PredicateCodeGenerator;
import vadl.lcb.codegen.model.llvm.ValueType;

/**
 * Represents an immediate in TableGen.
 */
public class TableGenImmediateOperand {
  private final String name;
  private final String encoderMethod;
  private final String decoderMethod;
  private final String predicateMethod;
  private final ValueType type;

  /**
   * Constructor for an immediate operand.
   */
  public TableGenImmediateOperand(String rawName,
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
    TableGenImmediateOperand that = (TableGenImmediateOperand) o;
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