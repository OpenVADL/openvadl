package vadl.javaannotations.viam;

import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.List;

/**
 * The DefaultCollectMixin interface provides a default implementation of the
 * expectedMethodStatements method for collecting checkers based on the given
 * method name, parameter names, and field declarations.
 */
interface DefaultCollectMixin {
  default List<String> defaultCollectStatements(String methodName, List<String> paramNames,
                                                List<VariableTree> fields) {
    var stmts = new ArrayList<String>();

    stmts.add("super.%s(%s);".formatted(methodName, paramNames.get(0)));
    for (var field : fields) {
      var type = ASTHelpers.getType(field);

      assert type != null;
      var addName = type.toString().startsWith(CheckerUtils.NODELIST) ? "addAll" : "add";
      stmts.add("%s.%s(%s);".formatted(paramNames.get(0), addName, field.getName()));
    }
    return stmts;
  }
}
