package vadl.viam.passes;

import static vadl.error.Diagnostic.error;

import java.io.IOException;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Definition;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.WriteRegNode;

/**
 * Depends on {@link SideEffectConditionResolvingPass}.
 */
public class SingleResourceWriteValidationPass extends Pass {

  public SingleResourceWriteValidationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Single Resource Write Validation Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {
    return null;
  }
}

class SingleResourceWriteValidator {

  Definition definition;
  Graph behavior;

  SingleResourceWriteValidator(Graph behavior, Definition definition) {
    this.behavior = behavior;
    this.definition = definition;
  }

  void run() {

  }


  private void checkRegisters() {
    var regWrites = behavior.getNodes(WriteRegNode.class)
        .collect(Collectors.groupingBy(WriteRegNode::resourceDefinition));


    for (var regWrite : regWrites.entrySet()) {
      if (regWrite.getValue().size() > 1) {

        var builder = error("More than one write to register", definition.identifier)
            .description("The behavior writes the register %s twice.",
                regWrite.getKey().simpleName());
        for (var write : regWrite.getValue()) {
          builder.locationDescription(write, "");
        }

//        DeferredDiagnosticStore.add()

      }
    }

  }


}

