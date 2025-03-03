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

import vadl.cppCodeGen.model.GcbImmediateExtractionCppFunction;
import vadl.cppCodeGen.model.GcbUpdateFieldRelocationCppFunction;
import vadl.gcb.valuetypes.VariantKind;
import vadl.viam.Format;
import vadl.viam.Relocation;

/**
 * A concrete logical relocation is like a logical relocation (lo, hi) but it already
 * has the cpp functions for the compiler backend.
 */
public class ImplementedUserSpecifiedRelocation extends UserSpecifiedRelocation
    implements HasRelocationComputationAndUpdate {
  protected final VariantKind variantKind;
  protected final Modifier modifier;

  // This is the function which computes the value for the
  // relocation.
  protected final GcbImmediateExtractionCppFunction valueRelocation;
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
    super(format, field, originalRelocation);
    this.variantKind = variantKind;
    this.modifier = modifier;
    this.valueRelocation = valueRelocation;
    this.fieldUpdateFunction = fieldUpdateFunction;
  }

  @Override
  public VariantKind variantKind() {
    return variantKind;
  }

  public Modifier modifier() {
    return modifier;
  }

  @Override
  public GcbImmediateExtractionCppFunction valueRelocation() {
    return valueRelocation;
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
            + "_" + immediate.identifier.simpleName()
    );
  }
}
