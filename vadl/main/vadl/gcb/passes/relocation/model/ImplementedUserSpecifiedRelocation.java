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

package vadl.gcb.passes.relocation.model;

import static vadl.viam.ViamError.ensure;

import javax.annotation.Nullable;
import vadl.cppCodeGen.model.GcbImmediateExtractionCppFunction;
import vadl.cppCodeGen.model.GcbUpdateFieldRelocationCppFunction;
import vadl.gcb.valuetypes.VariantKind;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Relocation;

/**
 * A concrete logical relocation is like a logical relocation (lo, hi) but it already
 * has the cpp functions for the compiler backend.
 */
public class ImplementedUserSpecifiedRelocation extends UserSpecifiedRelocation
    implements HasRelocationComputationAndUpdate {

  protected final Format format;
  protected final Format.Field field;

  @Nullable
  protected Fixup fixup;

  // This is the function which updates the value in the format.
  protected final GcbUpdateFieldRelocationCppFunction fieldUpdateFunction;

  /**
   * Constructor.
   */
  public ImplementedUserSpecifiedRelocation(Relocation originalRelocation,
                                            VariantKind variantKind,
                                            Modifier modifier,
                                            GcbImmediateExtractionCppFunction valueRelocation,
                                            Format format,
                                            Format.Field field,
                                            GcbUpdateFieldRelocationCppFunction
                                                fieldUpdateFunction) {
    super(generateIdentifier(format, field,
            CompilerRelocation.Kind.fromRelocationKind(originalRelocation.kind())),
        modifier, variantKind, valueRelocation, originalRelocation);
    this.format = format;
    this.field = field;
    this.fieldUpdateFunction = fieldUpdateFunction;
  }

  private static Identifier generateIdentifier(Format format, Format.Field imm, Kind kind) {
    return format.identifier.append(kind.name(), imm.identifier.simpleName());
  }

  @Override
  public VariantKind variantKind() {
    return variantKind;
  }

  @Override
  public Format format() {
    return format;
  }

  @Override
  public Modifier modifier() {
    return modifier;
  }

  @Override
  public GcbImmediateExtractionCppFunction valueRelocation() {
    return valueRelocation;
  }

  @Override
  public Relocation relocation() {
    return relocationRef;
  }


  /**
   * Set fixup for this relocation.
   * Cannot be done in constructor because fixup and
   * {@link HasRelocationComputationAndUpdate} reference each other.
   */
  public void setFixup(Fixup fixup) {
    this.fixup = fixup;
  }

  @Override
  public Fixup fixup() {
    ensure(fixup != null,
        "Fixup must be set before calling fixup()");
    return fixup;
  }

  @Override
  public Format.Field field() {
    return field;
  }

  @Override
  public GcbUpdateFieldRelocationCppFunction fieldUpdateFunction() {
    return fieldUpdateFunction;
  }

  @Override
  public ElfRelocationName elfRelocationName() {
    return new ElfRelocationName(
        "R_" + relocation().identifier.lower() + "_"
            + format.identifier.simpleName()
            + "_" + field.identifier.simpleName()
    );
  }

  @Override
  public String llvmKind() {
    return kind.llvmKind();
  }
}
