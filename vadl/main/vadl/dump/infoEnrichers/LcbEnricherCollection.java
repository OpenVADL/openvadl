package vadl.dump.infoEnrichers;

import static vadl.dump.InfoEnricher.forType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import vadl.dump.Info;
import vadl.dump.InfoEnricher;
import vadl.dump.InfoUtils;
import vadl.dump.entities.DefinitionEntity;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionCtx;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.viam.Instruction;

/**
 * A static collection {@link InfoEnricher} that provides
 * information about the LCB.
 */
public class LcbEnricherCollection {

  /**
   * Some instruction have a predefined behavior semantic.
   * It was detected by VADL then emit the label.
   */
  public static InfoEnricher ISA_MATCHING_SUPPLIER_TAG =
      forType(DefinitionEntity.class, (definitionEntity, passResults) -> {
        // This supplier also runs for the VIAM dump.
        // But, the pass wasn't scheduled yet.
        if (!passResults.hasRunPassOnce(IsaMachineInstructionMatchingPass.class)) {
          return;
        }

        if (definitionEntity.origin() instanceof Instruction instruction) {
          if (instruction.hasExtension(MachineInstructionCtx.class)) {
            var label =
                Optional.ofNullable(
                        instruction.extension(
                            MachineInstructionCtx.class))
                    .map(MachineInstructionCtx::label)
                    .map(Enum::name).orElse("No label");
            var info = Info.Tag.of("Instruction Label", label);
            definitionEntity.addInfo(info);
          }
        }
      });

  /**
   * Renders tablegen's instruction operands.
   */
  public static InfoEnricher LLVM_LOWERING_OPERANDS =
      forType(DefinitionEntity.class, (definitionEntity, passResults) -> {
        // This supplier also runs for the VIAM dump.
        // But, the pass wasn't scheduled yet.
        if (!passResults.hasRunPassOnce(LlvmLoweringPass.class)) {
          return;
        }

        var results =
            (LlvmLoweringPass.LlvmLoweringPassResult)
                passResults.lastResultOf(LlvmLoweringPass.class);

        if (results != null && definitionEntity.origin() instanceof Instruction instruction) {
          var result = (LlvmLoweringRecord) results.machineInstructionRecords().get(instruction);

          if (result != null) {
            var renderedInputOperands =
                result.inputs().stream().map(TableGenInstructionOperand::render).collect(
                    Collectors.joining(", "));
            var renderedOutputOperands =
                result.outputs().stream().map(TableGenInstructionOperand::render).collect(
                    Collectors.joining(", "));
            definitionEntity.addInfo(InfoUtils.createCodeBlockExpandable(
                "TableGen Input Operands",
                renderedInputOperands
            ));
            definitionEntity.addInfo(InfoUtils.createCodeBlockExpandable(
                "TableGen Output Operands",
                renderedOutputOperands
            ));
          }
        }
      });

  /**
   * A list of all info enrichers for the default lcb.
   */
  public static List<InfoEnricher> all = List.of(
      ISA_MATCHING_SUPPLIER_TAG,
      LLVM_LOWERING_OPERANDS
  );
}
