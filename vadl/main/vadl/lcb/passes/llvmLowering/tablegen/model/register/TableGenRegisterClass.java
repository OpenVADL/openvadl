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

package vadl.lcb.passes.llvmLowering.tablegen.model.register;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import vadl.gcb.valuetypes.TargetName;
import vadl.gcb.valuetypes.ValueType;
import vadl.template.Renderable;
import vadl.viam.RegisterResource;

/**
 * Represents a single register file in TableGen. This is the lowered representation of a
 * register file.
 */
public class TableGenRegisterClass implements
    Renderable {
  private final TargetName namespace;
  private final String name;
  private final int alignment;
  private final List<ValueType> regTypes;
  private final List<TableGenRegister> registers;
  private final RegisterResource registerFileRef;

  /**
   * Constructor.
   */
  public TableGenRegisterClass(TargetName namespace,
                               String name,
                               int alignment,
                               List<ValueType> regTypes,
                               List<TableGenRegister> registers,
                               RegisterResource registerFileRef) {
    this.namespace = namespace;
    this.name = name;
    this.alignment = alignment;
    this.regTypes = regTypes;
    this.registers = registers;
    this.registerFileRef = registerFileRef;
  }

  public String regTypesString() {
    return regTypes.stream().map(ValueType::getLlvmType).collect(Collectors.joining(", "));
  }

  public List<ValueType> regTypes() {
    return regTypes;
  }

  @Override
  public Map<String, Object> renderObj() {
    return Map.of(
        "name", name(),
        "regTypes", regTypes(),
        "registerFileRef", Map.of(
            "name", registerFileRef().identifier().simpleName()
        )
    );
  }

  public TargetName namespace() {
    return namespace;
  }

  public String name() {
    return name;
  }

  public int alignment() {
    return alignment;
  }

  public List<TableGenRegister> registers() {
    return registers;
  }

  public RegisterResource registerFileRef() {
    return registerFileRef;
  }
}
