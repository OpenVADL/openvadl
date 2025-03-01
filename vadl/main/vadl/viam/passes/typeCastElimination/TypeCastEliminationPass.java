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

package vadl.viam.passes.typeCastElimination;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.DefProp;
import vadl.viam.Specification;

/**
 * Runs the type cast elimination on ALL behaviors in the given VIAM specification.
 *
 * @see TypeCastEliminator
 */
public class TypeCastEliminationPass extends Pass {
  public TypeCastEliminationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("typeCastElimination");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {

    ViamUtils.findDefinitionsByFilter(viam, DefProp.WithBehavior.class::isInstance)
        .stream()
        .map(DefProp.WithBehavior.class::cast)
        .flatMap(definition -> definition.behaviors().stream())
        .forEach(TypeCastEliminator::runOnGraph);

    return null;
  }
}
