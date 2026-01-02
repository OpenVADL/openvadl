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

package vadl.lcb.include.llvm.IR;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.DetermineIntrinsicAttributesPass;
import vadl.gcb.passes.InstructionIntrinsicAttributesCtx;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * Tablegen to generate middleend intrinsics.
 */
public class EmitMiddleendIntrinsicsTableGenPass extends LcbTemplateRenderingPass {
  public EmitMiddleendIntrinsicsTableGenPass(LcbConfiguration configuration)
      throws IOException {
    super(configuration);
  }

  @Override
  protected String getOutputPath() {
    return "llvm/include/llvm/IR/Intrinsics" + lcbConfiguration().targetName().value() + ".td";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var map =
        (Map<Instruction, List<InstructionIntrinsicAttributesCtx.Attribute>>) passResults.lastResultOf(
            DetermineIntrinsicAttributesPass.class);
    var records = ((List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class)).stream().collect(Collectors.toMap(
        TableGenMachineInstruction::instruction, x -> x));

    var intrinsics = genIntrinsics(map, records);

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "intrinsics", intrinsics);
  }

  private record Intrinsic(String name, List<String> resultTy, List<String> paramTy,
                           List<InstructionIntrinsicAttributesCtx.Attribute> attributes) implements
      Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "name", name,
          "result", String.join(", ", resultTy),
          "param", String.join(", ", paramTy),
          "attr",
          attributes.stream().map(this::getName).filter(Optional::isPresent).map(Optional::get)
              .collect(Collectors.joining(", "))
      );
    }

    private Optional<String> getName(InstructionIntrinsicAttributesCtx.Attribute x) {
      return switch (x) {
        case NoMem -> Optional.of("IntrNoMem");
        case WillReturn -> Optional.empty();
        case NoReturn -> Optional.empty();
        case Speculatable -> Optional.of("IntrSpeculatable");
      };
    }
  }

  private List<Intrinsic> genIntrinsics(
      Map<Instruction, List<InstructionIntrinsicAttributesCtx.Attribute>> attributes,
      Map<Instruction, TableGenMachineInstruction> records) {
    var result = new ArrayList<Intrinsic>();

    for (var entry : attributes.entrySet()) {
      var instruction = entry.getKey();
      var attr = entry.getValue();
      var record = ensureNonNull(records.get(instruction), "must not be null");

      var intrinsic =
          new Intrinsic(
              "int_" + lcbConfiguration().targetName().value() + "_" + instruction.simpleName(),
              record.getOutOperands().isEmpty() ? List.of("llvm_void_ty") : List.of("llvm_any_ty"),
              record.getInOperands().stream().map(x -> "LLVMMatchType<0>")
                  .collect(Collectors.toList()),
              attr);
      result.add(intrinsic);
    }

    result.sort(Comparator.comparing(o -> o.name));
    return result;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/include/llvm/IR/Intrinsics.td";
  }
}
