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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.GenerateGcbIntrinsicsPass;
import vadl.gcb.passes.InstructionIntrinsicAttributesCtx;
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
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.utils.SourceLocation;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.passes.SnapshotInstructionBehaviorPass;

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
    var output =
        (GenerateGcbIntrinsicsPass.Output) passResults
            .lastResultOf(
                GenerateGcbIntrinsicsPass.class);
    var snapshots =
        (Map<Instruction, Graph>) passResults.lastResultOf(SnapshotInstructionBehaviorPass.class);
    var records = ((List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class)).stream().collect(Collectors.toMap(
        TableGenMachineInstruction::instruction, x -> x));

    var intrinsics = genIntrinsics(output, records, snapshots);

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
          attributes.stream().map(this::getName).collect(Collectors.joining(", "))
      );
    }

    private String getName(InstructionIntrinsicAttributesCtx.Attribute x) {
      return switch (x) {
        case NoMem -> "IntrNoMem";
        case Speculatable -> "IntrSpeculatable";
        case ReadMem -> "IntrReadMem";
        case WriteMem -> "IntrWriteMem";
        case ReadWriteMem -> "IntrReadWriteMem";
      };
    }
  }

  private List<Intrinsic> genIntrinsics(
      GenerateGcbIntrinsicsPass.Output output,
      Map<Instruction, TableGenMachineInstruction> records,
      Map<Instruction, Graph> snapshots) {
    var result = new ArrayList<Intrinsic>();

    for (var intrinsic : output.intrinsics()) {
      var instruction = intrinsic.instruction();
      var attrs = intrinsic.intrinsicAttributes();
      var record = ensureNonNull(records.get(instruction), "must not be null");
      var snapshot = ensureNonNull(snapshots.get(instruction), "must not be null");
      var paramTypes = record.getInOperands().stream().map(this::mapParam)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.toCollection(ArrayList::new));
      pointerType(snapshot).ifPresent(pointerTy -> paramTypes.set(0, pointerTy));

      var lcbIntrinsic =
          new Intrinsic(
              intrinsic.intrinsicName(),
              List.of("llvm_any_ty"),
              paramTypes,
              attrs);
      result.add(lcbIntrinsic);
    }

    result.sort(Comparator.comparing(o -> o.name));
    return result;
  }

  private Type createType(ValueType valueType) {
    var width = valueType.getBitwidth();
    return valueType.isSigned() ? SIntType.bits(width) : Type.unsignedInt(width);
  }

  private Optional<String> mapParam(GcbInstructionOperand gcbInstructionOperand) {
    if (gcbInstructionOperand instanceof GcbInstructionRegisterFileOperand op) {
      return Optional.of(mapType(op.registerFile().resultType()));
    } else if (gcbInstructionOperand instanceof TableGenInstructionImmediateOperand op) {
      return Optional.of(mapType((DataType) createType(op.immediateOperand().llvmType())));
    }

    throw Diagnostic.error("Not supported operand", gcbInstructionOperand.origin().location())
        .build();
  }

  private String mapType(DataType dataType) {
    var upcasted = Objects.requireNonNull(dataType.fittingCppType()).toBitsType();

    if (upcasted.equals(Type.bits(8))) {
      return "llvm_i8_ty";
    } else if (upcasted.equals(Type.bits(16))) {
      return "llvm_i16_ty";
    } else if (upcasted.equals(Type.bits(32))) {
      return "llvm_i32_ty";
    } else if (upcasted.equals(Type.bits(64))) {
      return "llvm_i64_ty";
    }

    throw Diagnostic.error("Cannot map type: " + dataType, SourceLocation.INVALID_SOURCE_LOCATION)
        .build();
  }

  private Optional<String> pointerType(Graph snapshot) {
    if (snapshot.getNodes(ReadMemNode.class).findFirst().isPresent()) {
      return Optional.of("llvm_ptr_ty");
    }

    if (snapshot.getNodes(WriteMemNode.class).findFirst().isPresent()) {
      return Optional.of("llvm_ptr_ty");
    }

    return Optional.empty();
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/include/llvm/IR/Intrinsics.td";
  }
}
