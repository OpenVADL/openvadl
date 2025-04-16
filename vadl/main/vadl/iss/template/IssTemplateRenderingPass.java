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

package vadl.iss.template;

import static vadl.iss.template.IssRenderUtils.mapRegFiles;
import static vadl.iss.template.IssRenderUtils.mapRegs;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.formatting.CodeFormatter;
import vadl.iss.codegen.QemuClangFormatter;
import vadl.iss.passes.extensions.ExceptionInfo;
import vadl.iss.passes.extensions.MemoryInfo;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Register;
import vadl.viam.Specification;

/**
 * The template rendering pass all ISS (QEMU) rendering passes extend from.
 *
 * <p>It overrides the {@link #getOutputPath()} and uses the {@link #issTemplatePath()}
 * to determine the output location.
 * This is done by replacing all occurrences of {@code gen-arch} in the path, by the
 * {@link IssConfiguration#targetName()}.
 *
 * <p>All subclasses must provide the {@link #issTemplatePath()} that defines the
 * path inside the {@code resource/templates/iss} directory.
 *
 * <p>It also sets some default variables, required by many templates.
 * Subclasses should use the map returned by {@code super.createVariables} when overriding
 * the {@link #createVariables(PassResults, Specification)} method.
 */
public abstract class IssTemplateRenderingPass extends AbstractTemplateRenderingPass {

  public IssTemplateRenderingPass(IssConfiguration configuration) {
    super(configuration, "iss");
  }

  @Override
  public IssConfiguration configuration() {
    return (IssConfiguration) super.configuration();
  }

  @Override
  protected final String getTemplatePath() {
    return "iss/" + issTemplatePath();
  }

  /**
   * The path to the template within th {@code resource/templates/iss} directory.
   */
  protected abstract String issTemplatePath();

  @Override
  public PassName getName() {
    return PassName.of("Rendering ISS " + issTemplatePath());
  }

  @Override
  public @Nullable CodeFormatter getFormatter() {
    if (issTemplatePath().endsWith("translate.c")) {
      return QemuClangFormatter.INSTANCE;

    }
    return null;
  }

  @Override
  protected String getOutputPath() {
    var templatePath = issTemplatePath();

    // the iss template path is in the same hierarchy as the generated files.
    // however, if the path includes the generated architecture name, the template paths
    // use `gen-arch`, which must be replaced by the actual architecture name.
    return templatePath
        .replaceAll("gen-arch", configuration().targetName())
        .replaceAll("gen-machine", configuration().machineName().toLowerCase());
  }

  @Override
  protected String lineComment() {
    var filename = FilenameUtils.getName(getOutputPath());
    var ending = FilenameUtils.getExtension(filename);
    var hashEndings = Set.of("mak", "build");
    if (hashEndings.contains(ending)
        || filename.equals("Kconfig")
        || filename.equals("trace-events")
    ) {
      return "#";
    }
    return super.lineComment();
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = new HashMap<String, Object>();
    vars.put("gen_arch", configuration().targetName().toLowerCase());
    vars.put("gen_arch_upper", configuration().targetName().toUpperCase());
    vars.put("gen_arch_lower", configuration().targetName().toLowerCase());
    vars.put("gen_machine", configuration().machineName());
    vars.put("gen_machine_upper", configuration().machineName().toUpperCase());
    vars.put("gen_machine_lower", configuration().machineName().toLowerCase());
    vars.put("register_files", mapRegFiles(specification));
    vars.put("registers", mapRegs(specification));
    vars.put("pc_reg", getPcReg(specification));
    vars.put("target_size", configuration().targetSize().width);
    vars.put("mem_info", getMemoryInfo(specification));
    vars.put("exc_info", getExceptionInfo(specification));
    return vars;
  }

  private MemoryInfo getMemoryInfo(Specification viam) {
    return viam.mip().get().expectExtension(MemoryInfo.class);
  }

  private ExceptionInfo getExceptionInfo(Specification viam) {
    return viam.mip().get().isa().expectExtension(ExceptionInfo.class);
  }

  private Map<String, String> getPcReg(Specification viam) {
    var pc = viam.mip().get().isa().pc();
    if (pc == null) {
      throw new IllegalStateException("PC is null");
    }
    return IssRenderUtils.map((Register) pc.registerResource());
  }
}
