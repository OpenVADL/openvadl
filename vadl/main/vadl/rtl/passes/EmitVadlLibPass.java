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

import java.util.List;
import java.util.Map;
import vadl.configuration.RtlConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.template.RtlTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * Emit VADL.scala.
 *
 * <p>This file contains helpers for the implementation.
 */
public class EmitVadlLibPass extends RtlTemplateRenderingPass {

  public EmitVadlLibPass(RtlConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Emit VADL.scala");
  }

  @Override
  protected String getTemplatePath() {
    return "rtl/VADL.scala";
  }

  @Override
  protected List<RenderInput> createRenderInputs(PassResults passResults,
                                                 Specification specification,
                                                 Map<String, Object> baseVariables) {
    return List.of(
        new RenderInput("src/main/scala/VADL.scala", baseVariables)
    );
  }
}
