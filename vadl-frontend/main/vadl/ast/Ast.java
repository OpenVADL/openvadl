// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.ast.nodes.Definition;
import vadl.ast.nodes.ImportDefinition;

/**
 * The abstract syntax tree for the vadl language.
 */
public class Ast {
  public List<Definition> definitions = new ArrayList<>();

  @Nullable
  public Path filePath = null;

  public InterleavedTimingRecorder timingRecorder = new InterleavedTimingRecorder();


  @Nullable
  SymbolTable rootSymbolTable;


  SymbolTable rootSymbolTable() {
    return Objects.requireNonNull(rootSymbolTable, "Symbol collector has not been applied");
  }

  public <T> T withPassTiming(String name, Supplier<T> pass) {
    return timingRecorder.withPassTiming(name, pass);
  }

  public void withPassTiming(String name, Runnable pass) {
    timingRecorder.withPassTiming(name, pass);
  }

  /**
   * Returns the list of all files that are included from this AST.
   *
   * @param recursive if true, recursively include files from imported modules.
   * @return the list of paths.
   */
  public List<Path> includedFiles(boolean recursive) {
    return definitions.stream()
        .filter(def -> def instanceof ImportDefinition)
        .flatMap(def -> {
          var ast = ((ImportDefinition) def).moduleAst;
          var stream = Stream.of(ast.filePath);
          if (recursive) {
            stream = Stream.concat(stream, ast.includedFiles(true).stream());
          }
          return stream;
        })
        .toList();
  }

  /**
   * Returns the list of all files that were read to generate this AST.
   *
   * @return the list of paths.
   */
  public List<Path> allReadFiles() {
    var paths = new ArrayList<>(includedFiles(true));
    paths.addFirst(filePath);
    return paths;
  }


  /**
   * Convert the tree back into sourcecode.
   * The generated sourcecode might look quite different but is semantically equal. Some notable
   * details are however:
   * <li> All macros are expanded and macro definitions are no longer in the tree.
   * <li> Grouping with parenthesis might be lost.
   *
   * @return a source code resulting in the same AST.
   */
  public CharSequence prettyPrint() {
    StringBuilder builder = new StringBuilder();
    Definition.prettyPrintDefinitions(0, builder, definitions);
    return builder;
  }

  /**
   * Convert the tree back into sourcecode.
   * The generated sourcecode might look quite different but is semantically equal. Some notable
   * details are however:
   * <li> All macros are expanded and macro definitions are no longer in the tree.
   * <li> Grouping with parenthesis might be lost.
   *
   * <p>Consider prettyPrint() if performance is important.
   *
   * @return a source code resulting in the same AST.
   */
  public String prettyPrintToString() {
    return prettyPrint().toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Ast that = (Ast) o;
    return Objects.equals(definitions, that.definitions);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(definitions);
  }
}
