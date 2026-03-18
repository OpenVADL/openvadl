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
 */
public class AstFinderByPosition extends RecursiveAstVisitor {

  private final Path searchPath;
  private final SourceLocation.Position searchPosition;

  private static class FoundSignal extends RuntimeException {
    Identifier identifier;

    public FoundSignal(Identifier identifier) {
      this.identifier = identifier;
    }
  }

  private AstFinderByPosition(Path path, SourceLocation.Position position) {
    searchPath = path;
    searchPosition = position;
  }


  /**
   * Finds an Identifier at the given source code position, and returns it's target's location.
   *
   * @param path The source code file to search in
   * @param position The position to search for (within the file identified by {@code path})
   * @return Null if no identifier found at {@code position} or it has no target
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
   * Finds an Identifier at the given source code position.
   *
   * @param path The source code file to search in
   * @param position The position to search for (within the file identified by {@code path})
   * @return Null if no Identifier found at {@code position}
   */
  static @Nullable Identifier findIdentifier(Ast ast, Path path, SourceLocation.Position position) {
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
    if (expr instanceof Identifier identifier) {
      var location = identifier.location();

      if (searchPath.equals(location.path())
          // Both begin and end position are inclusive
          && location.end().compareTo(searchPosition) >= 0
          && location.begin().compareTo(searchPosition) <= 0) {
        throw new FoundSignal(identifier);
      }
    }
  }
}
