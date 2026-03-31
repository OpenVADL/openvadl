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

package vadl.gcb.passes;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Specification;
import vadl.viam.passes.functionInliner.Inliner;

/**
 * Inline functions that are used in {@link Format.FieldAccess#predicate()}.
 */
public class PredicateFunctionInlinerPass extends Pass {
  /**
   * Constructor.
   */
  public PredicateFunctionInlinerPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("PredicateFunctionInlinerPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    for (var formats : viam.isa().orElseThrow().ownFormats()) {
      for (var fieldAccess : formats.fieldAccesses()) {
        if (fieldAccess.predicate() != null) {
          Inliner.inlineFuncs(fieldAccess.predicate().behavior());
        }
      }
    }

    return null;
  }
}
