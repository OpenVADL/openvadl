package vadl.lcb.passes.llvmLowering.tablegen.model;

import static vadl.viam.ViamError.ensurePresent;

import java.util.Objects;
import vadl.cppCodeGen.model.VariantKind;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.gcb.passes.relocation.model.Fixup;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCCodeEmitterCppFilePass;
import vadl.types.BitsType;
import vadl.types.Type;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * Represents an immediate record in TableGen.
 */
public class TableGenImmediateRecord {
  private final String name;
  // The `encoderMethod` will be used by tablegen and has different arguments.
  private final Identifier encoderMethod;
  // The `rawEncoderMethod` is the method for the raw logic.
  private final Identifier rawEncoderMethod;
  private final Identifier decoderMethod;
  private final Identifier predicateMethod;
  private final ValueType type;
  private final BitsType originalType;
  private final Format.FieldAccess fieldAccessRef;
  private final VariantKind variantKind;

  /**
   * Constructor for an immediate operand.
   */
  private TableGenImmediateRecord(Identifier identifier,
                                  Identifier encoderIdentifier,
                                  Identifier rawEncoderIdentifier,
                                  Identifier decoderIdentifier,
                                  Identifier predicateIdentifier,
                                  ValueType type,
                                  Format.FieldAccess fieldAccessRef) {
    this.name = identifier.lower();
    this.encoderMethod = encoderIdentifier;
    this.rawEncoderMethod = rawEncoderIdentifier;
    this.decoderMethod = decoderIdentifier;
    this.predicateMethod = predicateIdentifier;
    this.type = type;
    this.originalType = (BitsType) fieldAccessRef.type();
    this.fieldAccessRef = fieldAccessRef;
    this.variantKind = new VariantKind(fieldAccessRef.fieldRef());
  }

  /**
   * Constructor.
   */
  public TableGenImmediateRecord(Format.FieldAccess fieldAccess,
                                 ValueType llvmType) {
    this(fieldAccess.fieldRef().identifier,
        Objects.requireNonNull(fieldAccess.encoding()).identifier.append(
            EmitMCCodeEmitterCppFilePass.WRAPPER),
        Objects.requireNonNull(fieldAccess.encoding()).identifier,
        fieldAccess.accessFunction().identifier.append(EmitDisassemblerCppFilePass.WRAPPER),
        fieldAccess.predicate().identifier,
        llvmType,
        fieldAccess);
  }

  public String rawName() {
    return name;
  }

  public String encoderMethod() {
    return encoderMethod.lower();
  }

  public String rawEncoderMethod() {
    return rawEncoderMethod.lower();
  }

  public String decoderMethod() {
    return decoderMethod.lower();
  }

  public ValueType type() {
    return type;
  }

  public String fullname() {
    return String.format("%sAs%s", this.name, type.getTableGen());
  }

  public String predicateMethod() {
    return predicateMethod.lower();
  }

  public BitsType originalType() {
    return originalType;
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