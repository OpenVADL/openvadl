package vadl.lcb.passes.isaMatching;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.RegisterFile;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
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
  default boolean findRR_OR_findRI(UninlinedGraph behavior, BuiltInTable.BuiltIn builtin) {
    return findRR(behavior, List.of(builtin)) || findRI(behavior, List.of(builtin));
  }

  /**
   * Find the pattern with one of {@code builtins} as root and register-register as children in the
   * {@code behavior} or find the pattern with one of {@code builtins} as root and
   * register-immediate as children in the {@code behavior}.
   */
  default boolean findRR_OR_findRI(UninlinedGraph behavior, List<BuiltInTable.BuiltIn> builtins) {
    return findRR(behavior, builtins) || findRI(behavior, builtins);
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
   * {@link BuiltInTable.BuiltIn}.
   * Also, it must only write one register result.
   */
  default boolean findRI(UninlinedGraph behavior, List<BuiltInTable.BuiltIn> builtins) {
    var matched = TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
        new BuiltInMatcher(builtins, List.of(
            new AnyChildMatcher(new AnyReadRegFileMatcher()),
            new AnyChildMatcher(new FieldAccessRefMatcher())
        )));

    return !matched.isEmpty() && writesExactlyOneRegisterClass(behavior);
  }

  /**
   * Return {@code true} if there is only one side effect which writes a {@link RegisterFile}.
   */
  default boolean writesExactlyOneRegisterClass(UninlinedGraph graph) {
    var writesRegFiles = graph.getNodes(WriteRegFileNode.class).toList();
    var writesReg = graph.getNodes(WriteRegNode.class).toList();
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
   * Return {@code true} if there is only one side effect which writes a {@link RegisterFile} with
   * the given {@link Type} as result type for the {@link RegisterFile}.
   */
  default boolean writesExactlyOneRegisterClassWithType(UninlinedGraph graph, Type resultType) {
    var writesRegFiles = graph.getNodes(WriteRegFileNode.class).toList();
    var writesReg = graph.getNodes(WriteRegNode.class).toList();
    var writesMem = graph.getNodes(WriteMemNode.class).toList();
    var readMem = graph.getNodes(ReadMemNode.class).toList();

    if (writesRegFiles.size() != 1
        || !writesReg.isEmpty()
        || !writesMem.isEmpty()
        || !readMem.isEmpty()) {
      return false;
    }

    return writesRegFiles.get(0).registerFile().resultType() == resultType;
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
