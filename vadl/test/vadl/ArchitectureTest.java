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

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;
import vadl.ast.AnnotationTable;

public class ArchitectureTest {
  @Test
  void shouldComplyWithArchitectureDesign() {
    JavaClasses jc = new ClassFileImporter()
        .withImportOption(
            new ImportOption.DoNotIncludeTests())
        .importPackages("vadl")
        .that(are(not(equivalentTo(AnnotationTable.class))));

    Architectures.LayeredArchitecture layeredArchitecture = layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .layer("Ast").definedBy("..ast..")
        .layer("Viam").definedBy("..viam..")
        .layer("Gcb").definedBy("..gcb..")
        .layer("Lcb").definedBy("..lcb..")
        .layer("CppGen").definedBy("..cppCodeGen..")
        .layer("Rtl").definedBy("..rtl..")
        .whereLayer("Ast").mayOnlyAccessLayers("Viam")
        .whereLayer("Viam").mayOnlyAccessLayers("Ast", "CppGen")
        .whereLayer("Gcb").mayOnlyAccessLayers("Viam", "CppGen")
        .whereLayer("CppGen").mayOnlyAccessLayers("Viam", "Gcb")
        .whereLayer("Lcb").mayOnlyAccessLayers("Gcb", "Viam", "CppGen")
        .whereLayer("Rtl").mayOnlyAccessLayers("Viam");

    layeredArchitecture.check(jc);
  }

  @Test
  void shouldNotUseSystemOut() {
    JavaClasses jc = new ClassFileImporter()
        .withImportOption(new ImportOption.DoNotIncludeTests())
        .importPackages("vadl");

    NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
        .check(jc);
  }

}
