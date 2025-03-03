package vadl.dump.infoEnrichers;

import static vadl.dump.InfoEnricher.forType;

import java.util.List;
import vadl.dump.InfoEnricher;
import vadl.dump.InfoUtils;
import vadl.dump.entities.DefinitionEntity;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.rtl.ipg.InstructionProgressGraphVisualizer;
import vadl.rtl.passes.InstructionProgressGraphExtension;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.graph.Graph;

/**
 * A collection of info enrichers that provide information during the RTL generation.
 */
public class RtlEnricherCollection {

  public static InfoEnricher ENRICH_ISA_WITH_IPG =
      forType(DefinitionEntity.class, (defEntity, passResults) -> {
        if (defEntity.origin() instanceof InstructionSetArchitecture isa) {
          var ext = isa.extension(InstructionProgressGraphExtension.class);
          if (ext != null) {
            var info = InfoUtils.createGraphModal(ext.ipg().name, ext.ipg().name, viz(ext.ipg()));
            defEntity.addInfo(info);
          }
        }
      });

  public static InfoEnricher ENRICH_INSTRUCTION_WITH_IPG =
      forType(DefinitionEntity.class, (defEntity, passResults) -> {
        if (defEntity.origin() instanceof Instruction instruction) {
          var isa = instruction.parentArchitecture();
          var ext = isa.extension(InstructionProgressGraphExtension.class);
          if (ext != null) {
            var info = InfoUtils.createGraphModal(ext.ipg().name,
                instruction.simpleName() + " " + ext.ipg().name,
                vizInstruction(ext.ipg(), instruction));
            defEntity.addInfo(info);
          }
        }
      });

  private static String viz(InstructionProgressGraph ipg) {
    return new InstructionProgressGraphVisualizer()
        .load(ipg)
        .visualize();
  }

  private static String vizInstruction(InstructionProgressGraph ipg, Instruction instruction) {
    return new InstructionProgressGraphVisualizer()
        .withNodeFilter(node -> ipg.getContext(node).instructions().contains(instruction))
        .load(ipg)
        .visualize();
  }

  /**
   * A list of all info enrichers for the RTL generation.
   */
  public static List<InfoEnricher> all = List.of(
      ENRICH_ISA_WITH_IPG,
      ENRICH_INSTRUCTION_WITH_IPG
  );

}
