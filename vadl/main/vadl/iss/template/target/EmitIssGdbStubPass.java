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

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.function.TriConsumer;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.IssGdbInfoExtractionPass;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.utils.codegen.CodeGeneratorAppendable;
import vadl.utils.codegen.StringBuilderAppendable;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Resource;
import vadl.viam.Specification;

/**
 * Emits the {@code target/gen-arch/gdbstub.c} file which implements target-specific callback
 * that are used by QEMU's generic gdbstub to read and modify the CPU state.
 * The minimal required callbacks are read/write of CPU registers.
 *
 * <p>The {@link vadl.iss.template.gdb_xml.EmitIssGdbXmlPass} emits the CPU register information
 * used by GDB to address certain registers.</p>
 */
public class EmitIssGdbStubPass extends IssTemplateRenderingPass {
  public EmitIssGdbStubPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/gdbstub.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = super.createVariables(passResults, specification);
    var gdbInfo = passResults.lastResultOf(IssGdbInfoExtractionPass.class,
        IssGdbInfoExtractionPass.Result.class);
    vars.put("regs", gdbInfo.regs());
    vars.put("read_regs", createRead(gdbInfo.regs()));
    vars.put("write_regs", createWrite(gdbInfo.regs()));
    return vars;
  }

  private String createRead(List<IssGdbInfoExtractionPass.Result.Reg> regs) {
    var read = createIfChainOverN(regs, (i, res, builder) -> {
      var resName = res.simpleName().toLowerCase();
      var envAccess = res instanceof RegisterFile ? resName + "[n]" : resName;
      builder.appendLn("return gdb_get_regl(mem_buf, env->" + envAccess + ");");
    });

    // if non of the above match
    read.appendLn("return 0;");
    return read.toString();
  }

  private String createWrite(List<IssGdbInfoExtractionPass.Result.Reg> regs) {
    var write = createIfChainOverN(regs, (i, res, builder) -> {
      var resName = res.simpleName().toLowerCase();
      var envAccess = res instanceof RegisterFile ? resName + "[n]" : resName;
      var ldFucn = "ld" + shortMachineWord(res) + "_p";
      var writtenBytes = res.resultType().bitWidth() / 8;

      if (res instanceof RegisterFile regFile) {
        var cond =
            Arrays.stream(regFile.constraints())
                .map(c -> "n == " + c.indices().getFirst().hexadecimal())
                .collect(Collectors.joining(" && "));
        builder.appendLn("if (" + cond + ") { return " + writtenBytes + "; }");
      }

      builder.appendLn("env->" + envAccess + " = " + ldFucn + "(mem_buf);")
          .appendLn("return " + writtenBytes + ";");
    });

    // if non of the above match
    write.appendLn("return 0;");
    return write.toString();
  }

  private CodeGeneratorAppendable createIfChainOverN(List<IssGdbInfoExtractionPass.Result.Reg> regs,
                                                     TriConsumer<Integer, Resource,
                                                         CodeGeneratorAppendable> accessBuilder) {
    var read = new StringBuilderAppendable("\s\s\s\s").indent();

    int i = 0;
    Resource currentFile = null;

    for (var reg : regs) {
      Resource origin = reg.origin();
      if (currentFile == null || !currentFile.equals(origin)) {
        if (currentFile != null) {
          emitIfCondition(read, accessBuilder, i - 1, currentFile);
        }
        currentFile = origin;
      }
      i++;
    }

    emitIfCondition(read, accessBuilder, i - 1, requireNonNull(currentFile));
    return read;
  }

  @SuppressWarnings("LineLength")
  private void emitIfCondition(CodeGeneratorAppendable read,
                               TriConsumer<Integer, Resource, CodeGeneratorAppendable> accessBuilder,
                               int index, Resource file) {
    var comp = file instanceof Register ? "==" : "<=";
    read.appendLn("if ( n " + comp + " " + index + ") {")
        .indent();
    accessBuilder.accept(index, file, read);
    read.unindent()
        .appendLn("}");
  }

  private static String shortMachineWord(Resource res) {
    int width = res.resultType().bitWidth();
    res.ensure(isStandard(width),
        "The resource with is none of 8, 16, 32, 64. "
            + "Currently the GDB register write requires those types.");
    return switch (width) {
      case 8 -> "b";
      case 16 -> "w";
      case 32 -> "l";
      case 64 -> "q";
      default -> throw new IllegalStateException("Unexpected width: " + width);
    };
  }

  private static boolean isStandard(int width) {
    return width == 8 || width == 16 || width == 32 || width == 64;
  }
}
