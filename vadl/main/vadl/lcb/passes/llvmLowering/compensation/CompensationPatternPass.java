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

package vadl.lcb.passes.llvmLowering.compensation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.llvmLowering.compensation.strategies.LlvmCompensationPatternStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Not every ISA has all the required instructions. For example, RISC-V has no machine instruction
 * for rotate-left. This pass will detect missing patterns and generate pattern, so they are covered
 * during instruction selection in LLVM.
 */
public class CompensationPatternPass extends Pass {
  private final List<LlvmCompensationPatternStrategy> patternStrategies =
      List.of();

  public CompensationPatternPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("CompensationPatternPass");
  }

  @Nullable
  @Override
  public List<TableGenSelectionWithOutputPattern> execute(PassResults passResults,
                                                          Specification viam) throws IOException {
    var patterns = new ArrayList<TableGenSelectionWithOutputPattern>();
    var database = new Database(passResults, viam);

    for (var strategy : patternStrategies) {
      if (strategy.isApplicable(database)) {
        patterns.addAll(strategy.lower(database, viam));
      }
    }

    return patterns;
  }
}
