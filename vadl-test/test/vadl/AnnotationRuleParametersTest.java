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

package vadl;

import static vadl.AnnotationRuleTest.JAKARTA_VALIDATION_CONSTRAINTS_NOT_NULL;
import static vadl.AnnotationRuleTest.JAVAX_VALIDATION_CONSTRAINTS_NOT_NULL;
import static vadl.AnnotationRuleTest.ORG_JETBRAINS_ANNOTATIONS_NOT_NULL;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

public class AnnotationRuleParametersTest {
  // We needed to extract this into a separate class because the rule was not executed with
  // ArchCondition.
  static final ArchCondition<JavaMethod> condition =
      new ArchCondition<>("have parameters NOT annotated with @NotNull") {
        @Override
        public void check(JavaMethod method, ConditionEvents events) {
          for (int paramIndex = 0; paramIndex < method.getParameters().size();
               paramIndex++) {
            var param = method.getParameters().get(paramIndex);
            for (JavaAnnotation<?> annotation : param.getAnnotations()) {
              String typeName = annotation.getType().getName();
              if (isNotNull(typeName)) {
                events.add(SimpleConditionEvent.violated(
                    annotation,
                    String.format("Method %s has parameter[%d] annotated with %s",
                        method.getFullName(), paramIndex, typeName)
                ));
              }
            }
          }
        }
      };

  @Test
  public void checkArchitecturalRules() {
    JavaClasses importedClasses = new ClassFileImporter().importPackages("vadl");

    ArchRule rule = ArchRuleDefinition.methods()
        // do not check for kotlin code
        .that().areDeclaredInClassesThat().areNotAnnotatedWith("kotlin.Metadata")
        .should(condition);

    rule.check(importedClasses);
  }

  private static boolean isNotNull(String typeName) {
    return typeName.equals(ORG_JETBRAINS_ANNOTATIONS_NOT_NULL)
        || typeName.equals(JAVAX_VALIDATION_CONSTRAINTS_NOT_NULL)
        || typeName.equals(JAKARTA_VALIDATION_CONSTRAINTS_NOT_NULL);
  }
}
