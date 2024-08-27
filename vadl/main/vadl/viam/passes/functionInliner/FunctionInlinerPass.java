package vadl.viam.passes.functionInliner;

import static vadl.utils.GraphUtils.getSingleNode;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.Arrays;
import java.util.IdentityHashMap;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * A pass which inlines all the function with the given function.
 * Note that the function must be {@code pure} to be inlined.
 * Also, the given {@link Specification} will be mutated in-place. However, the pass
 * saves the original uninlined instruction's behavior as pass result.
 */
public class FunctionInlinerPass extends Pass {
  public FunctionInlinerPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("FunctionInlinerPass");
  }

  record Pair(ExpressionNode arg, Parameter parameter) {

  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {
    IdentityHashMap<Instruction, UninlinedGraph> original = new IdentityHashMap<>();

    viam.isas()
        .flatMap(isa -> isa.instructions().stream())
        .forEach(instruction -> {
          var copy = instruction.behavior().copy();
          var functionCalls = instruction.behavior().getNodes(FuncCallNode.class)
              .filter(funcCallNode -> funcCallNode.function().behavior().isPureFunction())
              .toList();

          functionCalls.forEach(functionCall -> {
            // copy function behavior
            var behaviorCopy = functionCall.function().behavior().copy();
            // get return node of function behavior
            var returnNode = getSingleNode(behaviorCopy, ReturnNode.class);

            // Replace every occurrence of `FuncParamNode` by a copy of the
            // given argument from the `FunctionCallNode`.
            Streams.zip(functionCall.arguments().stream(),
                    Arrays.stream(functionCall.function().parameters()),
                    Pair::new)
                .forEach(pair -> {
                  behaviorCopy.getNodes(FuncParamNode.class)
                      .filter(n -> n.parameter() == pair.parameter())
                      .forEach(usedParam -> usedParam.replaceAndDelete(pair.arg.copy()));
                });

            // replace the function call by a copy of the return value of the function
            functionCall.replaceAndDelete(returnNode.value().copy());
          });

          var fieldAccesses = instruction.behavior().getNodes(FieldAccessRefNode.class)
              .toList();

          fieldAccesses.forEach(fieldAccessRefNode -> {
            var behavior = fieldAccessRefNode.fieldAccess().accessFunction().behavior();
            var returnNode = getSingleNode(behavior, ReturnNode.class);
            fieldAccessRefNode.replaceAndDelete(returnNode.value().copy());
          });

          original.put(instruction, new UninlinedGraph(copy));
        });

    return original;
  }
}
