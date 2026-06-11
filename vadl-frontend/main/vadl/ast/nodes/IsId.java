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

import javax.annotation.Nullable;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public sealed interface IsId extends IsSymExpr
    permits AsIdExpr, Identifier, IdentifierOrPlaceholder, IdentifierPath, MacroInstanceExpr,
    MacroMatchExpr, PlaceholderExpr {
  @Override
  public default IsId path() {
    return this;
  }

  @Override
  public default @Nullable Expr size() {
    return null;
  }

  public String pathToString();

  /**
   * The target this id refers to. It is resolved during symbol resolving and
   * is only valid for {@link Identifier} and {@link IdentifierPath}, which
   * are the only two {@link IsId} subtypes that survive the {@link MacroExpander}.
   */
  @Nullable
  public Node target();
}
