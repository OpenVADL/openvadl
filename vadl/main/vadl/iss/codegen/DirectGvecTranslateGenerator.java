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

import vadl.configuration.IssConfiguration;
import vadl.viam.Instruction;

/**
 * Translate generator for lowered direct-gvec plans.
 *
 * <p>Direct-gvec planning now lowers eligible vector loop regions into backend gvec nodes inside
 * the instruction graph. This generator therefore reuses the non-helper graph renderer while
 * preserving a dedicated vector strategy entry point in the translate dispatch.</p>
 */
class DirectGvecTranslateGenerator extends ScalarTcgTranslateGenerator {

  DirectGvecTranslateGenerator(Instruction instr,
                               IssConfiguration configuration) {
    super(instr, configuration);
  }
}
