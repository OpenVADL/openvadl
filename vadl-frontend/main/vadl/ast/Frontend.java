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
import vadl.error.DiagnosticList;
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
    return compileToAst(SingleFileVirtualFileSystem.DEFAULT_PATH,
        new SingleFileVirtualFileSystem(program));
  }

  /**
   * Compile a program from a provided path to a valid AST.
   * This entails all passes (but not including) until Viam lowering.
   *
   * @param path to compile.
   * @param fileSystem to load the files from.
   * @return  the parsed and checked AST.
   * @throws vadl.error.DiagnosticList  if the program isn't valid.
   */
  public static Ast compileToAst(Path path, VirtualFileSystem fileSystem) {
    var ast = VadlParser.parse(path, fileSystem);
    ModelRemover.removeModels(ast);
    Ungrouper.ungroup(ast);
    TypeChecker.verify(ast);
    return ast;
  }

  /**
   * Result of {@link #compileToAstBestEffort(Path, VirtualFileSystem)}.
   *
   * @param ast If not null, a usable AST
   * @param completedPass Up to and including this pass have been applied to the given {@code ast}
   * @param diagnostics If not null, diagnostics that have been encountered (these are usually the
   *                    reason that not all passes have been applied)
   */
  public record BestEffortCompilation(@Nullable Ast ast, AstPass completedPass,
                                      @Nullable DiagnosticList diagnostics) {}

  /**
   * Part of {@link BestEffortCompilation}.
   */
  public enum AstPass {
    NONE(0),
    PARSED(1),
    MODELS_REMOVED(2),
    UNGROUPED(3),
    PARTIALLY_TYPE_CHECKED(4),
    TYPE_CHECKED(5);

    private final int ordinal;

    AstPass(int ordinal) {
      this.ordinal = ordinal;
    }

    /**
     * Returns true if applying {@code this} pass means that {@code desiredPass} must have been
     * applied as well.
     */
    public boolean includes(AstPass desiredPass) {
      return desiredPass.ordinal <= this.ordinal;
    }
  }

  /**
   * Compile a program from a provided path to a valid AST, applying as many passes as possible
   * (Best effort), up to but not including Viam lowering.
   *
   * @param path to compile.
   * @param fileSystem to load the files from.
   * @return best effort result, which may contain a usable AST (up to a particular pass) and/or
   *              diagnostics
   * @throws InterruptedException MAY be thrown if interrupted via {@code Thread.interrupt()}.
   */
  public static BestEffortCompilation compileToAstBestEffort(
      Path path, VirtualFileSystem fileSystem) throws InterruptedException {

    Ast ast;
    try {
      ast = VadlParser.parse(path, fileSystem);
    } catch (DiagnosticList diagnostics) {
      return new BestEffortCompilation(null, AstPass.NONE, diagnostics);
    }
    throwIfInterrupted();

    // The next two passes don't throw diagnostics
    ModelRemover.removeModels(ast);
    throwIfInterrupted();
    Ungrouper.ungroup(ast);
    throwIfInterrupted();

    try {
      TypeChecker.verify(ast);
    } catch (DiagnosticList diagnostics) {
      return new BestEffortCompilation(ast, AstPass.PARTIALLY_TYPE_CHECKED, diagnostics);
    }

    return new BestEffortCompilation(ast, AstPass.TYPE_CHECKED, null);
  }

  private static void throwIfInterrupted() throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
  }


  /**
   * Compile a single program to VIAM.
   *
   * @param program to compile.
   * @return  the parsed and checked VIAM spec.
   * @throws vadl.error.DiagnosticList  if the program isn't valid.
   */
  public static Specification compileToViam(String program) {
    return compileToViam(SingleFileVirtualFileSystem.DEFAULT_PATH,
        new SingleFileVirtualFileSystem(program));
  }

  /**
   * Compile a single program to VIAM.
   *
   * @param path to compile.
   * @param fileSystem to load the files from.
   * @return  the parsed and checked VIAM spec.
   * @throws vadl.error.DiagnosticList  if the program isn't valid.
   */
  public static Specification compileToViam(Path path, VirtualFileSystem fileSystem) {
    var ast = compileToAst(path, fileSystem);
    return ViamLowering.generate(ast);
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
                                                             VirtualFileSystem fileSystem) {
    var ast = compileToAst(path, fileSystem);
    var spec = ViamLowering.generate(ast);
    return Pair.of(ast, spec);
  }
}
