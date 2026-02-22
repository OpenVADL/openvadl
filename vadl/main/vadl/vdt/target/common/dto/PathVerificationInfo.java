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

package vadl.vdt.target.common.dto;

import com.microsoft.z3.BoolExpr;
import vadl.vdt.target.common.DecisionTreeSoundnessVerifier;
import vadl.vdt.utils.Instruction;

/**
 * Encapsulates information about a path in the decode tree, used within the soundness verification
 * condition generator {@link DecisionTreeSoundnessVerifier}.
 *
 * @param leaf          The leaf of this decode path.
 * @param leafCondition The condition to select the path conditions.
 */
public record PathVerificationInfo(Instruction leaf, BoolExpr leafCondition) {
}
