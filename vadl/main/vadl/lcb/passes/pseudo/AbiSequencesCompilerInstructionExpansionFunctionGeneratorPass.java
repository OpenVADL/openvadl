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

package vadl.lcb.passes.pseudo;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.model.GcbExpandPseudoInstructionCppFunction;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Abi;
import vadl.viam.CompilerInstruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;

/**
 * Pass to create {@link GcbExpandPseudoInstructionCppFunction}.
 */
public class AbiSequencesCompilerInstructionExpansionFunctionGeneratorPass extends Pass {

  public AbiSequencesCompilerInstructionExpansionFunctionGeneratorPass(
      GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("CompilerExpansionFunctionGeneratorPass");
  }

  protected Stream<Pair<CompilerInstruction, Graph>> getApplicable(
      Specification viam) {
    var abi = (Abi) viam.definitions().filter(x -> x instanceof Abi).findFirst().get();

    return Stream.concat(abi.constantSequences().stream(),
            abi.registerAdjustmentSequences().stream())
        .map(compilerInstruction -> Pair.of(compilerInstruction, compilerInstruction.behavior()));
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var result = new IdentityHashMap<CompilerInstruction, GcbExpandPseudoInstructionCppFunction>();

    getApplicable(viam)
        .forEach(x -> {
          var instruction = x.left();
          result.put(instruction, Utils.create(instruction.identifier, instruction.behavior()));
        });

    return result;
  }
}
