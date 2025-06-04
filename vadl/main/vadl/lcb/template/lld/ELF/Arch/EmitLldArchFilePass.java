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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.relocation.model.AutomaticallyGeneratedRelocation;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.ImmediateEncodingFunctionProvider;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Specification;

/**
 * This files defines the relocations for the linker.
 */
public class EmitLldArchFilePass extends LcbTemplateRenderingPass {

  public EmitLldArchFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/lld/ELF/Arch/Target.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "lld/ELF/Arch/" + lcbConfiguration().targetName().value() + ".cpp";
  }

  record ElfInfo(boolean isBigEndian, int maxInstructionWordSize) {

  }

  record ElfRelocationInfo(String elfName, String kind,
                           String relocationFunction, String patchInstructionFunction,
                           String encodingFunction) implements
      Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "elfName", elfName,
          "kind", kind,
          "relocationFunction", relocationFunction,
          "patchInstructionFunction", patchInstructionFunction,
          "encodingFunction", encodingFunction
      );
    }
  }

  private ElfInfo createElfInfo() {
    return new ElfInfo(false, 32);
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var output =
        (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
            GenerateLinkerComponentsPass.class);

    var encodingFunctions = ImmediateEncodingFunctionProvider.generateEncodeFunctions(passResults);

    var elfRelocations = output.elfRelocations().stream().map(
        r -> new ElfRelocationInfo(r.elfRelocationName().value(),
            r.llvmKind(),
            r.valueRelocation().functionName().lower(),
            r.fieldUpdateFunction().functionName().lower(),
            r instanceof AutomaticallyGeneratedRelocation
                ? requireNonNull(encodingFunctions.get(r.field())).functionName().lower()
                : ""
        )
    ).toList();

    var elfInfo = createElfInfo();

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        CommonVarNames.MAX_INSTRUCTION_WORDSIZE, elfInfo.maxInstructionWordSize(),
        CommonVarNames.IS_BIG_ENDIAN, elfInfo.isBigEndian(),
        "elfRelocations", elfRelocations);
  }
}
