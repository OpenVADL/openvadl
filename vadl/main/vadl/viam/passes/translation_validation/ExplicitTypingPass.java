package vadl.viam.passes.translation_validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.types.BitsType;
import vadl.types.Type;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.passes.Pair;
import vadl.viam.passes.Triple;

/**
 * The {@link TranslationValidation#lower(Instruction, Instruction)} can only work with
 * explicit types. However, that is usually not required for the VIAM's happy flow since the code
 * generation works better on fewer nodes. This pass helps to verify the
 * {@link Instruction#behavior()} by inserting explicit types.
 */
public class ExplicitTypingPass extends Pass {
  @Override
  public PassName getName() {
    return new PassName("ExplicitTypingPass");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {
    ArrayList<Triple<BuiltInCall, ExpressionNode, Type>> worklist = new ArrayList<>();
    viam.isas()
        .flatMap(isa -> isa.instructions().stream())
        .flatMap(instruction -> instruction.behavior().getNodes(BuiltInCall.class))
        .filter(node -> !node.arguments().isEmpty())
        .filter(
            // Only relevant if all the arguments have BitsType.
            node -> node.arguments()
                .stream()
                .map(ExpressionNode::type)
                .filter(x -> x instanceof BitsType)
                .collect(Collectors.toSet())
                .size() != 1)
        .forEach(node -> {
          List<BitsType> types =
              node.arguments().stream().map(ExpressionNode::type).map(x -> (BitsType) x).toList();
          var join = ((BitsType) node.arguments().get(0).type()).join(types);

          node.arguments().forEach(arg -> {
            if (arg.type() != join) {
              // Insert typecast to match the sizes.
              // We use a separate list to avoid a concurrent modification exception.
              worklist.add(new Triple<>(node, arg, join));
            }
          });
        });

    for (var item : worklist) {
      var newCast = Objects.requireNonNull(item.left().graph())
          .add(new TypeCastNode(item.middle(), item.right()));
      item.left().replaceInput(item.middle(), newCast);
    }

    return null;
  }
}
