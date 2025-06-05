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

package vadl.viam.annotations;


import static vadl.error.Diagnostic.error;

import vadl.viam.Annotation;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ReadResourceNode;

/**
 * The {@code [ undefined when : <condition-expr> ]} annotation that can be set on an instruction
 * to mark it with undefined behavior under the given condition expression.
 * The assembler and compiler are not allowed to produce an instruction that fulfills the
 * condition.
 * The simulator handles it like an unknown instruction and just fails to decode.
 */
public class InstructionUndefinedAnno extends Annotation<Instruction> {

  private final Graph graph;

  public InstructionUndefinedAnno(Graph graph) {
    this.graph = graph;
  }

  public Graph graph() {
    return graph;
  }

  /**
   * Checks the expression for certain properties.
   * E.g., it is not allowed to access resources.
   */
  public void check() {
    // check no resource
    graph.getNodes(ReadResourceNode.class).forEach(node -> {
      throw error("Invalid undefined annotation", node)
          .locationDescription(node, "Resource access is not allowed in this expression.")
          .build();
    });
  }

  @Override
  public Class<Instruction> parentDefinitionClass() {
    return Instruction.class;
  }
}
