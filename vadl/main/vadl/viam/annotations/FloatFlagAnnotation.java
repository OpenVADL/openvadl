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
import javax.annotation.Nullable;
import vadl.viam.Annotation;
import vadl.viam.Constant;
import vadl.viam.FloatExceptionFlag;
import vadl.viam.RegisterTensor;

/**
 * Annotation for registers that are saved in the translation block state. Contains a
 * {@link Constant.BitSlice} that specifies what bits of the register are saved.
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

}
