// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.lsp;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import vadl.ast.Ast;
import vadl.ast.nodes.Definition;
import vadl.ast.nodes.Expr;
import vadl.ast.nodes.Node;
import vadl.ast.nodes.Statement;
import vadl.utils.SourceLocation;

/**
 * Finds one or several AST nodes based on an arbitrary caller-provided selection criteria.
 */
class AstFinder<N extends Node> {
  /**
   * Finds all root nodes that are the result of expanding the model invocation at the
   * given source code position.
   *
   * @param path The source code file to search in
   * @param position The position to search for (within the file identified by {@code path}
   * @return List of all found nodes. May be empty
   */
  static List<Node> findExpandedNodes(
      Ast ast, Path path, SourceLocation.Position position) {

    var visitor = new AstFinder<>(ast, (node) -> {
      // Before
      if (!(node.location() instanceof SourceLocation.ExpandedLocation expandedLocation)) {
        // Shortcut: Already collected some desired root nodes, but there cannot be more as the
        // current node has not been expanded from a model invocation.
        return Choice.STOP_IF_SOME_FOUND;
      }

      var outermostLocation = expandedLocation.expandedFromStack().getLast();
      if (path.equals(outermostLocation.path()) && position.isWithin(outermostLocation)) {
        return Choice.SELECT_AND_SKIP_CHILDREN;
      }
      // Stop condition: Already collected some desired root nodes, but there cannot be more as the
      // current node has not been expanded from a model invocation that fits the searched position
      // (i.e. we are past the searched position).
      return Choice.STOP_IF_SOME_FOUND;

    }, (node) -> {
      // After
      return Choice.CONTINUE;
    });

    return visitor.find();
  }


  private enum Choice {
    /**
     * No effect.
     */
    CONTINUE(false),
    /**
     * Skip all children of the current node. Has no effect if returned by {@code selectorAfter}.
     */
    SKIP_CHILDREN(false),
    /**
     * Stop traversal altogether.
     */
    STOP(false),
    /**
     * Add this node to the list of found nodes.
     */
    SELECT_AND_CONTINUE(true),
    /**
     * Add this node to the list of found nodes and skip all its children. Same effect as
     * {@code SELECT_AND_CONTINUE} if returned by {@code selectorAfter}.
     */
    SELECT_AND_SKIP_CHILDREN(true),
    /**
     * Add this node to the list of found nodes and stop traversal altogether.
     */
    SELECT_AND_STOP(true),
    /**
     * Stop traversal altogether if at least one node has been selected so far. Has no effect
     * otherwise.
     */
    STOP_IF_SOME_FOUND(false);

    final boolean select;

    Choice(boolean select) {
      this.select = select;
    }
  }

  /**
   * Called in {@code beforeTravel()}, decides which action to take.
   * Must NOT yield {@code SELECT_*} if {@code testedObject instanceof N} is false!
   */
  private final Function<Node, Choice> selectorBefore;

  /**
   * Called in {@code afterTravel()}, decides which action to take.
   * Must NOT yield {@code SELECT_*} if {@code testedObject instanceof N} is false!
   */
  private final Function<Node, Choice> selectorAfter;

  private final Ast ast;
  private final List<N> selectedNodes = new ArrayList<>();

  private AstFinder(Ast ast, Function<Node, Choice> selectorBefore,
      Function<Node, Choice> selectorAfter) {
    this.ast = ast;
    this.selectorBefore = selectorBefore;
    this.selectorAfter = selectorAfter;
  }

  private List<N> find() {
    try {
      for (var definition : ast.definitions) {
        processNode(definition);
      }
    } catch (StopSignal fs) {
      // Nothing
    }
    return selectedNodes;
  }

  private Choice applySelector(Node node, Function<Node, Choice> selector) {
    var choice = selector.apply(node);
    if (choice.select) {
      selectedNodes.add((N) node);
    }
    if (choice == Choice.STOP || choice == Choice.SELECT_AND_STOP
        || (choice == Choice.STOP_IF_SOME_FOUND && !selectedNodes.isEmpty())) {
      throw new StopSignal();
    }
    return choice;
  }

  private void processNode(Node node) {
    if (!(node instanceof Expr) && !(node instanceof Statement) && !(node instanceof Definition)) {
      // Skip intermediate nodes, just visit their children
      // (same as in RecursiveAstVisitor.travel())
      node.forEachChild(this::processNode);
      return;
    }

    var choice = applySelector(node, selectorBefore);
    if (choice != Choice.SKIP_CHILDREN && choice != Choice.SELECT_AND_SKIP_CHILDREN) {
      node.forEachChild(this::processNode);
    }
    applySelector(node, selectorAfter);
  }

  private static class StopSignal extends RuntimeException {}
}
