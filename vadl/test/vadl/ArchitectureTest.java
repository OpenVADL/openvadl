package vadl;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;

public class ArchitectureTest {
  @Test
  void shouldComplyWithArchitectureDesign() {
    JavaClasses jc = new ClassFileImporter()
        .importPackages("vadl");
    Architectures.LayeredArchitecture layeredArchitecture = layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .layer("Ast").definedBy("..ast..")
        .layer("Viam").definedBy("..viam..")
        .layer("Gcb").definedBy("..gcb..")
        .layer("Lcb").definedBy("..lcb..")
        .whereLayer("Ast").mayOnlyAccessLayers("Viam")
        .whereLayer("Viam").mayOnlyAccessLayers("Ast")
        .whereLayer("Gcb").mayOnlyAccessLayers("Viam")
        .whereLayer("Lcb").mayOnlyAccessLayers("Gcb", "Viam");

    layeredArchitecture.check(jc);
  }
}
