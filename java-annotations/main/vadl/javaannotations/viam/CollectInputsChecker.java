package vadl.javaannotations.viam;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import com.sun.source.tree.VariableTree;
import java.util.List;
import vadl.javaannotations.AbstractAnnotationChecker;

/**
 * The CollectInputsChecker class is a bug checker that checks for classes with fields
 * annotated with @Input and ensures that they override the collectInputs method.
 * It will fail if its implementation is not as expected.
 */
@AutoService(BugChecker.class)
@BugPattern(
    name = "CollectInputs",
    summary = "Classes with @Input annotated fields must override the collectInputs method",
    severity = BugPattern.SeverityLevel.ERROR
)
@SuppressWarnings("BugPatternNaming")
public class CollectInputsChecker extends AbstractAnnotationChecker implements DefaultCollectMixin {

  private static final String PARAM_TYPE = "java.util.List" + "<" + CheckerUtils.NODE + ">";

  /**
   * Constructs the bug checker.
   */
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
    return defaultCollectStatements(methodName, paramNames, fields, true);
  }
}
