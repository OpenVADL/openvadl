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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.utils.DiskVirtualFileSystem;
import vadl.utils.SingleFileVirtualFileSystem;
import vadl.utils.SourceLocation;
import vadl.utils.VirtualFileSystem;

/**
 * A parser for the VADL language, generated using Coco.
 */
public class VadlParser {

  /**
   * Parses the VADL source program at the specified path into an AST.
   * The returned AST is already macro expanded and all symbols are resolved.
   * This method loads the file always from disk.
   *
   * @param path to load the file from.
   * @throws DiagnosticList if the file cannot be loaded or not correctly parsed.
   */
  public static Ast parse(Path path) {
    return parse(path, new DiskVirtualFileSystem(), Collections.emptyMap());
  }

  /**
   * Parses the VADL source program at the specified path into an AST.
   * The returned AST is already macro expanded and all symbols are resolved.
   * The file will be loaded from the specified filesystem.
   *
   * @param path to load the file from.
   * @param fileSystem to load the file from.
   * @throws DiagnosticList if the file cannot be loaded or not correctly parsed.
   */
  public static Ast parse(Path path, VirtualFileSystem fileSystem) {
    return parse(path, fileSystem, Collections.emptyMap());
  }

  /**
   * Parses the VADL source program at the specified path into an AST.
   * The returned AST is already macro expanded and all symbols are resolved.
   * This method is most useful for testing.
   *
   * @param program to parse.
   * @throws DiagnosticList if the file cannot be loaded or not correctly parsed.
   */
  public static Ast parse(String program) {
    var path = Paths.get("memory");
    var fileSystem = new SingleFileVirtualFileSystem(program, path);
    return parse(path, fileSystem);
  }

  /**
   * Parses the VADL source program at the specified path into an AST.
   * The returned AST is already macro expanded and all symbols are resolved.
   * The file will be loaded from the specified filesystem.
   *
   * @param path to load the file from.
   * @param fileSystem to load the file from.
   * @param macroOverrides which are either specified from an import or the CLI.
   * @throws DiagnosticList if the file cannot be loaded or not correctly parsed.
   */
  public static Ast parse(Path path, VirtualFileSystem fileSystem,
                          Map<String, String> macroOverrides) {
    var scanner = new Scanner(fileSystem.getInputStream(path));
    var parser = new Parser(scanner);
    parser.sourceFile = fileSystem.toAbsolutePath(path);
    parser.fileSystem = fileSystem;
    macroOverrides.forEach((key, value) -> parser.macroOverrides.put(key,
        new Identifier(value, SourceLocation.INVALID_SOURCE_LOCATION)));
    var ast = parser.ast.withPassTiming("Parsing", () -> parse(parser));
    ast.filePath = fileSystem.toAbsolutePath(path);
    return ast;
  }

  private static Ast parse(Parser parser) {
    List<Diagnostic> errors = new ArrayList<>();

    try {
      parser.Parse();
    } catch (Diagnostic e) {
      errors.add(e);
    } catch (DiagnosticList e) {
      errors.addAll(e.items);
    }

    errors.addAll(parser.diagnostics);

    if (!errors.isEmpty()) {
      throw new DiagnosticList(errors.stream().distinct().toList());
    }

    // during parsing there might be errors in the macro table (e.g. conflicting macro definitions)
    if (!parser.macroTable.errors.isEmpty()) {
      throw new DiagnosticList(parser.macroTable.errors.stream().distinct().toList());
    }

    var ast = parser.ast;

    errors.addAll(SymbolTable.collectAndResolveSymbols(ast));

    if (!errors.isEmpty()) {
      throw new DiagnosticList(errors.stream().distinct().toList());
    }

    return ast;
  }

}
