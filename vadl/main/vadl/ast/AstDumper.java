// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
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

import java.util.Arrays;
import java.util.List;

/**
 * A pass over the AST that produces a textual representation of the AST.
 */
public class AstDumper extends RecursiveAstVisitor {
  private StringBuilder builder = new StringBuilder();
  private int indent;

  /**
   * Dumps the AST into a textual representation.
   *
   * @param ast to dump.
   * @return a textual representation of the tree.
   */
  public String dump(Ast ast) {
    builder = new StringBuilder();
    indent = 0;

    for (var definition : ast.definitions) {
      definition.accept(this);
    }
    return builder.toString();
  }

  private String indentString() {
    var indentBy = 2;
    var indentCharacters = ". : ' | ";
    var indentLength = indent * indentBy;
    return indentCharacters.repeat(indentLength / indentCharacters.length())
        + indentCharacters.substring(0, indentLength % indentCharacters.length());
  }

  private void dumpNode(Node node) {
    builder.append(indentString());
    builder.append(node.toString());
    if (node instanceof IdentifiableNode identifiable) {
      builder.append(" name: \"%s\"".formatted(identifiable.identifier().name));
    }

    // FIXME: Some other nodes can also have types but the dumper must also work with nullable types
    // which the current TypedNode interface doesn't provide.
    if (node instanceof Expr expr) {
      builder.append(" type: %s".formatted(expr.type));
    }
    builder.append('\n');
  }

  private void dumpChildren(List<? extends Node> children) {
    indent++;
    for (var child : children) {
      if (child instanceof Definition def) {
        def.accept(this);
      } else if (child instanceof Expr expr) {
        expr.accept(this);
      } else if (child instanceof Statement statement) {
        statement.accept(this);
      } else if (child == null) {
        builder.append(indentString()).append("null");
      } else {
        throw new RuntimeException("NOT IMPLEMENTED");
      }
    }
    indent--;
  }

  private void dumpChildren(Node... children) {
    dumpChildren(Arrays.asList(children));
  }

  @Override
  protected void beforeTravel(Expr expr) {
    dumpNode(expr);
    indent++;
  }

  @Override
  protected void beforeTravel(Statement statement) {
    dumpNode(statement);
    indent++;
  }

  @Override
  protected void beforeTravel(Definition definition) {
    dumpNode(definition);
    indent++;
  }

  @Override
  protected void afterTravel(Expr expr) {
    indent--;
  }


  @Override
  protected void afterTravel(Statement statement) {
    indent--;
  }


  @Override
  protected void afterTravel(Definition definition) {
    indent--;
  }

  @Override
  public Void visit(AnnotationDefinition definition) {
    // Also dump the keywords that aren't children
    dumpNode(definition);
    dumpChildren(definition.keywords.stream().map(k -> (Node) k).toList());
    dumpChildren(definition.values);
    return null;
  }

  @Override
  public Void visit(CallIndexExpr expr) {
    dumpNode(expr);
    dumpChildren((Expr) expr.target);
    indent++;
    for (CallIndexExpr.Arguments args : expr.argsIndices) {
      builder.append(indentString()).append("ArgsIndices\n");
      dumpChildren(args.values);
    }
    for (CallIndexExpr.SubCall subCall : expr.subCalls) {
      builder.append(indentString()).append("SubCall\n");
      dumpChildren(subCall.id);
      for (var args : subCall.argsIndices) {
        builder.append(indentString()).append("ArgsIndices\n");
        dumpChildren(args.values);
      }
    }
    indent--;
    return null;
  }

  @Override
  public Void visit(ImportDefinition importDefinition) {
    dumpNode(importDefinition);
    indent++;
    builder.append(indentString()).append("File\n");
    if (importDefinition.fileId != null) {
      dumpChildren(importDefinition.fileId);
    }
    if (importDefinition.filePath != null) {
      dumpChildren(importDefinition.filePath);
    }
    for (List<Identifier> importPath : importDefinition.importedSymbols) {
      builder.append(indentString()).append("Import\n");
      indent++;
      dumpChildren(importPath);
      indent--;
    }
    if (!importDefinition.args.isEmpty()) {
      builder.append(indentString()).append("Args\n");
      indent++;
      dumpChildren(importDefinition.args);
      indent--;
    }
    builder.append(indentString()).append("Module AST\n");
    dumpChildren(importDefinition.moduleAst.definitions);
    indent--;
    return null;
  }

  @Override
  public Void visit(EnumerationDefinition definition) {
    dumpNode(definition);
    indent++;
    for (var entry : definition.entries) {
      entry.name.accept(this);
      if (entry.value != null) {
        entry.value.accept(this);
      }
    }
    indent--;
    return null;
  }

  @Override
  public Void visit(LetExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.identifiers);
    dumpChildren(expr.valueExpr, expr.body);
    return null;
  }

  @Override
  public Void visit(LetStatement stmt) {
    dumpNode(stmt);
    dumpChildren(stmt.identifiers);
    dumpChildren(stmt.valueExpr, stmt.body);
    return null;
  }
}