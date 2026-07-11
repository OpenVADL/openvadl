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

import javax.annotation.CheckForNull;

/**
 * An IEEE-754 32/64-bit float.
 */
public class FloatType extends BitsType {

  /**
   * The size of the float.
   */
  public enum Size {
    FP32(32),
    FP64(64);

    final int bitWidth;

    Size(int bitWidth) {
      this.bitWidth = bitWidth;
    }
  }

  protected final Size size;

  protected FloatType(Size size) {
    super(size.bitWidth);
    this.size = size;
  }

  @Override
  public String name() {
    return "FP%s".formatted(size.bitWidth);
  }

  @CheckForNull
  @Override
  public DataType fittingCppType() {
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    return this.getClass() == obj.getClass() && this.size == ((FloatType) obj).size;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
