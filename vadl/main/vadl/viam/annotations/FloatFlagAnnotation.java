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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.viam.Annotation;
import vadl.viam.Constant;
import vadl.viam.FloatExceptionFlag;
import vadl.viam.RegisterTensor;

/**
 * Annotation for registers that store float exception flags.
 *
 * <p>Maps each bit index of the register to either a {@link FloatExceptionFlag}, which
 * can be either sticky or non-sticky, or {@code null}.
 */
public class FloatFlagAnnotation extends Annotation<RegisterTensor> {

  private final Map<Integer, FloatExceptionFlag> sticky = new HashMap<>();
  private final Map<Integer, FloatExceptionFlag> nonSticky = new HashMap<>();

  @Override
  public Class<RegisterTensor> parentDefinitionClass() {
    return RegisterTensor.class;
  }

  public boolean isSticky(int index) {
    return sticky.containsKey(index);
  }

  @Nullable
  public FloatExceptionFlag get(int index) {
    return isSticky(index) ? sticky.get(index) : nonSticky.get(index);
  }

  public void set(int index, boolean isSticky, FloatExceptionFlag flag) {
    (isSticky ? sticky : nonSticky).put(index, flag);
  }

  public Map<Integer, FloatExceptionFlag> stickyFlags() {
    return sticky;
  }

  public Map<Integer, FloatExceptionFlag> nonStickyFlags() {
    return nonSticky;
  }

  public Map<Integer, FloatExceptionFlag> flags(boolean sticky) {
    return sticky ? stickyFlags() : nonStickyFlags();
  }

  /**
   * Returns a bit slice containing all bits that contain a flag.
   */
  public Constant.BitSlice slice() {
    return new Constant.BitSlice(
        Stream.concat(
            sticky.keySet().stream(),
            nonSticky.keySet().stream()
        ).distinct().map(i -> Constant.BitSlice.Part.of(i, i))
            .toArray(Constant.BitSlice.Part[]::new)
    );
  }

  /**
   * Creates a binary mask for the float exception flags in the register this annotation
   * is attached to, where all flags are set, which exists in the register.
   *
   * @param sticky     Whether to compute the mask for sticky flags (if {@code false}, then
   *                   the mask is computed for the non-sticky flags)
   * @return           The mask
   */
  public long flagMask(boolean sticky) {
    return flags(sticky).keySet().stream().mapToLong(idx -> idx)
        .reduce(0, (mask, idx) -> mask | (1L << idx));
  }

  /**
   * Creates a binary mask for the QEMU float exception flags, where all flags are set,
   * which exists in the given annotation.
   *
   * @param sticky      Whether to compute the mask for sticky flags (if {@code false}, then
   *                    the mask is computed for the non-sticky flags)
   * @return            The mask
   */
  public int qemuFlagMask(boolean sticky) {
    return (short) flags(sticky).values().stream().mapToInt(f -> f.qemuFlagOffset)
        .reduce(0, (mask, idx) -> mask | (1 << idx));
  }

  /**
   * Creates a binary mask for the QEMU float exception flags, where all flags are set,
   * which exists in any of the given annotations.
   *
   * @param annotations The float flags annotations to consider
   * @param sticky      Whether to compute the mask for sticky flags (if {@code false}, then
   *                    the mask is computed for the non-sticky flags)
   * @return            The mask
   */
  public static int qemuFlagMask(Collection<FloatFlagAnnotation> annotations, boolean sticky) {
    return annotations.stream().mapToInt(ann -> ann.qemuFlagMask(sticky))
        .reduce(0, (a, b) -> a | b);
  }

}
