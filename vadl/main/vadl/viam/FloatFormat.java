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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import vadl.types.Type;

/**
 * Describes a float format. This covers bit-size, encoding and interpretation.
 */
public class FloatFormat extends Definition implements DefProp.WithType {

  private int size = 0;

  @Nullable
  private Constant canonicalSNaN = null;
  @Nullable
  private Constant canonicalQNaN = null;

  public FloatFormat(Identifier identifier) {
    super(identifier);
  }

  public void setSize(int size) {
    this.size = size;
  }

  public void setCanonicalSNaN(@CheckForNull Constant canonicalSNaN) {
    this.canonicalSNaN = canonicalSNaN;
  }

  public void setCanonicalQNaN(@CheckForNull Constant canonicalQNaN) {
    this.canonicalQNaN = canonicalQNaN;
  }

  /**
   * The bit-size of the float format.
   */
  public int size() {
    return size;
  }

  /**
   * The canonical signaling NaN encoding of the float format.
   */
  @Nullable
  public Constant canonicalSNaN() {
    return canonicalSNaN;
  }

  /**
   * The canonical quiet NaN encoding of the float format.
   */
  @Nullable
  public Constant canonicalQNaN() {
    return canonicalQNaN;
  }

  @Override
  public Type type() {
    return Type.floatType();
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
