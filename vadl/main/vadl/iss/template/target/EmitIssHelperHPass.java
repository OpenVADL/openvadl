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
import vadl.configuration.IssConfiguration;
import vadl.iss.codegen.HelperParamPacking;
import vadl.iss.passes.TcgPassUtils;
import vadl.iss.passes.extensions.InstrExecPlan;
import vadl.iss.passes.extensions.InstrInfo;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Emits the target/gen-arch/helper.h file that contains all macro helper definitions for
 * exceptions and instructions.
 */
public class EmitIssHelperHPass extends IssTemplateRenderingPass {
  public EmitIssHelperHPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/helper.h";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var variables = super.createVariables(passResults, specification);

    variables.put("instr_helper_defs", instrHelperDefs(specification));

    return variables;
  }

  private List<String> instrHelperDefs(Specification specification) {
    return specification.isa().get().ownInstructions().stream()
        .map(TcgPassUtils::instrInfo)
        .filter(info -> info.executionPath() == InstrExecPlan.ExecutionPath.HELPER_CALL)
        .map(this::instrHelperDef)
        .toList();
  }

  private String instrHelperDef(InstrInfo instr) {
    var packing = HelperParamPacking.from(instr);
    var argSize = packing.blockCount() + 1; // plus one because of the tcg env
    var paramTypes = packing.blockCount() == 0 ? ""
        : ", " + "i64,".repeat(packing.blockCount() - 1) + "i64";
    return "DEF_HELPER_%s(%s, void, env%s)".formatted(
        argSize,
        instr.helperName(),
        paramTypes
    );
  }
}
