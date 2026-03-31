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

package vadl.cppCodeGen.model;

import vadl.types.Type;

/**
 * Indicates that the type is a cpp type.
 */
public class CppType extends Type {
  protected final String typeName;
  private final boolean isReference;
  private final boolean isConst;

  /**
   * Constructor.
   */
  public CppType(String typeName, boolean isReference, boolean isConst) {
    this.typeName = typeName;
    this.isReference = isReference;
    this.isConst = isConst;
  }

  @Override
  public String name() {
    return typeName;
  }

  public String lower() {
    return String.format("%s %s%s", isConst ? "const" : "", typeName, isReference ? "&" : "");
  }
}
