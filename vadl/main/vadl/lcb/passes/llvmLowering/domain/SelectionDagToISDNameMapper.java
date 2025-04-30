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

package vadl.lcb.passes.llvmLowering.domain;

import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBSwapSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmRotlSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmRotrSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmShlPartsSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSraPartsSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSrlPartsSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUMulLoHiSD;
import vadl.utils.SourceLocation;

/**
 * This class maps {@link LlvmNodeLowerable} to their LLVM ISD node name.
 */
public class SelectionDagToISDNameMapper {
  /**
   * Map a class in our domain model to an ISD in LLVM.
   *
   * <pre>
   *     setOperationAction(ISD::ROTR, MVT::i32, Expand);
   * </pre>
   */
  public static String map(Class<LlvmNodeLowerable> nodeLowerable) {
    if (nodeLowerable.isAssignableFrom(LlvmRotlSD.class)) {
      return "ROTL";
    } else if (nodeLowerable.isAssignableFrom(LlvmRotrSD.class)) {
      return "ROTR";
    } else if (nodeLowerable.isAssignableFrom(LlvmBSwapSD.class)) {
      return "BSWAP";
    } else if (nodeLowerable.isAssignableFrom(LlvmShlPartsSD.class)) {
      return "SHL_PARTS";
    } else if (nodeLowerable.isAssignableFrom(LlvmSrlPartsSD.class)) {
      return "SRL_PARTS";
    } else if (nodeLowerable.isAssignableFrom(LlvmSraPartsSD.class)) {
      return "SRA_PARTS";
    } else if (nodeLowerable.isAssignableFrom(LlvmUMulLoHiSD.class)) {
      return "UMUL_LOHI";
    }

    throw Diagnostic.error("Cannot map to class name", SourceLocation.INVALID_SOURCE_LOCATION)
        .build();
  }
}
