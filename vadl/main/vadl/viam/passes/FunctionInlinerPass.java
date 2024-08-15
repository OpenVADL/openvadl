package vadl.viam.passes;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
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
  @Override
  public PassName getName() {
    return new PassName("FunctionInlinerPass");
  }

  record Pair(ExpressionNode arg, Parameter parameter) {

  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {
    IdentityHashMap<Instruction, Graph> original = new IdentityHashMap<>();

    viam.isas()
        .flatMap(isa -> isa.instructions().stream())
        .forEach(instruction -> {
          var copy = instruction.behavior().copy();
          var functionCalls = instruction.behavior().getNodes(FuncCallNode.class)
              .filter(funcCallNode -> funcCallNode.function().behavior().isPureFunction())
              .toList();

          functionCalls.forEach(functionCall -> {
            var inlinedBehavior = functionCall.function().behavior().copy();
            // We deinitialize the nodes so we can add them when we inline. Otherwise,
            // get an exception because it is already initialized.
            inlinedBehavior.deinitialize_nodes();
            var returnNodes = inlinedBehavior.getNodes(ReturnNode.class).toList();
            ensure(returnNodes.size() == 1, "Inlined function must only have one return node");
            var returnNode = returnNodes.get(0);

            // Replace every occurrence of `FuncParamNode` by the
            // given argument from the `FunctionCallNode`.
            Streams.zip(functionCall.arguments().stream(),
                    Arrays.stream(functionCall.function().parameters()),
                    Pair::new)
                .forEach(pair -> inlinedBehavior.getNodes(FuncParamNode.class)
                    .filter(n -> n.parameter() == pair.parameter())
                    .forEach(usedParam -> usedParam.replaceAndDelete(pair.arg)));

            // Actual inlining
            functionCall.replaceAndDelete(returnNode.value());
          });

          var fieldAccesses = instruction.behavior().getNodes(FieldAccessRefNode.class)
              .toList();

          fieldAccesses.forEach(fieldAccessRefNode -> {
            var inlinedBehavior =
                fieldAccessRefNode.fieldAccess().accessFunction().behavior().copy();
            // We deinitialize the nodes so we can add them when we inline. Otherwise,
            // get an exception because it is already initialized.
            inlinedBehavior.deinitialize_nodes();
            var returnNodes = inlinedBehavior.getNodes(ReturnNode.class).toList();
            ensure(returnNodes.size() == 1, "Inlined function must only have one return node");
            var returnNode = returnNodes.get(0);

            fieldAccessRefNode.replaceAndDelete(returnNode.value());
          });

          original.put(instruction, copy);
        });

    return original;
  }
}
