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

import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.codegen.IssResetGen;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.utils.codegen.CCodeBuilder;
import vadl.utils.codegen.CStringBuilder;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;

/**
 * Emits the target/gen-arch/cpu.c file that contains all required
 * CPU function/method implementations required by QEMU.
 */
public class EmitIssCpuSourcePass extends IssTemplateRenderingPass {
  public EmitIssCpuSourcePass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/cpu.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = super.createVariables(passResults, specification);
    vars.put("reg_dump_code", dumpRegsCode(specification));
    vars.put("reset", getResetCode(specification));
    return vars;
  }

  private String getResetCode(Specification specification) {
    var proc = specification.processor().get();
    return new IssResetGen(proc.reset()).fetch();
  }

  private String dumpRegsCode(Specification specification) {
    var sb = new CStringBuilder();
    var isa = specification.processor().get().isa();
    sb.indent();
    isa.registerTensors().forEach(tensor -> {
      dumpRegsCode(sb, tensor);
      sb.append("\n");
    });
    return sb.toString();
  }

  private void dumpRegsCode(CCodeBuilder sb, RegisterTensor reg) {
    var regLower = reg.simpleName().toLowerCase();
    var target = configuration().targetName().toLowerCase();
    var names = target + "_cpu_" + regLower + "_names";

    var dims = reg.indexDimensions();

    if (dims.isEmpty()) {
      sb.callStmt("qemu_fprintf", "f",
          "\" " + reg.simpleName() + ":    \" TARGET_FMT_lx \"\\n\"",
          "env->" + regLower);
      sb.append("qemu_fprintf(f, \" %s:    \" TARGET_FMT_lx \"\\n\", env->%s);"
          .formatted(reg.simpleName(), regLower));
    } else if (dims.size() == 1) {
      sb.forLoop("i", dims.getFirst().size() - 1, (_) -> {
        sb.callStmt("qemu_fprintf", "f", "\" %-8s \" TARGET_FMT_lx", names + "[i]",
            "env->" + regLower + "[i]");
        sb.ifStmt("i & 3 == 3", (_) ->
            sb.callStmt("qemu_fprintf", "f", "\"\\n\"")
        );
      });
    } else {
      sb.forLoop("i", dims.getFirst().size() - 1, (_) -> {
        sb.callStmt("qemu_fprintf", "f", "\" %-8s \"", names + "[i]");
        sb.varDecl("uint8_t *", "p", "(uint8_t *) env->" + regLower);
        var innerSizeBytes = reg.resultType(1).bitWidth() / 8;
        sb.forLoop("int j = " + innerSizeBytes, "j >= 0", "j--", (_) -> {
          sb.callStmt("qemu_fprintf", "f", "\"%02x\"", "*(p + i * " + innerSizeBytes + " + j)");
        });
      });
    }
  }
}
