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

import java.util.function.Function;
import vadl.iss.passes.tcgLowering.TcgLabel;
import vadl.viam.graph.Node;

/**
 * Used to define the label position in the emitted TCG code.
 * When branching to the label, execution continues at the position of this label set operation.
 */
public class TcgSetLabel extends TcgLabelNode {


  /**
   * Constructs a new {@code TcgSetLabel} with the specified {@link TcgLabel}.
   *
   * @param label the label to set at the position of this label set operation
   */
  public TcgSetLabel(TcgLabel label) {
    super(label);
  }

  @Override
  public Node copy() {
    return new TcgSetLabel(label());
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "gen_set_label(" + label().varName() + ");";
  }
}
