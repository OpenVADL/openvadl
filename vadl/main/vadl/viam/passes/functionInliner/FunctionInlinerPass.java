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

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.stream.Streams;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.ArtificialResource;
import vadl.viam.DefProp;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;

/**
 * A pass which inlines all the function calls with the given function in certain behaviors.
 * Calls inside functions are only inlined when the function itself is inlined into another
 * behavior or when the function is used later by something that requires it to have no
 * function calls.
 *
 * <p>Note that the function must be {@code pure} to be inlined.
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
    IdentityHashMap<Instruction, UninlinedGraph> uninlined = new IdentityHashMap<>();

    // functions calls in function are not inlined by default, but in some places it is
    // still required
    getFunctionsToInline(viam).map(Function::behavior).forEach(Inliner::inlineFuncs);

    // we inline into every behavior except functions
    getDefs(viam, DefProp.WithOptionalBehaviour.class)
        .filter(Predicate.not(Function.class::isInstance))
        .forEach(def -> handleDefinition(def, uninlined));

    return new Output(uninlined);
  }

  private Stream<Function> getFunctionsToInline(Specification viam) {
    // TODO: check if other functions need to be inlined
    return Stream.concat(
        getDefs(viam, Format.class)
            .flatMap(f -> f.fieldAccesses().stream())
            .flatMap(fa -> Streams.of(fa.accessFunction(), fa.predicate()))
            .filter(Objects::nonNull),
        getDefs(viam, ArtificialResource.class)
            .map(ArtificialResource::readFunction)
    );
  }

  private void handleDefinition(DefProp.WithOptionalBehaviour def,
                                IdentityHashMap<Instruction, UninlinedGraph> uninlined) {
    def.behaviors().forEach(behavior -> {
      if (def instanceof Instruction instr) {
        // we save the uninlined version of the instruction behavior
        uninlined.put(instr, new UninlinedGraph(behavior.copy(), def.asDefinition()));
      }
      Inliner.inlineFuncs(behavior);
    });
  }

  private <T> Stream<T> getDefs(Specification viam, Class<T> type) {
    return ViamUtils.findDefinitionsByFilter(viam, type::isInstance).stream().map(type::cast);
  }
}