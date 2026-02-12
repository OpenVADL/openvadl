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

package vadl.gcb.passes;

import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.gcb.passes.operands.InstructionOperandsCtx;
import vadl.gcb.passes.operands.model.GcbInstructionImmediateOperand;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionRegisterFileOperand;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.ReadsRegisterTensor;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.passes.SnapshotInstructionBehaviorPass;

/**
 * Compute the intrinsics for an {@link Instruction}.
 */
public class GenerateGcbIntrinsicsPass extends Pass {
  private final GcbConfiguration gcbConfiguration;

  public GenerateGcbIntrinsicsPass(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
    this.gcbConfiguration = gcbConfiguration;
  }

  @Override
  public PassName getName() {
    return new PassName("DetermineIntrinsicAttributesPass");
  }

  /**
   * Value type for an intrinsic.
   */
  public record GcbIntrinsic(String builtinName,
                             String intrinsicName,
                             Instruction instruction,
                             List<InstructionBuiltinAttributesCtx.Attribute> builtinAttributes,
                             List<InstructionIntrinsicAttributesCtx.Attribute> intrinsicAttributes
  ) {

  }

  /**
   * Output container of the pass.
   */
  public record Output(
      IdentityHashMap<Instruction, List<InstructionIntrinsicAttributesCtx.Attribute>> lookup,
      List<GcbIntrinsic> intrinsics) {

  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var snapshots =
        (Map<Instruction, Graph>) passResults.lastResultOf(SnapshotInstructionBehaviorPass.class);
    var builtins =
        (IdentityHashMap<Instruction, List<InstructionBuiltinAttributesCtx.Attribute>>)
            passResults.lastResultOf(DetermineBuiltinAttributesPass.class);
    var intrinsics = new ArrayList<GcbIntrinsic>();

    IdentityHashMap<Instruction, List<InstructionIntrinsicAttributesCtx.Attribute>> map =
        new IdentityHashMap<>();

    for (var instruction : viam.isa().orElseThrow().ownInstructions()) {
      var snapshot = Objects.requireNonNull(snapshots.get(instruction));
      var operands = instruction.expectExtension(InstructionOperandsCtx.class);

      if (!builtins.containsKey(instruction)
          || isRedFlag(viam, snapshot)
          || !hasValidInputOperands(operands.inputs())
          || !hasValidOutputOperands(operands.outputs())) {
        continue;
      }

      var isNoMem = isNoMem(snapshot);
      var speculatable = speculatable(snapshot);

      var attributes = new ArrayList<InstructionIntrinsicAttributesCtx.Attribute>();
      if (isNoMem) {
        attributes.add(InstructionIntrinsicAttributesCtx.Attribute.NoMem);
      }
      if (speculatable) {
        attributes.add(InstructionIntrinsicAttributesCtx.Attribute.Speculatable);
      }

      var builtinName = instruction.simpleName();
      var intrinsicName = gcbConfiguration.targetName().value() + "_" + instruction.simpleName();

      map.put(instruction, attributes);
      instruction.attachExtension(new InstructionIntrinsicAttributesCtx(attributes));
      intrinsics.add(
          new GcbIntrinsic(builtinName,
              intrinsicName,
              instruction,
              builtins.get(instruction),
              attributes));
    }

    return new Output(map, intrinsics);
  }

  private boolean isRedFlag(Specification viam, Graph snapshot) {
    var pc =
        Objects.requireNonNull(ensurePresent(viam.isa(), "must be present").pc()).registerTensor();
    return snapshot.getNodes(ReadsRegisterTensor.class).anyMatch(
        x -> x.registerTensor().isSingleRegister() && x.registerTensor() == pc);
  }

  private boolean isMem(Graph snapshot) {
    return !snapshot.getNodes(WriteMemNode.class).toList().isEmpty()
        || !snapshot.getNodes(ReadMemNode.class).toList().isEmpty();
  }

  private boolean isNoMem(Graph snapshot) {
    return !isMem(snapshot);
  }

  /**
   * Compute a special attribute for LLVM.
   * The following conditions must hold:
   * x) No side effects
   * x) Does not write memory
   * x) Does not perform I/O
   * x) Does not change global state
   * x) Cannot trap or fault
   * x) No division-by-zero traps
   */
  private boolean speculatable(Graph snapshot) {
    return isNoMem(snapshot) && snapshot.getNodes(ProcCallNode.class).toList().isEmpty();
  }

  private boolean hasValidInputOperands(List<GcbInstructionOperand> operands) {
    return operands.stream()
        .allMatch(operand -> operand instanceof GcbInstructionRegisterFileOperand
            || operand instanceof GcbInstructionImmediateOperand);
  }

  private boolean hasValidOutputOperands(List<GcbInstructionOperand> operands) {
    return operands.size() <= 1 /* requires exactly one output and or zero */
        && operands.stream()
        .allMatch(operand -> operand instanceof GcbInstructionRegisterFileOperand);
  }
}
