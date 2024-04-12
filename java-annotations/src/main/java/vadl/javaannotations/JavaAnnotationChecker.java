package vadl.javaannotations;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * Provides functionality to check if a given class element meets specific method specifications
 * for an java annotation. This class checks the presence and signatures of methods required by
 * an annotation on a class element, throwing exceptions if the expectations are not met.
 */
public class JavaAnnotationChecker {

  private final Class<?> annoClass;
  private final List<MethodSpec> methodSpecs;

  public <T extends Annotation> JavaAnnotationChecker(Class<T> annoClass, MethodSpec... methods) {
    this.annoClass = annoClass;
    this.methodSpecs = Arrays.stream(methods).toList();
  }


  @SuppressWarnings("unchecked")
  public <T extends Annotation> Class<T> getAnnoClass() {
    return (Class<T>) annoClass;
  }

  /**
   * Checks if the given annotation instance can be checked by this checker.
   *
   * @param annotation The annotation instance to check.
   * @return {@code true} if this checker is applicable to the given annotation.
   */
  public boolean canCheck(Annotation annotation) {
    return this.annoClass.isInstance(annotation);
  }

  /**
   * Validates that the specified class element meets the method specifications defined for
   * the annotation this checker is concerned with. It throws an {@link IllegalStateException}
   * if the class does not meet the specifications.
   *
   * @param clazz The class element to check.
   * @throws IllegalStateException if the class element does not meet the method specifications.
   */
  public void check(Element clazz) {
    var methods = clazz.getEnclosedElements().stream()
        .filter(e -> e.getKind() == ElementKind.METHOD)
        .toList();

    for (var spec : methodSpecs) {
      if (!checkMethod(spec, methods)) {
        throw new IllegalStateException(
            String.format("Class `%s` has @%s fields but no `%s` method of type `%s`.%s",
                clazz,
                annoClass.getSimpleName(),
                spec.methodName,
                spec.typeString,
                spec.hint != null ? "\n\nNote: " + spec.hint : ""
            ));
      }
    }
  }

  /**
   * Checks if any method in the provided list matches the given method specification.
   */
  private <M extends Element> boolean checkMethod(
      MethodSpec methodSpec, List<M> methods) {
    return methods.stream().anyMatch(methodSpec::check);
  }


}
