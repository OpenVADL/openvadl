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

package vadl.lcb.passes.llvmLowering;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This pass generates {@link TableGenPseudoInstruction} from the {@link LlvmLoweringPass}.
 */
public class GenerateTableGenPseudoInstructionRecordPass extends Pass {

  public GenerateTableGenPseudoInstructionRecordPass(
      GeneralConfiguration configuration) {
    super(configuration);
  }

  public LcbConfiguration lcbConfiguration() {
    return (LcbConfiguration) configuration();
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateTableGenPseudoInstructionRecordsPass");
  }

  @Nullable
  @Override
  public List<TableGenPseudoInstruction> execute(PassResults passResults, Specification viam)
      throws IOException {
    var llvmLoweringPassResult =
        (LlvmLoweringPass.LlvmLoweringPassResult) ensureNonNull(
            passResults.lastResultOf(LlvmLoweringPass.class),
            "llvmLowering must exist");

    return
        llvmLoweringPassResult.pseudoInstructionRecords().entrySet().stream()
            .sorted(Comparator.comparing(o -> o.getKey().identifier.simpleName()))
            .map(entry -> {
              var instruction = entry.getKey();
              var result = entry.getValue();
              return new TableGenPseudoInstruction(
                  instruction.identifier.simpleName(),
                  lcbConfiguration().processorName().value(),
                  result.info().flags(),
                  result.info().inputs(),
                  result.info().outputs(),
                  result.info().uses(),
                  result.info().defs(),
                  result.patterns()
              );
            })
            .toList();
  }
}