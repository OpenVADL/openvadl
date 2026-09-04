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

package vadl.ast;

import static vadl.error.Diagnostic.error;

import com.google.errorprone.annotations.concurrent.LazyInit;
import vadl.ast.nodes.AnnotationDefinition;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;

/**
 * An Annotation in Vadl keeps state and knows how to resolve and type check itself. Further checks
 * can be defined on the {@link AnnotationGroupProvider} and who also knows how to apply the
 * annotation to the VIAM.
 *
 * <p>Every Annotation also has a group it belongs to, though it might be the only annotation in
 * the group.
 */
public abstract class Annotation implements AnnotationDeclaration, WithLocation {
  @LazyInit
  String name;

  @LazyInit
  AnnotationGroupProvider groupProvider;

  @LazyInit
  AnnotationDefinition definition;

  protected boolean allowMultiple;

  public Annotation() {
  }

  public Annotation(boolean allowMultiple) {
    this.allowMultiple = allowMultiple;
  }

  /**
   * Called by the symbol resolver to resolve the subparts of the annotation.
   *
   * <p>Can be overwritten by subclasses to specify additional
   * annotation-specific behavior to execute during name-resolution.
   *
   * <p>In general, subclasses overwriting this method should call
   * {@code super.resolveName} so that names in the annotation's values are
   * resolved. If this is explicitly *not* desired, the {@code super} call must
   * be omitted.
   *
   * @param definition The `AnnotationDefinition` corresponding to this `Annotation`.
   * @param resolver The active name resolver.
   */
  void resolveName(AnnotationDefinition definition, SymbolTable.SymbolResolver resolver) {
    definition.values.forEach(value -> value.accept(resolver));
  }

  /**
   * Called by the type checker to type check the annotation.
   *
   * <p>Can be overwritten by subclasses to specify additional
   * annotation-specific behavior to execute during typechecking.
   *
   * <p>In general, subclasses overwriting this method should call
   * {@code super.typeCheck} so that the annotation's values are also
   * typechecked. If this is explicitly *not* desired, the {@code super} call
   * must be omitted.
   *
   * @param definition The `AnnotationDefinition` corresponding to this `Annotation`.
   * @param typeChecker the active typechecker.
   */
  void typeCheck(AnnotationDefinition definition, TypeChecker typeChecker) {
    definition.values.forEach(typeChecker::check);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public SourceLocation location() {
    return definition.location();
  }

  protected static void verifyValuesCntBetween(AnnotationDefinition definition, int min, int max) {
    if (definition.values.size() < min || definition.values.size() > max) {
      throw error("Invalid annotation arguments", definition)
          .locationDescription(definition, "Expected between %d and %d arguments but got %d", min,
              max,
              definition.values.size())
          .build();
    }
  }

  protected static void verifyValuesCnt(AnnotationDefinition definition, int cnt) {
    if (definition.values.size() != cnt) {
      throw error("Invalid annotation arguments", definition)
          .locationDescription(definition, "Expected %d arguments but got %d", cnt,
              definition.values.size())
          .build();
    }
  }

  protected static void verifyValuesNonEmpty(AnnotationDefinition definition) {
    if (definition.values.isEmpty()) {
      throw error("Invalid annotation arguments", definition)
          .locationDescription(definition, "Expected at leat one argument but got none")
          .build();
    }
  }

  protected boolean allowMultiple() {
    return false;
  }
}
