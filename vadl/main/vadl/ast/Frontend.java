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

import java.io.IOException;
import java.nio.file.Path;
import vadl.utils.Pair;
import vadl.utils.SingleFileVirtualFileSystem;
import vadl.utils.VirtualFileSystem;
import vadl.viam.Specification;

/**
 * A utility class to invoke all the parts and passes of the frontend.
 * The frontend persists out of.
 * - Parsing (which includes):
 *    - Scanning (aka tokenization)
 *    - Parsing
 *    - Macro Expansion
 *    - Symbol resolution
 * - Model removal
 * - Ungrouping
 * - Type checking
 * - Viam lowering
 */
public class Frontend {

  /**
   * Compile a single program to an valid AST.
   * This entails all passes (but not including) until Viam lowering.
   *
   * @param program to compile.
   * @return  the parsed and checked AST.
   * @throws vadl.error.DiagnosticList  if the program isn't valid.
   */
  public static Ast compileToAst(String program) {
    try {
      return compileToAst(SingleFileVirtualFileSystem.PATH,
          new SingleFileVirtualFileSystem(program));
    } catch (IOException e) {
      throw new RuntimeException("This can never happen as the filesystem always succeeds here.");
    }
  }

  /**
   * Compile a program from a provided path to an valid AST.
   * This entails all passes (but not including) until Viam lowering.
   *
   * @param path to compile.
   * @param fileSystem to load the files from.
   * @return  the parsed and checked AST.
   * @throws vadl.error.DiagnosticList  if the program isn't valid.
   */
  public static Ast compileToAst(Path path, VirtualFileSystem fileSystem) throws
      IOException {
    var ast = VadlParser.parse(path, fileSystem);
    var remover = new ModelRemover();
    remover.removeModels(ast);
    var ungrouper = new Ungrouper();
    ungrouper.ungroup(ast);
    var typechecker = new TypeChecker();
    typechecker.verify(ast);
    return ast;
  }

  /**
   * Compile a single program to VIAM.
   *
   * @param program to compile.
   * @return  the parsed and checked VIAM spec.
   * @throws vadl.error.DiagnosticList  if the program isn't valid.
   */
  public static Specification compileToViam(String program) {
    try {
      return compileToViam(SingleFileVirtualFileSystem.PATH,
          new SingleFileVirtualFileSystem(program));
    } catch (IOException e) {
      throw new RuntimeException("This can never happen as the filesystem always succeeds here.");
    }
  }

  /**
   * Compile a single program to VIAM.
   *
   * @param path to compile.
   * @param fileSystem to load the files from.
   * @return  the parsed and checked VIAM spec.
   * @throws vadl.error.DiagnosticList  if the program isn't valid.
   */
  public static Specification compileToViam(Path path, VirtualFileSystem fileSystem) throws
      IOException {
    var ast = compileToAst(path, fileSystem);
    var lowering = new ViamLowering();
    return lowering.generate(ast);
  }

  /**
   * Compile a single program to AST and VIAM.
   * The AST is in the state after all passes except Viam lowering.
   *
   * @param path to compile.
   * @param fileSystem to load the files from.
   * @return  the parsed and checked VIAM spec.
   * @throws vadl.error.DiagnosticList  if the program isn't valid.
   */
  public static Pair<Ast, Specification> compileToAstAndViam(Path path,
                                                             VirtualFileSystem fileSystem) throws
      IOException {
    var ast = compileToAst(path, fileSystem);
    var lowering = new ViamLowering();
    var spec = lowering.generate(ast);
    return Pair.of(ast, spec);
  }
}
