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

package vadl.ast;

import java.nio.file.Path;
import javax.annotation.Nullable;
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
public class AstFinderByPosition extends RecursiveAstVisitor {

  private final Path searchPath;
  private final SourceLocation.Position searchPosition;

  private static class FoundSignal extends RuntimeException {
    IsId identifier;

    public FoundSignal(IsId identifier) {
      this.identifier = identifier;
    }
  }

  private AstFinderByPosition(Path path, SourceLocation.Position position) {
    searchPath = path;
    searchPosition = position;
  }

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
   * Finds an Identifier or IdentifierPath at the given source code position, and returns it's
   * target's location.
   *
   * @param path The source code file to search in
   * @param position The position to search for (within the file identified by {@code path})
   * @return Null if no Identifier or IdentifierPath found at {@code position} or it has no target
   */
  public static @Nullable SourceLocation findIdentifierTargetLocation(
      Ast ast, Path path, SourceLocation.Position position) {
    var identifier = findIdentifier(ast, path, position);
    if (identifier == null) {
      return null;
    }
    var target = identifier.target();
    if (target == null) {
      return null;
    }
    return target.location();
  }

  /**
   * Finds an Identifier or IdentifierPath at the given source code position.
   *
   * @param path The source code file to search in
   * @param position The position to search for (within the file identified by {@code path})
   * @return Null if no Identifier or IdentifierPath found at {@code position}
   */
  static @Nullable IsId findIdentifier(Ast ast, Path path, SourceLocation.Position position) {
    var visitor = new AstFinderByPosition(path, position);
    try {
      for (var definition : ast.definitions) {
        definition.accept(visitor);
      }
    } catch (FoundSignal fs) {
      return fs.identifier;
    }
    return null;
  }


  @Override
  protected void beforeTravel(Expr expr) {
    if (expr instanceof IdentifierPath || expr instanceof Identifier) {
      IsId identifier = (IsId) expr;
      var location = identifier.location();

      if (searchPath.equals(location.path()) && searchPosition.isWithin(location)) {
        throw new FoundSignal(identifier);
      }
    }
  }
}
