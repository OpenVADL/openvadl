package vadl.viam.passes.translation_validation;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.types.BoolType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.passes.Pair;

/**
 * In Z3 logical comparisons result in boolean types.
 * These cannot be used as BitVec. Therefore, we have to wrap it with a
 * {@link SelectNode}.
 */
public class WrapBooleanPass extends Pass {
  @Override
  public PassName getName() {
    return new PassName("WrapBooleanPass");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {
    var worklist = viam.isas().flatMap(isa -> isa.instructions().stream())
        .flatMap(instruction -> instruction.behavior().getNodes(BuiltInCall.class))
        .filter(node -> node.type() == Type.bool())
        .map(node -> {
          return node;
          /*return new Pair<>(node, new SelectNode(node, new ConstantNode(Constant.Value.of(true)),
              new ConstantNode(Constant.Value.of(false))));*/
        })
        .toList();

    for (var item : worklist) {
      var selectNode = new SelectNode(item,
          new ConstantNode(Constant.Value.of(true)),
          new ConstantNode(Constant.Value.of(false)));
      var addedNode = Objects.requireNonNull(item.graph()).addWithInputs(selectNode);
      var usages = item.usages().filter(n -> n != addedNode).toList();
      for (var usage : usages) {
        usage.replaceInput(item, addedNode);
      }
    }

    return null;
  }
}
