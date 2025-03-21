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

import java.util.ArrayList;
import java.util.List;
import vadl.types.Type;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * The Assembly definition of an instruction in a VADL specification.
 */
public class Assembly extends Definition {

  private final Function function;

  /**
   * Creates an Assembly object with the specified identifier and arguments.
   *
   * @param identifier the identifier of the Assembly definition
   * @param function   the function to create an assembly string
   */
  public Assembly(Identifier identifier, Function function) {
    super(identifier);

    this.function = function;

    verify();
  }

  public Function function() {
    return function;
  }

  @Override
  public void verify() {
    super.verify();
    ensure(function.returnType().equals(Type.string()),
        "Assembly function does not return a String, but %s", function.returnType());
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Some instructions require that the outputted immediate is encoded. VADL allows
   * to write the field access function in the assembly to indicate this. This class
   * is wrapper structure to return these cases with the operand index, since it is
   * possible to have multiple immediates in an assembly definition and not all have
   * to be encoded.
   * Here {@code immS} has to be encoded when {@code immS} is a field access function.
   * <pre>
   * assembly $name = (mnemonic, " ", register(rd), ",", decimal(immS), $asm)
   * </pre>
   * When {@code immS} is a field in the format, it does not need to be encoded.
   */
  public record FieldAccessFunctionPosition(FieldAccessRefNode fieldAccessRefNode,
      /* Operand in Assembly */ int opIndex) {

  }

  /**
   * Return the {@link FieldAccessFunctionPosition} which are referenced in {@link #function()}.
   */
  public List<FieldAccessFunctionPosition> fieldAccessPositions() {
    var returnNode = function.behavior().getNodes(ReturnNode.class)
        .findFirst()
        .orElseThrow();

    var nodes = new ArrayList<Node>();
    returnNode.collectInputsWithChildren(nodes);

    var operands = nodes
        .stream()
        .filter(x -> x instanceof BuiltInCall
            || x instanceof FieldRefNode
            || x instanceof FieldAccessRefNode)
        .toList();

    return function.behavior().getNodes(FieldAccessRefNode.class)
        .map(fieldAccessRefNode -> new FieldAccessFunctionPosition(fieldAccessRefNode,
            operands.indexOf(fieldAccessRefNode)))
        .toList();
  }
}
