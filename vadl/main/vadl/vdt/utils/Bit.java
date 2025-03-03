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
 * Represents a single bit.
 *
 * @param value the value of the bit (true for 1, false for 0)
 */
public record Bit(boolean value) implements BitWise<Bit> {

  @Override
  public Bit and(Bit other) {
    return this.value && other.value ? new Bit(true) : new Bit(false);
  }

  @Override
  public Bit or(Bit other) {
    return this.value || other.value ? new Bit(true) : new Bit(false);
  }

  @Override
  public Bit xor(Bit other) {
    return this.value ^ other.value ? new Bit(true) : new Bit(false);
  }

  @Override
  public Bit not() {
    return !this.value ? new Bit(true) : new Bit(false);
  }

  @Override
  public int hashCode() {
    return this.value ? 1 : 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Bit)) {
      return false;
    }
    return this.value == ((Bit) obj).value;
  }
}
