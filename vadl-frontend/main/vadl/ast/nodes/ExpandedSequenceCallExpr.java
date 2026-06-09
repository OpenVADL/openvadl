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

import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

/**
 * It is allowed to define shortcuts in {@link SequenceCallExpr}. These shortcuts will be
 * expanded into {@link ExpandedSequenceCallExpr} and {@link ExpandedAliasDefSequenceCallExpr}.
 * So {@code a{0..10}} will become {@code a0, a1, ...}. Each entry is then a
 * {@link ExpandedAliasDefSequenceCallExpr}. However, single entries need to be also represented.
 * If it is a simple entry like {@code a0} then this will be also to
 * {@link ExpandedAliasDefSequenceCallExpr} mapped. {@code X(1)} is a {@link CallIndexExpr} and will
 * be mapped to {@link ExpandedSequenceCallExpr}.
 */
@SuppressWarnings("MissingJavadocMethod")
public sealed class ExpandedSequenceCallExpr extends Expr permits ExpandedAliasDefSequenceCallExpr {
  @Child
  public Expr target;
  public SourceLocation loc;

  public ExpandedSequenceCallExpr(Expr target, SourceLocation loc) {
    this.target = target;
    this.loc = loc;
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    target.prettyPrint(0, builder);
  }
}
