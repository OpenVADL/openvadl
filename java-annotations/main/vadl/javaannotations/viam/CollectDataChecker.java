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
    return defaultCollectStatements(methodName, paramNames, fields, false);
  }
}
