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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.utils.SourceLocation;

/**
 * A parser for the VADL language, generated using Coco.
 */
public class VadlParser {

  /**
   * Parses the VADL source program at the specified path into an AST.
   */
  public static Ast parse(Path path) throws IOException {
    return parse(path, Collections.emptyMap());
  }

  /**
   * Parses the VADL source program at the specified path into an AST.
   * Works just like {@link VadlParser#parse(String, Map, URI)},
   * except errors will have the proper file locations set.
   */
  public static Ast parse(Path path, Map<String, String> macroOverrides) throws IOException {
    final var startTime = System.nanoTime();
    var scanner = new Scanner(Files.newInputStream(path));
    var parser = new Parser(scanner);
    parser.sourceFile = path.toUri();
    macroOverrides.forEach((key, value) -> parser.macroOverrides.put(key,
        new Identifier(value, SourceLocation.INVALID_SOURCE_LOCATION)));
    var ast = parse(parser);
    ast.fileUri = path.toUri();
    ast.passTimings.add(
        new Ast.PassTimings("Parsing", (System.nanoTime() - startTime) / 1_000_000));


    return ast;
  }

  /**
   * Convenience overload for {@link VadlParser#parse(String, Map, URI)} without any overrides.
   */
  public static Ast parse(String program) {
    return parse(program, Map.of(), null);
  }

  /**
   * Convenience overload for {@link VadlParser#parse(String, Map, URI)} without any overrides.
   */
  public static Ast parse(String program, URI resolutionUri) {
    return parse(program, Map.of(), resolutionUri);
  }

  /**
   * Parses a source program into an AST.
   *
   * @param program        a source code file to parse
   * @param macroOverrides The overrides to perform in the macro evaluation
   * @return The parsed syntax tree.
   * @throws DiagnosticList if there are any parsing errors.
   */
  public static Ast parse(String program, Map<String, String> macroOverrides,
                          @Nullable URI resolutionUri) {
    var scanner = new Scanner(new ByteArrayInputStream(program.getBytes(StandardCharsets.UTF_8)));
    var parser = new Parser(scanner);
    parser.resolutionUri = resolutionUri;
    parser.sourceFile = URI.create("memory://internal");
    macroOverrides.forEach((key, value) -> parser.macroOverrides.put(key,
        new Identifier(value, SourceLocation.INVALID_SOURCE_LOCATION)));
    return parse(parser);
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

    errors.addAll(new SymbolTable.SymbolResolver().resolveSymbols(ast));

    if (!errors.isEmpty()) {
      throw new DiagnosticList(errors.stream().distinct().toList());
    }

    return ast;
  }

}
