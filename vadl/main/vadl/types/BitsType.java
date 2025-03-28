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

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

/**
 * An arbitrary sized sequence of Bits to represent anything.
 */
public class BitsType extends DataType {
  protected final int bitWidth;

  protected BitsType(int bitWidth) {
    this.bitWidth = bitWidth;
  }

  @Override
  public int bitWidth() {
    return bitWidth;
  }

  @Override
  public int useableBitWidth() {
    return bitWidth;
  }

  @Override
  public String name() {
    return "Bits<%d>".formatted(bitWidth);
  }

  /**
   * Finds the meet (lower bound) of the given BitsType types
   * by returning the one with the smaller bit width.
   *
   * <p>E.g. {@code Type.bits(3).meet(Type.bits(4)) == Type.bits(3)}
   *
   * @param others the BitsType objects to find the meet of
   * @return the meet (lower bound) of the given BitsType types
   */
  @SafeVarargs
  public final <T extends BitsType> T meet(T... others) {
    var lowerBound = this;
    for (var other : others) {
      if (lowerBound.bitWidth > other.bitWidth) {
        lowerBound = other;
      }
    }
    //noinspection unchecked
    return (T) lowerBound;
  }

  /**
   * Returns a new Bits type scaled by the provided factor.
   *
   * @param factor to scale by
   * @return the scaled type
   */
  public BitsType scaleBy(int factor) {
    return withBitWidth(bitWidth * factor);
  }

  /**
   * Finds the join (upper bound) for the given BitsType types
   * by returning the one with the largest bit width.
   *
   * <p>E.g. {@code Type.bits(3).meet(Type.bits(4)) == Type.bits(4)}
   *
   * @param others the BitsType objects to join
   * @param <T>    a subtype of BitsType
   * @return the BitsType object with the largest bit width
   */
  @SafeVarargs
  public final <T extends BitsType> T join(T... others) {
    return join(List.of(others));
  }

  /**
   * Finds the join (upper bound) for the given BitsType types
   * by returning the one with the largest bit width.
   *
   * <p>E.g. {@code Type.bits(3).meet(Type.bits(4)) == Type.bits(4)}
   *
   * @param others the BitsType objects to join
   * @param <T>    a subtype of BitsType
   * @return the BitsType object with the largest bit width
   */
  public final <T extends BitsType> T join(Collection<T> others) {
    var upperBound = this;
    for (var other : others) {
      if (upperBound.bitWidth < other.bitWidth) {
        upperBound = other;
      }
    }
    //noinspection Variable,unchecked
    return (T) upperBound;
  }


  @Override
  public boolean isSigned() {
    // while it is possible to auto cast bits to SInt, the BitsType is not
    // signed, as it doesn't make sense for most bits purposes
    return false;
  }

  @Override
  @Nullable
  public DataType fittingCppType() {
    if (bitWidth <= 8) {
      return constructDataType(this.getClass(), 8);
    } else if (bitWidth <= 16) {
      return constructDataType(this.getClass(), 16);
    } else if (bitWidth <= 32) {
      return constructDataType(this.getClass(), 32);
    } else if (bitWidth <= 64) {
      return constructDataType(this.getClass(), 64);
    } else if (bitWidth <= 128) {
      return constructDataType(this.getClass(), 128);
    } else {
      return null;
    }
  }

  /**
   * Calculates the minimal required bit-width to hold this value in two's complement.
   */
  public static int minimalRequiredWidthFor(long value) {
    if (value == 0) {
      return 1;
    }
    return Long.SIZE - Long.numberOfLeadingZeros(value);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return this.getClass() == obj.getClass() && this.bitWidth == ((BitsType) obj).bitWidth;
  }

  public BitsType withBitWidth(int bitWidth) {
    return Type.bits(bitWidth);
  }
}
