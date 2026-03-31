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

package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.DirectionalNode;

/**
 * The generic TCG node during the lowering by the ISS generator.
 */
public abstract class TcgNode extends DirectionalNode {

  /**
   * Creates the TCG cCode for the given node.
   * This is later used when generating the QEMU frontend.
   *
   * @param nodeToCCode A function that returns the c code for a node passed as an argument.
   * @return the c code of this ndoe.
   */
  @SuppressWarnings("MethodName")
  public abstract String cCode(Function<Node, String> nodeToCCode);

  public abstract Set<TcgVRefNode> usedVars();

  public abstract List<TcgVRefNode> definedVars();

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // Do not visit by graph visitor
  }

}
