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

package vadl.iss.template.target;

import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Emits the target/gen-arch/machine.c that contains VM state information to
 * save and restore the state of QEMU for the generated CPU device.
 *
 * <p>Note that this is not yet activated and doesn't work yet.</p>
 */
// TODO: Make this work (look at machine.c TODO comment).
public class EmitIssMachinePass extends IssTemplateRenderingPass {
  public EmitIssMachinePass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/machine.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    return super.createVariables(passResults, specification);
  }
}
