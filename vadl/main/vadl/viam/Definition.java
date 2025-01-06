package vadl.viam;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.InlineMe;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.jetbrains.annotations.Contract;
import vadl.utils.SourceLocation;
import vadl.utils.WithSourceLocation;
import vadl.viam.graph.Graph;

/**
 * An abstract VADL Definition, such as an {@link Instruction}, {@link Format}, {@link Encoding},...
 *
 * <p>
 * A definition is identified by an Identifier and may have a SourceLocation associated with it.
 * As creator of a definition, the source location should be set where possible.
 * Use the {@link #ensure(boolean, String, Object...)} method to check for conditions that
 * must be met. It will throw an error with the definition as context if the condition is not true.
 * </p>
 */
public abstract class Definition implements WithSourceLocation {

  public final Identifier identifier;
  private SourceLocation sourceLocation = SourceLocation.INVALID_SOURCE_LOCATION;

  private final Map<Class<? extends Annotation>, Annotation> annotations;

  public Definition(Identifier identifier) {
    this.identifier = identifier;
    this.annotations = new HashMap<>();
  }

  @Override
  public SourceLocation sourceLocation() {
    return sourceLocation;
  }

  public void setSourceLocation(SourceLocation sourceLocation) {
    this.sourceLocation = sourceLocation;
  }


  public String simpleName() {
    return identifier.simpleName();
  }

  /**
   * This is only used because some templates rely on it.
   *
   * @deprecated use {@link #simpleName()} instead
   */
  @Deprecated
  @InlineMe(replacement = "identifier.simpleName()")
  public final String name() {
    return identifier.simpleName();
  }

  /**
   * Verifies the definition's state and properties.
   *
   * <p>This method should be overridden by all definitions that require some
   * verification. The implementation should ensure that thinks like type consistency and other
   * properties are given. If the definition contains a behavior,
   * it should call {@link Graph#verify()} in its own verify method.
   * It is called by the {@link vadl.viam.passes.verification.ViamVerifier} during the
   * {@link vadl.viam.passes.verification.ViamVerificationPass}.</p>
   *
   * @see vadl.viam.passes.verification.ViamVerificationPass
   * @see vadl.viam.passes.verification.ViamVerifier
   */
  public void verify() {
    for (Annotation<?> annotation : annotations.values()) {
      // verify all annotations of the definition
      annotation.verify();
    }
  }

  /**
   * Ensures that the condition is true. Otherwise, it will throw an error with the
   * definition's context.
   */
  @FormatMethod
  @Contract("false, _, _-> fail")
  public void ensure(boolean condition, String message, @Nullable Object... args) {
    if (!condition) {
      throw new ViamError(message.formatted(args))
          .shrinkStacktrace(1)
          .addContext(this);
    }
  }

  public abstract void accept(DefinitionVisitor visitor);

  /**
   * Adds an annotation to this definition, ensuring that there is no existing
   * annotation of the same type.
   * If the operation succeeds, the annotation's definition is set to this instance.
   *
   * @param annotation the annotation to be added
   */
  public <T extends Definition> void addAnnotation(Annotation<T> annotation) {
    var clazz = annotation.getClass();
    ensure(!annotations.containsKey(clazz),
        "Expected no annotation of type %s", clazz);
    ensure(annotation.parentDefinitionClass().isInstance(this),
        "Annotation is incompatible with definition. Annotation can be assigned to %s",
        annotation.parentDefinitionClass());
    //noinspection unchecked
    annotation.setParentDefinition((T) this);
    annotations.put(clazz, annotation);
  }

  /**
   * Retrieves the annotation of the specified type if it exists.
   *
   * @param <T>             the type of the annotation
   * @param annotationClass the class object corresponding to the annotation type
   * @return the annotation of the specified type, or null if it does not exist
   */
  @Nullable
  public <T extends Annotation<?>> T annotation(Class<T> annotationClass) {
    var anno = annotations.get(annotationClass);
    ensure(annotationClass.isInstance(anno), "Expected annotation of type %s, but found %s",
        annotationClass, anno);
    return (T) anno;
  }

  /**
   * Retrieves the annotation of the specified type and ensures that it exists.
   *
   * @param <T>             the type of the annotation
   * @param annotationClass the class object corresponding to the annotation type
   * @return the annotation of the specified type if it exists
   */
  public <T extends Annotation<?>> T expectAnnotation(Class<T> annotationClass) {
    var anno = annotation(annotationClass);
    ensure(anno != null, "Expected annotation of type %s, but found none", annotationClass);
    return anno;
  }

  /**
   * Checks if the definition has an annotation of the specified type.
   *
   * @param annotationClass the class object corresponding to the annotation type
   * @return true if the annotation of the specified type exists, false otherwise
   */
  public boolean hasAnnotation(Class<? extends Annotation<?>> annotationClass) {
    return annotations.containsKey(annotationClass);
  }

}
