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

package vadl.dump;

import static vadl.utils.ViamUtils.findDefinitionsByFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.DefProp;
import vadl.viam.Definition;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.visualize.DotGraphVisualizer;


/**
 * CollectBehaviorDotGraphPass is a type of {@code Pass} that processes a VADL specification
 * to collect behavior definitions and generate corresponding DOT graph representations.
 * The resulting graph representations are returned along with the results of the
 * previously executed pass.
 * This is later used by the {@link HtmlDumpPass} to dump the behavior at different
 * points in time.
 */
public class CollectBehaviorDotGraphPass extends Pass {

  /**
   * Represents the result of executing the {@code CollectBehaviorDotGraphPass},
   * which includes a mapping of behavior definitions to their respective DOT
   * graph representations and the result from the previously executed pass.
   *
   * @param behaviors A map where each key is a {@link Definition} representing a
   *                  VADL definition with behaviors, and the associated value is a
   *                  list of strings where each string is a DOT graph representation
   *                  of the behavior.
   * @param prevPass  The result from the previous pass execution encapsulated in
   *                  {@link PassResults.SingleResult}.
   */
  public record Result(
      Map<Definition, List<String>> behaviors,
      PassResults.SingleResult prevPass
  ) {
  }

  public CollectBehaviorDotGraphPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Collect Behavior Dot Graphs");
  }

  @Override
  public Result execute(PassResults passResults, Specification viam)
      throws IOException {

    var lastPass = passResults.lastExecution();
    var result = new HashMap<Definition, List<String>>();

    var definitions = findDefinitionsByFilter(viam, DefProp.WithBehavior.class::isInstance);

    for (var definition : definitions) {
      var withBehavior = (DefProp.WithBehavior) definition;
      var dotGraphs = withBehavior.behaviors().stream()
          .map(CollectBehaviorDotGraphPass::createDotGraphFor)
          .toList();
      result.put(definition, dotGraphs);
    }

    return new Result(result, lastPass);
  }

  /**
   * Return the dot representation for {@link Graph}.
   */
  public static String createDotGraphFor(Graph graph) {
    return new DotGraphVisualizer()
        .load(graph)
        .visualize();
  }
}
