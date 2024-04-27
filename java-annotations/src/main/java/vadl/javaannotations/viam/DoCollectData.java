package vadl.javaannotations.viam;

import static com.google.errorprone.matchers.Matchers.hasAnnotation;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.List;

//@AutoService(BugChecker.class)
//
//@BugPattern(
//    summary = "Do implement the collectData method on Node",
//    severity = BugPattern.SeverityLevel.ERROR
//)
@SuppressWarnings("TreeToString")
public class DoCollectData extends BugChecker implements BugChecker.ClassTreeMatcher {


  public final String methodName = "collectData";
  public final String returnType = "void";
  public final List<String> parameterTypes = List.of("java.util.List<java.lang.Object>");


  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    var annotatedFields = tree.getMembers().stream()
        .filter(VariableTree.class::isInstance)
        .map(VariableTree.class::cast)
        .filter(t -> hasAnnotation(DataValue.class.getCanonicalName()).matches(t, state))
        .toList();


    for (var variable : annotatedFields) {
      System.out.println("Annotated field: " + variable);
    }

    if (annotatedFields.isEmpty()) {
      return Description.NO_MATCH;
    }


    var method = tree.getMembers().stream()
        .filter(e -> e.getKind() == Tree.Kind.METHOD)
        .map(MethodTree.class::cast)
        .filter(e -> e.getName().toString().equals(methodName))
        .findFirst()
        .orElse(null);

    if (method == null) {
      return describeMatch(tree);
    }

    var type = ASTHelpers.getType(method.getParameters().get(0));

    System.out.println("Found type: " + type);

    if (!methodHasCorrectSignature(method, methodName, returnType, parameterTypes)) {
      return buildDescription(method)
          .setMessage("Signature of method is not as expected.")
          .build();
    }

    var parameters = method.getParameters().stream()
        .map(VariableTree.class::cast)
        .map(e -> e.getName().toString())
        .toList();

    var expectedStatements = methodStatements(parameters, annotatedFields);
    var actualStatements = method.getBody().getStatements();

    if (expectedStatements.size() != actualStatements.size()) {
      return buildDescription(method)
          .setMessage("Invalid number of statements. Expected %s statements".formatted(
              expectedStatements.size()
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


    System.out.println(methodStatements(parameters, annotatedFields));

    return Description.NO_MATCH;
  }

  private List<String> methodStatements(List<String> paramNames, List<VariableTree> fields) {
    var stmts = new ArrayList<String>();

    stmts.add("super.%s(%s);".formatted(methodName, paramNames.get(0)));
    for (var fieldName : fields) {
      stmts.add("%s.add(%s);".formatted(paramNames.get(0), fieldName.getName()));
    }
    return stmts;
  }

  private static Matcher<ClassTree> hasField(Matcher<VariableTree> matcher) {
    return ((classTree, state) -> classTree.getMembers().stream()
        .filter(VariableTree.class::isInstance)
        .map(VariableTree.class::cast)
        .anyMatch(e -> matcher.matches(e, state)));
  }

  private boolean methodHasCorrectSignature(MethodTree method, String name, String returnType,
                                            List<String> paramTypes) {

    if (!method.getName().toString().equals(name)) {
      return false;
    }

    if (paramTypes.size() != method.getParameters().size()) {
      return false;
    }

    var actualTypes = method.getParameters().stream()
        .map(ASTHelpers::getType)
        .toList();

    for (int i = 0; i < paramTypes.size(); i++) {
      if (!paramTypes.get(i).equals(actualTypes.get(i).toString())) {
        return false;
      }
    }

    var actualReturnType = ASTHelpers.getType(method.getReturnType());
    if (actualReturnType == null || !actualReturnType.toString().equals(returnType)) {
      return false;
    }

    return true;
  }
}
