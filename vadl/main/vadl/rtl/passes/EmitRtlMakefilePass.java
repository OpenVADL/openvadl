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

package vadl.rtl.passes;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vadl.configuration.RtlConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.template.RtlTemplateRenderingPass;
import vadl.template.AbstractMultiTemplateRenderingPass;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * Emit RTL Makefile.
 */
public class EmitRtlMakefilePass extends RtlTemplateRenderingPass {

  public EmitRtlMakefilePass(RtlConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Emit Makefile");
  }

  @Override
  protected String getTemplatePath() {
    return "rtl/Makefile";
  }

  @Override
  protected String lineComment() {
    return "#";
  }

  @Override
  protected List<RenderInput> createRenderInputs(PassResults passResults,
                                                 Specification specification,
                                                 Map<String, Object> baseVariables) {
    // get list of emitted files
    var renderResults = passResults.allResultsOf(RtlTemplateRenderingPass.class,
        AbstractMultiTemplateRenderingPass.Result.class);

    var scalaFiles = renderResults
        .map(AbstractMultiTemplateRenderingPass.Result::emittedFiles)
        .flatMap(Collection::stream)
        .filter(file -> file.toString().endsWith(".scala") || file.toString().endsWith(".sbt"))
        .map(this::relativePathToOutputPath)
        .map(this::escapePath)
        .toList();

    var vars = new HashMap<String, Object>();
    vars.put("hasEcall", specification.isa().map(this::hasEcall).orElse(false));
    vars.put("scalaFiles", scalaFiles);
    return List.of(
        new RenderInput("Makefile", mergeVariables(baseVariables, vars))
    );
  }

  private boolean hasEcall(InstructionSetArchitecture isa) {
    return isa.ownInstructions().stream().anyMatch(instr ->
      instr.simpleName().equals("ECALL")
          && instr.behavior().getNodes().anyMatch(SideEffectNode.class::isInstance)
    );
  }

  private String relativePathToOutputPath(Path path) {
    var outputPath = Path.of(configuration().outputPath().toString(), subDir);
    var relPath = outputPath.relativize(path);
    return relPath.toString();
  }

  private String escapePath(String filename) {
    return filename.replace("\\", "/");
  }
}
