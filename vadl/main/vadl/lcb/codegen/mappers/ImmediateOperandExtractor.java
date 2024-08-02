package vadl.lcb.codegen.mappers;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import vadl.lcb.codegen.DecodingCodeGenerator;
import vadl.lcb.codegen.EncodingCodeGenerator;
import vadl.lcb.codegen.model.ImmediateOperand;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.viam.Specification;

/**
 * Extracts {@link ImmediateOperand} from {@link Specification}.
 */
public class ImmediateOperandExtractor {
  /**
   * Extracts {@link ImmediateOperand} from {@link Specification}.
   */
  public static Stream<ImmediateOperand> extract(Specification specification) {
    return specification.isas()
        .flatMap(x -> x.formats().stream())
        .flatMap(format -> Arrays.stream(
            format.fieldAccesses()))
        .map(fieldAccess -> {
          var encoder = new EncodingCodeGenerator().generateFunctionName(
              Objects.requireNonNull(fieldAccess.encoding()));
          var decoder =
              new DecodingCodeGenerator().generateFunctionName(fieldAccess.accessFunction());

          return new ImmediateOperand(fieldAccess.name(), encoder, decoder,
              ValueType.from(fieldAccess.type()));
        });
  }
}
