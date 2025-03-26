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
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenCompilerInstruction;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

public class GenerateTableGenConstantMatInstructionRecordPass extends Pass {
  public GenerateTableGenConstantMatInstructionRecordPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateTableGenConstantMatInstructionRecordPass");
  }

  public LcbConfiguration lcbConfiguration() {
    return (LcbConfiguration) configuration();
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var llvmLoweringPassResult =
        (LlvmLoweringPass.LlvmLoweringPassResult) ensureNonNull(
            passResults.lastResultOf(LlvmLoweringPass.class),
            "llvmLowering must exist");

    return
        llvmLoweringPassResult.compilerInstructionRecords().entrySet().stream()
            .sorted(Comparator.comparing(o -> o.getKey().identifier.simpleName()))
            .map(entry -> {
              var instruction = entry.getKey();
              var result = entry.getValue();
              return new TableGenCompilerInstruction(
                  instruction,
                  instruction.identifier.simpleName(),
                  lcbConfiguration().targetName().value(),
                  result.info().flags(),
                  result.info().inputs(),
                  result.info().outputs(),
                  result.info().uses(),
                  result.info().defs()
              );
            })
            .toList();
  }
}
