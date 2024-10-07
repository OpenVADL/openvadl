package vadl.lcb.template.utils;

import static vadl.viam.ViamError.ensure;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.cppCodeGen.model.VariantKind;
import vadl.lcb.passes.llvmLowering.immediates.GenerateConstantMaterialisationPass;
import vadl.lcb.passes.llvmLowering.immediates.GenerateTableGenImmediateRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.pass.PassResults;
import vadl.viam.Format;

/**
 * Utility class for getting {@link VariantKind} from {@link TableGenImmediateRecord}.
 */
public class ImmediateVariantKindProvider {

  private static Stream<TableGenImmediateRecord> records(PassResults passResults) {
    ensure(passResults.hasRunPassOnce(GenerateConstantMaterialisationPass.class
    ), "Pass has to run at least once.");
    return ((List<TableGenImmediateRecord>) passResults.lastResultOf(
        GenerateTableGenImmediateRecordPass.class))
        .stream();
  }

  /**
   * Extract the {@link VariantKind} from the {@link TableGenImmediateRecord}. It uses
   * the {@link GenerateTableGenImmediateRecordPass} as source.
   */
  public static List<VariantKind> variantKinds(PassResults passResults) {
    return records(passResults)
        .map(TableGenImmediateRecord::variantKind)
        .sorted(Comparator.comparing(VariantKind::value))
        .toList();
  }

  /**
   * Extract the {@link VariantKind} from the {@link TableGenImmediateRecord} and
   * create a {@link Map} with {@link Format.Field} as key. It uses
   * the {@link GenerateTableGenImmediateRecordPass} as source.
   */
  public static Map<Format.Field, VariantKind> variantKindsByField(PassResults passResults) {
    return records(passResults)
        .collect(Collectors.toMap(immediateRecord -> immediateRecord.fieldAccessRef().fieldRef(),
            TableGenImmediateRecord::variantKind, (o1, o2) -> o1, IdentityHashMap::new));
  }
}
