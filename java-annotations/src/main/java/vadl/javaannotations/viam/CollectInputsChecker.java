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
public class CollectInputsChecker extends AbstractAnnotationChecker {

  private static String GRAPH_PKG = "vadl.viam.";
  private static String NODELIST = GRAPH_PKG + "NodeList";
  private static String NODE = GRAPH_PKG + "Node";

  private static String PARAM_TYPE = NODELIST + "<" + NODE + ">";

  public CollectInputsChecker() {
    super(
        Input.class,
        "collectInputs",
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
      var type = ASTHelpers.getType(field);

      assert type != null;
      var addName = type.toString().startsWith(NODELIST) ? "addAll" : "add";
      stmts.add("%s.%s(%s);".formatted(paramNames.get(0), addName, field.getName()));
    }
    return stmts;
  }
}
