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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vadl.viam.Definition;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;

/**
 * Child class of {@link Graph} to indicate that the inlining was already done but
 * this graph has been not inlined.
 */
public class UninlinedGraph extends Graph {
  public UninlinedGraph(String name, List<Node> nodes, Definition parentDefinition) {
    super(name, new ArrayList<>(nodes), parentDefinition);
  }

  public UninlinedGraph(Graph graph, Definition parentDefinition) {
    this(graph.name, graph.getNodes().toList(), parentDefinition);
  }

  @Override
  protected Graph createEmptyInstance(String name, Definition parentDefinition) {
    return new UninlinedGraph(name, Collections.emptyList(), parentDefinition);
  }
}
