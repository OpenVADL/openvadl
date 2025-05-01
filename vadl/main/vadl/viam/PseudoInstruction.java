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

import java.util.stream.Stream;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.FuncParamNode;


/**
 * Represents a pseudo instruction in a VADL specification.
 *
 * <p>The behavior graph must fulfill certain condition to be valid and can be checked
 * using the {@link Graph#isPseudoInstruction()} method. The most
 * important graph node to handle is the {@link vadl.viam.graph.control.InstrCallNode}.</p>
 */
public class PseudoInstruction extends CompilerInstruction implements PrintableInstruction {

  private final Assembly assembly;

  /**
   * Instantiates a PseudoInstruction object and verifies it.
   *
   * @param identifier the identifier of the pseudo instruction
   * @param parameters the list of parameters for the pseudo instruction
   * @param behavior   the behavior graph of the pseudo instruction
   * @param assembly   the assembly of the pseudo instruction
   */
  public PseudoInstruction(
      Identifier identifier,
      Parameter[] parameters,
      Graph behavior,
      Assembly assembly
  ) {
    super(identifier, parameters, behavior);
    this.assembly = assembly;
  }

  @Override
  public Identifier identifier() {
    return identifier;
  }

  public Assembly assembly() {
    return assembly;
  }

  @Override
  public void verify() {
    ensure(behavior.isPseudoInstruction(),
        "The given behavior is not a valid pseudo instruction behaviour");

    behavior.getNodes(FuncParamNode.class)
        .forEach(node ->
            node.ensure(Stream.of(parameters).anyMatch(e -> e.equals(node.parameter())),
                "The given parameter is not a known pseudo instruction parameter")
        );
    behavior.verify();
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
