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
 * A class that represents the built-in type for float-type expressions.
 *
 * <pre>{@code
 * // This declares a float format
 * [ IEEE : 32 ]
 * float-type binary32
 *
 * // It can later be used as an expression
 * VADL::fcvtfs::<binary32, 32>(...)
 * //             ^^^^^^^^ this is an expression with the type `FloatType`
 * }</pre>
 *
 * <strong>Note:</strong> FloatType is NOT a {@link DataType}. It is NOT the type of float
 * expressions. Float expressions are {@link BitsType}. There exist no conversions between this
 * and any other type.
 */
public class FloatType extends Type {

  protected FloatType() { }

  @Override
  public String name() {
    return "FloatType";
  }
}
