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
 * Instruction type as it occurs in quantified logical expressions of group annotations.
 * E.g.:
 * <pre>
 *   [stop: exists i in {O2} then true ]
 *   [assert: forall i in {O1, O2} then i.par = 0]
 *   group VLIW = O1.O2
 * </pre>
 */
class PseudoFormatType extends Type {

  /**
   * The pseudo format representing the intersection format of all instructions in the operation
   * set. Initialized during type checking.
   */
  private final PseudoFormat pseudoFormat;

  public PseudoFormatType(PseudoFormat pseudoFormat) {
    this.pseudoFormat = pseudoFormat;
  }

  public PseudoFormat format() {
    return pseudoFormat;
  }

  @Override
  public String name() {
    return "Instruction : " + pseudoFormat.name();
  }


  @Override
  public boolean isTrivialCastTo(Type other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PseudoFormatType iType)) {
      return false;
    }
    return Objects.equals(pseudoFormat, iType.pseudoFormat);
  }
}

/**
 * A pseudo format constructed by intersecting multiple formats.
 */
class PseudoFormat extends Definition implements TypedNode {

  private final SourceLocation location;
  private final LinkedHashSet<InstructionDefinition> instructions = new LinkedHashSet<>();
  private final LinkedHashSet<FormatDefinition> formats = new LinkedHashSet<>();

  @Nullable
  private LinkedHashSet<FormatField> fields = null;

  public PseudoFormat(SourceLocation loc) {
    this.location = loc;
  }

  public PseudoFormat(WithLocation node) {
    this(node.location());
  }

  public PseudoFormat(SourceLocation loc, Collection<InstructionDefinition> insns) {
    this.location = loc;
    insns.forEach(this::add);
  }

  /**
   * Add the given instruction to the pseudo format, retaining only the intersection
   * of the pseudo format's current fields and the fields of the given instruction's format.
   *
   * @param insn the insn to append.
   */
  void add(InstructionDefinition insn) {
    instructions.add(insn);
    formats.add(requireNonNull(insn.formatNode));
    final var formatFields = toPseudoFields(insn.formatNode);
    if (fields == null) {
      fields = new LinkedHashSet<>(formatFields);
    } else {
      fields.retainAll(formatFields);
    }
  }

  private List<PseudoFormat.FormatField> toPseudoFields(FormatDefinition format) {
    final List<PseudoFormat.FormatField> fields = new ArrayList<>();
    for (vadl.ast.FormatField f : format.fieldsWithoutEncodingPredicate()) {
      final DataType fieldType = switch (f) {
        case DerivedFormatField field -> field.expr.type().asDataType();
        case RangeFormatField field -> requireNonNull(field.type).asDataType();
        case TypedFormatField field -> field.typeLiteral.type().asDataType();
        case EncodingFormatField _, PredicateFormatField _ -> throw new IllegalStateException();
      };
      fields.add(new PseudoFormat.FormatField(f.identifier().name, fieldType));
    }
    return fields;
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

  public String name() {
    return formats.stream()
        .map(f -> f.identifier().name)
        .collect(Collectors.joining(" ∩ ", "(", ")"));
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  public Set<InstructionDefinition> instructions() {
    return Set.of(instructions.toArray(new InstructionDefinition[0]));
  }

  public Set<FormatDefinition> formats() {
    return Set.of(formats.toArray(new FormatDefinition[0]));
  }

  public Set<FormatField> fields() {
    return fields == null ? Set.of() : Set.of(fields.toArray(new FormatField[0]));
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

  @Nullable
  public DataType getFieldType(String fieldName) {
    return fields().stream()
        .filter(f -> Objects.equals(f.name(), fieldName))
        .map(FormatField::type)
        .findAny().orElse(null);
  }

  public record FormatField(String name, DataType type) {
  }
}
