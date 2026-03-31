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

package vadl.rtl.passes;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.RtlConfiguration;
import vadl.error.Diagnostic;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Definition;
import vadl.viam.Specification;

/**
 * Set fields fo the {@link RtlConfiguration} that depend on the VIAM.
 */
public class RtlConfigurationPass extends AbstractRtlPass {

  public RtlConfigurationPass(RtlConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("RTL Configuration");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    var processorName = viam.processor().map(Definition::simpleName).orElseGet(
        () -> viam.mia().map(Definition::simpleName).orElseThrow(
            () -> Diagnostic.error("Processor or MiA definition required for emitting RTL",
                viam.location()).build()));

    configuration().setTopModuleIfEmpty(processorName);
    configuration().setProjectNameIfEmpty(viam.simpleName());

    return null;
  }
}
