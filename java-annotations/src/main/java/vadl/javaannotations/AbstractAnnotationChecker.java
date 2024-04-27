package vadl.javaannotations;

import static com.google.errorprone.matchers.Matchers.hasAnnotation;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.List;
import vadl.javaannotations.viam.DataValue;


@SuppressWarnings("TreeToString")
public abstract class AbstractAnnotationChecker extends BugChecker implements
    BugChecker.ClassTreeMatcher {

  protected final String methodName;
  protected final String returnType;
  protected final List<String> parameterTypes;
  protected final Class<?> annotation;

  public AbstractAnnotationChecker(Class<?> annotation, String methodName, String returnType,
                                   List<String> parameterTypes) {
    this.methodName = methodName;
    this.returnType = returnType;
    this.parameterTypes = parameterTypes;
    this.annotation = annotation;
  }

  protected abstract List<String> expectedMethodStatements(List<String> paramNames,
                                                           List<VariableTree> fields);

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    var annotatedFields = classTree.getMembers().stream()
        .filter(VariableTree.class::isInstance)
        .map(VariableTree.class::cast)
        .filter(t -> hasAnnotation(DataValue.class.getCanonicalName()).matches(t, state))
        .toList();

    if (annotatedFields.isEmpty()) {
      return Description.NO_MATCH;
    }

    var method = classTree.getMembers().stream()
        .filter(e -> e.getKind() == Tree.Kind.METHOD)
        .map(MethodTree.class::cast)
        .filter(this::methodHasCorrectSignature)
        .findFirst()
        .orElse(null);

    if (method == null) {
      return buildDescription(annotatedFields.get(0))
          .setMessage("Class %s must override method `%s`. \nUse the signature `%s`."
              .formatted(classTree.getSimpleName(), methodName, methodSignature()))
          .build();
    }

    var paramNames = method.getParameters().stream()
        .map(VariableTree.class::cast)
        .map(e -> e.getName().toString())
        .toList();

    var expectedStatements = expectedMethodStatements(paramNames, annotatedFields);
    var actualStatements = method.getBody().getStatements();

    if (expectedStatements.size() != actualStatements.size()) {
      return buildDescription(method)
          .setMessage(
              "Invalid number of statements. Expected %s statements.\n\nUse this implementation:\n%s\n"
                  .formatted(
                      expectedStatements.size(),
                      demoImplementation(paramNames, annotatedFields)
                  ))
          .build();
    }

    for (int i = 0; i < expectedStatements.size(); i++) {
      var actual = actualStatements.get(i)
          .toString()
          .replaceAll("\\s+", "");
      var expected = expectedStatements.get(i)
          .replaceAll("\\s+", "");

      if (!actual.equals(expected)) {
        return buildDescription(actualStatements.get(i))
            .setMessage("Invalid statement. Expected `%s`".formatted(expectedStatements.get(i)))
            .build();
      }
    }


    return Description.NO_MATCH;
  }

  private String methodSignature() {
    return returnType + " " + methodName + "(" + String.join(", ", parameterTypes) + ")";
  }

  private boolean methodHasCorrectSignature(MethodTree method) {
    if (!method.getName().toString().equals(this.methodName)) {
      return false;
    }

    if (this.parameterTypes.size() != method.getParameters().size()) {
      return false;
    }

    var actualTypes = method.getParameters().stream()
        .map(ASTHelpers::getType)
        .toList();

    for (int i = 0; i < this.parameterTypes.size(); i++) {
      if (!this.parameterTypes.get(i).equals(actualTypes.get(i).toString())) {
        return false;
      }
    }

    var actualReturnType = ASTHelpers.getType(method.getReturnType());
    return actualReturnType != null && actualReturnType.toString().equals(returnType);
  }

  private String demoImplementation(List<String> paramNames, List<VariableTree> fields) {
    var builder = new StringBuilder();

    builder.append("public ")
        .append(returnType)
        .append(" ")
        .append(methodName)
        .append("(");

    var params = new ArrayList<String>();
    for (int i = 0; i < paramNames.size(); i++) {
      params.add(parameterTypes.get(i) + " " + paramNames.get(i));
    }
    builder.append(String.join(", ", params))
        .append(") {\n\t")
        .append(String
            .join("\n\t", expectedMethodStatements(paramNames, fields))
        )
        .append("\n}");

    return builder.toString();
  }

}
