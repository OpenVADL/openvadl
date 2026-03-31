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

package vadl.iss.passes.tcgLowering;

/**
 * An enumeration defining various types of comparison conditions for TCG (Tiny Code Generator).
 * This enum provides a set of logical and comparison operators that can be used to generate
 * condition checks in TCG.
 */
public enum TcgCondition {
  EQ,
  NE,
  LT,
  GE,
  LE,
  GT,
  LTU,
  GEU,
  LEU,
  GTU,
  TSTEQ,
  TSTNE;

  @SuppressWarnings("MethodName")
  public String cCode() {
    return "TCG_COND_" + this.name();
  }

  /**
   * Returns the negated TCG condition.
   * E.g., LT -> GE.
   */
  public TcgCondition not() {
    return switch (this) {
      case EQ -> NE;
      case NE -> EQ;
      case LT -> GE;
      case GE -> LT;
      case LE -> GT;
      case GT -> LE;
      case LTU -> GEU;
      case GEU -> LTU;
      case LEU -> GTU;
      case GTU -> LEU;
      case TSTEQ -> TSTNE;
      case TSTNE -> TSTEQ;
    };
  }
}
