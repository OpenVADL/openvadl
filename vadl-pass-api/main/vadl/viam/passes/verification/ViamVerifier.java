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

package vadl.viam.passes.verification;

import vadl.viam.Abi;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.graph.Node;
import vadl.viam.passes.GraphProcessor;

/**
 * Calls the verification method on all definitions in the given one and all its
 * member definitions.
 */
public class ViamVerifier extends DefinitionVisitor.Recursive {

  private ViamVerifier() {
  }

  public static void verifyAllIn(Definition toVerify) {
    new ViamVerifier().verifyDefinition(toVerify);
  }

  private void verifyDefinition(Definition toVerify) {
    toVerify.accept(this);
  }

  @Override
  public void afterTraversal(Definition definition) {
    super.afterTraversal(definition);
    definition.verify();
  }

  @Override
  public void visit(Abi abi) {

  }


  /**
   * Calls the verify method on all nodes in the given subgraph.
   */
  public static class Graph extends GraphProcessor<Node> {

    public static void verifySubGraph(Node node) {
      new Graph().processNode(node);
    }

    @Override
    protected Node processUnprocessedNode(Node toProcess) {
      // verify inputs
      toProcess.visitInputs(this);
      toProcess.verify();
      return toProcess;
    }
  }
}
