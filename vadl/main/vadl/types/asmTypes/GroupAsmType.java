// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
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

package vadl.types.asmTypes;

import java.util.Map;
import java.util.Objects;

/**
 * GroupAsmType is a special AsmType that contains a list of subtypes.
 * It represents the type of a sequence of grammar elements.
 */
public class GroupAsmType implements AsmType {

  Map<String, AsmType> subtypeMap;

  public GroupAsmType(Map<String, AsmType> subtypeMap) {
    this.subtypeMap = subtypeMap;
  }

  public Map<String, AsmType> getSubtypeMap() {
    return subtypeMap;
  }

  @Override
  public String name() {
    return "GroupAsmType";
  }

  @Override
  public String toCppTypeString(String prefix) {
    if (subtypeMap.isEmpty()) {
      return "NoData";
    }
    return subtypeMap.keySet().stream()
        .reduce("struct_", (acc, type) -> acc + type);
  }

  @Override
  public boolean canBeCastTo(AsmType to) {

    var subtypes = subtypeMap.values().stream().toList();

    if (subtypes.size() == 1 && subtypes.get(0) == to) {
      return true;
    }

    if (to == InstructionAsmType.instance()) {
      if (subtypes.isEmpty()) {
        return false;
      }
      return subtypes.stream().allMatch(subtype -> subtype == OperandAsmType.instance());
    }

    if (to == OperandAsmType.instance()) {
      return subtypes.size() == 2 && subtypes.get(0) == ModifierAsmType.instance()
          && subtypes.get(1) == ExpressionAsmType.instance();
    }

    if (to == StatementsAsmType.instance()) {
      if (subtypes.isEmpty()) {
        return false;
      }
      return subtypes.stream().allMatch(subtype -> subtype == InstructionAsmType.instance());
    }

    if (to == OperandsAsmType.instance()) {
      if (subtypes.isEmpty()) {
        return false;
      }
      return subtypes.stream().allMatch(subtype -> subtype == OperandAsmType.instance());
    }

    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    GroupAsmType that = (GroupAsmType) o;
    boolean equal = true;
    if (subtypeMap.size() != that.subtypeMap.size()) {
      return false;
    }
    for (int i = 0; i < subtypeMap.size(); i++) {
      equal &= subtypeMap.values().toArray()[i].equals(that.subtypeMap.values().toArray()[i]);
    }
    return equal;
  }

  @Override
  public int hashCode() {
    return Objects.hash(subtypeMap.values());
  }

  @Override
  public String toString() {
    var nameWithSubtypes = new StringBuilder("@").append(name()).append("(");

    var subtypes = subtypeMap.values().toArray();
    for (int i = 0; i < subtypes.length; i++) {
      nameWithSubtypes.append(subtypes[i]);
      if (i != subtypes.length - 1) {
        nameWithSubtypes.append(", ");
      }
    }
    nameWithSubtypes.append(")");

    return nameWithSubtypes.toString();
  }
}
