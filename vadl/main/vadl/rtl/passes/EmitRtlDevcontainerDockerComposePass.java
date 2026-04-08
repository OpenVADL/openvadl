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
 * Emit the RTL Devcontainer Docker Compose Specification.
 */
public class EmitRtlDevcontainerDockerComposePass extends AbstractTemplateRenderingPass {

  public static final String RTL_BASE_IMAGE = "ghcr.io/openvadl/rtl-test-base"
      + "@sha256:285ef834047b0e5eeadbd8a3e06d817cf9879f22136617beb413c6be0a0f18d5";

  /**
   * Construct the RTL Devcontainer Docker Compose emit pass.
   *
   * @param configuration the configuration
   */
  public EmitRtlDevcontainerDockerComposePass(GeneralConfiguration configuration) {
    super(configuration, "rtl");
  }

  @Override
  protected String getTemplatePath() {
    return "rtl/.devcontainer/docker-compose.yaml";
  }

  @Override
  protected String getOutputPath() {
    return ".devcontainer/docker-compose.yaml";
  }

  @Override
  protected String lineComment() {
    return "#";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    return Map.of(
        "image", RTL_BASE_IMAGE
    );
  }
}
