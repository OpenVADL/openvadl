// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.dump.infoEnrichers;

import static vadl.dump.InfoEnricher.forType;

import java.util.List;
import vadl.dump.InfoEnricher;
import vadl.dump.InfoUtils;
import vadl.dump.entities.DefinitionEntity;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.rtl.ipg.InstructionProgressGraphVisualizer;
import vadl.rtl.map.MiaMapping;
import vadl.rtl.passes.InstructionProgressGraphExtension;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Stage;

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

  public static InfoEnricher ENRICH_STAGE_WITH_IPG =
      forType(DefinitionEntity.class, (defEntity, passResults) -> {
        if (defEntity.origin() instanceof Stage stage) {
          var mapping = stage.mia().extension(MiaMapping.class);
          if (mapping != null) {
            var info = InfoUtils.createGraphModal(mapping.ipg().name,
                stage.simpleName() + " " + mapping.ipg().name,
                vizStage(stage, mapping));
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

  private static String vizStage(Stage stage, MiaMapping mapping) {
    return new InstructionProgressGraphVisualizer()
        .withNodeFilter(node -> mapping.containsInStage(stage, node))
        .load(mapping.ipg())
        .visualize();
  }

  /**
   * A list of all info enrichers for the RTL generation.
   */
  public static List<InfoEnricher> all = List.of(
      ENRICH_ISA_WITH_IPG,
      ENRICH_INSTRUCTION_WITH_IPG,
      ENRICH_STAGE_WITH_IPG
  );

}
