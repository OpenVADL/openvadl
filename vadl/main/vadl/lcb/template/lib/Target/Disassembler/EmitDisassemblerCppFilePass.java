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

package vadl.lcb.template.lib.Target.Disassembler;

import static vadl.viam.ViamError.ensure;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.ImmediateDecodingFunctionProvider;
import vadl.lcb.templateUtils.RegisterUtils;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Abi;
import vadl.viam.Format;
import vadl.viam.Specification;

/**
 * This file contains the target specific implementation for the disassembler.
 */
public class EmitDisassemblerCppFilePass extends LcbTemplateRenderingPass {

  public EmitDisassemblerCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/Disassembler/TargetDisassembler.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/lib/Target/" + lcbConfiguration().targetName().value() + "/Disassembler/"
        + lcbConfiguration().targetName().value()
        + "Disassembler.cpp";
  }

  /**
   * The LLVM's encoder/decoder does not interact with the {@code uint64_t decode(uint64_t)}
   * functions but with {@code unsigned decode(const MCInst InstMI, ...} from the MCCodeEmitter.
   * This {@code WRAPPER} is just the magic suffix for the
   * function.
   */
  public static final String WRAPPER = "wrapper";

  record Immediate(String wrapperName, String decodeMethodName, int bitWidth, long mask) implements
      Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "wrapperName", wrapperName,
          "decodeMethodName", decodeMethodName,
          "bitWidth", bitWidth,
          "mask", mask
      );
    }
  }

  private List<RegisterUtils.RegisterClass> extractRegisterClasses(Specification specification,
                                                                   Abi abi) {
    return specification.isa().map(x -> x.ownRegisterFiles().stream())
        .orElse(Stream.empty())
        .map(x -> RegisterUtils.getRegisterClass(x, abi.aliases()))
        .toList();
  }

  private List<Immediate> extractImmediates(PassResults passResults) {
    return ImmediateDecodingFunctionProvider.generateDecodeFunctions(passResults)
        .entrySet()
        .stream()
        .map(entry -> {
          var field = entry.getKey();
          var wrapperName = entry.getValue().identifier.append(WRAPPER).lower();
          var decoderMethod = entry.getValue().functionName().lower();
          var bitWidth = field.size();

          return new Immediate(wrapperName, decoderMethod, bitWidth,
              (int) Math.pow(2, bitWidth) - 1);
        })
        .sorted(Comparator.comparing(o -> o.wrapperName))
        .toList();
  }

  /**
   * Get the instruction bit size from the {@link Specification}.
   * The method throws an exception when different sizes exist.
   */
  private int getInstructionSize(Specification specification) {
    List<Integer> sizes = specification.isa()
        .map(x -> x.ownFormats().stream()).orElseGet(Stream::empty)
        .mapToInt(x -> Arrays.stream(x.fields()).mapToInt(Format.Field::size).sum())
        .distinct()
        .boxed()
        .toList();

    ensure(sizes.size() == 1, "Vadl only support a constant instruction size."
        + " Found multiple sizes");
    return sizes.get(0);
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "immediates",
        extractImmediates(passResults),
        "instructionSize", getInstructionSize(specification),
        CommonVarNames.REGISTERS_CLASSES, extractRegisterClasses(specification, abi));
  }
}
