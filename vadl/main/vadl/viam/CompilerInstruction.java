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
import java.util.stream.Stream;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.FuncParamNode;


/**
 * Represents a compiler instruction in a VADL specification.
 *
 * <p>The behavior graph must fulfill certain condition to be valid and can be checked
 * using the {@link Graph#isPseudoInstruction()} method. The most
 * important graph node to handle is the {@link vadl.viam.graph.control.InstrCallNode}.</p>
 */
public class CompilerInstruction extends Definition implements DefProp.WithBehavior {

  protected final Parameter[] parameters;
  protected final Graph behavior;

  /**
   * Instantiates a PseudoInstruction object and verifies it.
   *
   * @param identifier the identifier of the pseudo instruction
   * @param parameters the list of parameters for the pseudo instruction
   * @param behavior   the behavior graph of the pseudo instruction
   */
  public CompilerInstruction(
      Identifier identifier,
      Parameter[] parameters,
      Graph behavior
  ) {
    super(identifier);

    this.parameters = parameters;
    this.behavior = behavior;

    Arrays.stream(this.parameters).forEach(p -> p.setParent(this));
    behavior.setParentDefinition(this);
  }

  public Parameter[] parameters() {
    return parameters;
  }

  public Graph behavior() {
    return behavior;
  }

  @Override
  public void verify() {
    ensure(behavior.isPseudoInstruction(),
        "The given behavior is not a valid compiler instruction behaviour");

    behavior.getNodes(FuncParamNode.class)
        .forEach(node ->
            node.ensure(Stream.of(parameters).anyMatch(e -> e.equals(node.parameter())),
                "The given parameter is not a known compiler instruction parameter")
        );
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

  /**
   * Get the largest parameter in {@link #parameters}. Signed 32 is smaller than unsigned 32 bit.
   * When two parameters have the same bitwidth then the returned parameter is undefined.
   */
  public Parameter getLargestParameter() {
    return Arrays.stream(parameters).sorted((o1, o2) -> {
      var ty1 = o1.type().asDataType().isSigned() ? o1.type().asDataType().bitWidth() - 1 :
          o1.type().asDataType().bitWidth();
      var ty2 = o2.type().asDataType().isSigned() ? o2.type().asDataType().bitWidth() - 1 :
          o2.type().asDataType().bitWidth();
      if (ty1 < ty2) {
        return 1;
      } else {
        return -1;
      }
    }).findFirst().orElseThrow();
  }
}
