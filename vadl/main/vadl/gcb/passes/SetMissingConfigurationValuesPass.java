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
import vadl.configuration.GcbConfiguration;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.TargetName;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * The {@link GeneralConfiguration} is created before a {@link Specification} was created.
 * This has a disadvantage because the configuration cannot read values which are defined in
 * the specification. This pass sets the values if they are not {@code null}. If they are
 * already set then we treat it as overwritten value by user. This value was supplied as argument
 * in the CLI.
 */
public class SetMissingConfigurationValuesPass extends Pass {
  public SetMissingConfigurationValuesPass(GcbConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("SetMissingConfigurationValuesPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var config = (GcbConfiguration) configuration();
    var targetName = viam.isa().map(x -> x.identifier).orElseThrow();

    if (config.isTargetNameNull()) {
      config.setTargetName(new TargetName(targetName.simpleName().toLowerCase()));
    }

    return null;
  }
}
