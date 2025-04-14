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

import java.nio.ByteOrder;
import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.vdt.model.Node;
import vadl.vdt.passes.VdtLoweringPass;
import vadl.vdt.target.iss.IssDecisionTreeCodeGenerator;
import vadl.viam.Specification;

/**
 * Emits the target/gen-arch/vdt-decode.c that contains the decoding tree for the given ISA.
 */
public class EmitIssDecodeTreePass extends IssTemplateRenderingPass {

  private static final String VDT_CODE_KEY = "vdt_code";

  /**
   * Constructor for the pass.
   *
   * @param configuration the ISS configuration
   */
  public EmitIssDecodeTreePass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/vdt-decode.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {

    final Map<String, Object> variables = super.createVariables(passResults, specification);

    if (!passResults.hasRunPassOnce(VdtLoweringPass.class)) {
      // Nothing to emit
      return variables;
    }

    // TODO: get the byte order from the VADL specification -> Implement memory annotations
    final ByteOrder bo = ByteOrder.LITTLE_ENDIAN;

    final var vdt = passResults.lastResultOf(VdtLoweringPass.class, Node.class);
    final var code = new IssDecisionTreeCodeGenerator(vdt, bo).generate();

    variables.put(VDT_CODE_KEY, code.toString());
    return variables;
  }
}
