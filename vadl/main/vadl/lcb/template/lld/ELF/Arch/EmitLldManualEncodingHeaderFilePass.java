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

package vadl.lcb.template.lld.ELF.Arch;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.common.UpdateFieldRelocationFunctionCodeGenerator;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file contains the function to update an immediate when a relocation has to be applied.
 */
public class EmitLldManualEncodingHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitLldManualEncodingHeaderFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/lld/ELF/Arch/TargetManualEncoding.hpp";
  }

  @Override
  protected String getOutputPath() {
    return "lld/ELF/Arch/" + lcbConfiguration().processorName().value() + "ManualEncoding.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var output =
        (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
            GenerateLinkerComponentsPass.class);
    var elfRelocations = output.elfRelocations();
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "functions", elfRelocations.stream()
            .collect(Collectors.groupingBy(x -> x.fieldUpdateFunction().functionName().lower()))
            .values()
            .stream()
            .map(x -> x.get(0)) // only consider one relocation because we do not need duplication
            .map(elfRelocation -> {
              var generator = new UpdateFieldRelocationFunctionCodeGenerator(
                  elfRelocation.fieldUpdateFunction());
              return generator.genFunctionDefinition();
            })
            .toList());
  }
}
