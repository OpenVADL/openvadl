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

import java.util.function.Consumer;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public final class ExpandedAliasDefSequenceCallExpr extends ExpandedSequenceCallExpr {
  ExpandedAliasDefSequenceCallExpr(Identifier target,
                                   SourceLocation loc) {
    super(target, loc);
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  void forEachChild(Consumer<Node> action) {
    // Remove this method when #293 is fixed.
    action.accept(target);
  }
}
