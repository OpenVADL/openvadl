package vadl.javaannotations.viam;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import com.sun.source.tree.VariableTree;
import java.util.List;
import vadl.javaannotations.AbstractAnnotationChecker;

/**
 * The CollectSuccessorsChecker class is a bug checker that checks for classes with @Successor
 * annotated fields that must override the collectSuccessors method.
 * It will fail if the method implementation is not as expected.
 */
@AutoService(BugChecker.class)
@BugPattern(
    name = "CollectSuccessors",
    summary = "Classes with @Successor annotated fields must override the collectSuccessors method",
    severity = BugPattern.SeverityLevel.ERROR
)
@SuppressWarnings("BugPatternNaming")
public class CollectSuccessorsChecker extends AbstractAnnotationChecker
    implements DefaultCollectMixin {

  private static final String PARAM_TYPE = "java.util.List<" + CheckerUtils.NODE + ">";

  /**
   * Constructs the bug checker.
   */
  public CollectSuccessorsChecker() {
    super(
        Successor.class,
        "collectSuccessors",
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
