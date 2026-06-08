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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vadl.ast.nodes.Definition;
import vadl.ast.nodes.Expr;
import vadl.ast.nodes.FormatDefinition;
import vadl.ast.nodes.FunctionDefinition;
import vadl.ast.nodes.ImportDefinition;
import vadl.ast.nodes.InstructionDefinition;
import vadl.ast.nodes.ModelDefinition;
import vadl.ast.nodes.ModelTypeDefinition;
import vadl.ast.nodes.RecordTypeDefinition;
import vadl.ast.nodes.RecursiveAstVisitor;
import vadl.ast.nodes.Statement;
import vadl.utils.SourceLocation;
import vadl.utils.VirtualFileSystem;
import vadl.utils.WithLocation;

/**
 * An AST analyzer that collects statistics about the content of the specification.
 * This can be useful for publications to let the reader estimate the size of the specification.
 */
public class SpecStatAnalyser extends RecursiveAstVisitor {

  private final VirtualFileSystem fileSystem;
  private final Set<SourceLocation> seenUnexpandedLocations = new HashSet<>();
  private int files = 0;
  private int linesOfCode = 0;
  private int functionDefinitions = 0;
  private int formatDefinitions = 0;
  private int instructionDefinition = 0;
  private int totalDefinitions = 0;
  private int totalStatements = 0;
  private int totalExpressions = 0;
  private int modelDefinitions = 0;
  private int recordDefinitions = 0;
  private int modelTypeDefinitions = 0;
  private int functionDefinitionsUnexpanded = 0;
  private int formatDefinitionsUnexpanded = 0;
  private int instructionDefinitionUnexpanded = 0;
  private int totalDefinitionsUnexpanded = 0;
  private int totalStatementsUnexpanded = 0;
  private int totalExpressionsUnexpanded = 0;

  private SpecStatAnalyser(VirtualFileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  /**
   * Runs the analyzer on the given AST and returns the collected statistics.
   *
   * @param ast        to analyze (should already have all models removed).
   * @param fileSystem from which the files can be read again to calculate the lines of code.
   * @return the collected statistics.
   */
  public static List<SpecStat> run(Ast ast, VirtualFileSystem fileSystem) {
    var analyser = new SpecStatAnalyser(fileSystem);
    ast.definitions.forEach(definition -> definition.accept(analyser));
    analyser.addModelNodes(ast.rootSymbolTable());

    ast.allReadFiles().forEach(analyser::handlePath);

    return List.of(
        new SpecStat("Files", analyser.files),
        new SpecStat("Lines of code", analyser.linesOfCode),
        new SpecStat("Model Definitions", analyser.modelDefinitions),
        new SpecStat("Record Definitions", analyser.recordDefinitions),
        new SpecStat("Model-Type Definitions", analyser.modelTypeDefinitions),
        new SpecStat("Function Definitions Unexpanded", analyser.functionDefinitionsUnexpanded),
        new SpecStat("Format Definitions Unexpanded", analyser.formatDefinitionsUnexpanded),
        new SpecStat("Instruction Definitions Unexpanded",
            analyser.instructionDefinitionUnexpanded),
        new SpecStat("Total Definitions Unexpanded", analyser.totalDefinitionsUnexpanded),
        new SpecStat("Total Statements Unexpanded", analyser.totalStatementsUnexpanded),
        new SpecStat("Total Expressions Unexpanded", analyser.totalExpressionsUnexpanded),
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
    fileSystem.readLines(path).forEach(unused -> linesOfCode++);
  }

  /**
   * Execute a runnable if the node is contained in the unexpanded spec.
   * So obviously this is a bit tricky because the parser already expands all macros so and there
   * is no parser that can tell us if which node were in the original spec.
   * However, expanded nodes always have the same location with only differing in the
   * expanded from field. So if we ignore that field, we can calculate the
   * unexpanded node count by only taking each location once. So we track them in
   * {@link #seenUnexpandedLocations} and if we find a new one that isn't in there already, it must
   * have also been in the unexpanded form once. And if we have already seen it, it was from a
   * macro, and we already have added it once so we can ignore it.
   *
   * @param loctable on which it will be decided if we have seen this node before.
   * @param action   that will be executed if the node hasn't been seen before.
   * @return true if the node was new and action was executed, false otherwise.
   */
  private boolean ifNewUnexpanded(WithLocation loctable, Runnable action) {
    var directLocation =
        new SourceLocation.DirectLocation(loctable.location().path(), loctable.location().begin(),
            loctable.location().end());
    var isNew = !seenUnexpandedLocations.contains(directLocation);
    if (isNew) {
      seenUnexpandedLocations.add(directLocation);
      action.run();
    }
    return isNew;
  }

  /**
   * Counts the number of models and model-related types.
   * This again seems hard because {@link ModelRemover} already has removed all the models from the
   * AST. However, since models are scoped, they get entered into the symbolTables and never removed
   * from them. So we can traverse all symbol tables of all definitions and count the models.
   *
   * @param table of a definition to import all the macros from.
   */
  private void addModelNodes(SymbolTable table) {
    table.macroSymbols.values().forEach(symbol -> {
      var node = symbol;
      ifNewUnexpanded(node, () -> {
        if (node instanceof ModelDefinition) {
          modelDefinitions++;
        } else if (node instanceof RecordTypeDefinition) {
          recordDefinitions++;
        } else if (node instanceof ModelTypeDefinition) {
          modelTypeDefinitions++;
        }
      });
    });
  }

  @Override
  public void beforeTravel(Definition definition) {
    totalDefinitions++;
    var isNew = ifNewUnexpanded(definition, () -> totalDefinitionsUnexpanded++);
    if (definition instanceof FunctionDefinition) {
      functionDefinitions++;
      if (isNew) {
        functionDefinitionsUnexpanded++;
      }
    }
    if (definition instanceof FormatDefinition) {
      formatDefinitions++;
      if (isNew) {
        formatDefinitionsUnexpanded++;
      }
    }
    if (definition instanceof InstructionDefinition) {
      instructionDefinition++;
      if (isNew) {
        instructionDefinitionUnexpanded++;
      }
    }

    if (definition.symbolTable != null) {
      addModelNodes(definition.symbolTable());
    }
  }

  @Override
  public void beforeTravel(Statement statement) {
    totalStatements++;
    ifNewUnexpanded(statement, () -> totalStatementsUnexpanded++);
  }

  @Override
  public void beforeTravel(Expr expr) {
    totalExpressions++;
    ifNewUnexpanded(expr, () -> totalExpressionsUnexpanded++);
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
