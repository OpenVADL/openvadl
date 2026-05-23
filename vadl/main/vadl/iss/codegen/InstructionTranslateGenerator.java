// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.codegen;

/**
 * Common interface for instruction translation emitters.
 *
 * <p>Each implementation renders one complete {@code trans_<instr>} function for QEMU ISS
 * generation. The dispatcher in {@link IssTranslateCodeGenerator} selects an implementation from
 * the instruction's execution plan, so the code-generation strategy stays aligned with the planning
 * pass structure.</p>
 */
interface InstructionTranslateGenerator {

  /**
   * Renders the QEMU translate function for one instruction.
   */
  String fetch();
}
