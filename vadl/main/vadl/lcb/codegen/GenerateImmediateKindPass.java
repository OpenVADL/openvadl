package vadl.lcb.codegen;

import java.io.IOException;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.model.VariantKind;
import vadl.gcb.passes.relocation.IdentifyFieldUsagePass;
import vadl.gcb.passes.relocation.model.LogicalRelocation;
import vadl.lcb.passes.llvmLowering.immediates.GenerateTableGenImmediateRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
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

    var immRecords = (List<TableGenImmediateRecord>) passResults.lastResultOf(
        GenerateTableGenImmediateRecordPass.class);

    immRecords.forEach(immediateRecord -> {
      var field = immediateRecord.fieldAccessRef().fieldRef();
      result.put(field, new VariantKind(field));
    });

    return result;
  }
}
