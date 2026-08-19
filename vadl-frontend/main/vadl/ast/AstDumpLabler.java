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

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.List;
import vadl.ast.nodes.AnnotationDefinition;
import vadl.ast.nodes.CallIndexExpr;
import vadl.ast.nodes.ConstantDefinition;
import vadl.ast.nodes.DefaultAstVisitor;
import vadl.ast.nodes.Expr;
import vadl.ast.nodes.FloatTypeDefinition;
import vadl.ast.nodes.IdentifiableNode;
import vadl.ast.nodes.ImportDefinition;
import vadl.ast.nodes.LetExpr;
import vadl.ast.nodes.LetStatement;
import vadl.ast.nodes.Node;
import vadl.ast.nodes.SyntaxType;
import vadl.utils.SourceLocation;


public class AstDumpLabler extends DefaultAstVisitor<AstDumpLabler.DumpLabel> {
  public record DumpLabel(String description, List<Node> children) {}

  public class  PseudoChild<T extends Node> extends Node {
    public String name;
    public List<T> children;

    public PseudoChild(String name, List<T> children) {
      this.name = name;
      this.children = children;
    }

    @Override
    public SyntaxType syntaxType() {
      // Just implemented to pass Node
      throw new IllegalStateException();
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      // Just implemented to pass Node
      throw new IllegalStateException();
    }

    @Override
    public SourceLocation location() {
      // Just implemented to pass Node
      throw new IllegalStateException();
    }
  }

  private String defaultDescription(Node node) {
    var builder = new StringBuilder();
    builder.append(node.toString());
    if (node instanceof IdentifiableNode identifiable) {
      builder.append(" name: \"%s\"".formatted(identifiable.identifier().name));
    }

    // FIXME: Some other nodes can also have types but the dumper must also work with nullable types
    // which the current TypedNode interface doesn't provide.
    if (node instanceof Expr expr) {
      builder.append(" type: %s".formatted(expr.type));
    }

    return builder.toString();
  }

  @Override
  public DumpLabel visitNode(Node node) {
    // Special Handling for some nodes
    if (node instanceof PseudoChild pseudoChild) {
      return new DumpLabel(pseudoChild.name, pseudoChild.children);
    }

    return new DumpLabel(defaultDescription(node), node.children());
  }

  @Override
  public DumpLabel visit(ConstantDefinition definition) {
    var builder = new StringBuilder();
    builder.append(definition.toString());
    builder.append(" name: \"%s\"".formatted(definition.identifier().name));

    builder.append(" evaluatedValue: %s".formatted(definition.evaluatedValue));

    return new DumpLabel(builder.toString(), definition.children());
  }

  @Override
  public DumpLabel visit(AnnotationDefinition definition) {
    // Also dump the keywords that aren't children
    var children = Streams.concat(definition.keywords.stream().map(k -> (Node) k), definition.values.stream());
    return new DumpLabel(defaultDescription(definition), children.toList());
  }

  @Override
  public DumpLabel visit(CallIndexExpr expr) {
    var children = new ArrayList<Node>();
    children.add((Node) expr.target);
    expr.subCalls.forEach(subCall -> children.add(new PseudoChild<>("Subcall", List.of(subCall))));
    expr.argsIndices.forEach(args -> children.add(new PseudoChild<>("ArgsIndices", args.values)));
    return new DumpLabel(defaultDescription(expr), children);
  }

  @Override
  public DumpLabel visit(ImportDefinition importDefinition) {

    var children = new ArrayList<Node>();
    children.add(new PseudoChild<>("File", List.of(
        importDefinition.fileId != null ? importDefinition.fileId : importDefinition.filePath)));

    importDefinition.importedSymbols.forEach(importPath -> children.add(new PseudoChild<>("Import", importPath)));
    if (!importDefinition.args.isEmpty()) {
      children.add(new PseudoChild<>("Args", importDefinition.args));
    }
    children.add(new PseudoChild<>("Module AST", importDefinition.moduleAst.definitions));

    return new DumpLabel(defaultDescription(importDefinition), children);
  }

  @Override
  public DumpLabel visit(LetExpr expr) {
    var children = new ArrayList<Node>();
    children.addAll(expr.identifiers());
    children.add(expr.valueExpr);
    children.add(expr.body);
    return new DumpLabel(defaultDescription(expr), children);
  }

  @Override
  public DumpLabel visit(LetStatement stmt) {
    var children = new ArrayList<Node>();
    children.addAll(stmt.identifiers());
    children.add(stmt.valueExpr);
    children.add(stmt.body);
    return new DumpLabel(defaultDescription(stmt), children);
  }

  @Override
  public DumpLabel visit(FloatTypeDefinition definition) {
    // FIXME: This is a bug and should be fixed
    var children = new ArrayList<Node>();
    children.addAll(definition.annotations);
    children.addAll(definition.children());
    return new DumpLabel(defaultDescription(definition), children);
  }
}
