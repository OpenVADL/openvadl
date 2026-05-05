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

package vadl.viam.graph;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.viam.Counter;
import vadl.viam.passes.staticCounterAccess.CounterAccessResolvingPass;

/**
 * Interface to indicate that the implementing class could access a {@link Counter}.
 *
 * <p>The {@link #staticCounterAccess()} indicates if this access is known to be
 * a (program) counter access. It is set by the
 * {@link CounterAccessResolvingPass}. If it is not set, it is known that the access
 * is <strong>not</strong> a counter access.</p>
 */
public interface CanAccessCounter extends HasRegisterTensor {

  /**
   * Determines if the register is a PC based on whether staticCounterAccess is set.
   */
  default boolean isPcAccess() {
    // FIXME: can there be other counters that are not PCs? If yes, fix this
    return staticCounterAccess() != null;
  }

  /**
   * Returns the statically known counter this accesses, or {@code null}, if
   * this accesses no counter.
   */
  @Nullable
  Counter staticCounterAccess();

  /**
   * This is set by the
   * {@link CounterAccessResolvingPass}.
   *
   * @param staticCounterAccess the counter that is accessed.
   * @see CounterAccessResolvingPass
   */
  void setStaticCounterAccess(@Nonnull Counter staticCounterAccess);
}
