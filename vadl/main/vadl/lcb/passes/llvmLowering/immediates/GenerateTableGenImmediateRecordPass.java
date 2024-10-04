package vadl.lcb.passes.llvmLowering.immediates;


import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
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
    return new PassName("ExtractTableGenImmediatePass");
  }

  @Nullable
  @Override
  public List<TableGenImmediateRecord> execute(PassResults passResults,
                                               Specification viam) throws IOException {
    var tableGenMachineRecords = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);

    return tableGenMachineRecords
        .stream()
        .flatMap(tableGenRecord -> tableGenRecord.getInOperands().stream())
        .filter(operand -> operand instanceof TableGenInstructionImmediateOperand)
        .map(operand -> ((TableGenInstructionImmediateOperand) operand).immediateOperand())
        .distinct()
        .toList();
  }
}
