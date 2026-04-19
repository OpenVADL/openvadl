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
 * Annotation for registers or register aliases that are saved in the
 * translation block state. Contains a {@link Constant.BitSlice} that specifies
 * what bits of the register are saved.
 */
public class TbStateRegisterAnnotation extends Annotation<RegisterTensor> {

  private final int registerBitWidth;

  /**
   * Determines what bits of the register are saved in the translation block state.
   * If this is null, then the whole register is saved.
   */
  @Nullable
  private Constant.BitSlice slice;

  /**
   * Constructs a new annotation for a register (or register alias) that is saved
   * in the translation block state. If the given bit slice is `null`, then the
   * whole register is considered. Otherwise, only the bits in the slice are
   * marked as saved.
   */
  public TbStateRegisterAnnotation(int registerBitWidth, @Nullable Constant.BitSlice slice) {
    this.registerBitWidth = registerBitWidth;
    this.slice = slice;
    normalizeBitSlice();
  }

  @Override
  public Class<RegisterTensor> parentDefinitionClass() {
    return RegisterTensor.class;
  }

  /**
   * Marks the bits in the given slice as translation block state saved, additional
   * to the already marked bits.
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
   * Returns whether the whole register is saved in the translation block state.
   */
  public boolean wholeRegister() {
    return slice == null;
  }

  /**
   * Returns whether the given slice of bits of the register are saved in the
   * translation block state.
   */
  public boolean covers(Constant.BitSlice slice) {
    return this.slice == null || this.slice.covers(slice);
  }

  @Nullable
  public Constant.BitSlice slice() {
    return slice;
  }

  /**
   * Returns how many bits are marked as saved in the translation block state.
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
