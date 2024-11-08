package vadl.viam.passes;

import static java.util.Objects.requireNonNull;
import static vadl.error.Diagnostic.error;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.DiagnosticBuilder;
import vadl.error.DiagnosticList;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.viam.Definition;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.WriteResourceNode;

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
    var diagnostics = new ArrayList<DiagnosticBuilder>();
    viam.isa().ifPresent(e -> e.ownInstructions().forEach(i ->
        new SingleResourceWriteValidator(i.behavior(), i, diagnostics).run()
    ));

    if (!diagnostics.isEmpty()) {
      throw new DiagnosticList(
          diagnostics.stream().map(DiagnosticBuilder::build)
              .collect(Collectors.toList()));
    }
    return null;
  }
}

class SingleResourceWriteValidator {

  List<DiagnosticBuilder> diagnostics;
  Definition definition;
  Graph behavior;

  SingleResourceWriteValidator(Graph behavior, Definition definition,
                               List<DiagnosticBuilder> diagnostics) {
    this.behavior = behavior;
    this.definition = definition;
    this.diagnostics = diagnostics;
  }

  void run() {
    checkRegisters();
  }


  private void checkRegisters() {
    var regWrites = behavior.getNodes(WriteRegNode.class)
        .collect(Collectors.groupingBy(WriteRegNode::resourceDefinition));

    for (var regWrite : regWrites.entrySet()) {
      checkSameRegisterWrites(regWrite.getValue());
    }
  }

  private void checkSameRegisterWrites(List<WriteRegNode> writes) {

    var conjuncts = new HashMap<WriteRegNode, Set<ExpressionNode>>();

    for (var write : writes) {
      var conjunct = new HashSet<ExpressionNode>();
      produceConjunction(write.condition(), conjunct);
      conjuncts.put(write, conjunct);
    }

    checkConjunctions(conjuncts);
  }

  private void checkConjunctions(Map<WriteRegNode, Set<ExpressionNode>> conjuncts) {
    // Convert the key set to a list for indexed access
    var writes = new ArrayList<>(conjuncts.keySet());

    // Iterate over all pairs of write operations
    for (int i = 0; i < writes.size(); i++) {
      WriteRegNode write1 = writes.get(i);
      var conjuncts1 = requireNonNull(conjuncts.get(write1));

      for (int j = i + 1; j < writes.size(); j++) {
        WriteRegNode write2 = writes.get(j);
        var conjuncts2 = requireNonNull(conjuncts.get(write2));

        // Check if both writes have all common conjuncts
        if (sameExecutionPath(conjuncts1, conjuncts2)) {
          addDiagnostic("Same register written twice", write1, write2);
        }
      }
    }
  }

  private boolean sameExecutionPath(Set<ExpressionNode> conjuncts1,
                                    Set<ExpressionNode> conjuncts2) {
    return conjuncts1.containsAll(conjuncts2) || conjuncts2.containsAll(conjuncts1);
  }

  private void produceConjunction(ExpressionNode root, Set<ExpressionNode> conjuncts) {
    if (root instanceof BuiltInCall builtInCall && builtInCall.builtIn() == BuiltInTable.AND) {
      produceConjunction(builtInCall.arguments().get(0), conjuncts);
      produceConjunction(builtInCall.arguments().get(1), conjuncts);
    } else {
      conjuncts.add(root);
    }
  }

  private void addDiagnostic(String reason, WriteResourceNode write1, WriteResourceNode write2) {
    var resourceName = write1.resourceDefinition().simpleName();
    var diagnostic = error(reason, definition.identifier)
        .locationDescription(write1, "%s is written here", resourceName)
        .locationDescription(write2, "%s is written here", resourceName)
        .description("%s is written twice in same execution path.",
            resourceName);

    diagnostics.add(diagnostic);
  }


}

