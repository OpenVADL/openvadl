package vadl.annotations;

import java.util.function.Function;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * Represents a specification for a method, encapsulating method name, type signature,
 * an optional hint, and an optional further check represented as a {@link Function}.
 * This class is designed to facilitate method matching and validation based on predefined criteria.
 */
public class MethodSpec {
  String methodName;
  String typeString;

  /**
   * An optional hint message providing additional information about the method.
   */
  String hint;

  /**
   * An optional {@link Function} for further validation of the method beyond its name and type.
   */
  Function<Element, Boolean> furtherCheck;

  public MethodSpec(String methodName, String typeString) {
    this.methodName = methodName;
    this.typeString = typeString;
  }

  public MethodSpec(String methodName, String typeString, Function<Element, Boolean> furtherCheck) {
    this.methodName = methodName;
    this.typeString = typeString;
    this.furtherCheck = furtherCheck;
  }

  /**
   * Sets a formatted hint message for this {@code MethodSpec}. The format and arguments
   * are used similarly to {@link String#format(String, Object...)}.
   */
  public MethodSpec setHint(String format, Object... args) {
    hint = String.format(format, args);
    return this;
  }

  /**
   * Checks if the given {@link Element} matches this {@code MethodSpec}.
   * The check is based on the method's kind, name, and type signature.
   * If a further check is defined, it is also applied.
   *
   * @param element The {@link Element} to check against this {@code MethodSpec}.
   * @return {@code true} if the element matches this {@code MethodSpec}, {@code false} otherwise.
   */
  public boolean check(Element element) {
    return element.getKind().equals(ElementKind.METHOD) &&
        element.getSimpleName().toString().equals(methodName) &&
        element.asType().toString().equals(typeString) &&
        (furtherCheck == null || furtherCheck.apply(element));
  }

}
