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

package vadl.viam;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.InlineMe;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.jetbrains.annotations.Contract;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;
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
 *
 * <p>All definitions may have {@link Annotation} attached to it.</p>
 *
 * <p>All definitions may have {@link DefinitionExtension} attached to it.
 * Those definition extensions are added by passes that want to directly associate
 * information to specific definitions. </p>
 */
public abstract class Definition implements WithLocation {

  public final Identifier identifier;
  private SourceLocation sourceLocation = SourceLocation.INVALID_SOURCE_LOCATION;

  @Nullable
  private Supplier<String> prettyPrintSourceFunc;

  // lazily constructed, as most definitions don't have annotations
  @SuppressWarnings("rawtypes")
  @Nullable
  private Map<Class<? extends Annotation>, Annotation> annotations;
  // lazily constructed, as most definitions don't have extensions
  @SuppressWarnings("rawtypes")
  @Nullable
  private Map<Class<? extends DefinitionExtension>, DefinitionExtension> extensions;

  public Definition(Identifier identifier) {
    this.identifier = identifier;
  }

  @Override
  public SourceLocation location() {
    return sourceLocation;
  }

  public void setSourceLocation(SourceLocation sourceLocation) {
    this.sourceLocation = sourceLocation;
  }

  /**
   * Sets the source location of the node if it wasn't already set.
   */
  public void setSourceLocationIfNotSet(SourceLocation sourceLocation) {
    if (this.sourceLocation.equals(SourceLocation.INVALID_SOURCE_LOCATION)) {
      this.sourceLocation = sourceLocation;
    }
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
    if (annotations == null) {
      return;
    }
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
   * Sets a function that will be called to prettyprint the source code that generated this
   * definition.
   *
   * @param prettyPrintSourceFunc that will be called.
   */
  public void setPrettyPrintSourceFunc(@Nullable Supplier<String> prettyPrintSourceFunc) {
    this.prettyPrintSourceFunc = prettyPrintSourceFunc;
  }

  /**
   * Pretty print the source code snippet that is responsible for this definition.
   *
   * @return the string if the prettyPrintSourceFunc is set, otherwise null.
   */
  @Nullable
  public String prettyPrintSource() {
    if (prettyPrintSourceFunc == null) {
      return null;
    }

    return prettyPrintSourceFunc.get();
  }

  // lazy construction of annotation map
  @SuppressWarnings("rawtypes")
  private Map<Class<? extends Annotation>, Annotation> getAnnotations() {
    if (annotations == null) {
      annotations = new HashMap<>();
    }
    return annotations;
  }

  // lazy construction of extension map
  @SuppressWarnings("rawtypes")
  private Map<Class<? extends DefinitionExtension>, DefinitionExtension> getExtensions() {
    if (extensions == null) {
      extensions = new HashMap<>();
    }
    return extensions;
  }

  /**
   * Adds an annotation to this definition, ensuring that there is no existing
   * annotation of the same type.
   * If the operation succeeds, the annotation's definition is set to this instance.
   *
   * @param annotation the annotation to be added
   */
  public <T extends Definition> void addAnnotation(Annotation<T> annotation) {
    var clazz = annotation.getClass();
    var annotations = getAnnotations();
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
    var anno = getAnnotations().get(annotationClass);
    if (anno == null) {
      return null;
    }
    ensure(annotationClass.isInstance(anno), "Expected annotation of type %s, but found %s",
        annotationClass, anno);
    //noinspection unchecked
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
    return getAnnotations().containsKey(annotationClass);
  }

  /**
   * Adds an extension to this definition, ensuring that there is no existing
   * extension of the same type.
   * If the operation succeeds, the extension's definition is set to this instance.
   *
   * @param extension to be added
   */
  public <T extends Definition> void attachExtension(DefinitionExtension<T> extension) {
    var clazz = extension.getClass();
    var extensions = getExtensions();
    ensure(!extensions.containsKey(clazz),
        "Extension of type %s does already exist on this definition", clazz);
    ensure(extension.extendsDefClass().isInstance(this),
        "Extension is incompatible with definition. Extension can be added to %s",
        extension.extendsDefClass());
    //noinspection unchecked
    extension.setExtendingDefinition((T) this);
    extensions.put(clazz, extension);
  }

  /**
   * Retrieves the extension of the specified type if it exists.
   *
   * @param <T>            the type of the extension
   * @param extensionClass the class object corresponding to the extension type
   * @return the extension of the specified type, or null if it does not exist
   */
  @Nullable
  public <T extends DefinitionExtension<?>> T extension(Class<T> extensionClass) {
    var anno = getExtensions().get(extensionClass);
    if (anno == null) {
      return null;
    }
    //noinspection unchecked
    return (T) anno;
  }

  /**
   * Retrieves the extension of the specified type and ensures that it exists.
   *
   * @param <T>            the type of the extension
   * @param extensionClass the class object corresponding to the extension type
   * @return the extension of the specified type
   */
  public <T extends DefinitionExtension<?>> T expectExtension(Class<T> extensionClass) {
    var anno = extension(extensionClass);
    ensure(anno != null, "Expected extension of type %s, but found none", extensionClass);
    return anno;
  }

  /**
   * Checks if the definition has an extension of the specified type.
   *
   * @param extensionClass the class object corresponding to the extension type
   * @return true if the extension of the specified type exists, false otherwise
   */
  public boolean hasExtension(Class<? extends DefinitionExtension<?>> extensionClass) {
    return getExtensions().containsKey(extensionClass);
  }

}
