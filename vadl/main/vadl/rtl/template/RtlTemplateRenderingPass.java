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
import vadl.configuration.RtlConfiguration;
import vadl.error.Diagnostic;
import vadl.pass.PassResults;
import vadl.template.AbstractMultiTemplateRenderingPass;
import vadl.viam.Definition;
import vadl.viam.MicroArchitecture;
import vadl.viam.Specification;
import vadl.viam.ViamError;

/**
 * Base class for all RTL template rendering passes. Defines common variables and generates
 * filenames based on the {@link RtlConfiguration}.
 */
public abstract class RtlTemplateRenderingPass extends AbstractMultiTemplateRenderingPass {

  private final RtlConfiguration configuration;

  public RtlTemplateRenderingPass(RtlConfiguration configuration) {
    super(configuration, "rtl");
    this.configuration = configuration;
  }

  @Override
  public RtlConfiguration configuration() {
    return configuration;
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
    vars.put("package", configuration.getScalaPackage());
    vars.put("topModule", configuration.getTopModule());
    vars.put("projectName", configuration.getProjectName());
    vars.put("isaName", viam.mia().map(MicroArchitecture::isa)
        .map(Definition::simpleName).orElseThrow(() ->
            Diagnostic.error("Can not emit RTL without ISA", viam.location()).build()));
    return vars;
  }

  protected String getSourceFilePath(String filename) {
    return configuration.getScalaPackageDir() + "/" + filename;
  }

  protected String getSourceTestFilePath(String filename) {
    return configuration.getScalaTestPackageDir() + "/" + filename;
  }

  protected String getResourceTestFilePath(String filename) {
    return configuration.getScalaTestResourcesDir() + "/" + filename;
  }

  protected Map<String, Object> mergeVariables(Map<String, Object> baseVariables,
                                               Map<String, Object> variables) {
    var result = new HashMap<>(baseVariables);
    result.putAll(variables);
    return result;
  }
}
