package vadl.lcb.codegen;

import java.io.IOException;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.model.VariantKind;
import vadl.gcb.passes.relocation.DetectImmediatePass;
import vadl.gcb.passes.relocation.model.LogicalRelocation;
import vadl.lcb.passes.relocation.GenerateElfRelocationPass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Specification;

/**
 * LLVM requires kinds to identify(?) relocations and immediates.
 * For relocations this is already done in {@link LogicalRelocation}.
 * For immediates, we generate it here.
 */
public class GenerateImmediateKindPass extends Pass {
  public GenerateImmediateKindPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("generateImmediateKindPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var result = new IdentityHashMap<Format.Field, VariantKind>();

    var fieldUsages = (DetectImmediatePass.ImmediateDetectionContainer) passResults.lastResultOf(
        DetectImmediatePass.class);

    viam.isa()
        .map(isa -> isa.ownFormats().stream()).orElseGet(Stream::empty)
        .flatMap(format -> Arrays.stream(format.fields()))
        .filter(field -> {
          var usage = fieldUsages.get(field.format()).get(field);
          return usage == DetectImmediatePass.FieldUsage.IMMEDIATE;
        })
        .forEach(field -> result.put(field, new VariantKind(field)));

    return result;
  }
}
