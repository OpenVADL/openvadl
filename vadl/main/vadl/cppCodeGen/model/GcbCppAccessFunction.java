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

import vadl.viam.Format;

/**
 * A {@link GcbCppFunctionWithBody} which embodies a field access function with an implementation.
 */
public class GcbCppAccessFunction extends GcbCppFunctionWithBody {
  private final Format.FieldAccess fieldAccess;

  public GcbCppAccessFunction(GcbCppFunctionBodyLess header,
                              Format.FieldAccess fieldAccess,
                              String code) {
    super(header, code);
    this.fieldAccess = fieldAccess;
  }

  public Format.FieldAccess fieldAccess() {
    return fieldAccess;
  }
}
