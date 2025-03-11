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

package vadl.viam;

import java.util.List;
import vadl.utils.GraphUtils;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ProcEndNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * A procedure is a callable definition with behavior.
 * In comparison to a {@link Function} a procedure has no return value but is able to
 * have side effects (e.g., writing a register file).
 * It is used by the {@link ArtificialResource} as write behavior.
 * Additionally, a VADL exception is also a procedure.
 */
public class Procedure extends Definition implements DefProp.WithBehavior {

  private final Parameter[] parameters;
  private final Graph behavior;

  public Procedure(Identifier identifier, Parameter[] parameters, Graph behavior) {
    super(identifier);
    this.parameters = parameters;
    this.behavior = behavior;
    this.behavior.setParentDefinition(this);
  }

  public Parameter[] parameters() {
    return parameters;
  }

  public List<Resource> writtenResources() {
    return behavior.getNodes(WriteResourceNode.class).map(WriteResourceNode::resourceDefinition)
        .toList();
  }

  public List<Resource> readResources() {
    return behavior.getNodes(ReadResourceNode.class).map(ReadResourceNode::resourceDefinition)
        .toList();
  }

  @Override
  public void verify() {
    super.verify();

    GraphUtils.getSingleNode(behavior, ProcEndNode.class);

    behavior.verify();
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public List<Graph> behaviors() {
    return List.of(behavior);
  }
}
