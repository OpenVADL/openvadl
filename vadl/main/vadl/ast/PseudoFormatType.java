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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import vadl.types.DataType;
import vadl.types.StructType;
import vadl.types.Type;
import vadl.utils.Pair;

/**
 * Instruction type as it occurs in quantified logical expressions of group annotations.
 * E.g.:
 * <pre>
 *   [stop: exists i in {O2} then true ]
 *   [assert: forall i in {O1, O2} then i.par = 0]
 *   group VLIW = O1.O2
 * </pre>
 */
public class PseudoFormatType extends StructType {

  private final LinkedHashSet<OperationDefinition> operations;
  private final LinkedHashSet<FormatDefinition> formats;

  /**
   * Construct a pseudo format type.
   *
   * @param operations the operations that are used to construct the format.
   * @param formats    the formats that are combined.
   * @param types      the types of the fields.
   */
  public PseudoFormatType(Collection<OperationDefinition> operations,
                          Collection<FormatDefinition> formats,
                          Map<String, Type> types) {
    super(types);
    this.operations = new LinkedHashSet<>(operations);
    this.formats = new LinkedHashSet<>(formats);
  }

  /**
   * Construct a pseudo format type from a set of operations.
   *
   * @param operations the operations that are used to construct the format.
   * @return a pseudo format type.
   */
  public static PseudoFormatType of(Collection<OperationDefinition> operations) {

    if (operations.isEmpty()) {
      return new PseudoFormatType(List.of(), List.of(), Map.of());
    }

    final var formats = new ArrayList<FormatDefinition>();
    final var types = new LinkedHashMap<String, Type>();

    boolean first = true;
    for (OperationDefinition op : operations) {
      for (InstructionDefinition insn : op.instructions) {

        final var format = requireNonNull(insn.formatNode);
        formats.add(format);

        final var fields = toPseudoFields(format);

        if (first) {
          types.putAll(fields);
          first = false;
          continue;
        }

        types.entrySet().removeIf(e ->
            !fields.containsKey(e.getKey())
                || !Objects.equals(fields.get(e.getKey()), e.getValue()));
      }
    }

    return new PseudoFormatType(operations, formats, types);
  }

  private static Map<String, Type> toPseudoFields(FormatDefinition format) {
    return format.fieldsWithoutEncodingPredicate().map(f -> {
      final DataType fieldType = switch (f) {
        case DerivedFormatField field -> field.expr.type().asDataType();
        case RangeFormatField field -> requireNonNull(field.type).asDataType();
        case TypedFormatField field -> field.typeLiteral.type().asDataType();
        default -> throw new IllegalStateException();
      };
      return Pair.of(f.identifier().name, fieldType);
    }).collect(Collectors.toMap(Pair::left, Pair::right));
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

  /**
   * Name of this pseudo format.
   *
   * @return the name
   */
  @Override
  public String name() {
    final List<FormatDefinition> formats = new ArrayList<>(this.formats);
    final var sb = new StringBuilder("(");

    int i = 0;
    while (i < formats.size()) {
      if (i > 0) {
        sb.append(" ∩ ");
      }

      if (i >= 2 && formats.size() > 3 && i < formats.size() - 1) {
        sb.append("...");

        // Skip to the last format to avoid long names
        i = formats.size() - 1;
        continue;
      }

      sb.append(formats.get(i++).identifier().name);
    }

    return sb.append(")").toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (PseudoFormatType) obj;
    return Objects.equals(this.fields(), that.fields());
  }

  @Override
  public int hashCode() {
    return Objects.hash(fields());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, Type> field : fields().entrySet()) {
      if (!first) {
        sb.append(", ");
      }
      first = false;
      sb.append(field.getKey());
      sb.append(": ");
      sb.append(field.getValue());
    }
    return sb.toString();
  }
}

