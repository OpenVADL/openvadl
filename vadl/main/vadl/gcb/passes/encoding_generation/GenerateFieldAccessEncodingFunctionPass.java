package vadl.gcb.passes.encoding_generation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.gcb.passes.encoding_generation.strategies.EncodingGenerationStrategy;
import vadl.gcb.passes.encoding_generation.strategies.impl.ArithmeticImmediateStrategy;
import vadl.gcb.passes.encoding_generation.strategies.impl.ShiftedImmediateStrategy;
import vadl.gcb.passes.encoding_generation.strategies.impl.TrivialImmediateStrategy;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.viam.Encoding;
import vadl.viam.Format.FieldAccess;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;

/**
 * This pass generate the behavior {@link Graph} of {@link FieldAccess} when the {@link Encoding}
 * is {@code null}.
 */
public class GenerateFieldAccessEncodingFunctionPass extends Pass {

  private final List<EncodingGenerationStrategy> strategies = List.of(
      new TrivialImmediateStrategy(),
      new ShiftedImmediateStrategy(),
      new ArithmeticImmediateStrategy());

  @Override
  public PassName getName() {
    return new PassName("GenerateFieldAccessEncodingFunctionPass");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam) {
    viam.isas()
        .flatMap(x -> x.instructions().stream())
        .map(Instruction::format)
        .distinct()
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .filter(x -> x.encoding() == null)
        .forEach(fieldAccess -> {
          // We need to compute multiple encoding functions based on the field access function.
          // Different field access functions require different heuristics for the encoding.
          for (var strategy : strategies) {
            if (strategy.checkIfApplicable(fieldAccess)) {
              strategy.generateEncoding(fieldAccess);
              break;
            }
          }
        });

    var allHaveEncoding = viam.isas()
        .flatMap(x -> x.instructions().stream())
        .map(Instruction::format)
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .filter(x -> x.encoding() == null)
        .toList();

    if (!allHaveEncoding.isEmpty()) {
      throw new ViamError("Not all formats have an encoding");
    }


    return null;
  }
}
