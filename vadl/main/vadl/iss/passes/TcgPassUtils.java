package vadl.iss.passes;

import vadl.viam.graph.dependency.ConstantNode;
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
}
