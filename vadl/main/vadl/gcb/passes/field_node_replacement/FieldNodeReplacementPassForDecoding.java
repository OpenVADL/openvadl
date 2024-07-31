package vadl.gcb.passes.field_node_replacement;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import vadl.oop.passes.field_node_replacement.FieldNodeReplacementPass;
import vadl.pass.PassName;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Instruction;
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
    return new PassName("FieldNodeReplacementPassForDecoding");
  }

  @Override
  protected Stream<Function> getApplicable(Specification viam) {
    return viam.isas()
        .flatMap(x -> x.instructions().stream())
        .map(Instruction::format)
        .distinct()
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .map(Format.FieldAccess::accessFunction)
        .filter(Objects::nonNull);
  }
}
