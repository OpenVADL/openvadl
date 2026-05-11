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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "vadl")
public class AnnotationRuleTest {
  public static final String ORG_JETBRAINS_ANNOTATIONS_NOT_NULL =
      "org.jetbrains.annotations.NotNull";

  public static final String JAVAX_ANNOTATION_CHECK_FOR_NULL =
      "javax.annotation.CheckForNull";

  public static final String JAVAX_VALIDATION_CONSTRAINTS_NOT_NULL =
      "javax.validation.constraints.NotNull";
  public static final String JAKARTA_VALIDATION_CONSTRAINTS_NOT_NULL =
      "jakarta.validation.constraints.NotNull";
  @ArchTest
  static final ArchRule noNotNullAnnotationsForClasses = // Intellij shows not used, but it is
      noClasses().should().beAnnotatedWith(ORG_JETBRAINS_ANNOTATIONS_NOT_NULL)
          .orShould().beAnnotatedWith(JAVAX_ANNOTATION_CHECK_FOR_NULL)
          .orShould().beAnnotatedWith(JAVAX_VALIDATION_CONSTRAINTS_NOT_NULL)
          .orShould().beAnnotatedWith(JAKARTA_VALIDATION_CONSTRAINTS_NOT_NULL);
  @ArchTest
  static final ArchRule noNotNullAnnotationsForMethods = // Intellij shows not used, but it is
      noMethods().that().areDeclaredInClassesThat().areNotAnnotatedWith("kotlin.Metadata")
          .should().beAnnotatedWith(ORG_JETBRAINS_ANNOTATIONS_NOT_NULL)
          .orShould().beAnnotatedWith(JAVAX_ANNOTATION_CHECK_FOR_NULL)
          .orShould().beAnnotatedWith(JAVAX_VALIDATION_CONSTRAINTS_NOT_NULL)
          .orShould().beAnnotatedWith(JAKARTA_VALIDATION_CONSTRAINTS_NOT_NULL);


}
