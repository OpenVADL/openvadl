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
import java.util.function.Consumer;
import javax.annotation.Nullable;
import vadl.ast.GroupDefUtils;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class GroupDefinition extends Definition implements IdentifiableNode {
  public IdentifierOrPlaceholder name;
  @Nullable
  public TypeLiteral type;
  public Group.Sequence groupSequence;
  public SourceLocation loc;

  public GroupDefinition(IdentifierOrPlaceholder name, @Nullable TypeLiteral type,
                         Group.Sequence groupSequence, SourceLocation loc) {
    this.name = name;
    this.type = type;
    this.groupSequence = groupSequence;
    this.loc = loc;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) name;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("group ");
    name.prettyPrint(indent, builder);
    if (type != null) {
      builder.append(" : ");
      type.prettyPrint(indent, builder);
    }
    builder.append(" = ");
    groupSequence.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    if (type != null) {
      action.accept(type);
    }

    action.accept(groupSequence);
  }

  public Group expr() {
    return groupSequence;
  }

  public List<OperationDefinition> operations() {
    return GroupDefUtils.OperationCollector.operations(groupSequence);
  }
  
  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GroupDefinition that = (GroupDefinition) o;
    return Objects.equals(name, that.name) && Objects.equals(type, that.type)
        && Objects.equals(groupSequence, that.groupSequence);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, groupSequence);
  }
}
