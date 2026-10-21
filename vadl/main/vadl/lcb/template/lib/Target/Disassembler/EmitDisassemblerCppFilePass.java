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

package vadl.lcb.template.lib.Target.Disassembler;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.CreateFunctionsFromImmediatesPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.templateUtils.RegisterUtils;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Abi;
import vadl.viam.Specification;

/**
 * This file contains implementation boilerplate for the target disassembler.
 */
public class EmitDisassemblerCppFilePass extends LcbTemplateRenderingPass {
  public EmitDisassemblerCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/Disassembler/Disassembler.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().targetName().value();
    return "llvm/lib/Target/" + processorName + "/Disassembler/"
        + processorName + "Disassembler.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {

    var instructionSizes = instructionSizes(specification);

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "instructionSizes", instructionSizes,
        "minInstructionSizeInBytes", instructionSizes.get(0).bytes(),
        "decodeImmediates", decodeImmediates(passResults),
        "decodeRegisterClasses", decodeRegisterClasses(specification)
    );
  }

  record InstructionSize(int bits, int bytes) implements Renderable {
    @Override
    public Map<String, Object> renderObj() {
      return Map.of("bits", bits, "bytes", bytes);
    }
  }

  private List<InstructionSize> instructionSizes(Specification specification) {
    return specification.isa().stream()
        .flatMap(isa -> isa.ownInstructions().stream())
        .filter(inst -> {
          int bits = inst.format().type().bitWidth();
          if (bits % 8 != 0 || bits > 64) {
            DeferredDiagnosticStore.add(Diagnostic.warning(
                "Instruction " + inst.simpleName() + " has a non-byte-aligned or too large size of "
                    + bits + " bits. It will be ignored in the disassembler.", inst.location()));
            return false;
          }
          return true;
        })
        .map(i -> i.format().type().bitWidth())
        .distinct()
        .sorted()
        .map(bits -> new InstructionSize(bits, bits / 8))
        .toList();
  }

  record DecodeImmediate(String decodeFunction, String decodeWrapper)
      implements Renderable {
    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "decodeFunction", decodeFunction,
          "decodeWrapper", decodeWrapper
      );
    }
  }

  private List<DecodeImmediate> decodeImmediates(PassResults passResults) {
    var immediatesPassResult = (CreateFunctionsFromImmediatesPass.Output)
        passResults.lastResultOf(CreateFunctionsFromImmediatesPass.class);

    return immediatesPassResult.decodings().entrySet().stream()
        .map(entry -> {
          var decodeFunction = entry.getValue().header().functionName().lower();
          var decodeWrapper = requireNonNull(
              immediatesPassResult.decodingWrappers().get(entry.getKey())
          ).functionName().lower();

          return new DecodeImmediate(decodeFunction, decodeWrapper);
        })
        .toList();
  }

  private List<RegisterUtils.RegisterClass> decodeRegisterClasses(Specification specification) {
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    return RegisterUtils.registerClasses(specification, abi);
  }
}

