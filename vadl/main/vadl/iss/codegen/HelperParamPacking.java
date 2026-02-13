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

package vadl.iss.codegen;

import java.util.ArrayList;
import java.util.List;
import vadl.iss.passes.extensions.InstrInfo;
import vadl.viam.graph.dependency.ParamNode;

/**
 * Describes how instruction helper parameters are packed into 64-bit helper argument blocks.
 *
 * <p>Example (C-like):
 * </p>
 * <pre>{@code
 * // Original helper params (in sorted order):
 * //   a: 32 bits, b: 40 bits, c: 8 bits
 * //
 * // Concatenated bitstream (LSB -> MSB):
 * //   [a(32)][b(40)][c(8)] => total 80 bits => 2 blocks
 * //
 * // block 0 (bits 0..63):  a[31:0] | b[31:0] << 32
 * // block 1 (bits 64..79): b[39:32] | c[7:0] << 8
 *
 * uint64_t packed0 = ((uint64_t)a)
 *                  | (((uint64_t)b & UINT64_C(0xFFFFFFFF)) << 32);
 * uint64_t packed1 = (((uint64_t)b >> 32) & UINT64_C(0xFF))
 *                  | (((uint64_t)c & UINT64_C(0xFF)) << 8);
 *
 * // Unpack in helper:
 * uint32_t a_u = (uint32_t)(packed0 & UINT64_C(0xFFFFFFFF));
 * uint64_t b_u = ((packed0 >> 32) & UINT64_C(0xFFFFFFFF))
 *              | (((packed1 >> 0) & UINT64_C(0xFF)) << 32);
 * uint32_t c_u = (uint32_t)((packed1 >> 8) & UINT64_C(0xFF));
 * }</pre>
 */
public final class HelperParamPacking {
  public static final int BLOCK_WIDTH = 64;
  public static final int MAX_HELPER_ARG_BLOCKS = 6;

  /**
   * A helper parameter with its bit placement in the concatenated bit stream.
   */
  public record ParamPlacement(ParamNode param, int startBit, int bitWidth) {
  }

  /**
   * A contiguous part of a parameter that is stored in one 64-bit block.
   */
  public record ParamSlice(ParamPlacement param,
                           int blockIndex,
                           int blockOffset,
                           int paramOffset,
                           int width) {
  }

  private final List<ParamPlacement> params;
  private final int totalBits;
  private final int blockCount;

  private HelperParamPacking(List<ParamPlacement> params, int totalBits) {
    this.params = List.copyOf(params);
    this.totalBits = totalBits;
    this.blockCount = (totalBits + BLOCK_WIDTH - 1) / BLOCK_WIDTH;
  }

  public static HelperParamPacking from(InstrInfo instrInfo) {
    return fromParams(instrInfo.helperFormatParamOrder().toList());
  }

  /**
   * Creates a new `HelperParamPacking` instance from a list of ordered parameter nodes.
   *
   * <p>This method processes the provided list of `ParamNode` objects, calculates
   * their bit placements in a concatenated bit stream, and constructs the
   * corresponding `HelperParamPacking` object.
   */
  public static HelperParamPacking fromParams(List<ParamNode> orderedParams) {
    var placements = new ArrayList<ParamPlacement>();
    var bitOffset = 0;
    for (var param : orderedParams) {
      var width = param.type().asDataType().bitWidth();
      placements.add(new ParamPlacement(param, bitOffset, width));
      bitOffset += width;
    }
    return new HelperParamPacking(placements, bitOffset);
  }

  public List<ParamPlacement> params() {
    return params;
  }

  public int totalBits() {
    return totalBits;
  }

  public int blockCount() {
    return blockCount;
  }

  /**
   * Calculates and returns a list of contiguous slices for the specified parameter, representing
   * how the parameter is split across one or more 64-bit blocks.
   *
   * <p>The method processes the bit placement of the parameter within the overall bit stream and
   * divides it into multiple slices when it spans multiple blocks. Each slice corresponds
   * to a section of the parameter stored within a single block.
   */
  public List<ParamSlice> slicesForParam(ParamPlacement param) {
    var slices = new ArrayList<ParamSlice>();
    var remaining = param.bitWidth();
    var currentBit = param.startBit();
    var consumedParamBits = 0;

    while (remaining > 0) {
      var blockIndex = currentBit / BLOCK_WIDTH;
      var blockOffset = currentBit % BLOCK_WIDTH;
      var partWidth = Math.min(remaining, BLOCK_WIDTH - blockOffset);
      slices.add(new ParamSlice(param, blockIndex, blockOffset, consumedParamBits, partWidth));
      currentBit += partWidth;
      consumedParamBits += partWidth;
      remaining -= partWidth;
    }

    return slices;
  }

  /**
   * Retrieves a list of parameter slices that belong to the specified block index.
   *
   * <p>This method filters and collects all `ParamSlice` objects from the parameters
   * whose `blockIndex` matches the given block index.
   */
  public List<ParamSlice> slicesForBlock(int blockIndex) {
    var slices = new ArrayList<ParamSlice>();
    for (var param : params) {
      for (var slice : slicesForParam(param)) {
        if (slice.blockIndex() == blockIndex) {
          slices.add(slice);
        }
      }
    }
    return slices;
  }

  /**
   * Returns a uint64 mask literal that keeps the lower {@code width} bits.
   */
  public static String u64MaskLiteral(int width) {
    if (width <= 0 || width > 64) {
      throw new IllegalArgumentException("Mask width must be in [1, 64], but was " + width);
    }
    if (width == 64) {
      return "UINT64_MAX";
    }
    var mask = (1L << width) - 1;
    return "UINT64_C(0x" + Long.toUnsignedString(mask, 16).toUpperCase() + ")";
  }
}
