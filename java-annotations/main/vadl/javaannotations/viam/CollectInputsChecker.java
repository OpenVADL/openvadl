// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.javaannotations.viam;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.List;
import vadl.javaannotations.AbstractAnnotationChecker;

/**
 * The CollectInputsChecker class is a bug checker that checks for classes with fields
 * annotated with @Input and ensures that they override the forEachInput method.
 * It will fail if its implementation is not as expected.
 */
@AutoService(BugChecker.class)
@BugPattern(
    name = "CollectInputs",
    summary = "Classes with @Input annotated fields must override the forEachInput method",
    severity = BugPattern.SeverityLevel.ERROR
)
@SuppressWarnings("BugPatternNaming")
public class CollectInputsChecker extends AbstractAnnotationChecker {

  private static final String PARAM_TYPE =
      "java.util.function.Consumer" + "<" + CheckerUtils.NODE + ">";

  /**
   * Constructs the bug checker.
   */
  public CollectInputsChecker() {
    super(
        Input.class,
        "forEachInput",
        "void",
        List.of(PARAM_TYPE)
    );
  }

  @Override
  protected List<String> expectedMethodStatements(List<String> paramNames,
                                                  List<VariableTree> fields) {
    var stmts = new ArrayList<String>();
    var consumerName = paramNames.get(0);

    stmts.add("super.%s(%s);".formatted(methodName, consumerName));
    for (var field : fields) {
      var type = ASTHelpers.getType(field);
      assert type != null;

      var hasAnnotation = ASTHelpers
          .hasDirectAnnotationWithSimpleName(field, "Nullable");

      var stmt = type.toString().startsWith(CheckerUtils.NODELIST)
          ? "%s.forEach(%s);".formatted(field.getName(), consumerName)
          : "%s.accept(%s);".formatted(consumerName, field.getName());

      if (hasAnnotation) {
        stmt = "if (this.%s != null) { %s }".formatted(field.getName(), stmt);
      }

      stmts.add(stmt);
    }
    return stmts;
  }
}
