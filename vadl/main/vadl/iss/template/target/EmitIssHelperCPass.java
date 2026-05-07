// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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
import java.util.stream.Stream;
import vadl.configuration.IssConfiguration;
import vadl.iss.codegen.IssCpuFunctionGenerator;
import vadl.iss.codegen.IssInstrHelperGenerator;
import vadl.iss.passes.TcgPassUtils;
import vadl.iss.passes.common.IssRegisterAccessInfoRetrievalPass;
import vadl.iss.passes.extensions.InstrInfo;
import vadl.iss.passes.extensions.IssAccessorRegistry;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Emits the target/gen-arch/helper.c file that contains all helper functions
 * for exceptions and instructions.
 */
public class EmitIssHelperCPass extends IssTemplateRenderingPass {
  public EmitIssHelperCPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/helper.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = super.createVariables(passResults, specification);
    var accessorRegistry = passResults.lastResultOf(IssRegisterAccessInfoRetrievalPass.class,
        IssAccessorRegistry.class);
    vars.put("instr_helper_impls", helperImplementations(specification, accessorRegistry));
    return vars;
  }

  private List<String> helperImplementations(Specification specification,
                                             IssAccessorRegistry accessorRegistry) {
    return specification.isa().get().ownInstructions().stream()
        .map(TcgPassUtils::instrInfo)
        .filter(InstrInfo::asHelperCall)
        .flatMap(e -> Stream.concat(
            instrExtractedFunctionImpl(e, accessorRegistry),
            Stream.of(instrHelperImpl(e, accessorRegistry))
        ))
        .toList();
  }

  private String instrHelperImpl(InstrInfo info, IssAccessorRegistry accessorRegistry) {
    return new IssInstrHelperGenerator(configuration(), info, accessorRegistry).fetch();
  }

  private Stream<String> instrExtractedFunctionImpl(InstrInfo info,
                                                    IssAccessorRegistry accessorRegistry) {
    var cpuStateName = "CPU" + configuration().targetName().toUpperCase() + "State";
    return info.extractedFunctions().stream()
        .map(f -> new IssCpuFunctionGenerator(f, accessorRegistry).fetch(cpuStateName));
  }

}
