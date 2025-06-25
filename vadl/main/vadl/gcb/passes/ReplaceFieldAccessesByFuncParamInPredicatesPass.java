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
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Format.FieldAccess;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * Replaces {@link FieldAccessRefNode} by {@link FuncParamNode} for the predicate function code
 * generator. If it encounters a {@link FieldAccessRefNode#fieldAccess()} in the
 * {@link FieldAccess#predicate()} which is not {@link FieldAccess} on the LHS then throw
 * an error. Generally, predicates allow only one {@link FieldAccess}.
 */
public class ReplaceFieldAccessesByFuncParamInPredicatesPass extends Pass {
  /**
   * Constructor.
   */
  public ReplaceFieldAccessesByFuncParamInPredicatesPass(GeneralConfiguration configuration) {
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
          var candidates =
              fieldAccess.predicate().behavior().getNodes(FieldAccessRefNode.class).toList();
          for (var candidate : candidates) {
            if (!candidate.fieldAccess().equals(fieldAccess)) {
              throw Diagnostic.error(
                      "It is not allowed to have multiple field access functions in the predicate.",
                      candidate.fieldAccess().location().join(fieldAccess.predicate().location()))
                  .build();
            }

            candidate.replaceAndDelete(
                new FuncParamNode(new Parameter(fieldAccess.identifier, fieldAccess.type())));
          }
        }
      }
    }

    return null;
  }
}
