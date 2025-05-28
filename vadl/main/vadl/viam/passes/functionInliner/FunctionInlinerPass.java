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
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.DefProp;
import vadl.viam.Instruction;
import vadl.viam.Specification;

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

    allWithBehavior(viam, behaviors);

    return new Output(behaviors);
  }

  private void allWithBehavior(Specification viam,
                               IdentityHashMap<Instruction, UninlinedGraph> behaviors) {

    ViamUtils.findDefinitionsByFilter(viam, d -> d instanceof DefProp.WithBehavior)
        .stream()
        .map(DefProp.WithBehavior.class::cast)
        .forEach(withBehavior -> {
          var uninlinedGraph = handleMainBehavior(withBehavior);
          var def = withBehavior.asDefinition();
          if (def instanceof Instruction instr) {
            behaviors.put(instr, uninlinedGraph);
          }
        });
  }

  private UninlinedGraph handleMainBehavior(DefProp.WithBehavior def) {
    var behavior = def.behaviors().getFirst();
    var copy = behavior.copy();
    Inliner.inlineFuncs(behavior);
    return new UninlinedGraph(copy, def.asDefinition());
  }


}