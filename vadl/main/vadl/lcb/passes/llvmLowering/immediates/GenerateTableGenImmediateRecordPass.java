// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.lcb.passes.llvmLowering.immediates;

import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.CppTypeMap;
import vadl.error.Diagnostic;
import vadl.gcb.valuetypes.ValueType;
import vadl.lcb.passes.llvmLowering.GenerateTableGenRegistersPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.viam.Instruction;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.annotations.FieldAccessAnnotation;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.passes.NormalizeFieldsToFieldAccessFunctionsPass;
import vadl.viam.passes.SnapshotInstructionBehaviorPass;

/**
 * This pass extracts the immediates from the TableGen records.
 */
public class GenerateTableGenImmediateRecordPass extends Pass {

  public GenerateTableGenImmediateRecordPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateTableGenImmediateRecordPass");
  }

  @Nullable
  @Override
  public List<TableGenImmediateRecord> execute(PassResults passResults,
                                               Specification viam) throws IOException {
    var snapshots =
        (Map<Instruction, Graph>) passResults.lastResultOf(SnapshotInstructionBehaviorPass.class);
    var immediates = new ArrayList<TableGenImmediateRecord>();
    var generateTableGenRegistersPassOutput =
        ((GenerateTableGenRegistersPass.Output) passResults.lastResultOf(
            GenerateTableGenRegistersPass.class));
    var registerFiles =
        Stream.concat(generateTableGenRegistersPassOutput.registerClasses().stream(),
            generateTableGenRegistersPassOutput.aliasRegisterClasses().stream()).toList();
    var smallestRegisterWidth = registerFiles
      .stream()
      .flatMap(r -> r.regTypes().stream())
      .min(new Comparator<>() {
        @Override
        public int compare(ValueType o1, ValueType o2) {
          return o1.getBitwidth() - o2.getBitwidth();
        }
      })
      // TODO
      .get();

    // We do it first for machine instructions.
    snapshots.entrySet().stream().sorted(
            Comparator.comparing(o -> o.getKey().identifier.simpleName()))
        .forEach(
            (entry) -> {
              var instruction = entry.getKey();
              var graph = entry.getValue();
              var fieldAccesses = graph.getNodes(FieldAccessRefNode.class).toList();

              fieldAccesses.forEach(fieldAccessRefNode -> {
                var fieldAccess = fieldAccessRefNode.fieldAccess();
                // When a field access is changed to a field access function it is
                // added the instruction format's field accesses. Therefore,
                // we will have a lot field accesses which are not part of the instruction's
                // behavior.
                if (fieldAccess
                    instanceof NormalizeFieldsToFieldAccessFunctionsPass.GeneratedFieldAccess
                    genFieldAccess) {
                  if (!genFieldAccess.instruction().equals(instruction)) {
                    // If we have generated a field access for an instruction then only generate
                    // an immediate record if it's the same instruction.
                    return;
                  }
                }

                var type = (BitsType) fieldAccessRefNode.type().asDataType();
                var upcastedType = CppTypeMap.upcast(type.makeSigned());
                var upcastedValueType =
                    ensurePresent(
                        ValueType.from(upcastedType), 
                        () -> Diagnostic.error("Compiler generator was not able to change the type to the architecture's " + "bit width: " + upcastedType.toString(),
                          fieldAccess.location()));
                upcastedValueType = upcastedValueType.getBitwidth() < smallestRegisterWidth.getBitwidth()
                    ? smallestRegisterWidth
                    : upcastedValueType;
                var upcastAnnotation = instruction.annotation(FieldAccessAnnotation.class);
                if (upcastAnnotation != null) {
                  var upcastedCppType = CppTypeMap.upcast(upcastAnnotation.resultBitWidth());
                  upcastedValueType = ValueType.from(upcastedCppType.makeSigned())
                      .orElseThrow(() -> Diagnostic.error(
                          "Unable to cast access to requested bit width: "
                              + upcastAnnotation.resultBitWidth(),
                          upcastAnnotation.location()).build());
                }
                immediates.add(new TableGenImmediateRecord(instruction,
                    fieldAccess,
                    upcastedValueType));

              });
            });

    // But, we also have to do it for pseudo instructions.
    // Because, we generate immediates for every instruction (and not format anymore).
    // In the case of RISC-V's `J` case, we have to generate an immediate for `immS`.
    viam.isa().orElseThrow()
        .ownPseudoInstructions().forEach(pseudoInstruction -> {
          for (var machineInstruction : pseudoInstruction.behavior().getNodes(InstrCallNode.class)
              .toList()) {
            for (var operand : machineInstruction.getParamFieldsOrAccesses()) {
              /*
              # Here is `immS` a field access function, and we need to generate an immediate record.
              pseudo instruction J( offset : SIntR ) =
              {
                JAL{ rd = 0 as Bits5, immS = offset }
              }
               */
              if (operand.isRight()) {
                var fieldAccess = operand.right();
                var llvmType = ValueType.from(CppTypeMap.upcast(
                    fieldAccess.accessFunction().signature().resultType()));
                immediates.add(
                    new TableGenImmediateRecord(pseudoInstruction, fieldAccess, llvmType.get()));
              }
            }
          }
        });

    return immediates;
  }
}
