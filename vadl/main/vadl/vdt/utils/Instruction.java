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

package vadl.vdt.utils;

/**
 * Represents an instruction in the decode tree.
 *
 * <p>In addition to the underlying source instruction from the VIAM,
 * an instruction is augmented with relevant information for decoding, such as the width of the
 * instruction and the fixed bit pattern that represents the instruction.
 */
public record Instruction(vadl.viam.Instruction source, int width, BitPattern pattern) {

  public static Instruction from(vadl.viam.Instruction insn) {
    BitPattern pattern = PatternUtils.toFixedBitPattern(insn);
    return new Instruction(insn, pattern.width(), pattern);
  }

}
