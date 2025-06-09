// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.Objects;
import vadl.gcb.valuetypes.VariantKind;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCCodeEmitterCppFilePass;
import vadl.types.BitsType;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.PrintableInstruction;

/**
 * Represents an immediate record in TableGen.
 */
public class TableGenImmediateRecord {
  private final String rawName;
  // The `encoderMethod` will be used by tablegen and has different arguments.
  private final Identifier encoderMethod;
  // The `rawEncoderMethod` is the method for the raw logic.
  private final Identifier rawEncoderMethod;
  private final Identifier decoderMethod;
  private final Identifier rawDecoderMethod;
  private final Identifier predicateMethod;
  private final ValueType llvmType;
  private final BitsType rawType;
  private final int formatFieldBitSize;
  private final Format.FieldAccess fieldAccessRef;
  private final VariantKind absoluteVariantKind;
  private final VariantKind relativeVariantKind;

  private final PrintableInstruction instructionRef;

  /**
   * Constructor.
   */
  public TableGenImmediateRecord(
      PrintableInstruction instruction,
      Format.FieldAccess fieldAccess,
      ValueType llvmType) {
    this.instructionRef = instruction;
    var decodingIdentifier =
        Objects.requireNonNull(fieldAccess).accessFunction().identifier.dropLast().last();
    var predicateIdentifier = fieldAccess.predicate().identifier.last();
    this.rawName = fieldAccess.identifier.last().prepend(instruction.identifier()).lower();
    this.rawEncoderMethod = instruction.identifier();
    this.encoderMethod = rawEncoderMethod.append(EmitMCCodeEmitterCppFilePass.WRAPPER);
    this.rawDecoderMethod = decodingIdentifier.prepend(instruction.identifier()).append("decode");
    this.decoderMethod = rawDecoderMethod.append(EmitDisassemblerCppFilePass.WRAPPER);
    this.predicateMethod = predicateIdentifier.prepend(instruction.identifier());
    this.llvmType = llvmType;
    this.fieldAccessRef = fieldAccess;
    this.absoluteVariantKind = VariantKind.absolute(fieldAccessRef.fieldRef());
    this.relativeVariantKind = VariantKind.relative(fieldAccess.fieldRef());
    this.rawType = (BitsType) fieldAccessRef.type();
    this.formatFieldBitSize = fieldAccessRef.fieldRef().size();
  }

  public PrintableInstruction instructionRef() {
    return instructionRef;
  }

  public String rawName() {
    return rawName;
  }

  public Identifier encoderMethod() {
    return encoderMethod;
  }

  public Identifier rawEncoderMethod() {
    return rawEncoderMethod;
  }

  public Identifier decoderMethod() {
    return decoderMethod;
  }

  public Identifier rawDecoderMethod() {
    return rawDecoderMethod;
  }

  public ValueType llvmType() {
    return llvmType;
  }

  public BitsType rawType() {
    return rawType;
  }

  public String fullname() {
    return String.format("%sAs%s", this.rawName, llvmType.getTableGen());
  }

  public Identifier predicateMethod() {
    return predicateMethod;
  }

  public int formatFieldBitSize() {
    return formatFieldBitSize;
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
    return Objects.equals(rawName, that.rawName)
        && Objects.equals(encoderMethod, that.encoderMethod)
        && Objects.equals(decoderMethod, that.decoderMethod)
        && Objects.equals(predicateMethod, that.predicateMethod)
        && llvmType == that.llvmType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawName, encoderMethod, decoderMethod, predicateMethod, llvmType);
  }

  public Format.FieldAccess fieldAccessRef() {
    return fieldAccessRef;
  }

  public VariantKind absoluteVariantKind() {
    return absoluteVariantKind;
  }

  public VariantKind relativeVariantKind() {
    return relativeVariantKind;
  }
}