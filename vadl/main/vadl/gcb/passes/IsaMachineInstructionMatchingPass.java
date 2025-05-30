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
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Counter;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
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
import vadl.viam.matching.impl.AnyReadRegFileMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.FieldAccessRefMatcher;
import vadl.viam.matching.impl.IsReadRegMatcher;
import vadl.viam.matching.impl.WriteResourceMatcherForValue;
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
  public Result execute(PassResults passResults,
                        Specification viam)
      throws IOException {
    // The instruction matching happens on the uninlined graph
    // because the field accesses are uninlined.
    IdentityHashMap<Instruction, UninlinedGraph> uninlined =
        ((FunctionInlinerPass.Output) passResults
            .lastResultOf(FunctionInlinerPass.class)).behaviors();
    Objects.requireNonNull(uninlined);

    var isa = viam.isa().orElse(null);
    if (isa == null) {
      return new Result(Collections.emptyMap(), Collections.emptyMap());
    }

    var pc = isa.pc();
    ensure(pc != null && pc.registerTensor().isSingleRegister(),
        () -> Diagnostic.error("Only counter to single registers are supported.",
            Objects.requireNonNull(isa.pc()).location()));

    isa.ownInstructions().forEach(instruction -> {
      // Get uninlined or the normal behaviors if nothing was uninlined.
      var behavior = ensureNonNull(uninlined.get(instruction),
          () -> Diagnostic.error("Cannot find the uninlined graph of this instruction",
              instruction.location()));

      var ty = getType(behavior);

      // Some are typed and some aren't.
      // The reason is that most of the time we do not care because
      // the instruction selection will figure out the types anyway.
      // The raw cases where we need the type are typed like addition.
      if (findLui(behavior)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.LUI, ty));
      } else if (findAdd32Bit(behavior)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.ADD_32, ty));
      } else if (findAdd64Bit(behavior)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.ADD_64, ty));
      } else if (findAddWithImmediate32Bit(behavior)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.ADDI_32, ty));
      } else if (findAddWithImmediate64Bit(behavior)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.ADDI_64, ty));
      } else if (findRegisterRegisterOrRegisterImmediateOrImmediateRegister(behavior,
          List.of(SDIV, SDIVS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.SDIV, ty));
      } else if (findRegisterRegisterOrRegisterImmediateOrImmediateRegister(behavior,
          List.of(UDIV, UDIVS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.UDIV, ty));
      } else if (findRegisterRegisterOrRegisterImmediateOrImmediateRegister(behavior,
          List.of(SMOD, SMODS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.SMOD, ty));
      } else if (findRegisterRegisterOrRegisterImmediateOrImmediateRegister(behavior,
          List.of(UMOD, UMODS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.UMOD, ty));
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
      } else if (findRegisterImmediateOrImmediateRegister(behavior, List.of(LSL, LSLS))
          /* the `hasNot` constraints are to differentiate between `SLLI` and `SLLIW` */
          && hasNot(behavior, TruncateNode.class)
          && hasNot(behavior, SignExtendNode.class)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.SLLI, ty));
      } else if (findRR(behavior, List.of(LSR, LSRS))) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.SRL, ty));
      } else if (pc != null && findBranchWithConditional(behavior, EQU)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BEQ, Optional.empty()));
      } else if (pc != null && findBranchWithConditional(behavior, NEQ)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BNEQ, Optional.empty()));
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(SGEQ))) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BSGEQ, Optional.empty()));
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(UGEQ))) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BUGEQ, Optional.empty()));
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(SLEQ))) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BSLEQ, Optional.empty()));
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(ULEQ))) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BULEQ, Optional.empty()));
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(SLTH))) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BSLTH, Optional.empty()));
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(ULTH))) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BULTH, Optional.empty()));
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(SGTH))) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.BSGTH, Optional.empty()));
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(UGTH))) {
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
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.STORE_MEM, ty));
      } else if (findLoadMem(behavior)) {
        instruction.attachExtension(
            new MachineInstructionCtx(MachineInstructionLabel.LOAD_MEM, ty));
      } else if (pc != null && findJalr(behavior, pc)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.JALR, ty));
      } else if (pc != null && findJal(behavior, pc)) {
        instruction.attachExtension(new MachineInstructionCtx(MachineInstructionLabel.JAL, ty));
      }
    });

    var labels = createLabelMap(viam);
    return new Result(labels, flipIsaMatching(labels));
  }

  private Optional<BitsType> getType(UninlinedGraph behavior) {
    var candidates =
        Stream.concat(
                behavior.getNodes(WriteRegTensorNode.class)
                    .filter(x -> x.regTensor().isRegisterFile())
                    .map(x -> (DataType) x.value().type()),
                Stream.concat(
                    behavior.getNodes(WriteRegTensorNode.class)
                        .filter(x -> x.regTensor().isSingleRegister())
                        .map(x -> (DataType) x.value().type()),
                    Stream.concat(
                        behavior.getNodes(ReadRegTensorNode.class)
                            .filter(x -> x.regTensor().isSingleRegister())
                            .map(x -> x.regTensor().resultType()),
                        behavior.getNodes(ReadRegTensorNode.class)
                            .filter(x -> x.regTensor().isRegisterFile())
                            .map(x -> x.regTensor().resultType())
                    )
                )
            )
            .map(x -> BitsType.bits(x.bitWidth())) // LLVM only accepts signed integers anyway
            .toList();

    if (candidates.isEmpty()) {
      return Optional.empty();
    }

    var allSame = candidates.stream()
        .allMatch(element -> Objects.equals(candidates.get(0), element));

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
            new BuiltInMatcher(builtins, List.of(
                new AnyChildMatcher(new AnyReadRegFileMatcher()),
                new AnyChildMatcher(new AnyReadRegFileMatcher())
            )))
        .stream()
        .map(x -> (BuiltInCall) x)
        .anyMatch(x -> x.usages().allMatch(y -> y instanceof TruncateNode)
            || x.arguments().stream().allMatch(arg -> arg instanceof TruncateNode)
            || behavior.getNodes(TruncateNode.class).findAny().isEmpty()
        );
  }

  private boolean findRR_MultiplicationHigh(UninlinedGraph behavior,
                                            Set<BuiltInTable.BuiltIn> builtins) {
    // We need a multiplication which is defined in `builtins` and then a slice node
    // which gets the top part.
    return
        TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
                new BuiltInMatcher(builtins, List.of(
                    new AnyChildMatcher(new AnyReadRegFileMatcher()),
                    new AnyChildMatcher(new AnyReadRegFileMatcher())
                )))
            .stream()
            .map(x -> (BuiltInCall) x)
            .anyMatch(node -> {
              /*
                Example: `ty` is `int128`
                then `high` is `128` and `64`.
                A SliceNode requires the bounds `lsb` = `64` and `msb` = `127`.
               */
              var ty = (BitsType) node.type();
              var high = ty.bitWidth();
              var low = high / 2;
              return node.usages().allMatch(
                  usage -> usage instanceof SliceNode sliceNode
                      && sliceNode.bitSlice().lsb() == low
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

      ensure(hasAddi,
          () -> Diagnostic.error(
              "There must be an instruction (addition with immediate), but we haven't found any.",
              viam.location()));
    });
  }

  private boolean findLoadMem(UninlinedGraph graph) {
    // TODO: @kper refactor (duplicated code)
    var writesRegFile = graph.getNodes(WriteRegTensorNode.class)
        .filter(e -> e.regTensor().isRegisterFile())
        .toList().size();
    var writesReg = graph.getNodes(WriteRegTensorNode.class)
        .filter(e -> e.regTensor().isSingleRegister())
        .toList().size();

    if ((writesRegFile == 1) == (writesReg == 1)) {
      return false;
    }

    var matched = TreeMatcher.matches(
        graph.getNodes(WriteResourceNode.class).map(x -> x),
        new WriteResourceMatcherForValue(new AnyChildMatcher(new AnyReadMemMatcher())));

    return !matched.isEmpty();
  }

  private boolean findWriteMem(UninlinedGraph graph) {
    if (graph.getNodes(WriteMemNode.class).toList().size() != 1) {
      return false;
    }

    var matched = TreeMatcher.matches(graph.getNodes(WriteResourceNode.class).map(x -> x),
        new WriteResourceMatcherForValue(new AnyChildMatcher(new AnyReadRegFileMatcher())));

    return !matched.isEmpty();
  }

  private boolean findLui(UninlinedGraph behavior) {
    var fieldAccess = behavior.getNodes(FieldAccessRefNode.class).findFirst();

    if (fieldAccess.isPresent()) {
      var matched = TreeMatcher.matches(
              fieldAccess.get()
                  .fieldAccess()
                  .accessFunction()
                  .behavior()
                  .getNodes(BuiltInCall.class)
                  .map(x -> x),
              new BuiltInMatcher(LSL, List.of(
                  new AnyNodeMatcher(),
                  new AnyConstantValueMatcher()
              )))
          .stream()
          .findFirst();

      return matched.isPresent()
          && writesExactlyOneRegisterClass(behavior)
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
            new BuiltInMatcher(ADD, List.of(
                new AnyChildMatcher(new AnyReadRegFileMatcher()),
                new AnyChildMatcher(new AnyReadRegFileMatcher())
            )))
        .stream()
        .map(x -> ((BuiltInCall) x).type())
        .filter(ty -> ty instanceof BitsType bi && bi.bitWidth() == bitWidth)
        .findFirst();

    return matched.isPresent()
        && writesExactlyOneRegisterClassWithType(behavior, Type.bits(bitWidth));
  }

  private boolean findAddWithImmediate32Bit(UninlinedGraph behavior) {
    return findAddWithImmediate(behavior, 32);
  }

  private boolean findAddWithImmediate64Bit(UninlinedGraph behavior) {
    return findAddWithImmediate(behavior, 64);
  }

  private boolean findAddWithImmediate(UninlinedGraph behavior, int bitWidth) {
    var matcher =
        new BuiltInMatcher(List.of(ADD, ADDS),
            List.of(new AnyChildMatcher(new AnyReadRegFileMatcher()),
                new AnyChildMatcher(new FieldAccessRefMatcher())));

    // We use a set because we want to allow commutativity.
    Set<Matcher> matchers = Set.of(
        matcher,
        matcher.swapOperands()
    );

    var matched = TreeMatcher.matches(
            () -> behavior.getNodes(BuiltInCall.class).map(x -> x), matchers)
        .stream()
        .map(x -> ((BuiltInCall) x).type())
        .filter(ty -> ty instanceof BitsType && ((BitsType) ty).bitWidth() == bitWidth)
        .findFirst();

    return matched.isPresent()
        && writesExactlyOneRegisterClassWithType(behavior, Type.bits(bitWidth));
  }

  private boolean findBranchWithConditional(UninlinedGraph behavior,
                                            BuiltInTable.BuiltIn builtin) {
    return findBranchWithConditional(behavior, Set.of(builtin));
  }

  private boolean findBranchWithConditional(UninlinedGraph behavior,
                                            Set<BuiltInTable.BuiltIn> builtins) {
    var hasCondition =
        behavior.getNodes(IfNode.class)
            .anyMatch(
                x -> x.condition() instanceof BuiltInCall
                    && builtins.contains(((BuiltInCall) x.condition()).builtIn()));
    var writesPc = behavior.getNodes(WriteRegTensorNode.class)
        .anyMatch(x -> x.staticCounterAccess() != null);

    return hasCondition && writesPc;
  }

  /**
   * Match Jump and Link Register when {@link Instruction} writes PC, writes
   * a register file and has an operation (ADD, SUB) where one input is a registerfile.
   */
  private boolean findJalr(UninlinedGraph behavior, Counter pcRegister) {
    var writesPc =
        behavior.getNodes(WriteRegTensorNode.class)
            .filter(x -> x.regTensor().equals(pcRegister.registerTensor()))
            .toList();
    var writesRegFile = behavior.getNodes(WriteRegTensorNode.class)
        .filter(w -> w.regTensor().isRegisterFile()).toList();

    var matcher = new BuiltInMatcher(List.of(BuiltInTable.ADD, BuiltInTable.ADDS, SUB), List.of(
        new AnyChildMatcher(new AnyReadRegFileMatcher()),
        new AnyNodeMatcher()
    ));
    Set<Matcher> matchers = Set.of(
        matcher,
        matcher.swapOperands()
    );

    var inputRegister = TreeMatcher.matches(
        () -> behavior.getNodes(BuiltInCall.class).map(x -> x),
        matchers
    );

    return writesPc.size() == 1 && writesRegFile.size() == 1 && !inputRegister.isEmpty();
  }

  /**
   * Match Jump and Link when {@link Instruction} writes PC, writes
   * a register file and has an operation (ADD, SUB) where one input is a PC.
   */
  private boolean findJal(UninlinedGraph behavior, Counter pcRegister) {
    var writesPc =
        behavior.getNodes(WriteRegTensorNode.class)
            .filter(x -> x.regTensor().equals(pcRegister.registerTensor()))
            .toList();
    var writesRegFile = behavior.getNodes(WriteRegTensorNode.class)
        .filter(w -> w.regTensor().isRegisterFile()).toList();

    var matcher = new BuiltInMatcher(List.of(BuiltInTable.ADD, BuiltInTable.ADDS, SUB), List.of(
        new AnyChildMatcher(new IsReadRegMatcher(pcRegister.registerTensor())),
        new AnyNodeMatcher()
    ));
    Set<Matcher> matchers = Set.of(
        matcher,
        matcher.swapOperands()
    );
    var inputRegister = TreeMatcher.matches(() -> behavior.getNodes(BuiltInCall.class).map(x -> x),
        matchers
    );

    return writesPc.size() == 1 && writesRegFile.size() == 1 && !inputRegister.isEmpty();
  }
}
