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
import java.util.List;
import vadl.utils.VirtualFileSystem;

/**
 * An AST analyzer that collects statistics about the content of the specification.
 */
public class SpecStatAnalyser extends RecursiveAstVisitor {

  private final VirtualFileSystem fileSystem;
  private int files = 0;
  private int linesOfCode = 0;
  private int functionDefinitions = 0;
  private int formatDefinitions = 0;
  private int instructionDefinition = 0;
  private int totalDefinitions = 0;
  private int totalStatements = 0;
  private int totalExpressions = 0;

  private SpecStatAnalyser(VirtualFileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  /**
   * Runs the analyser on the given AST and returns the collected statistics.
   *
   * @param ast to analyze.
   * @param fileSystem from which the files can be read again to calcuulate the lines of code.
   * @return the collected statistics.
   */
  public static List<SpecStat> run(Ast ast, VirtualFileSystem fileSystem) {
    var analyser = new SpecStatAnalyser(fileSystem);
    ast.definitions.forEach(definition -> definition.accept(analyser));

    ast.allReadFiles().forEach(analyser::handlePath);

    return List.of(
        new SpecStat("Files", analyser.files),
        new SpecStat("Lines of code", analyser.linesOfCode),
        new SpecStat("Function Definitions", analyser.functionDefinitions),
        new SpecStat("Format Definitions", analyser.formatDefinitions),
        new SpecStat("Instruction Definitions", analyser.instructionDefinition),
        new SpecStat("Total Definitions", analyser.totalDefinitions),
        new SpecStat("Total Statements", analyser.totalStatements),
        new SpecStat("Total Expressions", analyser.totalExpressions)
    );
  }

  private void handlePath(Path path) {
   files++;
    fileSystem.readLines(path).forEach(line -> linesOfCode++);
  }

  @Override
  public void beforeTravel(Definition definition) {
    totalDefinitions++;
    if (definition instanceof FunctionDefinition) {
      functionDefinitions++;
    }
    if (definition instanceof FormatDefinition) {
      formatDefinitions++;
    }
    if (definition instanceof InstructionDefinition) {
      instructionDefinition++;
    }
  }

  @Override
  public void beforeTravel(Statement statement) {
    totalStatements++;
  }

  @Override
  public void beforeTravel(Expr expr) {
    totalExpressions++;
  }

  @Override
  public Void visit(ImportDefinition definition) {
    beforeTravel(definition);
    definition.moduleAst.definitions.forEach(def -> def.accept(this));
    afterTravel(definition);
    return null;
  }

  /**
   * Statistics container to hold the collected data.
   */
  public record SpecStat(String name, int count) {
  }

}
