// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.common.IssSideEffectReorderingPass;
import vadl.pass.PassOrders;
import vadl.pass.PassResults;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.Pair;
import vadl.utils.ViamUtils;
import vadl.viam.DefProp;
import vadl.viam.Instruction;
import vadl.viam.Procedure;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.SideEffectNode;

public class IssSideEffectReorderingPassTest extends AbstractTest {

  /**
   * Tests that before and after running the {@link IssSideEffectReorderingPass},
   * all side effects are equal. The pass should NOT remove or add any side effects.
   */
  @Test
  public void testSideEffectsPresence() throws DuplicatedPassKeyException, IOException {
    var specAndPass = specAndPass();
    var spec = specAndPass.left();
    var pass = specAndPass.right();
    var graphs = behaviors(spec);

    var before = new IdentityHashMap<Graph, Map<SideEffectNode, Integer>>();
    graphs.forEach(graph -> before.put(graph, sideEffects(graph)));

    pass.execute(PassResults.empty(), spec);

    var after = new IdentityHashMap<Graph, Map<SideEffectNode, Integer>>();
    graphs.forEach(graph -> after.put(graph, sideEffects(graph)));

    graphs.forEach(graph -> compareSideEffects(before.get(graph), after.get(graph)));
  }

  private void compareSideEffects(Map<SideEffectNode, Integer> before,
                                  Map<SideEffectNode, Integer> after) {
    assertEquals(before, after,
        "Side effects before and after running IssSideEffectReorderingPass are not equal");
  }

  private Set<Graph> behaviors(Specification spec) {
    return ViamUtils.findDefinitionsByFilter(spec, def ->
            def instanceof Instruction || def instanceof Procedure)
        .stream().flatMap(def -> ((DefProp.WithBehavior) def).behaviors().stream())
        .collect(Collectors.toSet());
  }

  private Map<SideEffectNode, Integer> sideEffects(Graph graph) {
    return graph.getNodes(SideEffectNode.class).collect(
        Collectors.toMap(node -> node, Node::usageCount, (a, b) -> a, IdentityHashMap::new));
  }

  private Pair<Specification, IssSideEffectReorderingPass> specAndPass()
      throws IOException, DuplicatedPassKeyException {
    var config = new IssConfiguration(getConfiguration(false));
    var specFile = "passes/sideEffectReordering/reordering-spec.vadl";
    var spec = setupPassManagerAndRunSpec(specFile,
        PassOrders.iss(config).untilFirst(IssSideEffectReorderingPass.class)
            .skip(IssSideEffectReorderingPass.class)
    ).specification();
    return Pair.of(spec, new IssSideEffectReorderingPass(config));
  }
}
