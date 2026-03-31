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
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgLabel;
import vadl.javaannotations.viam.DataValue;

/**
 * An abstract base class for TCG nodes that are associated with a {@link TcgLabel}.
 * This class provides common functionality for nodes that work with
 * labels in the TCG lowering process.
 */
public abstract class TcgLabelNode extends TcgNode {

  /**
   * The label associated with this TCG node.
   */
  @DataValue
  private TcgLabel label;

  /**
   * Constructs a new {@code TcgLabelNode} with the specified label.
   *
   * @param label The {@link TcgLabel} associated with this node.
   */
  protected TcgLabelNode(TcgLabel label) {
    this.label = label;
  }

  /**
   * Returns the {@link TcgLabel} associated with this node.
   *
   * @return The label of this node.
   */
  public TcgLabel label() {
    return label;
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return new HashSet<>();
  }

  @Override
  public List<TcgVRefNode> definedVars() {
    return List.of();
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(label);
  }
}