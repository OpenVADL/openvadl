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

package vadl.iss.template.hw;

import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.common.PureFunctionCodeGenerator;
import vadl.iss.codegen.IssFirmwareCodeGenerator;
import vadl.iss.passes.extensions.MemoryInfo;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Procedure;
import vadl.viam.Specification;
import vadl.viam.annotations.EnableHtifAnno;

/**
 * Emits the {@code hw/gen-arch/gen-machine.c} which is the core implementation for the
 * virtual hardware board.
 * It defines things like memory, start address, HTIF, firmware loading, etc...
 *
 * @see IssFirmwareCodeGenerator
 * @see MemoryInfo
 */
public class EmitIssHwMachineCPass extends IssTemplateRenderingPass {

  public EmitIssHwMachineCPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "hw/gen-arch/gen-machine.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = super.createVariables(passResults, specification);
    vars.put("dram_base", getDramBaseExpr());
    vars.put("start_addr", getStartAddrExpr(specification));
    vars.put("htif_enabled", htifEnabled(specification));

    // firmware
    var processor = specification.mip().get();
    var firmware = processor.firmware();
    if (firmware != null) {
      vars.put("setup_rom_reset_vec",
          firmwareWriteFunction(firmware, processor.expectExtension(MemoryInfo.class)));
      System.out.println(vars.get("firmware"));
    }
    return vars;
  }


  private String getDramBaseExpr() {
    // TODO: Don't hardcode this
    return "0x80000000";
  }

  private String getStartAddrExpr(Specification specification) {
    var mip = specification.mip().orElse(null);
    specification.ensure(mip != null, "No MicroProcessor definition found");
    return new PureFunctionCodeGenerator(mip.start()).genReturnExpression();
  }

  private boolean htifEnabled(Specification specification) {
    var mip = specification.mip().orElse(null);
    specification.ensure(mip != null, "No MicroProcessor definition found");
    return mip.hasAnnotation(EnableHtifAnno.class);
  }

  private String firmwareWriteFunction(Procedure firmware, MemoryInfo memoryInfo) {
    return new IssFirmwareCodeGenerator(firmware, memoryInfo)
        .fetch(configuration().machineName());
  }

}
