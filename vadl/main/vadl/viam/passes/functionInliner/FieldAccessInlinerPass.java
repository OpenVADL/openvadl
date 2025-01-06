package vadl.viam.passes.functionInliner;

import static vadl.utils.GraphUtils.getSingleNode;
import static vadl.utils.ViamUtils.findDefinitionsByFilter;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;

/**
 * FieldAccessInlinerPass is a transformation pass that inlines field access operations
 * in the VADL specification. It is responsible for identifying and replacing field
 * accesses with field access function body.
 */
public class FieldAccessInlinerPass extends Pass {
  public FieldAccessInlinerPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Field Access Inliner Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    findDefinitionsByFilter(viam, d -> d instanceof Instruction)
        .stream().map(Instruction.class::cast)
        .forEach(instruction -> {
          var fieldAccesses = instruction.behavior().getNodes(FieldAccessRefNode.class)
              .toList();

          fieldAccesses.forEach(fieldAccessRefNode -> {
            var behavior = fieldAccessRefNode.fieldAccess().accessFunction().behavior();
            var returnNode = getSingleNode(behavior, ReturnNode.class);
            fieldAccessRefNode.replaceAndDelete(returnNode.value().copy());
          });
        });
    return null;
  }
}
