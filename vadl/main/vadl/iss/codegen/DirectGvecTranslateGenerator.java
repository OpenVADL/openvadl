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
import vadl.iss.passes.extensions.InstrExecPlan.StrategyKind;
import vadl.viam.Instruction;

/**
 * Temporary translate generator for selected direct-gvec plans.
 *
 * <p>Until PR 4 emits direct gvec calls, vector instructions selected as
 * {@link StrategyKind#DIRECT_GVEC} still reuse the helper-call wrapper. Keeping the generator
 * separate makes the emission strategy seam explicit and avoids mixing future gvec logic into the
 * helper path.</p>
 *
 * <p>This class exists so the translate/codegen layer already has a dedicated vector strategy
 * entry point. When the real direct-gvec emission work lands, it can grow here without reworking
 * dispatch or conflating vector-specific code with the generic helper wrapper.</p>
 */
class DirectGvecTranslateGenerator extends HelperCallTranslateGenerator {

  DirectGvecTranslateGenerator(Instruction instr,
                               IssConfiguration configuration) {
    super(instr, configuration);
  }
}
