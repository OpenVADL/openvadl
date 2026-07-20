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

package vadl.viam.annotations;

import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.viam.Annotation;
import vadl.viam.Constant;
import vadl.viam.RegisterTensor;

/**
 * Annotation for registers, which contains a {@link Constant.BitSlice} that specifies
 * what bits of the register are annotated.
 */
public abstract class RegisterSliceAnnotation extends Annotation<RegisterTensor> {

  private final int registerBitWidth;

  /**
   * Determines what bits of the register are annotated.
   * If this is null, then the whole register (i.e. all bits) are annotated.
   */
  @Nullable
  private Constant.BitSlice slice;

  /**
   * Constructs a new annotation for a register. If the given bit slice is `null`, then the
   * whole register is annotated. Otherwise, only the bits in the slice are annotated.
   */
  public RegisterSliceAnnotation(int registerBitWidth, @Nullable Constant.BitSlice slice) {
    this.registerBitWidth = registerBitWidth;
    this.slice = slice;
    normalizeBitSlice();
  }

  @Override
  public Class<RegisterTensor> parentDefinitionClass() {
    return RegisterTensor.class;
  }

  /**
   * Marks the bits in the given slice annotated, additional to the already marked bits.
   *
   * @param slice What bits to add
   */
  public void addSlice(@Nullable Constant.BitSlice slice) {
    if (slice == null || this.slice == null) {
      this.slice = null;
    } else {
      new Constant.BitSlice(Stream.concat(
          this.slice.parts(), slice.parts()
      ).toArray(Constant.BitSlice.Part[]::new));
      normalizeBitSlice();
    }
  }

  /**
   * Returns whether the whole register is annotated.
   */
  public boolean wholeRegister() {
    return slice == null;
  }

  /**
   * Returns whether the given slice of bits of the register are annotated.
   */
  public boolean covers(Constant.BitSlice slice) {
    return this.slice == null || this.slice.covers(slice);
  }

  @Nullable
  public Constant.BitSlice slice() {
    return slice;
  }

  /**
   * Returns how many bits are annotated.
   */
  public int bitSize() {
    if (slice == null) {
      return registerBitWidth;
    }
    return slice.bitSize();
  }

  private void normalizeBitSlice() {
    if (covers(Constant.BitSlice.of(registerBitWidth - 1, 0))) {
      // set to null if the whole register is covered
      slice = null;
    }
    if (slice != null) {
      // merge all overlapping parts and ensure that every bit is only covered once

      // Note: calling slice.hasOverlappingParts() does NOT work here, because it does not
      // check overlapping parts that are equal
      slice = new Constant.BitSlice(
          slice.stream().distinct()
              .mapToObj(i -> new Constant.BitSlice.Part(i, i))
              .toArray(Constant.BitSlice.Part[]::new)
      );
    }
  }

}
