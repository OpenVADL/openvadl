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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Abi;
import vadl.viam.RegisterRef;
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

    var config = new HashMap<String, Object>();
    config.put("sysReg", accessor(ume.getSyscallNumber()));
    config.put("retReg", accessor(ume.getSyscallReturn()));
    config.put("spReg", accessor(abi.stackPointer().registerRef()));
    /*
     * tries to find the most idiomatic name for the stack pointer register;
     * "sp" is the default; if user defines an alias -> uses that instead
     * */
    config.put("spRegName", abi.aliases()
        .getOrDefault(
            Pair.of(abi.stackPointer().registerFile(), abi.stackPointer().addr()),
            List.of(new Abi.RegisterAlias("sp")))
        .getFirst().value());
    config.put("raReg", accessor(abi.returnAddress().registerRef()));
    abi.threadPointer().ifPresent(tp -> config.put("tpReg", accessor(tp.registerRef())));
    config.put("args", ume.args().stream()
        .map(this::accessor)
        .toList());
    config.put("syscallInstr", ume.syscallInstr().simpleName());
    config.put("insn_width_bytes", ume.syscallInstr().format().type().bitWidth() / 8);

    vars.put("config", config);
    return vars;
  }

  private String accessor(RegisterRef ref) {
    var refName = ref.resource().simpleName().toLowerCase();

    if(ref.indices().isEmpty()) {
      return refName;
    }

    return refName + "[ " + ref.singleIndex() + " ]";
  }
}
