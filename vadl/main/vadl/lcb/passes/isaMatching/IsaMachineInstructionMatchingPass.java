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

package vadl.lcb.passes.isaMatching;

import static vadl.types.BuiltInTable.ADD;
import static vadl.types.BuiltInTable.ADDS;
import static vadl.types.BuiltInTable.AND;
import static vadl.types.BuiltInTable.ANDS;
import static vadl.types.BuiltInTable.EQU;
import static vadl.types.BuiltInTable.LSL;
import static vadl.types.BuiltInTable.LSLS;
import static vadl.types.BuiltInTable.LSR;
import static vadl.types.BuiltInTable.LSRS;
import static vadl.types.BuiltInTable.MUL;
import static vadl.types.BuiltInTable.MULS;
import static vadl.types.BuiltInTable.NEQ;
import static vadl.types.BuiltInTable.OR;
import static vadl.types.BuiltInTable.ORS;
import static vadl.types.BuiltInTable.SDIV;
import static vadl.types.BuiltInTable.SDIVS;
import static vadl.types.BuiltInTable.SGEQ;
import static vadl.types.BuiltInTable.SGTH;
import static vadl.types.BuiltInTable.SLEQ;
import static vadl.types.BuiltInTable.SLTH;
import static vadl.types.BuiltInTable.SMOD;
import static vadl.types.BuiltInTable.SMODS;
import static vadl.types.BuiltInTable.SMULL;
import static vadl.types.BuiltInTable.SMULLS;
import static vadl.types.BuiltInTable.SUB;
import static vadl.types.BuiltInTable.SUBB;
import static vadl.types.BuiltInTable.SUBC;
import static vadl.types.BuiltInTable.SUBSB;
import static vadl.types.BuiltInTable.SUBSC;
import static vadl.types.BuiltInTable.UDIV;
import static vadl.types.BuiltInTable.UDIVS;
import static vadl.types.BuiltInTable.UGEQ;
import static vadl.types.BuiltInTable.UGTH;
import static vadl.types.BuiltInTable.ULEQ;
import static vadl.types.BuiltInTable.ULTH;
import static vadl.types.BuiltInTable.UMOD;
import static vadl.types.BuiltInTable.UMODS;
import static vadl.types.BuiltInTable.UMULL;
import static vadl.types.BuiltInTable.UMULLS;
import static vadl.types.BuiltInTable.XOR;
import static vadl.types.BuiltInTable.XORS;
import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.annotations.StatusRegisterAnnotation;
import vadl.gcb.passes.IsaMatchingUtils;
import vadl.gcb.passes.MachineInstructionCtx;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Counter;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.HasRegisterTensor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.ReadsRegisterTensor;
import vadl.viam.graph.WritesRegisterTensor;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.matching.Matcher;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyChildMatcher;
import vadl.viam.matching.impl.AnyConstantValueMatcher;
import vadl.viam.matching.impl.AnyNodeMatcher;
import vadl.viam.matching.impl.AnyReadMemMatcher;
import vadl.viam.matching.impl.AnyReadRegisterFileMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.FieldAccessRefMatcher;
import vadl.viam.matching.impl.IsReadRegMatcher;
import vadl.viam.matching.impl.ReadRegisterCounterMatcher;
import vadl.viam.matching.impl.WriteResourceMatcherForValue;
import vadl.viam.passes.SnapshotInstructionBehaviorPass;
import vadl.viam.passes.functionInliner.FunctionInlinerPass;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * A {@link InstructionSetArchitecture} contains a {@link List} of {@link Instruction}.
 * One of the most important tasks of the LCB is to recognize the semantics of the machine
 * instruction and create a mapping from LLVM's SelectionDag to the machine instruction.
 * Most instructions in an instruction set architecture will be "simple" and
 * a lot of them are required in almost every instruction set architecture. The goal
 * of {@link IsaMachineInstructionMatchingPass} to label instructions which can be recognized.
 * Why is this useful?
 * At some places, we need to create machine instructions by hand in LLVM, and we need to
 * know which instructions are supported by instruction set. This labelling makes it much
 * easier to search for these instructions.
 */
public class IsaMachineInstructionMatchingPass extends Pass implements IsaMatchingUtils {
  public IsaMachineInstructionMatchingPass(GcbConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("IsaMachineInstructionMatchingPass");
  }

  /**
   * Output of the pass.
   */
  public record Result(Map<MachineInstructionLabel, List<Instruction>> labels,
                       Map<Instruction, MachineInstructionLabel> reverse) {

  }

  @Nullable
  @Override
  public Result execute(PassResults passResults, Specification viam) throws IOException {
    // The instruction matching happens on the uninlined graph
    // because the field accesses are uninlined.
    IdentityHashMap<Instruction, UninlinedGraph> uninlined =
        ((FunctionInlinerPass.Output) passResults.lastResultOf(
            FunctionInlinerPass.class)).behaviors();
    Objects.requireNonNull(uninlined);
    final var snapshots =
        (Map<Instruction, Graph>) passResults.lastResultOf(SnapshotInstructionBehaviorPass.class);

    var isa = viam.isa().orElse(null);
    if (isa == null) {
      return new Result(Collections.emptyMap(), Collections.emptyMap());
    }

    // FIXME: a pc can be a single register in a register file. if that should be supported
    //        here, use `pc.isSingleRegister()` instead to check that
    //        See Issue #941
    var pc = isa.pc();
    ensureNonNull(pc, () -> Diagnostic.error("PC must not be null", isa.location()));
    ensure(pc.registerTensor().isSingleRegister(),
        () -> Diagnostic.error("Only counter to single registers are supported.",
            Objects.requireNonNull(isa.pc()).location()));

    isa.ownInstructions().forEach(instruction -> {
      // Get uninlined or the normal behaviors if nothing was uninlined.
      var behavior = ensureNonNull(uninlined.get(instruction),
          () -> Diagnostic.error("Cannot find the uninlined graph of this instruction",
              instruction.location()));
      var originalGraph = ensureNonNull(snapshots.get(instruction),
          () -> Diagnostic.error("Cannot find the unmodified graph of this instruction",
              instruction.location()));

      var ty = getType(behavior);

      // Some are typed and some aren't.
      // The reason is that most of the time we do not care because
      // the instruction selection will figure out the types anyway.
      // The raw cases where we need the type are typed like addition.
      if (findLui(behavior)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.LUI, ty));
      } else if (findAdd32Bit(behavior)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.ADD_32, Optional.empty()));
      } else if (findAdd64Bit(behavior)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.ADD_64, Optional.empty()));
      } else if (findAddWithImmediate32Bit(behavior)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.ADDI_32, ty));
      } else if (findAddWithImmediate64Bit(behavior)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.ADDI_64, ty));
      } else if (weakFindRR(behavior,
          List.of(SDIV, SDIVS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.SDIV, ty));
      } else if (weakFindRR(behavior,
          List.of(UDIV, UDIVS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.UDIV, ty));
      } else if (weakFindRR(behavior,
          List.of(SMOD, SMODS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.SMOD, ty));
      } else if (weakFindRR(behavior,
          List.of(UMOD, UMODS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.UMOD, ty));
      } else if (findSubS(behavior, originalGraph, Type.bits(64))) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.SUB_RR_WITH_STATUS_REGISTER_64,
                Optional.empty()));
      } else if (findSubS(behavior, originalGraph, Type.bits(32))) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.SUB_RR_WITH_STATUS_REGISTER_32,
                Optional.empty()));
      } else if (findCSEL_EQ(originalGraph, Type.signedInt(32))) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.CSEL_EQ_I32,
                Optional.empty()));
      } else if (findCSEL_EQ(originalGraph, Type.signedInt(64))) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.CSEL_EQ_I64,
                Optional.empty()));
      } else if (findCSEL_NEQ(originalGraph, Type.signedInt(32))) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.CSEL_EQ_I32,
                Optional.empty()));
      } else if (findCSEL_NEQ(originalGraph, Type.signedInt(64))) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.CSEL_NEQ_I64,
                Optional.empty()));
      } else if (findRegisterRegisterOrRegisterImmediateOrImmediateRegister(behavior, SUB)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.SUB, ty));
      } else if (findRegisterRegisterOrRegisterImmediateOrImmediateRegister(behavior,
          List.of(SUBB, SUBSB))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.SUBB, ty));
      } else if (findRegisterRegisterOrRegisterImmediateOrImmediateRegister(behavior,
          List.of(SUBC, SUBSC))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.SUBC, ty));
      } else if (findRegisterRegisterOrRegisterImmediateOrImmediateRegister(behavior,
          List.of(AND, ANDS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.AND, ty));
      } else if (findRR(behavior, List.of(OR, ORS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.OR, ty));
      } else if (findRR_MultiplicationHigh(behavior, Set.of(SMULL, SMULLS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.MULHS, ty));
      } else if (findRR_MultiplicationHigh(behavior, Set.of(UMULL, UMULLS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.MULHU, ty));
      } else if (findRegisterImmediateOrImmediateRegister(behavior, List.of(OR, ORS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.ORI, ty));
      } else if (findRR(behavior, List.of(XOR, XORS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.XOR, ty));
      } else if (findRegisterImmediateOrImmediateRegister(behavior, List.of(XOR, XORS))) {
        // Here is an exception:
        // Usually, it is good enough to group RR and RI together.
        // However, when generating alternative patterns for conditionals,
        // then we need the XORI instruction. Therefore, we put it extra.
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.XORI, ty));
      } else if (findRR_Mul(behavior, List.of(MUL, MULS, SMULL, SMULLS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.MUL, ty));
      } else if (findRR(behavior, List.of(LSL, LSLS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.SLL, ty));
      } else if (findRegisterImmediateOrImmediateRegister(behavior, List.of(LSL, LSLS)) && hasNot(
          behavior, TruncateNode.class) && hasNot(behavior, SignExtendNode.class)) {
        /* the `hasNot` constraints are to differentiate between `SLLI` and `SLLIW` */
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.SLLI, ty));
      } else if (findRR(behavior, List.of(LSR, LSRS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.SRL, ty));
      } else if (findBranchWithConditionalWithStatusRegisters(behavior, EQU)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BEQ_BY_STATUS_REGISTER,
                Optional.empty()));
      } else if (findBranchWithConditionalWithStatusRegisters(behavior, NEQ)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BNEQ_BY_STATUS_REGISTER,
                Optional.empty()));
      } else if (findBranchWithConditionalWithStatusRegisters(behavior, SGEQ)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BSGEQ_BY_STATUS_REGISTER,
                Optional.empty()));
      } else if (findBranchWithConditionalWithStatusRegisters(behavior, SGTH)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BSGTH_BY_STATUS_REGISTER,
                Optional.empty()));
      } else if (findBranchWithConditionalWithStatusRegisters(behavior, SLEQ)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BSLEQ_BY_STATUS_REGISTER,
                Optional.empty()));
      } else if (findBranchWithConditionalWithStatusRegisters(behavior, SLTH)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BSLTH_BY_STATUS_REGISTER,
                Optional.empty()));
      } else if (findBranchWithConditionalWithoutStatusRegisters(behavior, EQU)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BEQ, Optional.empty()));
      } else if (findBranchWithConditionalWithoutStatusRegisters(behavior, NEQ)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BNEQ, Optional.empty()));
      } else if (findBranchWithConditionalWithoutStatusRegisters(behavior, SGEQ)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BSGEQ, Optional.empty()));
      } else if (findBranchWithConditionalWithoutStatusRegisters(behavior, UGEQ)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BUGEQ, Optional.empty()));
      } else if (findBranchWithConditionalWithoutStatusRegisters(behavior, SLEQ)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BSLEQ, Optional.empty()));
      } else if (findBranchWithConditionalWithoutStatusRegisters(behavior, ULEQ)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BULEQ, Optional.empty()));
      } else if (findBranchWithConditionalWithoutStatusRegisters(behavior, SLTH)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BSLTH, Optional.empty()));
      } else if (findBranchWithConditionalWithoutStatusRegisters(behavior, ULTH)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BULTH, Optional.empty()));
      } else if (findBranchWithConditionalWithoutStatusRegisters(behavior, SGTH)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BSGTH, Optional.empty()));
      } else if (findBranchWithConditionalWithoutStatusRegisters(behavior, UGTH)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BUGTH, Optional.empty()));
      } else if (findRR(behavior, List.of(SLTH))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.LTS, ty));
      } else if (findRR(behavior, List.of(ULTH))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.LTU, ty));
      } else if (findRegisterImmediateOrImmediateRegister(behavior, List.of(SLTH))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.LTI, ty));
      } else if (findRegisterImmediateOrImmediateRegister(behavior, List.of(ULTH))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.LTIU, ty));
      } else if (findWriteMem(behavior)) {
        instruction.attachExtension(new MachineInstructionCtx(
            MachineInstructionLabel.STORE_MEM_WITH_IMMEDIATE, ty));
      } else if (findLoadMem(behavior)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.LOAD_MEM_WITH_IMMEDIATE, ty));
      } else if (findJalr(behavior, pc)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.JALR, ty));
      } else if (findJal(behavior, pc)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.JAL, ty));
      } else if (findJ(behavior, pc)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.J, ty));
      } else if (findJR(behavior, pc)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.JR, ty));
      }
    });

    var labels = createLabelMap(viam);
    return new Result(labels, flipIsaMatching(labels));
  }

  private boolean findCSEL_32(Graph originalGraph, Constant constant) {
    var selectNode = originalGraph.getNodes(SelectNode.class).findFirst();
    var writes = originalGraph.getNodes(WritesRegisterTensor.class).toList();

    if (writes.size() != 1) {
      return false;
    }

    if (writes.stream().anyMatch(x -> !x.hasRegisterFile())) {
      return false;
    }

    if (selectNode.isPresent()) {
      Predicate<Node> checkNode = (node) -> node instanceof ReadsRegisterTensor registerTensor
          && registerTensor.hasRegisterFile();

      if (checkNode.test(selectNode.get().trueCase())
          && checkNode.test(selectNode.get().falseCase())
          && selectNode.get().condition() instanceof BuiltInCall bc
          && bc.builtIn() == EQU
          && bc.arguments().get(1) instanceof ConstantNode constantNode
          && constantNode.constant().equals(constant)) {
        if (originalGraph.getNodes(ReadsRegisterTensor.class).anyMatch(x -> x.registerTensor()
            .hasAnnotation(StatusRegisterAnnotation.ZeroStatusRegisterAnnotation.class))) {

          return originalGraph.getNodes(TruncateNode.class)
              .anyMatch(x -> x.type().bitWidth() == Type.signedInt(32).bitWidth());
        }
      }
    }

    return false;
  }

  private boolean findCSEL_64(Graph originalGraph, Constant constant) {
    var selectNode = originalGraph.getNodes(SelectNode.class).findFirst();
    var writes = originalGraph.getNodes(WritesRegisterTensor.class).toList();

    if (writes.size() != 1) {
      return false;
    }

    if (writes.stream().anyMatch(x -> !x.hasRegisterFile())) {
      return false;
    }

    if (selectNode.isPresent()) {
      Predicate<Node> checkNode = (node) -> node instanceof ReadsRegisterTensor registerTensor
          && registerTensor.hasRegisterFile();

      if (checkNode.test(selectNode.get().trueCase())
          && checkNode.test(selectNode.get().falseCase())
          && selectNode.get().condition() instanceof BuiltInCall bc
          && bc.builtIn() == EQU
          && bc.arguments().get(1) instanceof ConstantNode constantNode
          && constantNode.constant().equals(constant)) {
        if (originalGraph.getNodes(ReadsRegisterTensor.class).anyMatch(x -> x.registerTensor()
            .hasAnnotation(StatusRegisterAnnotation.ZeroStatusRegisterAnnotation.class))) {

          return originalGraph.getNodes(TruncateNode.class).findAny().isEmpty();
        }
      }
    }

    return false;
  }


  private boolean findCSEL_EQ(Graph originalGraph, SIntType ty) {
    if (ty.bitWidth() == 32) {
      return findCSEL_32(originalGraph, Constant.Value.one(DataType.bits(1)));
    } else if (ty.bitWidth() == 64) {
      return findCSEL_64(originalGraph, Constant.Value.one(DataType.bits(1)));
    }

    return false;
  }

  private boolean findCSEL_NEQ(Graph originalGraph, SIntType ty) {
    if (ty.bitWidth() == 32) {
      return findCSEL_32(originalGraph, Constant.Value.zero(DataType.bits(1)));
    } else if (ty.bitWidth() == 64) {
      return findCSEL_64(originalGraph, Constant.Value.zero(DataType.bits(1)));
    }

    return false;
  }

  private Optional<BitsType> getType(UninlinedGraph behavior) {
    var candidates = Stream.concat(
            behavior.getNodes(WriteRegTensorNode.class).filter(x -> x.regTensor().isRegisterFile())
                .map(x -> (DataType) x.value().type()), Stream.concat(
                behavior.getNodes(WriteRegTensorNode.class)
                    .filter(x -> x.regTensor().isSingleRegister())
                    .map(x -> (DataType) x.value().type()), Stream.concat(
                    behavior.getNodes(ReadRegTensorNode.class)
                        .filter(x -> x.regTensor().isSingleRegister())
                        .map(x -> x.regTensor().resultType()),
                    behavior.getNodes(ReadRegTensorNode.class)
                        .filter(x -> x.regTensor().isRegisterFile())
                        .map(x -> x.regTensor().resultType()))))
        .map(x -> BitsType.bits(x.bitWidth())) // LLVM only accepts signed integers anyway
        .toList();

    if (candidates.isEmpty()) {
      return Optional.empty();
    }

    var allSame =
        candidates.stream().allMatch(element -> Objects.equals(candidates.get(0), element));

    if (allSame) {
      return candidates.stream().findFirst();
    } else {
      return Optional.empty();
    }
  }

  /**
   * Checks that the given {@code behavior} has no node of type {@code nodeClass} in the
   * graph.
   */
  private <T extends Node> boolean hasNot(UninlinedGraph behavior, Class<T> nodeClass) {
    return behavior.getNodes(nodeClass).findAny().isEmpty();
  }

  private boolean findRR_Mul(UninlinedGraph behavior, List<BuiltInTable.BuiltIn> builtins) {
    // There are two approaches:
    // (1) Cut the result
    // (2) Cut the inputs
    return TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
            new BuiltInMatcher(builtins, List.of(new AnyChildMatcher(
                    new AnyReadRegisterFileMatcher()),
                new AnyChildMatcher(new AnyReadRegisterFileMatcher())))).stream()
        .map(x -> (BuiltInCall) x).anyMatch(
            x -> x.usages().allMatch(y -> y instanceof TruncateNode) || x.arguments().stream()
                .allMatch(arg -> arg instanceof TruncateNode) || behavior.getNodes(
                TruncateNode.class).findAny().isEmpty());
  }

  private boolean findRR_MultiplicationHigh(UninlinedGraph behavior,
                                            Set<BuiltInTable.BuiltIn> builtins) {
    // We need a multiplication which is defined in `builtins` and then a slice node
    // which gets the top part.
    return TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
            new BuiltInMatcher(builtins, List.of(new AnyChildMatcher(
                    new AnyReadRegisterFileMatcher()),
                new AnyChildMatcher(new AnyReadRegisterFileMatcher())))).stream()
        .map(x -> (BuiltInCall) x).anyMatch(node -> {
          var ty = (BitsType) node.type();
          var high = ty.bitWidth();
          var low = high / 2;
          return node.usages().allMatch(
              usage -> usage instanceof SliceNode sliceNode && sliceNode.bitSlice().lsb() == low
                  && sliceNode.bitSlice().msb() == high - 1);
        }) && writesExactlyOneRegisterClass(behavior);
  }

  @Override
  public void verification(Specification viam, @Nullable Object passResult) {
    viam.isa().ifPresent(isa -> {
      Class<MachineInstructionCtx> clazz = MachineInstructionCtx.class;
      var hasAddi = isa.ownInstructions().stream().anyMatch(instruction -> {
        var ext = instruction.extension(clazz);

        return ext != null && (ext.label() == MachineInstructionLabel.ADDI_64
            || ext.label() == MachineInstructionLabel.ADDI_32);
      });

      ensure(hasAddi, () -> Diagnostic.error(
          "There must be an instruction (addition with immediate), but we haven't found any.",
          viam.location()));
    });
  }

  private boolean findLoadMem(UninlinedGraph graph) {
    var writesRegFile =
        graph.getNodes(WritesRegisterTensor.class).filter(HasRegisterTensor::hasRegisterFile)
            .count();

    var writesReg =
        graph.getNodes(WriteRegTensorNode.class).filter(e -> e.regTensor().isSingleRegister())
            .count();

    var immediates = graph.getNodes(FieldAccessRefNode.class).count();

    var readsMem = graph.getNodes(ReadMemNode.class).count();

    // We need at least one register file write and no single register write.
    if (writesRegFile != 1 || writesReg > 0) {
      return false;
    }

    // Requires to read memory.
    if (readsMem != 1) {
      return false;
    }

    // Requires at least one immediate
    if (immediates != 1) {
      return false;
    }

    // Only add is allowed as a builtin
    var builtins = graph.getNodes(BuiltInCall.class).toList();
    if (builtins.size() != 1 && builtins.stream()
        .noneMatch(x -> Set.of(ADD).contains(x.builtIn()))) {
      return false;
    }

    var matched = TreeMatcher.matches(graph.getNodes(WriteResourceNode.class).map(x -> x),
        new WriteResourceMatcherForValue(new AnyChildMatcher(new AnyReadMemMatcher())));

    return !matched.isEmpty();
  }

  private boolean findWriteMem(UninlinedGraph graph) {
    if (graph.getNodes(WriteMemNode.class).limit(2).count() != 1) {
      return false;
    }

    var writesRegFile =
        graph.getNodes(WritesRegisterTensor.class).filter(HasRegisterTensor::hasRegisterFile)
            .count();

    var writesReg =
        graph.getNodes(WriteRegTensorNode.class).filter(e -> e.regTensor().isSingleRegister())
            .count();

    var immediates = graph.getNodes(FieldAccessRefNode.class).count();

    // no writes except memory
    if (writesRegFile != 0 || writesReg != 0) {
      return false;
    }

    // Requires at least one immediate
    if (immediates != 1) {
      return false;
    }

    var matched = TreeMatcher.matches(graph.getNodes(WriteResourceNode.class).map(x -> x),
        new WriteResourceMatcherForValue(new AnyChildMatcher(new AnyReadRegisterFileMatcher())));

    return !matched.isEmpty();
  }

  private boolean findLui(UninlinedGraph behavior) {
    var fieldAccess = behavior.getNodes(FieldAccessRefNode.class).findFirst();

    if (fieldAccess.isPresent()) {
      var matched = TreeMatcher.matches(
              fieldAccess.get().fieldAccess().accessFunction().behavior()
                  .getNodes(BuiltInCall.class)
                  .map(x -> x),
              new BuiltInMatcher(LSL, List.of(new AnyNodeMatcher(), new AnyConstantValueMatcher())))
          .stream().findFirst();

      return matched.isPresent() && writesExactlyOneRegisterClass(behavior)
          // does not access PC
          && !hasAccessToPc(behavior);
    }

    return false;
  }

  private boolean hasAccessToPc(UninlinedGraph behavior) {
    return behavior.getNodes(ReadRegTensorNode.class)
        .anyMatch(x -> x.staticCounterAccess() != null);
  }

  private boolean findAdd32Bit(UninlinedGraph behavior) {
    return findAdd(behavior, 32);
  }

  private boolean findAdd64Bit(UninlinedGraph behavior) {
    return findAdd(behavior, 64);
  }

  private boolean findAdd(UninlinedGraph behavior, int bitWidth) {
    var matched = TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
            new BuiltInMatcher(List.of(ADD, ADDS),
                List.of(new AnyChildMatcher(new AnyReadRegisterFileMatcher()),
                    new AnyChildMatcher(new AnyReadRegisterFileMatcher())))).stream()
        .map(x -> ((BuiltInCall) x).type())
        .filter(ty -> ty instanceof BitsType bi && bi.bitWidth() == bitWidth).findFirst();

    return matched.isPresent()
        && writesExactlyOneRegisterClassWithType(behavior, Type.bits(bitWidth))
        && behavior.getNodes(SliceNode.class).findAny().isEmpty() // no slices to exclude `ADDXUXTB`
        && behavior.getNodes(BuiltInCall.class).count() == 1;
  }

  private boolean findAddWithImmediate32Bit(UninlinedGraph behavior) {
    return findAddWithImmediate(behavior, 32);
  }

  private boolean findAddWithImmediate64Bit(UninlinedGraph behavior) {
    return findAddWithImmediate(behavior, 64);
  }

  private boolean findAddWithImmediate(UninlinedGraph behavior, int bitWidth) {
    var matcher = new BuiltInMatcher(List.of(ADD, ADDS),
        List.of(new AnyChildMatcher(new AnyReadRegisterFileMatcher()),
            new AnyChildMatcher(new FieldAccessRefMatcher())));

    // We use a set because we want to allow commutativity.
    Set<Matcher> matchers = Set.of(matcher, matcher.swapOperands());

    var matched =
        TreeMatcher.matches(() -> behavior.getNodes(BuiltInCall.class).map(x -> x), matchers)
            .stream().map(x -> ((BuiltInCall) x).type())
            .filter(ty -> ty instanceof BitsType bitsType && bitsType.bitWidth() == bitWidth)
            .findFirst();

    // only one read is allowed
    var registerReads = behavior.getNodes(ReadsRegisterTensor.class)
        .filter(HasRegisterTensor::hasRegisterFile)
        .limit(2)
        .count();

    return registerReads == 1 && matched.isPresent()
        && writesExactlyOneRegisterClassWithType(behavior, Type.bits(bitWidth));
  }

  private boolean findBranchWithConditional(UninlinedGraph behavior, BuiltInTable.BuiltIn builtin) {
    var hasCondition = behavior.getNodes(IfNode.class)
        .anyMatch(x -> x.condition() instanceof BuiltInCall bc && builtin == bc.builtIn());
    var writesPc =
        behavior.getNodes(WriteRegTensorNode.class).anyMatch(x -> x.staticCounterAccess() != null);

    return hasCondition && writesPc;
  }

  private boolean findBranchWithConditionalWithoutStatusRegisters(UninlinedGraph behavior,
                                                                  BuiltInTable.BuiltIn builtin) {
    var base = findBranchWithConditional(behavior, builtin);
    return base
        && behavior.getNodes(ReadsRegisterTensor.class)
        .filter(x -> x.registerTensor().hasAnnotation(StatusRegisterAnnotation.class))
        .findAny()
        .isEmpty()
        && behavior.getNodes(ProcCallNode.class).findAny().isEmpty();
  }

  private boolean findBranchWithConditionalWithStatusRegisters(UninlinedGraph behavior,
                                                               BuiltInTable.BuiltIn builtin) {
    var writesPc =
        behavior.getNodes(WriteRegTensorNode.class).anyMatch(x -> x.staticCounterAccess() != null);
    var hasIfNode = behavior.getNodes(IfNode.class).toList();
    var statusRegisters = behavior.getNodes(ReadsRegisterTensor.class)
        .filter(x -> x.registerTensor().hasAnnotation(StatusRegisterAnnotation.class)).toList();

    return !hasIfNode.isEmpty() && writesPc && !statusRegisters.isEmpty() && checkConditionsForBase(
        builtin, behavior, statusRegisters);
  }

  /**
   * We would like to see whether the instruction fulfills all the condition to be matched for
   * the given {@code base}. For example, if the {@code base} is "equality" then it needs a
   * status register which is the Zero Register and a constant which is {@code 1}.
   */
  private boolean checkConditionsForBase(BuiltInTable.BuiltIn base, UninlinedGraph behavior,
                                         List<ReadsRegisterTensor> registers) {
    if (base == EQU) {
      // Z == 1
      if (registers.size() == 1) {
        var hasCondition = behavior.getNodes(IfNode.class)
            .allMatch(x -> x.condition() instanceof BuiltInCall bc && bc.builtIn() == EQU);
        var hasConstant = behavior.getNodes(ConstantNode.class)
            .anyMatch(x -> x.isConstant() && x.constant().asVal().intValue() == 1);
        var register = registers.stream().findFirst().get();
        return hasCondition && hasConstant && register.registerTensor()
            .hasAnnotation(StatusRegisterAnnotation.ZeroStatusRegisterAnnotation.class);
      }
    } else if (base == NEQ) {
      // Z == 0
      if (registers.size() == 1) {
        var hasCondition = behavior.getNodes(IfNode.class)
            .allMatch(x -> x.condition() instanceof BuiltInCall bc && bc.builtIn() == EQU);
        var hasConstant = behavior.getNodes(ConstantNode.class)
            .anyMatch(x -> x.isConstant() && x.constant().asVal().intValue() == 0);
        var register = registers.stream().findFirst().get();
        return hasCondition && hasConstant && register.registerTensor()
            .hasAnnotation(StatusRegisterAnnotation.ZeroStatusRegisterAnnotation.class);
      }
    } else if (base == SGEQ) {
      // NZCV_N = NZCV_V
      if (registers.size() == 2) {
        var builtin = behavior.getNodes(IfNode.class)
            .filter(x -> x.condition() instanceof BuiltInCall bc && bc.builtIn() == EQU)
            .map(x -> (BuiltInCall) x.condition()).findFirst();

        if (builtin.isPresent()) {
          var arguments = builtin.get().arguments();
          return hasAllAnnotations(arguments,
              Set.of(StatusRegisterAnnotation.NegativeStatusRegisterAnnotation.class,
                  StatusRegisterAnnotation.OverflowStatusRegisterAnnotation.class));
        }
      }
    } else if (base == SLTH) {
      // NZCV_N != NZCV_V
      if (registers.size() == 2) {
        var builtin = behavior.getNodes(IfNode.class)
            .filter(x -> x.condition() instanceof BuiltInCall bc && bc.builtIn() == NEQ)
            .map(x -> (BuiltInCall) x.condition()).findFirst();

        if (builtin.isPresent()) {
          var arguments = builtin.get().arguments();

          return hasAllAnnotations(arguments,
              Set.of(StatusRegisterAnnotation.NegativeStatusRegisterAnnotation.class,
                  StatusRegisterAnnotation.OverflowStatusRegisterAnnotation.class));
        }
      }
    } else if (base == SGTH) {
      // N == V and Z == 0
      if (registers.size() == 3) {
        var builtins =
            behavior.getNodes(BuiltInCall.class).filter(x -> x.builtIn() == EQU).toList();

        // Needs at least one AND.
        if (behavior.getNodes(BuiltInCall.class).noneMatch(x -> x.builtIn() == AND)) {
          return false;
        }

        if (builtins.size() == 2) {
          for (var condBuiltin : builtins) {
            var arguments = condBuiltin.arguments();

            var negativeAndOverflow = hasAllAnnotations(arguments,
                Set.of(StatusRegisterAnnotation.NegativeStatusRegisterAnnotation.class,
                    StatusRegisterAnnotation.OverflowStatusRegisterAnnotation.class));
            var zeroFlag = hasAllAnnotations(arguments,
                Set.of(StatusRegisterAnnotation.ZeroStatusRegisterAnnotation.class));

            if (negativeAndOverflow) {
              return true;
            } else if (zeroFlag) {
              return arguments.stream().anyMatch(x -> x instanceof ConstantNode constantNode
                  && constantNode.constant().asVal().intValue() == 0);
            }
          }
        }
      }
    } else if (base == SLEQ) {
      // N != V  or Z == 1
      if (registers.size() == 3) {
        var builtins = behavior.getNodes(BuiltInCall.class)
            .filter(x -> x.builtIn() == EQU || x.builtIn() == NEQ).toList();

        // Needs at least one AND.
        if (behavior.getNodes(BuiltInCall.class).noneMatch(x -> x.builtIn() == OR)) {
          return false;
        }

        if (builtins.size() == 2) {
          for (var condBuiltin : builtins) {
            var arguments = condBuiltin.arguments();

            var negativeAndOverflow = hasAllAnnotations(arguments,
                Set.of(StatusRegisterAnnotation.NegativeStatusRegisterAnnotation.class,
                    StatusRegisterAnnotation.OverflowStatusRegisterAnnotation.class));
            var zeroFlag = hasAllAnnotations(arguments,
                Set.of(StatusRegisterAnnotation.ZeroStatusRegisterAnnotation.class));

            if (negativeAndOverflow) {
              return true;
            } else if (zeroFlag) {
              return arguments.stream().anyMatch(x -> x instanceof ConstantNode constantNode
                  && constantNode.constant().asVal().intValue() == 1);
            }
          }
        }
      }
    }

    // Default
    return false;
  }

  private boolean hasAllAnnotations(NodeList<ExpressionNode> arguments,
                                    Set<Class<? extends StatusRegisterAnnotation>> annotations) {
    for (var annotation : annotations) {
      var result = arguments.stream().anyMatch(
          x -> x instanceof ReadsRegisterTensor readsRegisterTensor
              && readsRegisterTensor.registerTensor().hasAnnotation(annotation));

      if (!result) {
        return false;
      }
    }

    return true;
  }

  /**
   * Match Jump and Link Register when {@link Instruction} writes PC, writes
   * a register file and has an operation (ADD, SUB) where one input is a registerfile.
   */
  private boolean findJalr(UninlinedGraph behavior, Counter pcRegister) {
    var writesPc = behavior.getNodes(WriteRegTensorNode.class)
        .filter(x -> x.regTensor().equals(pcRegister.registerTensor())).toList();
    var writesRegFile =
        behavior.getNodes(WriteRegTensorNode.class).filter(w -> w.regTensor().isRegisterFile())
            .toList();

    var matcher = new BuiltInMatcher(List.of(BuiltInTable.ADD, BuiltInTable.ADDS, SUB),
        List.of(new AnyChildMatcher(new AnyReadRegisterFileMatcher()), new AnyNodeMatcher()));
    Set<Matcher> matchers = Set.of(matcher, matcher.swapOperands());

    var inputRegister =
        TreeMatcher.matches(() -> behavior.getNodes(BuiltInCall.class).map(x -> x), matchers);

    return writesPc.size() == 1 && writesRegFile.size() == 1 && !inputRegister.isEmpty();
  }

  /**
   * Match {@link Instruction} which modifies the PC without an immediate not store the result into
   * a register.
   */
  private boolean findJR(UninlinedGraph behavior, Counter pcRegister) {
    var writesPc = behavior.getNodes(WriteRegTensorNode.class)
        .filter(x -> x.regTensor().equals(pcRegister.registerTensor())).toList();
    var writes = behavior.getNodes(WriteResourceNode.class).toList();
    var immediates = behavior.getNodes(FieldAccessRefNode.class).toList();

    return writesPc.size() == 1 && writes.size() == 1 && immediates.isEmpty();
  }

  /**
   * Match {@link Instruction} which modifies the PC with an immediate not store the result into
   * a register.
   */
  private boolean findJ(UninlinedGraph behavior, Counter pcRegister) {
    var writesPc = behavior.getNodes(WriteRegTensorNode.class)
        .filter(x -> x.regTensor().equals(pcRegister.registerTensor())).toList();
    var writes = behavior.getNodes(WriteResourceNode.class).toList();
    var builtins = behavior.getNodes(BuiltInCall.class).toList();

    var matcher = new BuiltInMatcher(List.of(BuiltInTable.ADD),
        List.of(new AnyChildMatcher(new ReadRegisterCounterMatcher(pcRegister)),
            new FieldAccessRefMatcher()));
    Set<Matcher> matchers = Set.of(matcher, matcher.swapOperands());

    var addition =
        TreeMatcher.matches(() -> behavior.getNodes(BuiltInCall.class).map(x -> x), matchers);

    return writesPc.size() == 1 && writes.size() == 1 && !addition.isEmpty()
        && builtins.size() == 1;
  }

  /**
   * Match Jump and Link when {@link Instruction} writes PC, writes
   * a register file and has an operation (ADD, SUB) where one input is a PC.
   */
  private boolean findJal(UninlinedGraph behavior, Counter pcRegister) {
    var writesPc = behavior.getNodes(WriteRegTensorNode.class)
        .filter(x -> x.regTensor().equals(pcRegister.registerTensor())).toList();
    var writesRegFile =
        behavior.getNodes(WriteRegTensorNode.class).filter(w -> w.regTensor().isRegisterFile())
            .toList();

    var matcher = new BuiltInMatcher(List.of(BuiltInTable.ADD, BuiltInTable.ADDS, SUB),
        List.of(new AnyChildMatcher(new IsReadRegMatcher(pcRegister.registerTensor())),
            new AnyNodeMatcher()));
    Set<Matcher> matchers = Set.of(matcher, matcher.swapOperands());
    var inputRegister =
        TreeMatcher.matches(() -> behavior.getNodes(BuiltInCall.class).map(x -> x), matchers);

    return writesPc.size() == 1 && writesRegFile.size() == 1 && !inputRegister.isEmpty();
  }
}
