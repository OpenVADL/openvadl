// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;

/**
 * A pseudo format constructed by intersecting multiple formats.
 */
public class PseudoFormat extends Definition implements TypedNode {

  private final SourceLocation location;
  private final LinkedHashSet<OperationDefinition> operations = new LinkedHashSet<>();
  private final LinkedHashSet<FormatDefinition> formats = new LinkedHashSet<>();

  @Nullable
  private LinkedHashSet<FormatField> fields = null;

  public PseudoFormat(SourceLocation loc) {
    this.location = loc;
  }

  public PseudoFormat(WithLocation node) {
    this(node.location());
  }

  public PseudoFormat(SourceLocation loc, Collection<OperationDefinition> operations) {
    this.location = loc;
    operations.forEach(this::add);
  }

  /**
   * Add the operation to the pseudo format, retaining only the intersection of the pseudo format's
   * current fields and the fields of the operations' instruction's formats.
   *
   * @param op the operation to append.
   */
  void add(OperationDefinition op) {
    operations.add(op);
    for (InstructionDefinition insn : op.instructions) {
      formats.add(requireNonNull(insn.formatNode));
      final var formatFields = toPseudoFields(insn.formatNode);
      if (fields == null) {
        fields = new LinkedHashSet<>(formatFields);
      } else {
        fields.retainAll(formatFields);
      }
    }
  }

  private List<FormatField> toPseudoFields(FormatDefinition format) {
    return format.fieldsWithoutEncodingPredicate().map(f -> {
      final DataType fieldType = switch (f) {
        case DerivedFormatField field -> field.expr.type().asDataType();
        case RangeFormatField field -> requireNonNull(field.type).asDataType();
        case TypedFormatField field -> field.typeLiteral.type().asDataType();
        default -> throw new IllegalStateException();
      };
      return new FormatField(f.identifier().name, fieldType);
    }).toList();
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.COMMON_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("pseudo format ");

    builder.append(formats.stream()
        .map(f -> f.identifier().name)
        .collect(Collectors.joining(" ∩ ", "(", ")")));

    if (fields == null || fields.isEmpty()) {
      builder.append("\n");
      return;
    }
    builder.append(" = { \n");
    indent++;

    final List<FormatField> fs = new ArrayList<>(fields);

    builder.append(prettyIndentString(indent));
    builder.append(fs.getFirst().name());
    builder.append(": ");
    builder.append(fs.getFirst().type());

    for (int i = 1; i < fs.size(); i++) {
      builder.append(", ");
      builder.append("\n");
      builder.append(prettyIndentString(indent));
      builder.append(fs.get(i).name());
      builder.append(": ");
      builder.append(fs.get(i).type());
    }

    builder.append("\n");

    indent--;
    builder.append(prettyIndentString(indent));
    builder.append("}");
  }

  /**
   * Name of this pseudo format.
   *
   * @return the name
   */
  public String name() {
    return formats.stream()
        .map(f -> f.identifier().name)
        .limit(3)
        .collect(Collectors.joining(" ∩ ", "(", formats.size() > 3 ? " ... )" : ")"));
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  public Set<OperationDefinition> operations() {
    return Collections.unmodifiableSet(new LinkedHashSet<>(operations));
  }

  public Set<InstructionDefinition> instructions() {
    return operations.stream().flatMap(op -> op.instructions.stream())
        .collect(Collectors.toUnmodifiableSet());
  }

  public Set<FormatDefinition> formats() {
    return Collections.unmodifiableSet(new LinkedHashSet<>(formats));
  }

  public Set<FormatField> fields() {
    return fields == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(fields));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (PseudoFormat) obj;
    return Objects.equals(this.fields, that.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fields);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    prettyPrint(0, sb);
    return sb.toString();
  }

  @Override
  public Type type() {
    return new PseudoFormatType(this);
  }

  /**
   * Get the common data type of the field shared by all formats.
   *
   * @param fieldName the field name.
   * @return the data type.
   */
  @Nullable
  public DataType getFieldType(String fieldName) {
    return fields().stream()
        .filter(f -> Objects.equals(f.name(), fieldName))
        .map(FormatField::type)
        .findAny().orElse(null);
  }

  /**
   * Field of the pseudo format.
   *
   * @param name name.
   * @param type type.
   */
  public record FormatField(String name, DataType type) {
  }
}
