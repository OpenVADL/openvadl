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

package vadl.viam;

import javax.annotation.Nullable;
import vadl.types.FloatEncoding;
import vadl.types.Type;

/**
 * VIAM definition representing a float format. This currently only contains a
 * {@link FloatEncoding}, but will contain other things like NaN encoding and handling
 * in the future.
 */
public class FloatFormat extends Definition {

  private final FloatEncoding encoding;

  public FloatFormat(Identifier identifier, FloatEncoding encoding) {
    super(identifier);
    this.encoding = encoding;
  }

  /**
   * The encoding of the float format.
   */
  @Nullable
  public FloatEncoding encoding() {
    return encoding;
  }

  /**
   * The name of the float format in lower case.
   */
  public String nameLower() {
    return simpleName().toLowerCase();
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return identifier.simpleName();
  }
}
