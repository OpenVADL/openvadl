package vadl.gcb.passes.type_normalization;

import java.util.Arrays;
import java.util.stream.Stream;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.pass.PassName;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Specification;

/**
 * When transforming a graph into a CPP code, we have to take care of unsupported types.
 * For example, VADL allows arbitrary bit sizes, however CPP has only fixed size types.
 * This pass inserts a bit mask to ensure that the code generation works for predicates.
 */
public class CppTypeNormalizationForPredicatesPass extends CppTypeNormalizationPass {
  @Override
  public PassName getName() {
    return new PassName(CppTypeNormalizationForPredicatesPass.class.getName());
  }

  @Override
  protected Stream<Function> getApplicable(Specification viam) {
    return viam.isas()
        .flatMap(x -> x.formats().stream())
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .filter(x -> x.encoding() != null)
        .map(Format.FieldAccess::predicate);
  }
}
