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
public interface ExprVisitor<R> {
  public R visit(Identifier expr);

  public R visit(BinaryExpr expr);

  public R visit(GroupedExpr expr);

  public R visit(IntegerLiteral expr);

  public R visit(WildcardLiteral expr);

  public R visit(BinaryLiteral expr);

  public R visit(BoolLiteral expr);

  public R visit(StringLiteral expr);

  public R visit(PlaceholderExpr expr);

  public R visit(MacroInstanceExpr expr);

  public R visit(RangeExpr expr);

  public R visit(TypeLiteral expr);

  public R visit(IdentifierPath expr);

  public R visit(UnaryExpr expr);

  public R visit(CallIndexExpr expr);

  public R visit(IfExpr expr);

  public R visit(LetExpr expr);

  public R visit(CastExpr expr);

  public R visit(SymbolExpr expr);

  public R visit(MacroMatchExpr expr);

  public R visit(MatchExpr expr);

  public R visit(AsIdExpr expr);

  public R visit(AsStrExpr expr);

  public R visit(ExistsInExpr expr);

  public R visit(ExistsInThenExpr expr);

  public R visit(ForallThenExpr expr);

  public R visit(ForallExpr expr);

  public R visit(SequenceCallExpr expr);

  public R visit(ExpandedSequenceCallExpr expr);

  public R visit(ExpandedAliasDefSequenceCallExpr expr);

  public R visit(ResourceReferenceExression expr);
}
