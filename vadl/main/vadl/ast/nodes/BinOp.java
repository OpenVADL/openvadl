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

import java.util.Objects;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public final class BinOp extends Node implements IsBinOp {

  Operator operator;
  SourceLocation location;

  BinOp(Operator operator, SourceLocation location) {
    this.operator = operator;
    this.location = location;
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.BIN_OP;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(operator.symbol);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " " + operator.symbol;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BinOp that = (BinOp) o;
    return Objects.equals(operator, that.operator);
  }

  @Override
  public int hashCode() {
    return operator.hashCode();
  }
}
