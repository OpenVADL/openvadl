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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * Represents an abstract TCG (Tiny Code Generation) operation node.
 * It extends the DirectionalNode class and contains a result variable and a width specification.
 * This class is designed to be the base class for specific TCG operator nodes.
 */
public abstract class TcgOpNode extends TcgNode {

  @Input
  private NodeList<TcgVRefNode> destinations;
  @DataValue
  private Tcg_32_64 width;

  public TcgOpNode(TcgVRefNode singleDest, Tcg_32_64 width) {
    this.destinations = new NodeList<>(singleDest);
    this.width = width;
  }

  public TcgOpNode(NodeList<TcgVRefNode> destinations, Tcg_32_64 width) {
    this.destinations = destinations;
    this.width = width;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    for (var d : destinations) {
      ensure(d.var().width() == width, "result variable width does not match");
    }
  }

  public Tcg_32_64 width() {
    return width;
  }

  public TcgVRefNode firstDest() {
    return destinations.get(0);
  }

  public List<TcgVRefNode> destinations() {
    return destinations;
  }

  public void setDest(NodeList<TcgVRefNode> res) {
    this.destinations = res;
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return new HashSet<>();
  }

  @Override
  public List<TcgVRefNode> definedVars() {
    return destinations;
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // Do not visit by graph visitor
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(width);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    destinations = destinations.stream().map((e) -> visitor.apply(this, e, TcgVRefNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(destinations);
  }
}
