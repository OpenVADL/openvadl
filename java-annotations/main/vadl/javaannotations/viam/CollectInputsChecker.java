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
