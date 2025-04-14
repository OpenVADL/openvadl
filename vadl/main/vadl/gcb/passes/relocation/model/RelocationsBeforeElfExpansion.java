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
import vadl.gcb.valuetypes.VariantKind;
import vadl.viam.Relocation;

/**
 * Super type logical {@link UserSpecifiedRelocation} and {@link AutomaticallyGeneratedRelocation}.
 * For the {@link UserSpecifiedRelocation}s this is before they are expanded to
 * {@link ImplementedUserSpecifiedRelocation} for each immediate field of a format.
 */
public interface RelocationsBeforeElfExpansion {

  /**
   * Get the modifier of this relocation.
   */
  Modifier modifier();

  /**
   * Get the VariantKind of this relocation.
   */
  VariantKind variantKind();

  /**
   * Get the relocation function of this relocation.
   * Used in {@link vadl.lcb.template.utils.BaseInfoFunctionProvider}
   */
  GcbImmediateExtractionCppFunction valueRelocation();

  /**
   * Get the referenced VIAM relocation of this relocation.
   */
  Relocation relocation();
}
