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

package vadl.types;

/**
 * A class that represents the VADL status type.
 *
 * <p>It is actually a struct of with four boolean fields.
 * These four elements represent status flags:
 * <li>negative</li>
 * <li>zero</li>
 * <li>carry</li>
 * <li>overflow</li>
 * in that order.
 */
public class StatusType extends StructType {

  public static final String NEGATIVE = "negative";
  public static final String ZERO = "zero";
  public static final String CARRY = "carry";
  public static final String OVERFLOW = "overflow";

  protected StatusType() {
    super(Type.struct(
        NEGATIVE, Type.bool(),
        ZERO, Type.bool(),
        CARRY, Type.bool(),
        OVERFLOW, Type.bool()
    ).fields());
  }

  @Override
  public String name() {
    return "Status";
  }

}
