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
import java.util.Collection;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.gcb.passes.relocation.model.AutomaticallyGeneratedRelocation;
import vadl.gcb.passes.relocation.model.HasRelocationComputationAndUpdate;
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

    var elfRelocations = output.elfRelocations().stream().map(
        r -> new ElfRelocationInfo(r.elfRelocationName().value(),
            r.kind().llvmKind(),
            r.valueRelocation().functionName().lower(),
            r.fieldUpdateFunction().functionName().lower(),
            r instanceof AutomaticallyGeneratedRelocation
                ? encodingFunction(r, passResults)
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

  private String encodingFunction(HasRelocationComputationAndUpdate elfRelocation,
                                  PassResults passResults) {

    var fields = elfRelocation.fields();
    if (fields.size() != 1) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning("The Linker expects only one field for relocation operand",
              elfRelocation.format()));
    }

    var encodingFunctions = ImmediateEncodingFunctionProvider.generateEncodeFunctions(passResults);

    var encodingsForField = encodingFunctions.values().stream().flatMap(Collection::stream)
        .filter(encodingFunction -> encodingFunction.field().equals(fields.getFirst())).toList();

    // Just get first encoding for the field as they are all the same function.
    // There is more than one because the functions are generated per instruction.
    return encodingsForField.getFirst().header().functionName().lower();
  }
}
