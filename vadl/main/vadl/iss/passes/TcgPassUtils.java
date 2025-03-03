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

package vadl.iss.passes;

import javax.annotation.Nullable;
import vadl.iss.passes.tcgLowering.TcgCondition;
import vadl.types.BuiltInTable;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.LetNode;

/**
 * Contains utility methods for TCG passes.
 */
public class TcgPassUtils {

  /**
   * Returns a variable name for the given expression that is easy to read and understand
   * in the generated source code.
   */
  public static String exprVarName(ExpressionNode expr) {
    if (expr instanceof LetNode letNode) {
      return letNode.letName().name();
    } else if (expr instanceof FieldRefNode fieldRefNode) {
      return fieldRefNode.formatField().simpleName();
    } else if (expr instanceof FieldAccessRefNode fieldAccessRefNode) {
      return fieldAccessRefNode.fieldAccess().simpleName();
    } else if (expr instanceof ConstantNode constantNode) {
      return constantNode.constant().asVal().asString("0x", 16, false);
    } else {
      return "n" + expr.id;
    }
  }

  public static boolean isTcg(DependencyNode node) {
    return node.usages().anyMatch(u -> u instanceof ScheduledNode);
  }


  /**
   * Returns a {@link TcgCondition} for a given {@link vadl.types.BuiltInTable.BuiltIn}, if
   * there exists one, otherwise it returns null.
   * E.g., on the SLTH built-in it returns LT.
   */
  public static @Nullable TcgCondition conditionOf(BuiltInTable.BuiltIn builtIn) {
    if (builtIn == BuiltInTable.EQU) {
      return TcgCondition.EQ;
    } else if (builtIn == BuiltInTable.NEQ) {
      return TcgCondition.NE;
    } else if (builtIn == BuiltInTable.SLTH) {
      return TcgCondition.LT;
    } else if (builtIn == BuiltInTable.SLEQ) {
      return TcgCondition.LE;
    } else if (builtIn == BuiltInTable.ULTH) {
      return TcgCondition.LTU;
    } else if (builtIn == BuiltInTable.ULEQ) {
      return TcgCondition.LEU;
    } else if (builtIn == BuiltInTable.SGTH) {
      return TcgCondition.GT;
    } else if (builtIn == BuiltInTable.SGEQ) {
      return TcgCondition.GE;
    } else if (builtIn == BuiltInTable.UGTH) {
      return TcgCondition.GTU;
    } else if (builtIn == BuiltInTable.UGEQ) {
      return TcgCondition.GEU;
    } else if (builtIn == BuiltInTable.AND) {
      return TcgCondition.TSTNE;
    } else {
      return null;
    }
  }
}
