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

package vadl.types;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.Operation;

/**
 * Type of instances of operation sets. E.g. the type of the index of a for-all/exists-expression
 * as they occur in group annotations.
 * E.g.:
 * <pre>
 *   [stop: exists i in {O2} then true ]
 *   [assert: forall i in {O1, O2} then i.par = 0]
 *   [assert: VLIW(0) in ALU_FP]
 *   group VLIW = O1.O2
 * </pre>
 */
public class OperationType extends StructType {

  private final Set<Operation> operations;
  private final Set<Instruction> instructions;
  private final Set<Format> formats;

  /**
   * Constructor of the operation type.
   *
   * @param operations   The operations
   * @param instructions The instructions
   * @param formats      The formats
   * @param types        The field types
   */
  protected OperationType(
      Collection<Operation> operations,
      Collection<Instruction> instructions,
      Collection<Format> formats,
      Map<String, Type> types
  ) {
    super(types);
    this.operations = new LinkedHashSet<>(operations);
    this.instructions = new LinkedHashSet<>(instructions);
    this.formats = new LinkedHashSet<>(formats);
  }

  /**
   * Construct operation types from a set of operations.
   *
   * @param operations The operations involved in the type.
   * @return The {@link OperationType}
   */
  public static OperationType of(Collection<Operation> operations) {

    if (operations.isEmpty()) {
      return new OperationType(Set.of(), Set.of(), Set.of(), Map.of());
    }

    final var formats = new ArrayList<Format>();
    final var insns = new ArrayList<Instruction>();

    final var types = new LinkedHashMap<String, Type>();

    boolean first = true;
    for (Operation op : operations) {
      for (Instruction insn : op.getInstructions()) {

        insns.add(insn);

        final var format = requireNonNull(insn.format());
        formats.add(format);

        final var fields = resolveFields(format);

        if (first) {
          types.putAll(fields);
          first = false;
          continue;
        }

        // Retain only fields common to all formats
        types.entrySet().removeIf(e ->
            !fields.containsKey(e.getKey())
                || !Objects.equals(fields.get(e.getKey()), e.getValue()));
      }
    }

    return new OperationType(operations, insns, formats, types);
  }

  /**
   * The set of operations combined in this type.
   *
   * @return the operations.
   */
  public Set<Operation> operations() {
    return operations;
  }

  /**
   * The set of instructions combined in this type.
   *
   * @return the instructions.
   */
  public Set<Instruction> instructions() {
    return instructions;
  }

  /**
   * The set of instruction formats combined in this type.
   *
   * @return the formats.
   */
  public Set<Format> formats() {
    return formats;
  }

  private static Map<String, Type> resolveFields(Format format) {
    final Map<String, Type> map = new HashMap<>();
    for (Format.Field f : format.fields()) {
      map.put(f.simpleName(), f.type());
    }
    return map;
  }

}
