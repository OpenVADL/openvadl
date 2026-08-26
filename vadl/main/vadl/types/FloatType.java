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

package vadl.types;

import static java.util.Objects.requireNonNull;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import vadl.viam.FloatFormat;

/**
 * Represents a float type, which in turn represents a {@link FloatFormat}.
 *
 * <pre>{@code
 * // This declares a float format and its representing type
 * [ IEEE : 32 ]
 * float-type Float
 *
 * [ IEEE : 64 ]
 * float-type Double
 *
 * // Example usage: converts from `Float` to `Double`
 * VADL::fcvt<Double>(... as Float, ...)
 * }</pre>
 */
public class FloatType extends BitsType {

  @Nullable
  FloatFormat format;
  FloatEncoding encoding;

  /**
   * Constructs a float type. Only takes an encoding, since during type-checking, only the encoding
   * (not the whole format) is known.
   *
   * @param encoding the encoding of the float format this type represents.
   */
  public FloatType(FloatEncoding encoding) {
    super(encoding.size);
    this.encoding = encoding;
  }

  /**
   * Called during VIAM lowering to set the concrete float format.
   */
  public void setFormat(@CheckForNull FloatFormat format) {
    this.format = format;
  }

  public FloatFormat format() {
    return requireNonNull(format);
  }

  public FloatEncoding encoding() {
    return encoding;
  }

  @Override
  public String name() {
    return "Float(%s)".formatted(
        format != null ? format.simpleName() : encoding.name()
    );
  }

  @Override
  public BitsType withBitWidth(int bitWidth) {
    throw new IllegalStateException("FloatType cannot be scaled");
  }
}
