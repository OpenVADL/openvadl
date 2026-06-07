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

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;

@SuppressWarnings("MissingJavadocType")
public abstract class Definition extends Node {
  @Child
  List<AnnotationDefinition> annotations = new ArrayList<>();

  @LazyInit
  List<String> viamId;

  Definition withAnnotations(List<AnnotationDefinition> annotations) {
    this.annotations = annotations;
    for (var annotation : annotations) {
      annotation.target = this;
    }
    return this;
  }

  void prettyPrintAnnotations(int indent, StringBuilder builder) {
    annotations.forEach(annotation -> annotation.prettyPrint(indent, builder));
  }

  @Nullable
  <T extends Annotation> T getAnnotation(String name, Class<T> annotationClass) {
    return annotations.stream()
        .map(a -> a.annotation)
        .filter(Objects::nonNull)
        .filter(annotationClass::isInstance)
        .filter(a -> a.name().equals(name))
        .map(annotationClass::cast).findFirst().orElse(null);
  }

  static void prettyPrintDefinitions(int indent, StringBuilder builder,
                                     List<Definition> definitions) {
    Definition previousDefinition = null;
    for (Definition definition : definitions) {
      if (previousDefinition != null
          && (!definition.getClass().equals(previousDefinition.getClass())
          || !definition.annotations.isEmpty())) {
        builder.append("\n");
      }
      definition.prettyPrint(indent, builder);
      previousDefinition = definition;
    }
  }

  @Nullable
  <T extends Annotation> T findAnnotation(String name, Class<T> annotationClass) {
    return annotations.stream()
        .filter(a -> a.name().equals(name))
        .map(a -> a.annotation)
        .filter(annotationClass::isInstance)
        .map(annotationClass::cast).findFirst().orElse(null);
  }

  abstract <R> R accept(DefinitionVisitor<R> visitor);
}
