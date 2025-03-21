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

import java.util.Arrays;
import java.util.List;
import vadl.viam.graph.Graph;

/**
 * VADL ABI's constant sequence representation.
 */
public class AbiConstantSequence extends Definition implements DefProp.WithBehavior {
  private final Parameter[] parameters;
  private final Graph behavior;

  /**
   * Constructor.
   */
  public AbiConstantSequence(Identifier identifier,
                             Parameter[] parameters,
                             Graph behavior) {
    super(identifier);
    this.parameters = parameters;
    this.behavior = behavior;

    Arrays.stream(this.parameters).forEach(p -> p.setParent(this));
    behavior.setParentDefinition(this);
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public List<Graph> behaviors() {
    return List.of(behavior);
  }


  public Parameter[] parameters() {
    return parameters;
  }

  public Graph behavior() {
    return behavior;
  }
}
