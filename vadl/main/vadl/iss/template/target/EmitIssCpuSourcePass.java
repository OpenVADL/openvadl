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
import java.util.stream.Collectors;
import vadl.configuration.IssConfiguration;
import vadl.iss.codegen.IssResetGen;
import vadl.iss.template.IssRenderUtils;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.utils.codegen.CodeGeneratorAppendable;
import vadl.utils.codegen.StringBuilderAppendable;
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
    var sb = new StringBuilderAppendable();
    var isa = specification.processor().get().isa();
    sb.indent();
    isa.registerTensors().forEach(tensor -> {
      dumpRegsCode(sb, tensor);
      sb.append("\n");
    });
    return sb.toString();
  }

  private void dumpRegsCode(CodeGeneratorAppendable sb, RegisterTensor reg) {
    var layers = reg.indexDimensions().stream()
        .map(d -> Pair.of("d" + d.index(), d.size()))
        .toList();
    var indexAccess = layers.stream().map(l -> "[" + l.left() + "]")
        .collect(Collectors.joining());

    var nameLower = reg.simpleName().toLowerCase();

    IssRenderUtils.generateNestedLoops(sb, layers, (b) -> {
      if (layers.isEmpty()) {
        b.append("qemu_fprintf(f, \" %s:    \" TARGET_FMT_lx \"\\n\", env->%s);"
            .formatted(reg.simpleName(), nameLower));
      } else {
        var target = configuration().targetName().toLowerCase();
        var names = target + "_cpu_" + nameLower + "_names";
        var innerLayer = layers.getLast().left();
        b.appendLn("qemu_fprintf(f, \" %-8s \" TARGET_FMT_lx, "
            + names + indexAccess + ", env->"
            + nameLower + indexAccess + ");");
        b.append("if (" + innerLayer + " & 3 == 3) qemu_fprintf(f, \"\\n\");");
      }
    });
  }

}
