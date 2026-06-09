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

import java.util.Objects;
import java.util.stream.Collectors;
import vadl.utils.SourceLocation;

/**
 * An internal temporary placeholder of record type.
 * This node should never leave the parser.
 */
@SuppressWarnings("MissingJavadocMethod")
public final class RecordTypeDefinition extends Definition implements IdentifiableNode {
  public Identifier name;
  public RecordType recordType;
  public SourceLocation loc;

  public RecordTypeDefinition(Identifier name, RecordType recordType, SourceLocation loc) {
    this.name = name;
    this.recordType = recordType;
    this.loc = loc;
  }

  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.COMMON_DEFS;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("record ");
    builder.append(recordType.print());
    builder.append(recordType.entries.stream()
        .map(entry -> entry.name() + " : " + entry.type().print())
        .collect(Collectors.joining(",", "(", ")")));
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecordTypeDefinition that = (RecordTypeDefinition) o;
    return Objects.equals(name, that.name)
        && Objects.equals(recordType, that.recordType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, recordType);
  }

  @Override
  public Identifier identifier() {
    return name;
  }
}
