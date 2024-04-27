package vadl.javaannotations.viam;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import com.sun.source.tree.VariableTree;
import java.util.List;
import vadl.javaannotations.AbstractAnnotationChecker;

@AutoService(BugChecker.class)
@BugPattern(
    name = "CollectData",
    summary = "Classes with @DataValue annotated fields must override the collectCollect method",
    severity = BugPattern.SeverityLevel.ERROR
)
public class CollectDataChecker extends AbstractAnnotationChecker implements DefaultCollectMixin {

  private static final String PARAM_TYPE = "java.util.List<java.lang.Object>";

  public CollectDataChecker() {
    super(
        DataValue.class,
        "collectData",
        "void",
        List.of(PARAM_TYPE)
    );
  }

  @Override
  protected List<String> expectedMethodStatements(List<String> paramNames,
                                                  List<VariableTree> fields) {
    return defaultCollectStatements(methodName, paramNames, fields);
  }
}
