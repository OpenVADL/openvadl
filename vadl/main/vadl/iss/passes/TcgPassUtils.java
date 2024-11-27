package vadl.iss.passes;

import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.LetNode;

public class TcgPassUtils {

  public static String exprVarName(ExpressionNode expr) {
    if (expr instanceof LetNode letNode) {
      return letNode.letName().name();
    } else if (expr instanceof FieldRefNode fieldRefNode) {
      return fieldRefNode.formatField().simpleName();
    } else if (expr instanceof FieldAccessRefNode fieldAccessRefNode) {
      return fieldAccessRefNode.fieldAccess().simpleName();
    } else {
      return "n" + expr.id;
    }
  }
}
