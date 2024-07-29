package vadl.gcb.passes.field_node_replacement;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.oop.passes.field_node_replacement.FieldNodeReplacementPass;
import vadl.oop.passes.type_normalization.CppTypeNormalizer;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.Specification;

public class FieldNodeReplacementPassForEncoding extends FieldNodeReplacementPass {

  @Override
  public PassName getName() {
    return new PassName("FieldNodeReplacementPassForEncoding");
  }

  @Override
  protected Stream<Function> getApplicable(Specification viam) {
    return viam.isas()
        .flatMap(x -> x.instructions().stream())
        .map(Instruction::format)
        .distinct()
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .map(Format.FieldAccess::encoding)
        .filter(Objects::nonNull);
  }
}
