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

import java.util.List;
import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.codegen.IssExceptionHandlingCodeGenerator;
import vadl.iss.passes.extensions.ExceptionInfo;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

public class EmitIssDoExcCIncPass extends IssTemplateRenderingPass {
  public EmitIssDoExcCIncPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/do_exception.c.inc";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = super.createVariables(passResults, specification);
    vars.put("do_exc_funcs", doExcFuncs(specification));
    return vars;
  }

  private List<String> doExcFuncs(Specification specification) {
    var isa = specification.mip().get().isa();
    var excInfo = isa.expectExtension(ExceptionInfo.class);

    return excInfo.entries().stream()
        .map(e -> new IssExceptionHandlingCodeGenerator(e, configuration()).fetch())
        .toList();
  }

}
