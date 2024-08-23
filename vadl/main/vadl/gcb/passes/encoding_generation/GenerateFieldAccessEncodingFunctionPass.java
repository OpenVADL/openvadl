package vadl.gcb.passes.encoding_generation;

import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.configuration.GcbConfiguration;
import vadl.gcb.passes.encoding_generation.strategies.EncodingGenerationStrategy;
import vadl.gcb.passes.encoding_generation.strategies.impl.ArithmeticImmediateStrategy;
import vadl.gcb.passes.encoding_generation.strategies.impl.ShiftedImmediateStrategy;
import vadl.gcb.passes.encoding_generation.strategies.impl.TrivialImmediateStrategy;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Encoding;
import vadl.viam.Format.FieldAccess;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;

/**
 * This pass generate the behavior {@link Graph} of {@link FieldAccess} when the {@link Encoding}
 * is {@code null}.
 */
public class GenerateFieldAccessEncodingFunctionPass extends Pass {

  private static final Logger logger = LoggerFactory.getLogger(
      GenerateFieldAccessEncodingFunctionPass.class);

  private final List<EncodingGenerationStrategy> strategies = List.of(
      new TrivialImmediateStrategy(),
      new ShiftedImmediateStrategy(),
      new ArithmeticImmediateStrategy());

  public GenerateFieldAccessEncodingFunctionPass(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateFieldAccessEncodingFunctionPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) {
    viam.isas()
        .flatMap(x -> x.formats().stream())
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

    var hasNoEncoding = viam.findAllFormats()
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .filter(x -> x.encoding() == null)
        .toList();

    if (!hasNoEncoding.isEmpty()) {
      for (var format : hasNoEncoding) {
        logger.atError().log("Format {} has no encoding", format.name());
      }
      throw new ViamError("Not all formats have an encoding");
    }


    return null;
  }
}
