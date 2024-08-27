package vadl.dump.infoEnrichers;

import static vadl.dump.InfoEnricher.forType;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import vadl.dump.Info;
import vadl.dump.InfoEnricher;
import vadl.dump.entities.DefinitionEntity;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.viam.Instruction;

public class LcbEnricherCollection {

  public static InfoEnricher ISA_MATCHING_SUPPLIER_TAG =
      forType(DefinitionEntity.class, ((definitionEntity, passResults) -> {
        // This supplier also runs for the VIAM dump.
        // But, the pass wasn't scheduled yet.
        if (!passResults.hasRunPassOnce(IsaMatchingPass.class)) {
          return;
        }

        var labels = (HashMap<InstructionLabel, List<Instruction>>) passResults.lastResultOf(
            IsaMatchingPass.class);
        if (labels != null && definitionEntity.origin() instanceof Instruction instruction) {
          var flipped = LlvmLoweringPass.flipIsaMatching(labels);
          var label =
              Optional.ofNullable(flipped.get(instruction)).map(Enum::name).orElse("No label");
          var info = Info.Tag.of("Instruction Label", label);
          definitionEntity.addInfo(info);
        }

      }));


  /**
   * A list of all info enrichers for the default lcb.
   */
  public static List<InfoEnricher> all = List.of(
      ISA_MATCHING_SUPPLIER_TAG
  );
}
