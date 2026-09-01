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
import vadl.types.Type;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class RangeFormatField extends FormatField implements IdentifiableNode {
  public List<Expr> ranges;
  @Nullable
  public TypeLiteral typeLiteral;
  @Nullable
  public Type type;

  // While the ranges are expressions in diffrent forms, once computed they are stored here to
  // make them easier to process.
  // FIXME: @flofriday we should use a Constant.BitSlice instead of this list of BitRange.
  //   BitSlice is much more complete and also easier to handle
  @Nullable
  public List<FormatDefinition.BitRange> computedRanges;

  public RangeFormatField(IdentifierOrPlaceholder identifier, List<Expr> ranges,
                          @Nullable TypeLiteral typeLiteral) {
    super(identifier);
    this.ranges = ranges;
    this.typeLiteral = typeLiteral;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  public SourceLocation location() {
    return identifier.location().join(ranges.get(ranges.size() - 1).location());
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    identifier.prettyPrint(indent, builder);
    builder.append("\t [");
    ranges.get(0).prettyPrint(indent, builder);
    for (int i = 1; i < ranges.size(); i++) {
      builder.append(", ");
      ranges.get(i).prettyPrint(indent, builder);
    }
    builder.append("]");
    if (typeLiteral != null) {
      builder.append(" : ");
      typeLiteral.prettyPrint(0, builder);
    }
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    ranges.forEach(action);

    if (typeLiteral != null)
      action.accept(typeLiteral);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RangeFormatField that = (RangeFormatField) o;
    return Objects.equals(identifier, that.identifier)
        && Objects.equals(ranges, that.ranges);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(ranges);
    return result;
  }

  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
