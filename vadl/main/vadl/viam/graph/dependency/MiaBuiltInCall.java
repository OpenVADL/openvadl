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

import java.util.ArrayList;
import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Logic;
import vadl.viam.Resource;
import vadl.viam.graph.NodeList;

/**
 * Represents a VADL micro architecture built-in process.
 *
 * <p>Holds optional lists of resources and logic elements for reference in instruction built-ins,
 * for example {@code @X}, {@code @bypass} in {@code instruction.readOrForward(@X, @bypass)}.
 *
 * @see vadl.types.BuiltInTable
 */
public class MiaBuiltInCall extends BuiltInCall {

  @DataValue
  private final List<Resource> resources;

  @DataValue
  private final List<Logic> logic;

  /**
   * Create MiA builtin call.
   *
   * @param builtIn builtin type, must be in {@link BuiltInTable#MIA_BUILTINS}
   * @param args arguments
   * @param type result type
   */
  public MiaBuiltInCall(BuiltInTable.BuiltIn builtIn, NodeList<ExpressionNode> args, Type type) {
    super(builtIn, args, type);
    this.resources = new ArrayList<>();
    this.logic = new ArrayList<>();
    ensure(BuiltInTable.MIA_BUILTINS.contains(builtIn), "Not a micro architecture builtin");
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(resources);
    collection.add(logic);
  }

  public List<Resource> resources() {
    return resources;
  }

  public List<Logic> logic() {
    return logic;
  }

  public void add(Resource resource) {
    resources.add(resource);
  }

  public void add(Logic logic) {
    this.logic.add(logic);
  }

}
