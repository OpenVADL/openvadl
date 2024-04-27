package vadl.javaannotations.viam;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.List;
import vadl.javaannotations.AbstractAnnotationChecker;

@AutoService(BugChecker.class)
@BugPattern(
    summary = "Classes with @Input annotated fields must override collectInputs method",
    severity = BugPattern.SeverityLevel.ERROR
)
public class ApplyOnInputs extends AbstractAnnotationChecker {

  private static String GRAPH_PKG = "vadl.viam.";
  private static String NODELIST = GRAPH_PKG + "NodeList";
  private static String NODE = GRAPH_PKG + "Node";
  private static String GRAPH_APPLIER = GRAPH_PKG + "GraphVisitor.Applier";

  private static String PARAM_TYPE = GRAPH_APPLIER + "<" + NODE + ">";

  public ApplyOnInputs() {
    super(
        Input.class,
        "applyOnInputsUnsafe",
        "void",
        List.of(PARAM_TYPE)
    );
  }

  @Override
  protected List<String> expectedMethodStatements(List<String> paramNames,
                                                  List<VariableTree> fields) {
    var stmts = new ArrayList<String>();

    stmts.add("super.%s(%s);".formatted(methodName, paramNames.get(0)));
    for (var field : fields) {
      var fieldType = ASTHelpers.getType(field);
      var fieldName = field.getName();

      assert fieldType != null;
      var typeOverload = "";
      if (!fieldType.toString().equals(NODE)) {
        typeOverload = ", " + fieldType + ".class";
      }

      stmts.add("%s = %s.apply(this, %s%s);".formatted(
          fieldName, paramNames.get(0),
          fieldName, typeOverload));
    }
    return stmts;
  }
}
