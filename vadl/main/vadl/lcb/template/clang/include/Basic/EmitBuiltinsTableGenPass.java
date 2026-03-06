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

package vadl.lcb.template.clang.include.Basic;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.CppTypeMap;
import vadl.error.Diagnostic;
import vadl.gcb.passes.GenerateGcbIntrinsicsPass;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionRegisterFileOperand;
import vadl.gcb.valuetypes.ValueType;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.operands.TableGenInstructionImmediateOperand;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Specification;

/**
 * This file contains the tablegen setup for builtins.
 */
public class EmitBuiltinsTableGenPass extends LcbTemplateRenderingPass {

  public EmitBuiltinsTableGenPass(LcbConfiguration configuration)
      throws IOException {
    super(configuration);
  }

  @Override
  protected String getOutputPath() {
    return "clang/include/clang/Basic/Builtins" + lcbConfiguration().targetName().value() + ".td";
  }

  record Prototype(String returnType, List<String> params) {

  }

  record Builtin(String name, Prototype prototype) implements Renderable {
    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "name", name,
          "prototype",
          String.format("%s(%s)", prototype.returnType, String.join(", ", prototype.params))
      );
    }
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var machineRecords =
        (List<TableGenMachineInstruction>) passResults.lastResultOf(
            GenerateTableGenMachineInstructionRecordPass.class);
    var output = (GenerateGcbIntrinsicsPass.Output) passResults.lastResultOf(
        GenerateGcbIntrinsicsPass.class);
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "builtins", createBuiltins(output, machineRecords));
  }

  private List<Builtin> createBuiltins(GenerateGcbIntrinsicsPass.Output output,
                                       List<TableGenMachineInstruction> records) {
    var lookup = records.stream().collect(Collectors.toMap(
        TableGenMachineInstruction::instruction, x -> x));
    return output.intrinsics().stream()
        .map(intrinsic -> {
          var record = Objects.requireNonNull(lookup.get(intrinsic.instruction()));
          var returnTy = mapTy(record.getOutOperands().stream().findFirst());
          var paramsTy = record.getInOperands().stream().map(this::mapTy).toList();
          var prototype = new Prototype(returnTy, paramsTy);
          return new Builtin(intrinsic.builtinName(), prototype);
        })
        .toList();
  }

  private String mapTy(GcbInstructionOperand operand) {
    return mapTy(Optional.of(operand));
  }

  private String mapTy(Optional<GcbInstructionOperand> operand) {
    if (operand.isEmpty()) {
      return "void";
    }

    var o = operand.get();

    if (o instanceof GcbInstructionRegisterFileOperand registerFileOperand) {
      var ty = registerFileOperand.registerFile().resultType().fittingCppType();

      if (ty == null) {
        throw Diagnostic.error("Register file has no C++ type", registerFileOperand.origin())
            .build();
      }

      return CppTypeMap.getCppBuiltinTypeNameByVadlType(ValueType.from(ty).get());
    } else if (o instanceof TableGenInstructionImmediateOperand immediateOperand) {
      var ty = immediateOperand.immediateOperand().llvmType();

      if (ty == null) {
        throw Diagnostic.error("Immediate file has no C++ type", immediateOperand.origin())
            .build();
      }

      return CppTypeMap.getCppBuiltinTypeNameByVadlType(ty);
    }

    throw Diagnostic.error("Operand not supported for builtin generation",
            o.origin().location())
        .description(
            "If this exception is raised then add support for the operand type or skip it.")
        .build();
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/clang/include/Basic/Builtins.td";
  }
}
