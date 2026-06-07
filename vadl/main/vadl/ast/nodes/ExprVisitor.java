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

@SuppressWarnings("MissingJavadocType")
public interface ExprVisitor<R> {
  R visit(Identifier expr);

  R visit(BinaryExpr expr);

  R visit(GroupedExpr expr);

  R visit(IntegerLiteral expr);

  R visit(WildcardLiteral expr);

  R visit(BinaryLiteral expr);

  R visit(BoolLiteral expr);

  R visit(StringLiteral expr);

  R visit(PlaceholderExpr expr);

  R visit(MacroInstanceExpr expr);

  R visit(RangeExpr expr);

  R visit(TypeLiteral expr);

  R visit(IdentifierPath expr);

  R visit(UnaryExpr expr);

  R visit(CallIndexExpr expr);

  R visit(IfExpr expr);

  R visit(LetExpr expr);

  R visit(CastExpr expr);

  R visit(SymbolExpr expr);

  R visit(MacroMatchExpr expr);

  R visit(MatchExpr expr);

  R visit(AsIdExpr expr);

  R visit(AsStrExpr expr);

  R visit(ExistsInExpr expr);

  R visit(ExistsInThenExpr expr);

  R visit(ForallThenExpr expr);

  R visit(ForallExpr expr);

  R visit(SequenceCallExpr expr);

  R visit(ExpandedSequenceCallExpr expr);

  R visit(ExpandedAliasDefSequenceCallExpr expr);

  R visit(ResourceReferenceExression expr);
}
