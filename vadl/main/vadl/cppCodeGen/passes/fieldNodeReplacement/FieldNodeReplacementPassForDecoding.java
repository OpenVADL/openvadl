package vadl.cppCodeGen.passes.fieldNodeReplacement;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import vadl.pass.PassName;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * Replaces all {@link FieldRefNode} by {@link FuncParamNode} but only in the
 * {@link Format.FieldAccess#accessFunction()}.
 */
public class FieldNodeReplacementPassForDecoding extends FieldNodeReplacementPass {

  @Override
  public PassName getName() {
    return new PassName(FieldNodeReplacementPassForDecoding.class.getName());
  }

  @Override
  protected Stream<Function> getApplicable(Specification viam) {
    return viam.isas()
        .flatMap(x -> x.formats().stream())
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .map(Format.FieldAccess::accessFunction)
        .filter(Objects::nonNull);
  }
}
