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
import java.util.function.Predicate;
import javax.annotation.Nullable;
import vadl.ast.Ast;
import vadl.ast.nodes.Definition;
import vadl.ast.nodes.Expr;
import vadl.ast.nodes.Identifier;
import vadl.ast.nodes.IdentifierPath;
import vadl.ast.nodes.IsId;
import vadl.ast.nodes.Node;
import vadl.ast.nodes.RecursiveAstVisitor;
import vadl.ast.nodes.Statement;
import vadl.ast.nodes.TypedNode;
import vadl.utils.SourceLocation;

/**
 * Finds AST nodes based on their location in source code. This relies on the AST providing proper
 * location information.
 *
 * <p>Note: This search is performed in O(n) as the whole AST tree is traversed. It should be
 * possible to optimize this if needed: SourceLocations in an AST are generally in sorted order
 * (except model invocation edge cases), thus it is not necessary to traverse into all branches -
 * this would improve performance to O(log n).
 */
public abstract class AstFinderByPosition<N extends Node> extends RecursiveAstVisitor {

  // TODO When using this class for the LSP Goto Definition feature, there are some limitations -
  //      AST doesn't provide all the data we would like to have:
  //      - These Identifiers in ImportDefinition have no target set and are not visited
  //        (missing @Child annotations): fileId; importedSymbols[x]
  //      - Model invocations are already applied, i.e. we don't know that the searched position is
  //        on a model invocation, hence we cannot Goto Definition to the model. (Except if we
  //        analyze the expandedFrom data, but that is complex and/or points to only part of the
  //        model.)
  //      - References to Model parameters (within the model body, i.e. Placeholders) do not have an
  //        identifier nor a target

  /**
   * Finds an Identifier or IdentifierPath at the given source code position.
   *
   * @param path The source code file to search in
   * @param position The position to search for (within the file identified by {@code path})
   * @return Null if no Identifier or IdentifierPath found at {@code position}
   */
  static @Nullable <T extends Node & IsId> IsId findIdentifier(
      Ast ast, Path path, SourceLocation.Position position) {
    var visitor = new AstFinderByPosition.BeforeTravel<T>(ast, path, position,
        (n) -> n instanceof IdentifierPath || n instanceof Identifier);
    return visitor.find();
  }

  /**
   * Finds a TypedNode at the given source code position.
   *
   * @param ast Should have gone through the TypeChecker
   * @param path The source code file to search in
   * @param position The position to search for (within the file identified by {@code path})
   * @return Null if no TypedNode found at {@code position}
   */
  @SuppressWarnings("TypeParameterUnusedInFormals")
  static @Nullable <T extends Node & TypedNode> T findTypedNode(
      Ast ast, Path path, SourceLocation.Position position) {
    var visitor = new AstFinderByPosition.AfterTravel<T>(ast, path, position, (n) -> {
      if (!(n instanceof TypedNode tn)) {
        return false;
      }
      // Check if type is actually available
      try {
        tn.type();
      } catch (NullPointerException e) {
        return false;
      }

      // Ignore TypedNodes that may be part of a Model Definition (and thus their Type may depend
      // on one particular Model Invocation)
      // - i.e. they have an ExpandedLocation and their primary location is not fully contained
      //   within the outermost invocation's location.
      if (n.location() instanceof SourceLocation.ExpandedLocation(
          SourceLocation.DirectLocation primaryLocation,
          vadl.utils.RopeList<SourceLocation.DirectLocation> expandedFrom
        )
      ) {
        var outerLocation = expandedFrom.toList().getLast();
        if (!primaryLocation.begin().isWithin(outerLocation)
            || !primaryLocation.end().isWithin(outerLocation)) {
          return false;
        }
      }

      return true;
    });
    return visitor.find();
  }


  protected final Ast ast;
  protected final Path searchPath;
  protected final SourceLocation.Position searchPosition;

  /**
   * If this yields true in {@code beforeTravel()} or {@code afterTravel()}, the current node is a
   * candidate for what we are looking for. Must NOT yield true if {@code testedObject instanceof N}
   * is false!
   */
  final Predicate<Node> condition;

  @Nullable
  private N foundNode = null;

  /**
   * Instantiate one of the subclasses {@link BeforeTravel} or {@link AfterTravel}.
   */
  private AstFinderByPosition(Ast ast, Path path, SourceLocation.Position position,
                              Predicate<Node> condition) {
    this.ast = ast;
    this.searchPath = path;
    this.searchPosition = position;
    this.condition = condition;
  }

  protected @Nullable N find() {
    try {
      for (var definition : ast.definitions) {
        definition.accept(this);
      }
    } catch (FoundSignal fs) {
      return foundNode;
    }
    return null;
  }

  void testNode(Node node) {
    if (!condition.test(node)) {
      return;
    }

    var location = node.location();
    if (searchPath.equals(location.path()) && searchPosition.isWithin(location)) {
      foundNode = (N) node;
      throw new FoundSignal();
    }
  }


  private static class BeforeTravel<N extends Node> extends AstFinderByPosition<N> {
    /**
     * Creates a finder that checks {@code condition} in {@code beforeTravel()}. I.e. this finds
     * the outermost node that fits {@code condition} and {@code position}.
     *
     * @param path The source code file to search in
     * @param position The position to search for (within the file identified by {@code path})
     * @param condition If this yields true, the current node is a candidate for what we are looking
     *                  for. Must NOT yield true if {@code testedObject instanceof N} is false!
     */
    private BeforeTravel(
        Ast ast, Path path, SourceLocation.Position position,
        Predicate<Node> condition) {
      super(ast, path, position, condition);
    }

    @Override
    protected void beforeTravel(Definition definition) {
      testNode(definition);
    }

    @Override
    protected void beforeTravel(Expr expr) {
      testNode(expr);
    }

    @Override
    protected void beforeTravel(Statement statement) {
      testNode(statement);
    }
  }

  private static class AfterTravel<N extends Node> extends AstFinderByPosition<N> {
    /**
     * Creates a finder that checks {@code condition} in {@code afterTravel()}. I.e. this finds
     * the innermost node that fits {@code condition} and {@code position}.
     *
     * @param path The source code file to search in
     * @param position The position to search for (within the file identified by {@code path})
     * @param condition If this yields true, the current node is a candidate for what we are looking
     *                  for. Must NOT yield true if {@code testedObject instanceof N} is false!
     */
    private AfterTravel(
        Ast ast, Path path, SourceLocation.Position position,
        Predicate<Node> condition) {
      super(ast, path, position, condition);
    }

    @Override
    protected void afterTravel(Definition definition) {
      testNode(definition);
    }

    @Override
    protected void afterTravel(Expr expr) {
      testNode(expr);
    }

    @Override
    protected void afterTravel(Statement statement) {
      testNode(statement);
    }
  }

  private static class FoundSignal extends RuntimeException {}
}
