package vadl.lcb.passes.llvmLowering.immediates;

import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Abi;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;

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
    var abi = (Abi) viam.definitions().filter(x -> x instanceof Abi).findFirst().get();
    return viam.isa().map(isa -> isa.ownFormats().stream()).orElseGet(Stream::empty)
        .flatMap(fieldUsages -> fieldUsages.fieldAccesses().stream())
        .distinct()
        .map(fieldAccess -> {
          var originalType = abi.stackPointer().registerFile().resultType();
          var llvmType = ValueType.from(originalType);

          if (llvmType.isEmpty()) {
            var upcastedType = CppTypeNormalizationPass.upcast(originalType);
            var upcastedValueType =
                ensurePresent(ValueType.from(upcastedType), () -> Diagnostic.error(
                    "Compiler generator was not able to change the type to the architecture's "
                        + "bit width: " + upcastedType.toString(),
                    fieldAccess.sourceLocation()));
            return new TableGenImmediateRecord(fieldAccess,
                upcastedValueType);
          } else {
            return new TableGenImmediateRecord(fieldAccess,
                llvmType.get());
          }
        })
        .toList();
  }
}
