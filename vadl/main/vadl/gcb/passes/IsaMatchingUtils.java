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

import static vadl.viam.ViamError.ensureNonNull;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.gcb.valuetypes.RelocationCtx;
import vadl.gcb.valuetypes.RelocationFunctionLabel;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Relocation;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.matching.Matcher;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyChildMatcher;
import vadl.viam.matching.impl.AnyReadRegFileMatcher;
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
    return findRR(behavior, builtins)
        || findRegisterImmediateOrImmediateRegister(behavior, builtins);
  }

  /**
   * Find register-registers instructions when it matches one of the given
   * {@link BuiltInTable.BuiltIn}.
   * Also, it must only write one register result.
   */
  default boolean findRR(UninlinedGraph behavior, List<BuiltInTable.BuiltIn> builtins) {
    var matched = TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
        new BuiltInMatcher(builtins, List.of(
            new AnyChildMatcher(new AnyReadRegFileMatcher()),
            new AnyChildMatcher(new AnyReadRegFileMatcher())
        )));

    return !matched.isEmpty() && writesExactlyOneRegisterClass(behavior);
  }

  /**
   * Find register-immediate instructions when it matches one of the given
   * {@link BuiltInTable.BuiltIn}. Looking for the operands is commutative.
   * Also, it must only write one register result.
   */
  default boolean findRegisterImmediateOrImmediateRegister(UninlinedGraph behavior,
                                                           List<BuiltInTable.BuiltIn> builtins) {
    var matcher = new BuiltInMatcher(builtins, List.of(
        new AnyChildMatcher(new AnyReadRegFileMatcher()),
        new AnyChildMatcher(new FieldAccessRefMatcher())
    ));
    Set<Matcher> matchers = Set.of(
        matcher,
        matcher.swapOperands()
    );
    var matched = TreeMatcher.matches(() -> behavior.getNodes(BuiltInCall.class).map(x -> x),
        matchers
    );

    return !matched.isEmpty() && writesExactlyOneRegisterClass(behavior);
  }

  /**
   * Return {@code true} if there is only one side effect which writes a register file.
   */
  default boolean writesExactlyOneRegisterClass(UninlinedGraph graph) {
    var writesRegFiles = graph.getNodes(WriteRegTensorNode.class)
        .filter(w -> w.regTensor().isRegisterFile()).toList();
    var writesReg = graph.getNodes(WriteRegTensorNode.class)
        .filter(w -> w.regTensor().isSingleRegister()).toList();
    var writesMem = graph.getNodes(WriteMemNode.class).toList();
    var readMem = graph.getNodes(ReadMemNode.class).toList();

    if (writesRegFiles.size() != 1
        || !writesReg.isEmpty()
        || !writesMem.isEmpty()
        || !readMem.isEmpty()) {
      return false;
    }

    return true;
  }

  /**
   * Return {@code true} if there is only one side effect which writes a register file with
   * the given {@link Type} as result type for the register file.
   */
  default boolean writesExactlyOneRegisterClassWithType(UninlinedGraph graph, Type resultType) {
    var writesRegFiles = graph.getNodes(WriteRegTensorNode.class)
        .filter(w -> w.regTensor().isRegisterFile()).toList();
    var writesReg = graph.getNodes(WriteRegTensorNode.class)
        .filter(w -> w.regTensor().isSingleRegister()).toList();
    var writesMem = graph.getNodes(WriteMemNode.class).toList();
    var readMem = graph.getNodes(ReadMemNode.class).toList();

    if (writesRegFiles.size() != 1
        || !writesReg.isEmpty()
        || !writesMem.isEmpty()
        || !readMem.isEmpty()) {
      return false;
    }

    return writesRegFiles.get(0).regTensor().resultType() == resultType;
  }

  /**
   * Create a map from the specification with {@link MachineInstructionLabel}.
   */
  default Map<MachineInstructionLabel, List<Instruction>> createLabelMap(
      Specification specification) {
    return specification.isa()
        .map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
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
    return specification.isa()
        .map(isa -> isa.ownPseudoInstructions().stream())
        .orElse(Stream.empty())
        .filter(instruction -> instruction.hasExtension(PseudoInstructionCtx.class))
        .collect(Collectors.groupingBy(entry -> {
          var ext = ensureNonNull(entry.extension(PseudoInstructionCtx.class), "must not be null");
          return ext.label();
        }));
  }

  /**
   * Create a map from the specification with {@link RelocationFunctionLabel}.
   */
  default Map<RelocationFunctionLabel, List<Relocation>> createRelocationFunctionLabelMap(
      Specification specification) {
    return specification.isa()
        .map(isa -> isa.ownRelocations().stream())
        .orElse(Stream.empty())
        .filter(relocation -> relocation.hasExtension(RelocationCtx.class))
        .collect(Collectors.groupingBy(entry -> {
          var ext = ensureNonNull(entry.extension(RelocationCtx.class), "must not be null");
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
