package vadl.javaannotations.viam;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import com.sun.source.tree.VariableTree;
import java.util.List;
import vadl.javaannotations.AbstractAnnotationChecker;

/**
 * The CollectDataChecker class is a bug checker that checks for classes with fields annotated with
 * the @DataValue annotation. These classes must override the collectData method. If the method is
 * not overridden or if the method does not have the correct signature, a bug will be reported.
 *
 * <p>This class extends the AbstractAnnotationChecker class and implements the
 * DefaultCollectMixin interface to use the defaultCollectStatements method for
 * statement collecting.</p>
 */
@AutoService(BugChecker.class)
@BugPattern(
    name = "CollectData",
    summary = "Classes with @DataValue annotated fields must override the collectCollect method",
    severity = BugPattern.SeverityLevel.ERROR
)
@SuppressWarnings("BugPatternNaming")
public class CollectDataChecker extends AbstractAnnotationChecker implements DefaultCollectMixin {

  private static final String PARAM_TYPE = "java.util.List<java.lang.Object>";

  /**
   * Constructs the bug checker.
   */
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
