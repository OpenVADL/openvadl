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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

  private OperationType(Map<String, Type> types) {
    super(types);
  }

  /**
   * Creates an `OperationType` representing all the given instructions.
   * Duplicate instructions are only represented once.
   *
   * @param fields The fields of all the instructions which the resulting type
   *               represents. Each map describes the fields of one
   *               instruction.
   * @return The resulting {@code OperationType}.
   */
  public static OperationType of(Collection<Map<String, Type>> fields) {

    // Early-out since we rely on at least one instruction being present to
    // initialize the map of common fields.
    if (fields.isEmpty()) {
      return new OperationType(
          Collections.emptyMap()
      );
    }

    var commonFieldTypes = fields.iterator().next();

    for (var format : fields) {
      commonFieldTypes = fieldUnion(commonFieldTypes, format);
    }

    return new OperationType(commonFieldTypes);
  }

  /**
   * Creates an {@code OperationType} that represents the union of the instructions
   * contained in the given operation types.
   *
   * @param operations The {@code OperationType}s whose union is formed.
   * @return The resulting {@code OperationType}.
   */
  public static OperationType union(Collection<Operation> operations) {

    final var fields = new ArrayList<Map<String, Type>>();

    for (var operation : operations) {
      for (var instruction : operation.getInstructions()) {
        fields.add(instructionFieldTypes(instruction));
      }
    }

    return of(fields);
  }



  private static Map<String, Type> fieldUnion(Map<String, Type> a, Map<String, Type> b) {

    final var result = new HashMap<>(a);

    result.entrySet().removeIf(field -> {
      final var match = b.get(field.getKey());

      return match == null || !match.equals(field.getValue());
    });

    return result;
  }

  private static Map<String, Type> instructionFieldTypes(Instruction instruction) {

    final var fieldTypes = new HashMap<String, Type>();

    for (var field : instruction.format().fields()) {
      fieldTypes.put(field.simpleName(), field.type());
    }

    return fieldTypes;
  }

  @Override
  public String name() {
    return toString();
  }

  @Override
  public String toString() {
    final var sb = new StringBuilder("Operation<");

    appendMemberNames(sb);

    return sb.append(">").toString();
  }
}
