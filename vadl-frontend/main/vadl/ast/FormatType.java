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

package vadl.ast;

import java.util.Objects;
import vadl.types.BitsType;
import vadl.types.Type;

/**
 * A format type is a type from a format.
 * This needs to be part of the typesystem to resolve subcalls.
 *
 * <p>This type never leaves the frontend and will be lowered to the concrete datatype.
 */
class FormatType extends BitsType {
  FormatDefinition format;

  protected FormatType(FormatDefinition format) {
    super(((BitsType) Objects.requireNonNull(format.typeLiteral.type)).bitWidth());
    this.format = format;
  }

  Type innerType() {
    return Objects.requireNonNull(format.typeLiteral.type);
  }

  @Override
  public String toString() {
    return format.identifier().name;
  }
}
