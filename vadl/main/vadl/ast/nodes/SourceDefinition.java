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
public class SourceDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  String source;
  SourceLocation loc;

  SourceDefinition(Identifier id, String source, SourceLocation loc) {
    this.id = id;
    this.source = source;
    this.loc = loc;
  }

  @Override
  public Identifier identifier() {
    return id;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("source ");
    id.prettyPrint(0, builder);
    builder.append(" = ");
    builder.append("-<{").append(source).append("}>-\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SourceDefinition that = (SourceDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(source, that.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, source);
  }


}
