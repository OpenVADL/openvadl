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

package vadl.viam.passes.canonicalization;

import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.DefProp;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;

/**
 * It looks at all the {@link BuiltInCall} nodes and when two inputs are constant
 * then it replaces it with the result. It will repeat the process until nothing changes.
 * It will only consider machine instructions.
 */
public class CanonicalizationPass extends Pass {
  public CanonicalizationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("canonicalization");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) {

    ViamUtils.findDefinitionsByFilter(viam,
            definition -> definition instanceof DefProp.WithBehavior)
        .stream()
        .flatMap(d -> ((DefProp.WithBehavior) d).behaviors().stream())
        .forEach(Canonicalizer::canonicalize);

    return null;
  }
}
