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

package vadl.lcb.passes.llvmLowering;

/**
 * This is a marker trait that the side effect is also part of the pattern.
 * The normal patterns don't require any output because they are already
 * defined in the instruction.
 * {@code def : Pat<(add X:$rs1, X:$rs2),
 * (ADD X:$rs1, X:$rs2)>;}
 * However, stores require an output like {@code trunstorei8}.
 * {@code def : Pat<(truncstorei8 X:$rs2, (add X:$rs1,
 * RV32I_Stype_ImmediateS_immediateAsInt32:$imm)),
 * (SB X:$rs1, X:$rs2, RV32I_Stype_ImmediateS_immediateAsInt32:$imm)>;}
 * This interfaces defines that {@code truncstorei8} is also part of the pattern and added to
 * the pattern graph.
 */
public interface LlvmSideEffectPatternIncluded {
}
