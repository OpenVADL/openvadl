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

  /**
   * Returns the default statement implementation for collecting properties.
   *
   * @param methodName     The name of the collecting method.
   * @param paramNames     The names of the parameter of the collecting method.
   * @param fields         The fields that should get collected.
   * @param checkNullables Whether it should only add nullable fields if they are not null.
   * @return A list of statements that define the method implementation.
   */
  default List<String> defaultCollectStatements(String methodName, List<String> paramNames,
                                                List<VariableTree> fields, boolean checkNullables) {
    var stmts = new ArrayList<String>();

    stmts.add("super.%s(%s);".formatted(methodName, paramNames.get(0)));
    for (var field : fields) {
      var type = ASTHelpers.getType(field);
      assert type != null;

      var hasAnnotation = ASTHelpers
          .hasDirectAnnotationWithSimpleName(field, "Nullable");

      var addName = type.toString().startsWith(CheckerUtils.NODELIST) ? "addAll" : "add";
      var stmt = "%s.%s(%s);".formatted(paramNames.get(0), addName, field.getName());

      if (checkNullables && hasAnnotation) {
        stmt = "if (this.%s != null) { %s }".formatted(field.getName(), stmt);
      }

      stmts.add(stmt);

    }
    return stmts;
  }
}
