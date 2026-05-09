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
import vadl.lcb.templateUtils.RegisterUtils;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.UserModeEmulation;

/**
 * A specialized template rendering pass for QEMU User-Mode Emulation (UME) source files.
 * <p>
 * This pass populates the template context with architecture-specific configurations
 * required for Linux user-mode emulation, such as register mappings for system calls,
 * stack alignment, and exception handling indices.
 * </p>
 */
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
    return PassName.of("Rendering UME template: "
        + templateFilename);
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = super.createVariables(passResults, specification);

    UserModeEmulation ume = specification.userModeEmulation()
        .orElseThrow(() -> new IllegalStateException("No UserModeEmulation defined"));

    vars.put("config", Map.ofEntries(
        Map.entry("sysReg", ume.getSysReg().index()),
        Map.entry("retReg", ume.getRetReg().index()),
        Map.entry("spReg", ume.getSpReg().index()),
        Map.entry("raReg", ume.getRaReg().index()),
        Map.entry("tpReg", ume.getTpReg().index()),
        Map.entry("args", ume.getArgs().stream()
            .map(RegisterUtils.Register::index)
            .toList()),
        Map.entry("excIds", ume.getExcIds()),
        Map.entry("syscallInstr", ume.getSyscallInstr().simpleName()),
        Map.entry("syscallException", ume.getSyscallException().simpleName()),
        Map.entry("breakpointExcName", ume.getBreakpointExcName().simpleName()),
        Map.entry("illegalInstrExcName", ume.getIllegalInstrExcName().simpleName()),
        Map.entry("initialPc", ume.getInitialPc().simpleName()),
        Map.entry("initialSp", ume.getInitialSp().simpleName()),
        Map.entry("excCauseVar", ume.getExcCauseVar() != null
            ? ume.getExcCauseVar().simpleName()
            : ""),
        Map.entry("hasIcacheFlush", ume.hasIcacheFlush()),
        Map.entry("insn_width_bytes", ume.getInsnWidthBytes()),
        Map.entry("stack_align_mask", ume.getStackAlignMask()),
        Map.entry("sigtrampLoadSyscallInstr", ume.getSigtrampLoadSyscallInstr()),
        Map.entry("sigtrampTrapInstr", ume.getSigtrampTrapInstr()),
        Map.entry("mainRegisterFile", ume.getMainRegisterFile().simpleName().toLowerCase()),
        Map.entry("mainRegFileSize", ume.getMainRegisterFile().outermostDim().size()),
        Map.entry("signalStateTensors", ume.getSignalStateTensors().stream()
            .map(t -> Map.of(
                "name_lower", t.simpleName().toLowerCase(),
                "size", t.outermostDim().size()
            ))
            .toList())
        ));

    return vars;
  }
}
