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
    } else {
      return null;
    }
  }
}
