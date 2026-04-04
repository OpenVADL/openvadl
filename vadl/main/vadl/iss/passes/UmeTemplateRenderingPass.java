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

package vadl.iss.passes;

import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.UserModeEmulation;

public class UmeTemplateRenderingPass extends IssTemplateRenderingPass {

  private final String templateFilename;

  public UmeTemplateRenderingPass(IssConfiguration configuration, String templateFilename) {
    super(configuration);
    this.templateFilename = templateFilename;
  }

  @Override
  protected String issTemplatePath() {
    return "linux-user/gen-arch/" + templateFilename;
  }

  @Override
  public PassName getName() {
    return PassName.of("Rendering UME template: " + templateFilename);
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults, Specification specification) {
    var vars = super.createVariables(passResults, specification);

    //if (specification.userModeEmulation().isPresent()) {
    //  var umeDef = specification.userModeEmulation().get();

    //TODO remove hardcoded values (finish vadl with ume)
     /* int extractedSysReg = 17;
      int extractedRetReg = 10;
      int extractedSpReg = 2;
      int extractedRaReg = 1;
      int extractedTpReg = 4;
      List<Integer> extractedArgs = List.of(10, 11, 12, 13, 14, 15);
      Map<String, Integer> extractedExcIds = Map.of(
          "ILLEGAL_INSTR", 2,
          "BREAKPOINT", 3,
          "ECALL", 11
      );

    vars.put("config", new UmeInfo(extractedSysReg, extractedRetReg, extractedSpReg, extractedRaReg, extractedTpReg, extractedArgs, extractedExcIds));
    vars.put("insn_width_bytes", 4); */
   // }

    /*specification.definitions()
        .filter(UserModeEmulation.class::isInstance)
        .map(UserModeEmulation.class::cast)
        .findFirst()
        .ifPresent(ume -> {
          vars.put("config", ume);
          vars.put("insn_width_bytes", 4); // or get from elsewhere if needed
        });*/

    UserModeEmulation ume = UserModeEmulation.createDefault();
    vars.put("config", ume.asMap());
    vars.put("insn_width_bytes", 4); // or get from elsewhere if needed

    return vars;
  }
}
