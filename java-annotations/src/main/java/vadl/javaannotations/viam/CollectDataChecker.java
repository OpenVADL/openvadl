package vadl.javaannotations.viam;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.List;
import vadl.javaannotations.AbstractAnnotationChecker;

@AutoService(BugChecker.class)
@BugPattern(
    summary = "Do implement the collectData method on Node",
    severity = BugPattern.SeverityLevel.ERROR
)
@SuppressWarnings("TreeToString")
public class CollectDataChecker extends AbstractAnnotationChecker {


  public CollectDataChecker() {
    super(
        DataValue.class,
        "collectData",
        "void",
        List.of("java.util.List<java.lang.Object>")
    );
  }

  @Override
  protected List<String> expectedMethodStatements(List<String> paramNames,
                                                  List<VariableTree> fields) {
    var stmts = new ArrayList<String>();
    stmts.add("super.%s(%s);".formatted(methodName, paramNames.get(0)));
    for (var fieldName : fields) {
      stmts.add("%s.add(%s);".formatted(paramNames.get(0), fieldName.getName()));
    }
    return stmts;
  }
}
