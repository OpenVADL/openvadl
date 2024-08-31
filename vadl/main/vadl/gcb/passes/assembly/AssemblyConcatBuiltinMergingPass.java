package vadl.gcb.passes.assembly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;

/**
 * It is easier to generate a parser when the {@link BuiltInCall} with
 * {@link vadl.types.BuiltInTable#CONCATENATE_STRINGS} are merged into a
 * single builtin.
 */
public class AssemblyConcatBuiltinMergingPass extends Pass {

  public AssemblyConcatBuiltinMergingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("assemblyConcatBuiltinMergingPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    // It is a candidate when it is a builtin with concatenate string
    // and one argument is also a builtin with concatenate string.
    var candidates = viam.isas()
        .flatMap(isa -> isa.ownInstructions().stream())
        .flatMap(
            instruction -> instruction.assembly().function().behavior().getNodes(BuiltInCall.class))
        .filter(builtin -> builtin.builtIn() == BuiltInTable.CONCATENATE_STRINGS)
        .filter(built -> built.arguments().stream().anyMatch(
            arg -> arg instanceof BuiltInCall argNode
                && argNode.builtIn() == BuiltInTable.CONCATENATE_STRINGS))
        .toList();

    // Insert the child's arguments into parent's argument
    for (var candidate : candidates) {
      var copy = new ArrayList<>(candidate.arguments());
      for (var arg : copy) {
        if (arg instanceof BuiltInCall casted) {
          if (casted.builtIn() == BuiltInTable.CONCATENATE_STRINGS) {
            // The candidate is a concat
            // and the casted is also a concat.
            // Take casted's arguments and insert them into candidate's arguments
            replaceElementWithList(candidate.arguments(), casted, casted.arguments());
            for (var x : casted.arguments()) {
              x.removeUsage(casted);
              x.addUsage(candidate);
            }
            casted.clearUsages();
            casted.safeDelete();
          }
        }
      }
    }

    return null;
  }

  private static <T> void replaceElementWithList(List<T> list, T objectToReplace,
                                                 List<T> newElements) {
    // Find the index of the object to replace
    int index = list.indexOf(objectToReplace);

    // If the object is found, replace it
    if (index != -1) {
      // Remove the element at the found index
      list.remove(index);

      // Add all elements from the new list starting at the found index
      list.addAll(index, newElements);
    }
  }
}
