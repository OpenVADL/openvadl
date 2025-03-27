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
 * A Boolean which can only hold true/false.
 */
public class BoolType extends DataType {

  protected BoolType() {
  }

  @Override
  public String name() {
    return "Bool";
  }

  @Override
  public int bitWidth() {
    return 1;
  }

  @Override
  public int useableBitWidth() {
    return bitWidth();
  }

  @Override
  public DataType fittingCppType() {
    return this;
  }


}
