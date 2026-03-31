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

package vadl.lcb.passes.llvmLowering.compensation.strategies;

import java.util.Collection;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.viam.Specification;

/**
 * Defines a strategy how to generate a compensation pattern.
 */
public interface LlvmCompensationPatternStrategy {
  /**
   * Checks whether the strategy has to be applied.
   * It runs the returned query on the {@link Database} and if no result exists then
   * the strategy is applicable.
   */
  boolean isApplicable(Database database);

  /**
   * Generates a pattern with this strategy.
   */
  Collection<TableGenSelectionWithOutputPattern> lower(Database database, Specification viam);
}
