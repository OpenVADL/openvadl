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

package vadl.gcb.valuetypes;

import java.util.Map;
import vadl.gcb.passes.relocation.model.UserSpecifiedRelocation;
import vadl.template.Renderable;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.Relocation;

/**
 * A {@link VariantKind} specifies how a symbol get referenced. It can be over a
 * {@link UserSpecifiedRelocation} or GOT or more.
 */
public record VariantKind(String value, String human, boolean isImmediate) implements Renderable {
  public VariantKind(Relocation relocation) {
    this("VK_" + relocation.identifier.lower(), relocation.identifier.simpleName(), false);
  }

  public static VariantKind none() {
    return new VariantKind("VK_None", "None", false);
  }

  public static VariantKind plt() {
    return new VariantKind("VK_PLT", "plt", false);
  }

  public static VariantKind invalid() {
    return new VariantKind("VK_Invalid", "Invalid", false);
  }

  /**
   * Create a variant kind from a user defined relocation.
   * Gets its kind from the relocation kind.
   */
  public static VariantKind forUserDefinedRelocation(Relocation relocation) {
    return new VariantKind("VK_" + relocation.kind().toString() + "_"
        + relocation.identifier.lower(), relocation.identifier.simpleName(), false);
  }

  /**
   * Create an absolute variant kind.
   */
  public static VariantKind absolute(Relocation relocation, Format.Field field) {
    var name = relocation.identifier.lower() + "_" + field.identifier.tail().lower();

    return new VariantKind("VK_ABS_" + name,
        "ABS_" + name, false);
  }

  /**
   * Create an absolute variant kind.
   */
  public static VariantKind absolute(Format.Field field) {
    return new VariantKind("VK_SYMB_ABS_" + field.identifier.lower(),
        "SYMB_ABS_" + field.identifier.lower(), true);
  }

  /**
   * Create a relative variant kind.
   */
  public static VariantKind relative(Relocation relocation, Format.Field field) {
    var name = relocation.identifier.lower() + "_" + field.identifier.tail().lower();

    return new VariantKind("VK_PCREL_" + name,
        "PCREL_" + name, false);
  }

  /**
   * Create a relative variant kind.
   */
  public static VariantKind relative(Format.Field field) {
    return new VariantKind("VK_SYMB_PCREL_" + field.identifier.lower(),
        "SYMB_PCREL_" + field.identifier.lower(), true);
  }


  /**
   * Create a variant kind for a field access to be used as mapping for the decoding function.
   */
  public static VariantKind decode(Instruction instruction, Format.FieldAccess fieldAccess) {
    return new VariantKind(
        "VK_DECODE_" + fieldAccess.identifier.last().prepend(instruction.identifier()).lower(),
        "DECODE_" + fieldAccess.identifier.last().prepend(instruction.identifier()).lower(), true);
  }

  @Override
  public Map<String, Object> renderObj() {
    return Map.of(
        "value", value,
        "human", human,
        "isImmediate", isImmediate
    );
  }
}
