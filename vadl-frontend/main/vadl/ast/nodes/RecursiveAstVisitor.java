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

package vadl.ast.nodes;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class RecursiveAstVisitor extends DefaultAstVisitor<Void> {
  protected void beforeTravel(Expr expr) {
  }

  protected void beforeTravel(Statement statement) {
  }

  protected void beforeTravel(Definition definition) {
  }

  protected void afterTravel(Statement statement) {
  }

  protected void afterTravel(Definition definition) {
  }

  protected void afterTravel(Expr expr) {
  }


  protected final void travel(Node node) {
    if (node instanceof Expr expr) {
      expr.accept(this);
      return;
    }
    if (node instanceof Statement stmt) {
      stmt.accept(this);
      return;
    }
    if (node instanceof Definition def) {
      def.accept(this);
      return;
    }

    // If it's just a intermediate node, just visit it's children
    node.forEachChild(this::travel);
  }

  @Override
  public final Void visitNode(Node node) {
    throw new IllegalStateException("This should never be called because all three visit methods "
        + "are implemented.");
  }

  @Override
  public final Void visitDefinition(Definition definition) {
    beforeTravel(definition);
    definition.forEachChild(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public final Void visitStatement(Statement statement) {
    beforeTravel(statement);
    statement.forEachChild(this::travel);
    afterTravel(statement);
    return null;
  }

  @Override
  public final Void visitExpression(Expr expr) {
    beforeTravel(expr);
    expr.forEachChild(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(FloatTypeDefinition definition) {
    beforeTravel(definition);
    // FIXME: FloatTypeDefinition has no fields annotated with @Child, therefore the
    //        ChildNodeRegistry does not account for it. The children of the super type are
    //        not checked (this seems wrong). So we manually visit the annotations.
    definition.annotations.forEach(this::travel);
    //definition.forEachChild(this::travel);
    afterTravel(definition);
    return null;
  }

}
