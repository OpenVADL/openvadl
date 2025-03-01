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
import vadl.viam.Identifier;
import vadl.viam.Relocation;

/**
 * The {@link Fixup} requires already implemented functions. But there are two kinds which are
 * relevant: {@link ImplementedUserSpecifiedRelocation} and
 * {@link AutomaticallyGeneratedRelocation}.
 */
public interface HasRelocationComputationAndUpdate {

  /**
   * Get the identifier of the function.
   */
  Identifier identifier();

  /**
   * Get the {@link VariantKind} for the relocation.
   */
  VariantKind variantKind();

  /**
   * Get the {@link Format} on which the relocation should be applied on.
   */
  Format format();

  /**
   * Get the relocation.
   */
  Relocation relocation();

  /**
   * Get the cpp function for changing a value for a relocation.
   */
  GcbImmediateExtractionCppFunction valueRelocation();

  /**
   * Get the cpp function for updating a field in a format.
   */
  GcbUpdateFieldRelocationCppFunction fieldUpdateFunction();

  /**
   * Generates and returns the name of the ELF relocation.
   */
  ElfRelocationName elfRelocationName();
}
