package vadl;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;

@AnalyzeClasses(importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
public class ArchitectureTest {
  //  @Test
  void shouldComplyWithArchitectureDesign() {
    JavaClasses jc = new ClassFileImporter()
        .importPackages("vadl");
    Architectures.LayeredArchitecture layeredArchitecture = layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .layer("Ast").definedBy("..ast..")
        .layer("Viam").definedBy("..viam..")
        .layer("Gcb").definedBy("..gcb..")
        .layer("Lcb").definedBy("..lcb..")
        .layer("CppGen").definedBy("..cppCodeGen..")
        .whereLayer("Ast").mayOnlyAccessLayers("Viam")
        .whereLayer("Viam").mayOnlyAccessLayers("Ast", "CppGen")
        .whereLayer("Gcb").mayOnlyAccessLayers("Viam", "CppGen")
        .whereLayer("CppGen").mayOnlyAccessLayers("Viam")
        .whereLayer("Lcb").mayOnlyAccessLayers("Gcb", "Viam", "CppGen");

    layeredArchitecture.check(jc);
  }
}
