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

package vadl.vdt.utils;

/**
 * A pattern bit, i.e. a bit that can be either 0, 1 or <i>don't care</i>.
 */
public class PBit {

  /**
   * The possible values of a pattern bit.
   */
  public enum Value {
    ZERO, ONE, DONT_CARE
  }

  private final Value value;

  public PBit(Value value) {
    this.value = value;
  }

  public Value getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PBit)) {
      return false;
    }
    return value == ((PBit) obj).value;
  }
}
