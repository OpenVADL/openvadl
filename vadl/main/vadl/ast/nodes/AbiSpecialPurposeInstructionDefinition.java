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

import java.util.Map;
import java.util.Objects;
import vadl.ast.Occurrence;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class AbiSpecialPurposeInstructionDefinition extends Definition {

  public Kind kind;
  @Child
  public IdentifierOrPlaceholder target;
  public SourceLocation loc;

  public AbiSpecialPurposeInstructionDefinition(Kind kind,
                                         IdentifierOrPlaceholder target,
                                         SourceLocation loc) {
    this.kind = kind;
    this.target = target;
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
    return BasicSyntaxType.INVALID;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("special ").append(kind.keyword).append(" instruction = ");
    target.prettyPrint(indent + 1, builder);
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
    AbiSpecialPurposeInstructionDefinition that = (AbiSpecialPurposeInstructionDefinition) o;
    return kind == that.kind && target.equals(that.target);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, target);
  }

  public enum Kind {
    RETURN("return"),
    CALL("call"),
    LOCAL_ADDRESS_LOAD("local address load"),
    GLOBAL_ADDRESS_LOAD("global address load"),
    ABSOLUTE_ADDRESS_LOAD("absolute address load");

    private final String keyword;

    Kind(String keyword) {
      this.keyword = keyword;
    }

    /**
     * Determines how often a definition is allowed in the ABI.
     */
    public static final Map<Kind, Occurrence> numberOfOccurrencesAbi;

    static {
      numberOfOccurrencesAbi = Map.of(Kind.RETURN, Occurrence.ONE,
          Kind.CALL, Occurrence.ONE,
          Kind.ABSOLUTE_ADDRESS_LOAD, Occurrence.ONE,
          Kind.LOCAL_ADDRESS_LOAD, Occurrence.OPTIONAL,
          Kind.GLOBAL_ADDRESS_LOAD, Occurrence.OPTIONAL);
    }
  }
}
