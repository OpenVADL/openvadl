package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.Objects;
import vadl.cppCodeGen.model.VariantKind;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.viam.Format;
import vadl.viam.Identifier;

/**
 * Represents an immediate record in TableGen.
 */
public class TableGenImmediateRecord {
  private final String name;
  private final Identifier encoderMethod;
  private final Identifier decoderMethod;
  private final Identifier predicateMethod;
  private final ValueType type;
  private final Format.FieldAccess fieldAccessRef;
  private final VariantKind variantKind;

  /**
   * Constructor for an immediate operand.
   */
  public TableGenImmediateRecord(Identifier identifier,
                                 Identifier encoderIdentifier,
                                 Identifier decoderIdentifier,
                                 Identifier predicateIdentifier,
                                 ValueType type,
                                 Format.FieldAccess fieldAccessRef) {
    this.name = identifier.lower();
    this.encoderMethod = encoderIdentifier;
    this.decoderMethod = decoderIdentifier;
    this.predicateMethod = predicateIdentifier;
    this.type = type;
    this.fieldAccessRef = fieldAccessRef;
    this.variantKind = new VariantKind(fieldAccessRef.fieldRef());
  }

  public String rawName() {
    return name;
  }

  public String encoderMethod() {
    return encoderMethod.lower();
  }

  public String decoderMethod() {
    return decoderMethod.lower();
  }

  public ValueType type() {
    return type;
  }

  public String fullname() {
    return String.format("%sAs%s", this.name, type.getFancyName());
  }


  public String predicateMethod() {
    return predicateMethod.lower();
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

  public Format.FieldAccess fieldAccessRef() {
    return fieldAccessRef;
  }

  public VariantKind variantKind() {
    return variantKind;
  }
}