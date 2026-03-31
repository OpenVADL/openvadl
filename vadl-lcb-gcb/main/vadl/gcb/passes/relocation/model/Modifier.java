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

package vadl.gcb.passes.relocation.model;

import java.util.Map;
import vadl.template.Renderable;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.Relocation;

/**
 * Represents the transformation functions(?) in the assembler during fixups.
 *
 * @param value is the name of the modifier.
 * @param kind  is the kind of the {@link Relocation} it is then mapped to.
 */
public record Modifier(String value,
                       CompilerRelocation.Kind kind)
    implements Renderable {

  /**
   * Create a modifier for a VIAM relocation.
   */
  public static Modifier from(Relocation relocation) {
    var name = relocation.identifier.lower();
    var kind = relocation.isAbsolute() ? CompilerRelocation.Kind.ABSOLUTE
        : CompilerRelocation.Kind.RELATIVE;

    return new Modifier("MO_" + name, kind);
  }

  /**
   * Create an absolute modifier for an {@link AutomaticallyGeneratedRelocation}.
   */
  public static Modifier absolute(Instruction instruction, Format.FieldAccess fieldAccess) {
    return new Modifier(
        "MO_ABS_" + instruction.identifier.lower() + "_" + fieldAccess.identifier.simpleName(),
        CompilerRelocation.Kind.ABSOLUTE);
  }

  /**
   * Create a relative modifier for an {@link AutomaticallyGeneratedRelocation}.
   */
  public static Modifier relative(Instruction instruction, Format.FieldAccess fieldAccess) {
    return new Modifier(
        "MO_REL_" + instruction.identifier.lower() + "_" + fieldAccess.identifier.simpleName(),
        CompilerRelocation.Kind.RELATIVE);
  }

  @Override
  public Map<String, Object> renderObj() {
    return Map.of("value", value);
  }
}
