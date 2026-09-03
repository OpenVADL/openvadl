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

import java.util.List;
import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Abi;
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

    Abi abi = ume.abi();

    vars.put("config", Map.ofEntries(
        Map.entry("sysReg", ume.getSyscallNumber().singleIndex()),
        Map.entry("sysRegFile", ume.getSyscallNumber().resource().simpleName().toLowerCase()),
        Map.entry("retReg", ume.getSyscallReturn().singleIndex()),
        Map.entry("retRegFile", ume.getSyscallReturn().resource().simpleName().toLowerCase()),
        Map.entry("spReg", abi.stackPointer().addr()),
        Map.entry("spRegFile",  abi.stackPointer().registerFile().simpleName().toLowerCase()),
        /*
        * tries to find the most idiomatic name for the stack pointer register;
        * "sp" is the default; if user defines an alias -> uses that instead
        * */
        Map.entry("spRegName",  abi.aliases()
            .getOrDefault(
                Pair.of(abi.stackPointer().registerFile(), abi.stackPointer().addr()),
                List.of(new Abi.RegisterAlias("sp"))
            )
            .getFirst().value()),
        Map.entry("args", ume.args().stream()
            .map(ref -> Map.of(
                "index", ref.singleIndex(),
                "file",  ref.resource().simpleName().toLowerCase()
            ))
            .toList()),
        Map.entry("syscallInstr", ume.syscallInstr().simpleName()),
        Map.entry("insn_width_bytes", ume.syscallInstr().format().type().bitWidth() / 8)
    ));

    return vars;
  }
}
