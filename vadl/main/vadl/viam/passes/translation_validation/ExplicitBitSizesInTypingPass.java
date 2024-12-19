package vadl.viam.passes.translation_validation;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * The {@link TranslationValidation#lower(Specification, Instruction, Instruction)} can only work
 * with explicit types. However, that is usually not required for the VIAM's happy flow since the
 * code generation works better on fewer nodes. This pass helps to verify the
 * {@link Instruction#behavior()} by inserting explicit types.
 */
public class ExplicitBitSizesInTypingPass extends Pass {
  public ExplicitBitSizesInTypingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("ExplicitTypingPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {
    viam.isa().map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
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
              node.arguments().stream().map(ExpressionNode::type)
                  .map(x -> x.asDataType().toBitsType()).toList();
          var join = (node.arguments().get(0).type().asDataType().toBitsType())
              .join(types);

          node.arguments().forEach(arg -> arg.setType(join));
        });

    return null;
  }
}
