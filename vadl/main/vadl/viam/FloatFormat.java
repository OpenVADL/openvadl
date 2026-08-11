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

package vadl.viam;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import vadl.types.Type;

/**
 * Describes a float format. This covers bit-size, encoding and interpretation.
 */
public class FloatFormat extends Definition implements DefProp.WithType {

  /**
   * Represents all supported float encodings.
   */
  public enum Encoding {
    IEEE32(32, true),
    IEEE64(64, true);

    public final int size;
    public final boolean ieee;

    Encoding(int size, boolean ieee) {
      this.size = size;
      this.ieee = ieee;
    }

    /**
     * Returns the IEEE encoding for the given bit-size.
     */
    public static @Nullable Encoding ieee(int size) {
      return switch (size) {
        case 32 -> IEEE32;
        case 64 -> IEEE64;
        default -> null;
      };
    }
  }

  @Nullable
  private Encoding encoding = null;

  public FloatFormat(Identifier identifier) {
    super(identifier);
  }

  public void setEncoding(@CheckForNull Encoding encoding) {
    this.encoding = encoding;
  }

  /**
   * The encoding of the float format.
   */
  @Nullable
  public Encoding encoding() {
    return encoding;
  }

  /**
   * The name of the float format in lower case.
   */
  public String nameLower() {
    return simpleName().toLowerCase();
  }

  @Override
  public Type type() {
    return Type.floatType();
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public void verify() {
    super.verify();
    ensure(encoding != null, "Encoding missing");
  }

  @Override
  public String toString() {
    return identifier.simpleName();
  }
}
