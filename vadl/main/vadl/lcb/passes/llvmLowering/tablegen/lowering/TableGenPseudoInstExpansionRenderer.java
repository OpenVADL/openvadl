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

package vadl.lcb.passes.llvmLowering.tablegen.lowering;

import java.util.stream.Collectors;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstExpansionPattern;

/**
 * This class creates a special tablegen pattern to render pseudo instance without any C++.
 */
public class TableGenPseudoInstExpansionRenderer {
  /**
   * Lowers a pseudo inst.
   */
  public static String lower(
      TableGenPseudoInstExpansionPattern pattern) {
    var out = pattern.outputs().stream().map(TableGenInstructionRenderer::lower).collect(
        Collectors.joining(", "));
    var in = pattern.inputs().stream().map(TableGenInstructionRenderer::lower).collect(
        Collectors.joining(", "));
    var selector = TableGenInstructionPatternRenderer.lowerSelector(pattern.selector());
    var machine = TableGenInstructionPatternRenderer.lowerMachine(pattern.machine());
    if (!pattern.defs().isEmpty()) {
      return String.format("""
              let isCall = %s, isBranch = %s, isIndirectBranch = %s, isTerminator = %s,
              isBarrier = %s, Defs = [%s]
              in
                  def %s : Pseudo<(outs %s), (ins %s),
                                      [%s]>,
                               PseudoInstExpansion<%s>;
              """, pattern.isCall() ? "1" : "0",
          pattern.isBranch() ? "1" : "0",
          pattern.isIndirectBranch() ? "1" : "0",
          pattern.isTerminator() ? "1" : "0",
          pattern.isBarrier() ? "1" : "0",
          pattern.defs().stream().map(RegisterRef::lowerName).collect(Collectors.joining(", ")),
          pattern.name(), out, in, selector, machine);
    } else {
      return String.format("""
              let isCall = %s, isBranch = %s, isIndirectBranch = %s, isTerminator = %s,
              isBarrier = %s
              in
                  def %s : Pseudo<(outs %s), (ins %s),
                                      [%s]>,
                               PseudoInstExpansion<%s>;
              """, pattern.isCall() ? "1" : "0",
          pattern.isBranch() ? "1" : "0",
          pattern.isIndirectBranch() ? "1" : "0",
          pattern.isTerminator() ? "1" : "0",
          pattern.isBarrier() ? "1" : "0",
          pattern.name(), out, in, selector, machine);
    }
  }
}
