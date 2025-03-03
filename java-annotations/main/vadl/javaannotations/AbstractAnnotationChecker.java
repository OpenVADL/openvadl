// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.javaannotations;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.List;


/**
 * This abstract class represents a bug checker that checks for specific annotations and their
 * corresponding method overrides in a class. It extends the BugChecker class and implements the
 * BugChecker.ClassTreeMatcher interface.
 *
 * <p>Concrete implementations are e.g. {@link vadl.javaannotations.viam.CollectInputsChecker},
 * {@link vadl.javaannotations.viam.ApplyOnInputsChecker}...</p>
 */
@SuppressWarnings("TreeToString")
public abstract class AbstractAnnotationChecker extends BugChecker implements
    BugChecker.ClassTreeMatcher {

  protected final String methodName;
  protected final String returnType;
  protected final List<String> parameterTypes;
  protected final Class<?> annotation;

  /**
   * Constructor of the bug checker.
   *
   * @param annotation     to search for
   * @param methodName     that must be overridden
   * @param returnType     of the overridden method
   * @param parameterTypes parameter types of the overridden method
   */
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
        .filter(t -> Matchers.hasAnnotation(annotation.getCanonicalName()).matches(t, state))
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
              ("Invalid number of statements. Expected %s statements."
                  + "\n\nUse this implementation:\n%s\n")
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
