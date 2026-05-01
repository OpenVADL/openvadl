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
import vadl.types.Type;

/**
 * Instruction type as it occurs in quantified logical expressions of group annotations.
 * E.g.:
 * <pre>
 *   [stop: exists i in {O2} then true ]
 *   [assert: forall i in {O1, O2} then i.par = 0]
 *   group VLIW = O1.O2
 * </pre>
 */
public class PseudoFormatType extends Type {

  /**
   * The pseudo format representing the intersection format of all instructions in the operation
   * set. Initialized during type checking.
   */
  private final PseudoFormat pseudoFormat;

  public PseudoFormatType(PseudoFormat pseudoFormat) {
    this.pseudoFormat = pseudoFormat;
  }

  public PseudoFormat format() {
    return pseudoFormat;
  }

  @Override
  public String name() {
    return "Instruction : " + pseudoFormat.name();
  }


  @Override
  public boolean isTrivialCastTo(Type other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PseudoFormatType otherType)) {
      return false;
    }
    return Objects.equals(pseudoFormat, otherType.pseudoFormat);
  }
}

