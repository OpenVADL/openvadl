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

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class LogicDefinition extends Definition implements IdentifiableNode {
  public IdentifierOrPlaceholder id;
  public SourceLocation loc;
  public List<IdentifierOrPlaceholder> logicTypeIdentifiers;

  /// Set by the typechecker and inferred from the logicType identifiers.
  /// This cannot be directly set by the parser because of possible macro expansion.
  @Nullable
  public LogicType logicType;

  public LogicDefinition(IdentifierOrPlaceholder id,
                         List<IdentifierOrPlaceholder> logicTypeIdentifiers,
                         SourceLocation loc) {
    this.id = id;
    this.logicTypeIdentifiers = logicTypeIdentifiers;
    this.loc = loc;
  }

  public enum LogicType {
    Forwarding,
    BranchPrediction,
    Control;

    @Override
    public String toString() {
      return switch (this) {
        case Forwarding -> "forwarding";
        case BranchPrediction -> "branch prediction";
        case Control -> "control";
      };
    }
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
    builder.append("logic [ ");
    for (var logicTypeId : logicTypeIdentifiers) {
      var name = ((Identifier) logicTypeId).name;
      builder.append(name).append(" ");
    }
    builder.append("] ");
    id.prettyPrint(0, builder);
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
    LogicDefinition that = (LogicDefinition) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public Identifier identifier() {
    return (Identifier) id;
  }
}
