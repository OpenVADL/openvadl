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

package vadl.viam.passes.functionInliner;

import static vadl.utils.GraphUtils.getSingleNode;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Instruction;
import vadl.viam.Relocation;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * A pass which inlines all the function with the given function.
 * Note that the function must be {@code pure} to be inlined.
 * Also, the given {@link Specification} will be mutated in-place. However, the pass
 * saves the original uninlined instruction's behaviors as pass result.
 */
public class FunctionInlinerPass extends Pass {
  public FunctionInlinerPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("FunctionInlinerPass");
  }

  /**
   * Output of the pass.
   * {@code behaviors} saves the {@link UninlinedGraph} from the {@link Instruction}.
   */
  public record Output(IdentityHashMap<Instruction, UninlinedGraph> behaviors) {
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    IdentityHashMap<Instruction, UninlinedGraph> behaviors = new IdentityHashMap<>();

    instructions(viam, behaviors);

    return new Output(behaviors);
  }

  private void instructions(Specification viam,
                            IdentityHashMap<Instruction, UninlinedGraph> behaviors) {

    viam.isa().map(isa -> isa.ownInstructions().stream()).orElse(Stream.empty())
        .forEach(instruction -> {
          behaviors.put(instruction, handleMainBehavior(instruction));
        });
  }

  private UninlinedGraph handleMainBehavior(Instruction instruction) {
    var copy = instruction.behavior().copy();
    return inline(instruction, copy);
  }

  private @Nonnull UninlinedGraph inline(Instruction instruction, Graph copy) {
    var functionCalls = instruction.behavior().getNodes(FuncCallNode.class)
        .filter(funcCallNode -> funcCallNode.function().behavior().isPureFunction())
        .filter(funcCallNode -> !(funcCallNode.function() instanceof Relocation))
        .toList();

    functionCalls.forEach(functionCall -> {
      // copy function behaviors
      var behaviorCopy = functionCall.function().behavior().copy();
      // get return node of function behaviors
      var returnNode = getSingleNode(behaviorCopy, ReturnNode.class);

      // Replace every occurrence of `FuncParamNode` by a copy of the
      // given argument from the `FunctionCallNode`.
      Streams.zip(functionCall.arguments().stream(),
              Arrays.stream(functionCall.function().parameters()), Pair::new)
          .forEach(
              pair -> behaviorCopy.getNodes(FuncParamNode.class)
                  .filter(n -> n.parameter().equals(pair.right()))
                  .forEach(usedParam -> usedParam.replaceAndDelete(pair.left().copy())));

      // replace the function call by a copy of the return value of the function
      functionCall.replaceAndDelete(returnNode.value().copy());
    });

    return new UninlinedGraph(copy, instruction);
  }
}
