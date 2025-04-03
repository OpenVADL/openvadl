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

package vadl.ast;

import java.math.BigInteger;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;

/**
 * A type for constants that voids many casts.
 *
 * <p>Constants have the type of the value they hold.
 */
public class ConstantType extends Type {
  private final BigInteger value;

  public ConstantType(BigInteger value) {
    this.value = value;
  }

  public ConstantType(long value) {
    this.value = BigInteger.valueOf(value);
  }

  BigInteger getValue() {
    return value;
  }

  int requiredBitWidth() {
    var isNegative = value.compareTo(BigInteger.ZERO) < 0;
    return value.bitLength() + (isNegative ? 1 : 0);
  }

  SIntType closestSInt() {
    return Type.signedInt(requiredBitWidth());
  }

  UIntType closestUInt() {
    return Type.unsignedInt(requiredBitWidth());
  }

  BitsType closestBits() {
    return Type.bits(requiredBitWidth());
  }

  Type closestTo(Type target) {
    // Numbers cannot be cast to bools but the closest is a bits
    if (target instanceof BoolType) {
      if (value.equals(BigInteger.ZERO) || value.equals(BigInteger.ONE)) {
        return target;
      }

      var bitsWidth = requiredBitWidth();
      if (value.compareTo(BigInteger.ZERO) < 0) {
        // Cannot pack into uint is negative, closest is still SInt
        return Type.signedInt(bitsWidth);
      }
      return Type.bits(bitsWidth);
    }

    if (target instanceof SIntType targetSInt) {
      // If the number is positiver than required bit width doesn't include the sign bit and we need
      // to manually add it here.
      var bitsWidth = Math.max(targetSInt.bitWidth(),
          requiredBitWidth() + (value.compareTo(BigInteger.ZERO) < 0 ? 0 : 1));
      return Type.signedInt(bitsWidth);
    }

    if (target instanceof UIntType targetUInt) {
      var bitsWidth = Math.max(targetUInt.bitWidth(), requiredBitWidth());
      if (value.compareTo(BigInteger.ZERO) < 0) {
        // Cannot pack into uint is negative, closest is still SInt
        return Type.signedInt(bitsWidth);
      }
      return Type.unsignedInt(bitsWidth);
    }

    if (target instanceof BitsType targetBits) {
      var bitsWidth = Math.max(targetBits.bitWidth(), requiredBitWidth());

      if (value.compareTo(BigInteger.ZERO) < 0) {
        // Cannot pack into uint is negative, closest is still SInt
        return Type.signedInt(bitsWidth);
      }
      return Type.bits(bitsWidth);
    }

    return this;
  }

  @Override
  public String name() {
    return "Const<%s>".formatted(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConstantType that = (ConstantType) o;
    return value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }
}
