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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.Contract;
import vadl.utils.SourceLocation;
import vadl.utils.WithSourceLocation;

/**
 * Represents an abstract annotation that can be associated with a definition.
 * This class provides mechanisms to ensure the correctness of its state and
 * the associated definition.
 */
public abstract class Annotation<T extends Definition> implements WithSourceLocation {

  @Nullable
  private T parentDefinition;
  private SourceLocation sourceLocation = SourceLocation.INVALID_SOURCE_LOCATION;

  /**
   * Returns the class of the definition associated with this annotation.
   *
   * @return the class object representing the type of the associated definition.
   */
  public abstract Class<T> parentDefinitionClass();

  public T parentDefinition() {
    ensure(parentDefinition != null, "Expected definition to be not null, but was null");
    return parentDefinition;
  }

  /**
   * Retrieves the parent definition and ensures it is of the specified class type.
   *
   * @param clazz the class object representing the expected type of the parent definition
   * @return the parent definition if it is of the specified class type
   * @throws ViamError if the parent definition is not of the specified class type
   */
  public T parentDefinition(Class<T> clazz) {
    var def = parentDefinition();
    ensure(clazz.isInstance(def), "Expected definition of type %s, but found %s", clazz, def);
    return def;
  }

  /**
   * Verifies the validity and correctness of the annotation with its associated definition.
   *
   * <p>This method should be overridden by concrete subclasses of Annotation to implement
   * specific verification logic. The default implementation does nothing.</p>
   *
   * <p>Subclasses should ensure that the state and properties of the annotation is valid.
   * Any inconsistencies or violations must be reported, typically by using the ensure
   * method to throw a ViamError with relevant context.</p>
   */
  public void verify() {

  }


  /**
   * Ensures that the condition is true. Otherwise, it will throw an error with the
   * definition's context.
   */
  @FormatMethod
  @Contract("false, _, _-> fail")
  public void ensure(boolean condition, String message, Object... args) {
    if (!condition) {
      var err = new ViamError(message.formatted(args))
          .shrinkStacktrace(1)
          .addContext(this);
      if (parentDefinition != null) {
        err.addContext(parentDefinition);
      } else {
        err.addContext("parentDefinition", "null");
      }
      throw err;
    }
  }

  @Override
  public final SourceLocation sourceLocation() {
    return sourceLocation;
  }

  public final void setSourceLocation(SourceLocation sourceLocation) {
    this.sourceLocation = sourceLocation;
  }

  /**
   * This is called by the {@link Definition#addAnnotation(Annotation)} method ONLY.
   */
  final void setParentDefinition(@Nonnull T definition) {
    ensure(this.parentDefinition == null, "Expected definition to be null, but was %s", definition);
    this.parentDefinition = definition;
  }
}
