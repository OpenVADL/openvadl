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

package vadl.gcb.passes;

import static vadl.viam.ViamError.ensureNonNull;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.TupleType;
import vadl.types.Type;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.annotations.StatusRegisterAnnotation;
import vadl.viam.graph.Graph;
import vadl.viam.graph.WritesRegisterTensor;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.matching.Matcher;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyChildMatcher;
import vadl.viam.matching.impl.AnyReadRegisterFileMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.FieldAccessRefMatcher;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * This interface contains methods which might be useful for
 * {@link IsaMachineInstructionMatchingPass} and {@link IsaPseudoInstructionMatchingPass}.
 */
public interface IsaMatchingUtils {

  /**
   * The {@code matched} hashmap contains a list of {@link Instruction} or {@link PseudoInstruction}
   * as value.
   * This value extends this list with the given {@link Instruction} or {@link PseudoInstruction}
   * when the key is matched.
   */
  default <K, V> void extend(Map<K, List<V>> matched,
                             K label, V instruction) {
    matched.compute(label, (k, v) -> {
      if (v == null) {
        return new ArrayList<>(List.of(instruction));
      } else {
        v.add(instruction);
        return v;
      }
    });
  }

  /**
   * Find the pattern with {@code builtin} as root and register-register as children in the
   * {@code behavior} or find the pattern with {@code builtin} as root and register-immediate as
   * children in the {@code behavior}.
   */
  default boolean findRegisterRegisterOrRegisterImmediateOrImmediateRegister(
      UninlinedGraph behavior, BuiltInTable.BuiltIn builtin) {
    return findRR(behavior, List.of(builtin))
        || findRegisterImmediateOrImmediateRegister(behavior, List.of(builtin));
  }

  /**
   * Find the pattern with one of {@code builtins} as root and register-register as children in the
   * {@code behavior} or find the pattern with one of {@code builtins} as root and
   * register-immediate as children in the {@code behavior}.
   */
  default boolean findRegisterRegisterOrRegisterImmediateOrImmediateRegister(
      UninlinedGraph behavior, List<BuiltInTable.BuiltIn> builtins) {
    return (findRR(behavior, builtins)
        || findRegisterImmediateOrImmediateRegister(behavior, builtins))
        && noPcAccess(behavior);
  }

  private boolean noPcAccess(UninlinedGraph behavior) {
    return behavior.getNodes(WriteRegTensorNode.class).noneMatch(WriteRegTensorNode::isPcAccess)
        && behavior.getNodes(ReadRegTensorNode.class).noneMatch(ReadRegTensorNode::isPcAccess);
  }

  /**
   * Find register-registers instructions when it matches one of the given
   * {@link BuiltInTable.BuiltIn}.
   * Also, it must only write one register result. Multiple builtins can exist in the behavior.
   */
  default boolean weakFindRR(UninlinedGraph behavior, List<BuiltInTable.BuiltIn> builtins) {
    var matched = TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
        new BuiltInMatcher(builtins, List.of(
            new AnyChildMatcher(new AnyReadRegisterFileMatcher()),
            new AnyChildMatcher(new AnyReadRegisterFileMatcher())
        )));

    return !matched.isEmpty()
        && writesExactlyOneRegisterClass(behavior)
        && noPcAccess(behavior);
  }

  /**
   * Find register-registers instructions when it matches one of the given
   * {@link BuiltInTable.BuiltIn}.
   * Also, it must only write one register result and only one builtin must exist in the behavior.
   */
  default boolean findRR(UninlinedGraph behavior, List<BuiltInTable.BuiltIn> builtins) {
    return weakFindRR(behavior, builtins)
        && behavior.getNodes(BuiltInCall.class).count() == 1;
  }

  /**
   * Find register-immediate instructions when it matches one of the given
   * {@link BuiltInTable.BuiltIn}. Looking for the operands is commutative.
   * Also, it must only write one register result.
   */
  default boolean findRegisterImmediateOrImmediateRegister(UninlinedGraph behavior,
                                                           List<BuiltInTable.BuiltIn> builtins) {
    var matcher = new BuiltInMatcher(builtins, List.of(
        new AnyChildMatcher(new AnyReadRegisterFileMatcher()),
        new AnyChildMatcher(new FieldAccessRefMatcher())
    ));
    Set<Matcher> matchers = Set.of(
        matcher,
        matcher.swapOperands()
    );
    var matched = TreeMatcher.matches(() -> behavior.getNodes(BuiltInCall.class).map(x -> x),
        matchers
    );

    return !matched.isEmpty() && writesExactlyOneRegisterClass(behavior) && noPcAccess(behavior);
  }

  /**
   * Find an instruction which acts as subtraction but also sets the flags.
   *
   * @param behavior is a modified but uninlined graph.
   * @param original is the original VIAM.
   * @param ty       is the type of the result.
   */
  default boolean findSubS(UninlinedGraph behavior, Graph original, BitsType ty) {
    var writes =
        original.getNodes(WritesRegisterTensor.class).toList();

    var hasNegative = false;
    var hasOverflow = false;
    var hasZero = false;
    var hasCarry = false;

    for (var write : writes) {
      var tensor = write.registerTensor();
      if (tensor.hasAnnotation(StatusRegisterAnnotation.NegativeStatusRegisterAnnotation.class)) {
        hasNegative = true;
      } else if (tensor.hasAnnotation(
          StatusRegisterAnnotation.ZeroStatusRegisterAnnotation.class)) {
        hasZero = true;
      } else if (tensor.hasAnnotation(
          StatusRegisterAnnotation.CarryStatusRegisterAnnotation.class)) {
        hasCarry = true;
      } else if (tensor.hasAnnotation(
          StatusRegisterAnnotation.OverflowStatusRegisterAnnotation.class)) {
        hasOverflow = true;
      }
    }

    var matched = TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
        new BuiltInMatcher(List.of(BuiltInTable.SUBSC), List.of(
            new AnyChildMatcher(new AnyReadRegisterFileMatcher()),
            new AnyChildMatcher(new AnyReadRegisterFileMatcher())
        )));

    return !matched.isEmpty()
        && ((TupleType) ((BuiltInCall) matched.getFirst()).type()).first().equals(ty)
        && behavior.getNodes(Set.of(IfNode.class, SliceNode.class)).toList().isEmpty()
        && behavior.getNodes(BuiltInCall.class).toList().size() == 1
        && noPcAccess(behavior)
        && hasNegative
        && hasOverflow
        && hasZero
        && hasCarry;
  }

  /**
   * Return {@code true} if there is only one side effect which writes a register file.
   */
  default boolean writesExactlyOneRegisterClass(UninlinedGraph graph) {
    var writesRegFiles = graph.getNodes(WriteRegTensorNode.class)
        .filter(w -> w.regTensor().isRegisterFile()).toList();
    var writeArtificialRegFile = graph.getNodes(WriteArtificialResNode.class)
        .filter(WriteArtificialResNode::hasRegisterFile).toList();

    var writesReg = graph.getNodes(WriteRegTensorNode.class)
        .filter(w -> w.regTensor().isSingleRegister()).toList();
    var writesMem = graph.getNodes(WriteMemNode.class).toList();
    var readMem = graph.getNodes(ReadMemNode.class).toList();

    if (!writesReg.isEmpty()
        || !writesMem.isEmpty()
        || !readMem.isEmpty()) {
      return false;
    }

    if (writeArtificialRegFile.size() == 1 && writesRegFiles.isEmpty()) {
      return true;
    }

    return writeArtificialRegFile.isEmpty() && writesRegFiles.size() == 1;
  }

  /**
   * Return {@code true} if there is only one side effect which writes a register file with
   * the given {@link Type} as result type for the register file.
   */
  default boolean writesExactlyOneRegisterClassWithType(UninlinedGraph graph, Type resultType) {
    var writesRegFiles = graph.getNodes(WriteRegTensorNode.class)
        .filter(w -> w.regTensor().isRegisterFile()).toList();
    var writeArtificialRegFile = graph.getNodes(WriteArtificialResNode.class)
        .filter(w -> w.resourceDefinition().isRegisterFile()).toList();
    var writesReg = graph.getNodes(WriteRegTensorNode.class)
        .filter(w -> w.regTensor().isSingleRegister()).toList();
    var writesMem = graph.getNodes(WriteMemNode.class).toList();
    var readMem = graph.getNodes(ReadMemNode.class).toList();

    if (!writesReg.isEmpty()
        || !writesMem.isEmpty()
        || !readMem.isEmpty()) {
      return false;
    }

    if (!writesRegFiles.isEmpty()) {
      return writesRegFiles.getFirst().regTensor().resultType() == resultType;
    }

    if (!writeArtificialRegFile.isEmpty()) {
      return writeArtificialRegFile.getFirst().resourceDefinition().resultType() == resultType;
    }

    return false;
  }

  /**
   * Create a map from the specification with {@link MachineInstructionLabel}.
   */
  default Map<MachineInstructionLabel, List<Instruction>> createLabelMap(
      Specification specification) {
    return specification.isa().stream().flatMap(isa -> isa.ownInstructions().stream())
        .filter(instruction -> instruction.hasExtension(MachineInstructionCtx.class))
        .collect(Collectors.groupingBy(entry -> {
          var ext = ensureNonNull(entry.extension(MachineInstructionCtx.class), "must not be null");
          return ext.label();
        }));
  }

  /**
   * Create a map from the specification with {@link PseudoInstructionLabel}.
   */
  default Map<PseudoInstructionLabel, List<PseudoInstruction>> createPseudoLabelMap(
      Specification specification) {
    return specification.isa().stream().flatMap(isa -> isa.ownPseudoInstructions().stream())
        .filter(instruction -> instruction.hasExtension(PseudoInstructionCtx.class))
        .collect(Collectors.groupingBy(entry -> {
          var ext = ensureNonNull(entry.extension(PseudoInstructionCtx.class), "must not be null");
          return ext.label();
        }));
  }

  /**
   * The {@link IsaMachineInstructionMatchingPass} computes a hashmap with the instruction label as
   * a key and all the matched instructions as value. But we want to know whether a certain
   * {@link Instruction} or {@link PseudoInstruction} has a label.
   */
  default <K, V> IdentityHashMap<V, K> flipIsaMatching(
      Map<K, List<V>> isaMatched) {
    IdentityHashMap<V, K> inverse = new IdentityHashMap<>();

    for (var entry : isaMatched.entrySet()) {
      for (var item : entry.getValue()) {
        inverse.put(item, entry.getKey());
      }
    }

    return inverse;
  }
}
