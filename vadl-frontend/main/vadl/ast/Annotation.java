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
 * A Annotation in Vadl keeps state and knows how to resolve and type check itself. Further checks
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
   * Called by the symbol resolver to resolve parts of the annotation.
   *
   * @param definition to be resolved.
   * @param resolver   who resolves the annotation.
   */
  abstract void resolveName(AnnotationDefinition definition, SymbolTable.SymbolResolver resolver);

  /**
   * Called by the type checker to type check the annotation.
   *
   * @param definition  to be type checked.
   * @param typeChecker who type checks the annotation.
   */
  abstract void typeCheck(AnnotationDefinition definition, TypeChecker typeChecker);

  @Override
  public String name() {
    return name;
  }

  @Override
  public SourceLocation location() {
    return definition.location();
  }

  protected void verifyValuesCntBetween(AnnotationDefinition definition, int min, int max) {
    if (definition.values.size() < min || definition.values.size() > max) {
      throw error("Invalid annotation arguments", definition)
          .locationDescription(definition, "Expected between %d and %d arguments but got %d", min,
              max,
              definition.values.size())
          .build();
    }
  }

  protected void verifyValuesCnt(AnnotationDefinition definition, int cnt) {
    if (definition.values.size() != cnt) {
      throw error("Invalid annotation arguments", definition)
          .locationDescription(definition, "Expected %d arguments but got %d", cnt,
              definition.values.size())
          .build();
    }
  }

  protected void verifyValuesNonEmpty(AnnotationDefinition definition) {
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
