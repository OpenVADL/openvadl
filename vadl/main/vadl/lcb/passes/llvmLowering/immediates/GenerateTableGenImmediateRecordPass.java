package vadl.lcb.passes.llvmLowering.immediates;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * This pass extracts the immediates from the TableGen records. This makes it easier for the
 * {@link GenerateConstantMaterialisationPass} to generate the {@link PseudoInstruction} to
 * load immediates into registers.
 */
public class GenerateTableGenImmediateRecordPass extends Pass {

  public GenerateTableGenImmediateRecordPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateTableGenImmediateRecordPass");
  }

  @Nullable
  @Override
  public List<TableGenImmediateRecord> execute(PassResults passResults,
                                               Specification viam) throws IOException {
    var abi = (DummyAbi) viam.definitions().filter(x -> x instanceof DummyAbi).findFirst().get();
    return viam.isa().map(isa -> isa.ownFormats().stream()).orElseGet(Stream::empty)
        .flatMap(fieldUsages -> Arrays.stream(fieldUsages.fieldAccesses()))
        .distinct()
        .map(fieldAccess -> new TableGenImmediateRecord(fieldAccess,
            abi.stackPointer().registerFile().resultType()))
        .toList();
  }
}
