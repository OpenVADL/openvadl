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

import java.util.Map;

/**
 * A class that represents the VADL float status type.
 *
 * <p>It is actually a struct of with five boolean fields.
 * These five elements represent status flags:
 * <li>nv</li>
 * <li>dz</li>
 * <li>of</li>
 * <li>uf</li>
 * <li>nx</li>
 * in that order.
 */
public class FloatStatusType extends StructType {

  public static final String INVALID = "nv";
  public static final String DIVISION_BY_ZERO = "dz";
  public static final String OVERFLOW = "of";
  public static final String UNDERFLOW = "uf";
  public static final String INEXACT = "nx";

  protected FloatStatusType() {
    super(Type.struct(Map.of(
        INVALID, Type.bool(),
        DIVISION_BY_ZERO, Type.bool(),
        OVERFLOW, Type.bool(),
        UNDERFLOW, Type.bool(),
        INEXACT, Type.bool()
    )).fields());
  }

  @Override
  public String name() {
    return "FloatStatus";
  }

}
