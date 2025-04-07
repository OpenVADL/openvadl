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
import java.util.Objects;
import vadl.gcb.valuetypes.VariantKind;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Relocation;

/**
 * A logical relocation is helper construct for the {@link vadl.viam.Relocation}.
 * In contrast to {@link AutomaticallyGeneratedRelocation} are {@link UserSpecifiedRelocation}
 * always user generated like {@code %lo} or {@code %hi} in risc-v.
 */
public class UserSpecifiedRelocation extends CompilerRelocation {
  private final VariantKind variantKind;

  /**
   * Constructor.
   */
  public UserSpecifiedRelocation(
      Format format,
      Format.Field field,
      Relocation originalRelocation) {
    super(generateName(format,
            field,
            CompilerRelocation.Kind.fromRelocationKind(originalRelocation.kind())),
        format, field, originalRelocation);
    this.variantKind = new VariantKind(originalRelocation);
  }

  private static Identifier generateName(Format format, Format.Field imm, Kind kind) {
    return format.identifier.append(kind.name(), imm.identifier.simpleName());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserSpecifiedRelocation that = (UserSpecifiedRelocation) o;
    return kind == that.kind && Objects.equals(format, that.format);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, format);
  }

  @Override
  public Map<String, Object> renderObj() {
    var obj = super.renderObj();
    obj.put("variantKind", variantKind);
    obj.put("name", identifier.simpleName());
    return obj;
  }
}
