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
import vadl.cppCodeGen.model.CppGenericType;
import vadl.cppCodeGen.model.CppParameter;
import vadl.cppCodeGen.model.CppType;
import vadl.cppCodeGen.model.GcbExpandPseudoInstructionCppFunction;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.utils.SourceLocation;
import vadl.viam.Abi;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;

/**
 * The {@code PseudoExpansionCodeGenerator} requires a function to generate the expansion.
 * However, we only have a {@link Graph} as behavior. This pass wraps the graph to a
 * {@link GcbExpandPseudoInstructionCppFunction}.
 */
public class PseudoExpansionFunctionGeneratorPass extends Pass {
  public PseudoExpansionFunctionGeneratorPass(
      GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("PseudoExpansionFunctionGeneratorPass");
  }

  private Stream<Pair<PseudoInstruction, Graph>> getApplicable(
      Specification viam) {
    var abi = (Abi) viam.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var specifiedSequences = Stream.of(abi.returnSequence(), abi.callSequence());

    var pseudoInstructions = Stream.concat(viam.isa()
            .map(isa -> isa.ownPseudoInstructions().stream()).orElseGet(Stream::empty),
        specifiedSequences);
    return pseudoInstructions
        .map(pseudoInstruction -> Pair.of(pseudoInstruction, pseudoInstruction.behavior()));
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var result = new IdentityHashMap<PseudoInstruction, GcbExpandPseudoInstructionCppFunction>();

    getApplicable(viam)
        .forEach(x -> {
          var pseudoInstruction = x.left();
          var ty = new CppType("MCInst", true, true);
          var param = new CppParameter(new Identifier("instruction",
              SourceLocation.INVALID_SOURCE_LOCATION),
              ty);
          var function = new GcbExpandPseudoInstructionCppFunction(
              pseudoInstruction.identifier.append("expand"),
              new Parameter[] {param},
              new CppGenericType("std::vector", new CppType("MCInst", false, false)),
              pseudoInstruction.behavior());

          result.put(pseudoInstruction, function);
        });

    return result;
  }
}
