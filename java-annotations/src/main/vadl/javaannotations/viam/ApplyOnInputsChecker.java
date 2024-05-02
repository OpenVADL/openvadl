package vadl.javaannotations.viam;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.List;
import vadl.javaannotations.AbstractAnnotationChecker;

/**
 * The ApplyOnInputsChecker class is a bug checker that checks for classes with fields
 * annotated with @Input and ensures that they override the applyOnInputsUnsafe method.
 * It will fail if its implementation is not as expected.
 */
@AutoService(BugChecker.class)
@BugPattern(
    name = "ApplyOnInputs",
    summary = "Classes with @Input annotated fields must override applyOnInputsUnsafe method",
    severity = BugPattern.SeverityLevel.ERROR
)
@SuppressWarnings("BugPatternNaming")
public class ApplyOnInputsChecker extends AbstractAnnotationChecker {

  private static String GRAPH_PKG = "vadl.viam.graph.";
  private static String NODELIST = GRAPH_PKG + "NodeList";
  private static String NODE = GRAPH_PKG + "Node";
  private static String GRAPH_APPLIER = GRAPH_PKG + "GraphVisitor.Applier";

  private static String PARAM_TYPE = GRAPH_APPLIER + "<" + NODE + ">";

  /**
   * The ApplyOnInputsChecker class is a bug checker that checks for classes with fields
   * annotated with @Input and ensures that they override the applyOnInputsUnsafe method.
   * It will fail if its implementation is not as expected.
   */
  public ApplyOnInputsChecker() {
    super(
        Input.class,
        "applyOnInputsUnsafe",
        "void",
        List.of(PARAM_TYPE)
    );
  }

  /**
   * This method generates a list of expected method statements based on the given parameters
   * and fields.
   *
   * @param paramNames The names of the parameters for the method.
   * @param fields     The list of VariableTree objects representing the fields used in the method.
   * @return A List of String objects representing the expected method statements.
   */
  @Override
  protected List<String> expectedMethodStatements(List<String> paramNames,
                                                  List<VariableTree> fields) {
    var stmts = new ArrayList<String>();

    // call super method
    stmts.add("super.%s(%s);".formatted(methodName, paramNames.get(0)));

    for (var field : fields) {
      var fieldName = field.getName();
      var fieldType = ASTHelpers.getType(field);

      assert fieldType != null;
      var typeOverload = "";

      // if it's not the default base node class we use the overloaded apply method
      if (!fieldType.toString().equals(NODE)) {
        String simpleTypeName;

        // if the type is generic (NodeList<>) we want the generic argument as typeOverload
        if (fieldType.isParameterized()) {
          simpleTypeName =
              fieldType.getTypeArguments().get(0).asElement().getSimpleName().toString();
        } else {
          simpleTypeName = fieldType.asElement().getSimpleName().toString();
        }

        typeOverload = ", " + simpleTypeName + ".class";
      }

      String stmt;
      if (fieldType.toString().startsWith(NODELIST)) {
        // if the field is a nodelist, we implement it as stream
        stmt =
            ("%s = %s.stream().map((e) -> %s.apply(this, e%s))"
                + ".collect(Collectors.toCollection(NodeList::new));")
                .formatted(fieldName, fieldName, paramNames.get(0), typeOverload);
      } else {
        // otherwise we use the default apply method
        stmt = "%s = %s.apply(this, %s%s);".formatted(
            fieldName, paramNames.get(0),
            fieldName, typeOverload);
      }

      stmts.add(stmt);
    }
    return stmts;
  }
}
