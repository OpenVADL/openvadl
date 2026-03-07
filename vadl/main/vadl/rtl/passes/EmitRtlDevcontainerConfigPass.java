// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

import java.util.Map;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassResults;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * Emit the RTL Devcontainer JSON Config.
 */
public class EmitRtlDevcontainerConfigPass extends AbstractTemplateRenderingPass {

  /**
   * Construct the RTL Devcontainer JSON emit pass.
   *
   * @param configuration the configuration
   */
  public EmitRtlDevcontainerConfigPass(GeneralConfiguration configuration) {
    super(configuration, "rtl");
  }

  @Override
  protected String getTemplatePath() {
    return "rtl/.devcontainer/devcontainer.json";
  }

  @Override
  protected String getOutputPath() {
    return ".devcontainer/devcontainer.json";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    return Map.of();
  }
}
