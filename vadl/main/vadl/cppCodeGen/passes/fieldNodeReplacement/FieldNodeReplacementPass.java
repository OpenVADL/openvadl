package vadl.cppCodeGen.passes.fieldNodeReplacement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassResults;
import vadl.viam.Function;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * We cannot access fields when generating cpp functions. Field accesses
 * will be replaced to {@link FuncParamNode}.
 */
public abstract class FieldNodeReplacementPass extends Pass {

  protected FieldNodeReplacementPass(GcbConfiguration configuration) {
    super(configuration);
  }

  /**
   * Get a list of functions on which the pass should be applied on.
   */
  protected abstract Stream<Function> getApplicable(Specification viam);

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {

    getApplicable(viam).forEach(FieldNodeReplacementPass::replaceFieldRefNodes);

    return null;
  }

  /**
   * Given {@link Function} then replace all field references with {@link FuncParamNode}.
   * This function has side effects on the parameter {@code function}.
   */
  public static void replaceFieldRefNodes(Function function) {
    var params = new ArrayList<>(List.of(function.parameters()));

    var fieldRefs = function.behavior().getNodes(FieldRefNode.class).toList();

    for (var fieldRef : fieldRefs) {
      var param = new Parameter(fieldRef.formatField().identifier.append("param"), fieldRef.type(),
          function);
      fieldRef.replaceAndDelete(new FuncParamNode(param));
      params.add(param);
    }

    function.setParameters(params.toArray(Parameter[]::new));
  }

}
