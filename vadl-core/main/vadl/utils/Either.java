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

package vadl.utils;

import java.util.Objects;
import javax.annotation.Nullable;
import vadl.viam.ViamError;

/**
 * Represents that a value can be either of one type or another one.
 */
public final class Either<T, X> {
  @Nullable
  private T left;

  @Nullable
  private X right;

  /**
   * Constructor.
   */
  public Either(@Nullable T left, @Nullable X right) {
    ViamError.ensure((left != null && right == null) || (left == null && right != null),
        "left and right cannot both be set");
    this.left = left;
    this.right = right;
  }

  public boolean isLeft() {
    return this.left != null;
  }

  public boolean isRight() {
    return this.right != null;
  }

  public T left() {
    return Objects.requireNonNull(left);
  }

  public X right() {
    return Objects.requireNonNull(right);
  }
}
