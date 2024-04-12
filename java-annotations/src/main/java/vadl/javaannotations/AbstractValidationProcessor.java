package vadl.javaannotations;


import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;


/**
 * A {@link AbstractProcessor} for validating java annotations related to the VADL project.
 * It checks if classes annotated with specific annotations conform to required
 * method specifications.
 *
 * <p>This processor is designed to run with source versions up to {@code 21}.
 * It uses predefined {@link JavaAnnotationChecker}s to validate that annotated classes
 * have the correct methods defined as per the specifications of each annotation.</p>
 */
public abstract class AbstractValidationProcessor extends AbstractProcessor {

  /**
   * Returns the {@link JavaAnnotationChecker}s that should be applied by the processor.
   */
  public abstract List<JavaAnnotationChecker> checkers();

  /**
   * Processes a set of annotation types on elements in the round environment.
   * It iterates over each {@link JavaAnnotationChecker} and collects classes annotated with
   * the corresponding annotation. Then, it validates these classes against the specifications
   * defined in the {@code AnnotationChecker}.
   */
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    // go through all annotation checkers
    for (JavaAnnotationChecker checker : checkers()) {
      var fields = roundEnv.getElementsAnnotatedWith(checker.getAnnoClass());
      var classToFieldsMap = fields.stream()
          .collect(Collectors.groupingBy(
              Element::getEnclosingElement,
              Collectors.toList()
          ));


      // check for all collected classes if they confirm the annotation validation
      for (var entry : classToFieldsMap.entrySet()) {
        try {
          checker.check(entry.getKey());
        } catch (Throwable t) {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, t.getLocalizedMessage(),
              entry.getValue().get(0));
        }
      }
    }

    return false;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_17;
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return checkers().stream()
        .map(e -> e.getAnnoClass().getCanonicalName())
        .collect(Collectors.toSet());
  }
}
