package vadl.viam.passes.translation_validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.utils.Triple;

/**
 * Z3 has not the semantic for multiplication.
 * This passes replaces the built-ins with the extended implementation.
 */
public class ExtendMultiplicationPass extends Pass {
  @Override
  public PassName getName() {
    return new PassName("ExtendMultiplicationPass");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {
    ArrayList<Triple<BuiltInCall, ExpressionNode, Node>> worklist = new ArrayList<>();
    viam.isas()
        .flatMap(isa -> isa.instructions().stream())
        .flatMap(instruction -> instruction.behavior().getNodes(BuiltInCall.class))
        .forEach(builtin -> {
          if (builtin.builtIn() == BuiltInTable.SMULL
              || builtin.builtIn() == BuiltInTable.UMULL
              || builtin.builtIn() == BuiltInTable.SUMULL) {
            builtin.arguments().forEach(arg -> {
              var width = ((BitsType) arg.type()).bitWidth();
              var doubleCast = new TypeCastNode(arg, Type.bits(width * 2));
              worklist.add(new Triple<>(builtin, arg, doubleCast));
            });
          }
        });

    for (var item : worklist) {
      var addedNode = Objects.requireNonNull(item.left().graph()).add(item.right());
      item.left().replaceInput(item.middle(), addedNode);
    }

    return null;
  }
}
