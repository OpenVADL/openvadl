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

import static java.util.Objects.requireNonNull;

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

  @Nullable
  private Constant canonicalSNaN = null;
  @Nullable
  private Constant canonicalQNaN = null;

  public FloatFormat(Identifier identifier) {
    super(identifier);
  }

  public void setEncoding(@CheckForNull Encoding encoding) {
    this.encoding = encoding;
  }

  public void setCanonicalSNaN(@CheckForNull Constant canonicalSNaN) {
    this.canonicalSNaN = canonicalSNaN;
  }

  public void setCanonicalQNaN(@CheckForNull Constant canonicalQNaN) {
    this.canonicalQNaN = canonicalQNaN;
  }

  /**
   * The encoding of the float format.
   */
  @Nullable
  public Encoding encoding() {
    return encoding;
  }

  /**
   * The canonical signaling NaN encoding of the float format.
   */
  @Nullable
  public Constant canonicalSNaN() {
    return canonicalSNaN;
  }

  /**
   * The canonical quiet NaN encoding of the float format.
   */
  @Nullable
  public Constant canonicalQNaN() {
    return canonicalQNaN;
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
    // FIXME: for now this is checked here, but this should create a diagnostic instead of ViamError
    ensure(encoding != null, "Encoding not specified");
    checkNaN(canonicalSNaN, "sNaN");
    checkNaN(canonicalQNaN, "qNaN");
  }

  private void checkNaN(@Nullable Constant value, String kind) {
    ensure(value != null, "Canonical %s not specified", kind);
    var valueBits = value.asVal().integer().bitLength();
    var givenBits = requireNonNull(encoding).size;
    ensure(valueBits <= givenBits,
        "Canonical %s value cannot require more bits (%d) than the encoding size (%d)",
        kind, valueBits, givenBits);
  }

  @Override
  public String toString() {
    return identifier.simpleName();
  }
}
