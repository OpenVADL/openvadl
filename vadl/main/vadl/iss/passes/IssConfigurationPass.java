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

package vadl.iss.passes;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.DefProp;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * Sets configurations in the {@link IssConfiguration} if the information must be determined
 * from the VIAM.
 */
public class IssConfigurationPass extends AbstractIssPass {

  public IssConfigurationPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Configuration Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {
    var configuration = configuration();

    var mip = viam.processor().get();
    var isaName = mip.targetName().toLowerCase();
    configuration.setTargetName(isaName);
    configuration.setMachineName(mip.simpleName());

    viam.isa().ifPresent(isa -> {
      var targetSize = requireNonNull(isa.pc()).registerTensor().resultType().bitWidth();
      configuration.setTargetSize(Tcg_32_64.fromWidth(targetSize));
    });

    // remove all side effect conditions as we don't need them anymore
    removeSideEffectCondition(viam);

    // we return the configuration but also manipulate the original one,
    // so the return is actually not necessary.
    return configuration;
  }

  /**
   * The side effect condition property of side effects very suboptimal for the further
   * graph processing and never used. Therefore, we remove it here.
   */
  private void removeSideEffectCondition(Specification specification) {
    ViamUtils.findDefinitionsByFilter(specification, (d) -> d instanceof DefProp.WithBehavior)
        .stream()
        .flatMap(d -> ((DefProp.WithBehavior) d).behaviors().stream())
        .forEach(behavior -> {
          behavior.getNodes(SideEffectNode.class)
              .forEach(node -> node.setCondition(null));
          behavior.deleteUnusedDependencies();
        });
  }

}
