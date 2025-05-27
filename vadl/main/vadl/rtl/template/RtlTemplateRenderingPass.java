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

package vadl.rtl.template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.configuration.RtlConfiguration;
import vadl.pass.PassResults;
import vadl.template.AbstractMultiTemplateRenderingPass;
import vadl.viam.Specification;

public abstract class RtlTemplateRenderingPass extends AbstractMultiTemplateRenderingPass {

  private final RtlConfiguration configuration;

  public RtlTemplateRenderingPass(RtlConfiguration configuration) {
    super(configuration, "rtl");
    this.configuration = configuration;
  }

  @Override
  protected List<RenderInput> createRenderInputs(PassResults passResults,
                                                 Specification specification) {
    return createRenderInputs(passResults, specification,
        getBaseVariables(passResults, specification));
  }

  /**
   * Construct the rendering inputs used during template rendering.
   * Each render input corresponds to one rendered file.
   *
   * @param passResults pass results
   * @param specification VIAM specification
   * @param baseVariables map of variables common to all files
   * @return a list of rendering inputs, one per output file.
   */
  protected abstract List<RenderInput> createRenderInputs(PassResults passResults,
                                                          Specification specification,
                                                          Map<String, Object> baseVariables);

  protected Map<String, Object> getBaseVariables(PassResults passResults, Specification viam) {
    var vars = new HashMap<String, Object>();
    vars.put("configuration", configuration);
    vars.put("package", getPackage());
    vars.put("projectName", viam.simpleName());
    return vars;
  }

  public String getPackage() {
    return configuration.getScalaPackage();
  }
}
