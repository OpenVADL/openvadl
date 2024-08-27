package vadl.dump.infoEnrichers;

import static vadl.dump.InfoEnricher.forType;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import vadl.dump.Info;
import vadl.dump.InfoEnricher;
import vadl.dump.entities.DefinitionEntity;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
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
            (IdentityHashMap<Instruction, LlvmLoweringPass.LlvmLoweringIntermediateResult>)
                passResults.lastResultOf(LlvmLoweringPass.class);

        if (results != null && definitionEntity.origin() instanceof Instruction instruction) {
          var result = (LlvmLoweringPass.LlvmLoweringIntermediateResult) results.get(instruction);

          if (result != null) {
            var renderedInputOperands =
                result.inputs().stream().map(TableGenInstructionOperand::render).collect(
                    Collectors.joining(", "));
            var renderedOutputOperands =
                result.outputs().stream().map(TableGenInstructionOperand::render).collect(
                    Collectors.joining(", "));
            var template = """
                <pre><code id="code-block" class="text-sm whitespace-pre">
                %s
                </code></pre>
                """;
            definitionEntity.addInfo(new Info.Expandable(
                "TableGen Input Operands",
                template.formatted(renderedInputOperands)
            ));
            definitionEntity.addInfo(new Info.Expandable(
                "TableGen Output Operands",
                template.formatted(renderedOutputOperands)
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
