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

package vadl.viam.graph.dependency;

import static java.util.Objects.requireNonNull;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Stage;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * Read from stage output.
 */
public class ReadStageOutputNode extends ExpressionNode {

  @DataValue
  protected Stage stage;

  /**
   * Creates a new `ReadStageOutputNode`.
   *
   * @param stage The {@link Stage} being read from.
   */
  public ReadStageOutputNode(Stage stage) {
    // We can assume here that the read stage's output type is non-null, since
    // otherwise we would have run into an error in the typechecker already.
    super(requireNonNull(stage.outputType()));
    this.stage = stage;
  }

  public Stage stage() {
    return stage;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(stage);
  }

  @Override
  public ExpressionNode copy() {
    return new ReadStageOutputNode(stage);
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }
}
