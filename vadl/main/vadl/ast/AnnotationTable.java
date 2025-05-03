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

package vadl.ast;


import static java.util.Objects.requireNonNull;
import static vadl.error.Diagnostic.error;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.types.Type;
import vadl.utils.Pair;
import vadl.utils.functionInterfaces.TriConsumer;
import vadl.viam.AssemblyDescription;
import vadl.viam.Relocation;
import vadl.viam.annotations.AsmParserCaseSensitive;
import vadl.viam.annotations.AsmParserCommentString;

@SuppressWarnings("UnusedMethod")
class AnnotationTable {
  private static final Map<Class<? extends Definition>, Map<String, Supplier<Annotation>>>
      annotationFactories = new java.util.HashMap<>();

  static {
    on(AsmDescriptionDefinition.class)
        .add("caseSensitive", EnableAnnotation::new)
        .applyViam((def, annotation, lowering) -> {
          var asmDescription = (AssemblyDescription) def;
          var enableAnnotation = (EnableAnnotation) annotation;
          asmDescription.addAnnotation(new AsmParserCaseSensitive(enableAnnotation.isEnabled));
        })
        .build();

    on(AsmDescriptionDefinition.class)
        .add("commentString", StringAnnotation::new)
        .applyViam((def, annotation, lowering) -> {
          var asmDescription = (AssemblyDescription) def;
          var stringAnnotation = (StringAnnotation) annotation;
          asmDescription.addAnnotation(new AsmParserCommentString(stringAnnotation.value));
        })
        .build();

    groupOn(CounterDefinition.class)
        .add("current", EnableAnnotation::new)
        .add("next", EnableAnnotation::new)
        .add("next next", EnableAnnotation::new)
        .check((def, annotations, typeChecker) -> {
          verifyOnlyOneOfGroup(annotations);
        })
        // FIXME: Apply to AST
        .build();

    on(RegisterFileDefinition.class)
        .add("zero", ExprAnnotation::new)
        // FIXME: Typecheck
        // FIXME: Apply to VIAM
        .build();

    groupOn(RelocationDefinition.class)
        .add("globalOffset", EnableAnnotation::new)
        .add("relative", EnableAnnotation::new)
        .add("absolute", EnableAnnotation::new)
        .check((def, annotations, typeChecker) -> {
          verifyOnlyOneOfGroup(annotations);
        })
        .applyViam((def, annotations, lowering) -> {
          var mappings = Map.of(
              "globalOffset", Relocation.Kind.GLOBAL_OFFSET_TABLE,
              "relative", Relocation.Kind.RELATIVE,
              "absolute", Relocation.Kind.ABSOLUTE
          );

          var annotation = annotations.getFirst();
          var relocation = (Relocation) def;
          relocation.setKind(requireNonNull(mappings.get(annotation.name)));
        })
        .build();

  }


  /**
   * Creates an annotation from the given AST definition.
   *
   * @param definition for which the annotation should be created.
   * @return the annotation or null if no such annotation exists.
   */
  @Nullable
  static Annotation createAnnotation(AnnotationDefinition definition) {
    if (!annotationFactories.containsKey(definition.target.getClass())) {
      return null;
    }
    var annotationFactory =
        annotationFactories.get(definition.target.getClass()).get(definition.name());
    if (annotationFactory == null) {
      return null;
    }

    var annotation = annotationFactory.get();
    annotation.definition = definition;
    return annotation;
  }


  /**
   * Verifies that only one annotation exists in the group. If more than one annotation is
   * present an error is thrown.
   *
   * @param annotations a list of {@link Annotation} objects to verify.
   * @throws vadl.error.Diagnostic if more than one annotation is present in the group.
   */
  static void verifyOnlyOneOfGroup(List<Annotation> annotations) {
    if (annotations.size() > 1) {
      var diagnostic = error("Annotation clash", annotations.getFirst().definition)
          .locationDescription(annotations.getFirst().definition, "First defined here");
      for (int i = 1; i < annotations.size(); i++) {
        diagnostic.locationDescription(annotations.get(i).definition,
            "Conflicting defined here");
      }
      diagnostic.description("Only one of these annotations can be defined.");
      throw diagnostic.build();
    }
  }

  /**
   * Groups annotations from the provided definition into a map where the key is
   * the {@link AnnotationGroupProvider} and the value is a list of {@link Annotation}
   * objects belonging to that group.
   *
   * @param definition the {@link Definition} containing annotations to be grouped.
   * @return a map with {@link AnnotationGroupProvider} as keys and lists of {@link Annotation}
   *     objects as values grouped by their provider.
   */
  static Map<AnnotationGroupProvider, List<Annotation>> groupings(
      Definition definition) {
    return groupings(definition.annotations.stream().map(d -> d.annotation).toList());
  }

  private static Map<AnnotationGroupProvider, List<Annotation>> groupings(
      List<Annotation> annotations) {
    return annotations.stream().collect(Collectors.groupingBy(a -> a.groupProvider));
  }

  /**
   * Create a new annotation builder for a given {@link Definition} class.
   *
   * @param klass to which the annotation is bound.
   * @return a new annotation builder for the given class.
   */
  static AnnotationBuilder on(Class<? extends Definition> klass) {
    return new AnnotationBuilder(klass);
  }

  /**
   * Create a new annotation group builder for a given {@link Definition} class.
   *
   * @param klass to which the annotations are bound.
   * @return a new annotation builder for the given class.
   */
  static GroupedAnnotationBuilder groupOn(Class<? extends Definition> klass) {
    return new GroupedAnnotationBuilder(klass);
  }

  private static class AnnotationBuilder {
    private final Class<? extends Definition> targetClass;

    @Nullable
    private String name;

    @Nullable
    private Supplier<Annotation> annotationFactory;

    @Nullable
    private TriConsumer<Definition, Annotation, TypeChecker> checkCallback;

    @Nullable
    private BiConsumer<Definition, Annotation> applyAstCallback;

    @Nullable
    private TriConsumer<vadl.viam.Definition, Annotation, ViamLowering> applyViamCallback;


    AnnotationBuilder(Class<? extends Definition> targetClass) {
      this.targetClass = targetClass;
    }

    /**
     * Specifies an annotation name and an annotation factory.
     *
     * <p>The factory doesn't have to set the name, groupProvider or definition. These fiedls will
     * be set by the builder.
     *
     * @param name              of the annotation.
     * @param annotationFactory that creates the annotation.
     * @return itself.
     */
    AnnotationBuilder add(String name, Supplier<Annotation> annotationFactory) {
      if (this.name != null) {
        throw new IllegalStateException("Annotation name already set");
      }
      this.name = name;
      this.annotationFactory = annotationFactory;
      return this;
    }

    /**
     * Adds a check for arbitrary constraints on the annotation. The check is executed after
     * in the typechecker after the annotations itself have been checked and the definition they
     * are annotating. The check throws an {@link vadl.error.Diagnostic} if the check fails.
     *
     * @param checkCallback to be executed.
     * @return itself.
     */
    AnnotationBuilder check(TriConsumer<Definition, Annotation, TypeChecker> checkCallback) {
      if (this.checkCallback != null) {
        throw new IllegalStateException("Check callback already set");
      }
      this.checkCallback = checkCallback;
      return this;
    }

    /**
     * Add the steps of how the annotation will be applied to the Ast.
     * This should not throw anything since the check should do verification but it will
     * work if it does throw an exception.
     *
     * @param applyCallback to be executed to add the annotation to the VIAM.
     * @return itself.
     */
    AnnotationBuilder applyAst(BiConsumer<Definition, Annotation> applyCallback) {
      if (this.applyAstCallback != null) {
        throw new IllegalStateException("Apply callback already set");
      }
      this.applyAstCallback = applyCallback;
      return this;
    }

    /**
     * Add the steps of how the annotation will be applied to the VIAM.
     * This can still throw {@link vadl.error.Diagnostic} if some checks on the viam fail.
     *
     * @param applyCallback to be executed to add the annotation to the VIAM.
     * @return itself.
     */
    AnnotationBuilder applyViam(
        TriConsumer<vadl.viam.Definition, Annotation, ViamLowering> applyCallback) {
      if (this.applyViamCallback != null) {
        throw new IllegalStateException("Apply callback already set");
      }
      this.applyViamCallback = applyCallback;
      return this;
    }

    /**
     * Inserts the annotation into the annotationFactories table.
     */
    void build() {
      if (name == null || annotationFactory == null) {
        throw new IllegalStateException("Not all required are fields set");
      }

      TriConsumer<Definition, List<Annotation>, TypeChecker> groupCheckCallback;
      if (checkCallback != null) {
        groupCheckCallback = (definition, annotations, typeChecker) -> {
          requireNonNull(checkCallback).accept(definition, annotations.getFirst(), typeChecker);
        };
      } else {
        groupCheckCallback = (definition, annotations, typeChecker) -> {
        };
      }

      BiConsumer<Definition, List<Annotation>> groupApplyAstCallback;
      if (applyAstCallback != null) {
        groupApplyAstCallback = (definition, annotations) -> {
          requireNonNull(applyAstCallback).accept(definition, annotations.getFirst());
        };
      } else {
        groupApplyAstCallback = (definition, annotations) -> {
        };
      }

      TriConsumer<vadl.viam.Definition, List<Annotation>, ViamLowering> groupApplyViamCallback;
      if (applyViamCallback != null) {
        groupApplyViamCallback = (definition, annotations, lowering
        ) -> {
          requireNonNull(applyViamCallback).accept(definition, annotations.getFirst(), lowering);
        };
      } else {
        groupApplyViamCallback = (definition, annotations, lowering) -> {
        };
      }

      // Create a group only for this single annotation
      var group = new AnnotationGroupProvider() {
        @Override
        public void check(Definition definition, List<Annotation> annotations,
                          TypeChecker typeChecker) {
          requireNonNull(groupCheckCallback).accept(definition, annotations, typeChecker);
        }

        @Override
        public void applyAst(Definition definition, List<Annotation> annotations) {
          requireNonNull(groupApplyAstCallback).accept(definition, annotations);
        }

        @Override
        public void applyViam(vadl.viam.Definition definition, List<Annotation> annotations,
                              ViamLowering lowering) {
          requireNonNull(groupApplyViamCallback).accept(definition, annotations, lowering);
        }
      };

      // Wrap the annotation factory in a lambda that sets the annotation name and group
      Supplier<Annotation> realAnnotationFactory = () -> {
        var annotation = requireNonNull(annotationFactory).get();
        annotation.name = requireNonNull(name);
        annotation.groupProvider = group;
        return annotation;
      };

      annotationFactories.computeIfAbsent(targetClass, k -> new java.util.HashMap<>());

      annotationFactories.get(targetClass).compute(name, (k, v) -> {
        if (v != null) {
          throw new IllegalStateException(
              "Annotation name '%s' already occupied by '%s'.".formatted(k, v));
        }
        return realAnnotationFactory;
      });
    }

  }

  private static class GroupedAnnotationBuilder {

    private Class<? extends Definition> targetClass;

    private List<Pair<String, Supplier<Annotation>>> namedFactories = new ArrayList<>();

    @Nullable
    private TriConsumer<Definition, List<Annotation>, TypeChecker> checkCallback;

    @Nullable
    private BiConsumer<Definition, List<Annotation>> applyAstCallback;

    @Nullable
    private TriConsumer<vadl.viam.Definition, List<Annotation>, ViamLowering> applyViamCallback;


    GroupedAnnotationBuilder(Class<? extends Definition> targetClass) {
      this.targetClass = targetClass;
    }

    /**
     * Specifies an annotation name and an annotation factory.
     *
     * <p>The factory doesn't have to set the name, groupProvider or definition. These fiedls will
     * be set by the builder.
     *
     * @param name              of the annotation.
     * @param annotationFactory that creates the annotation.
     * @return itself.
     */
    GroupedAnnotationBuilder add(String name, Supplier<Annotation> annotationFactory) {
      if (namedFactories.stream().anyMatch(p -> p.left().equals(name))) {
        throw new IllegalStateException("Annotation with the name %s already set".formatted(name));
      }
      this.namedFactories.add(new Pair<>(name, annotationFactory));
      return this;
    }

    /**
     * Adds a check for arbitrary constraints on the group of annotation. The check is executed
     * after in the typechecker after the annotations itself have been checked and the definition
     * they are annotating. The check throws an {@link vadl.error.Diagnostic} if the check fails.
     *
     * @param checkCallback to be executed.
     * @return itself.
     */
    GroupedAnnotationBuilder check(
        TriConsumer<Definition, List<Annotation>, TypeChecker> checkCallback) {
      if (this.checkCallback != null) {
        throw new IllegalStateException("Check callback already set");
      }
      this.checkCallback = checkCallback;
      return this;
    }

    /**
     * Add the steps of how the annotation group will be applied to the VIAM.
     * This can still throw {@link vadl.error.Diagnostic} if some checks on the viam fail.
     *
     * @param applyCallback to be executed to add the annotation to the VIAM.
     * @return itself.
     */
    GroupedAnnotationBuilder applyAst(
        BiConsumer<Definition, List<Annotation>> applyCallback) {
      if (this.applyAstCallback != null) {
        throw new IllegalStateException("Apply callback already set");
      }
      this.applyAstCallback = applyCallback;
      return this;
    }

    /**
     * Add the steps of how the annotation group will be applied to the VIAM.
     * This can still throw {@link vadl.error.Diagnostic} if some checks on the viam fail.
     *
     * @param applyCallback to be executed to add the annotation to the VIAM.
     * @return itself.
     */
    GroupedAnnotationBuilder applyViam(
        TriConsumer<vadl.viam.Definition, List<Annotation>, ViamLowering> applyCallback) {
      if (this.applyViamCallback != null) {
        throw new IllegalStateException("Apply callback already set");
      }
      this.applyViamCallback = applyCallback;
      return this;
    }

    /**
     * Inserts all annotation of the group into the annotationFactories table.
     */
    void build() {
      // FIXME: apply should be optional.
      if (namedFactories.isEmpty()) {
        throw new IllegalStateException("Not all required are fields set");
      }

      TriConsumer<Definition, List<Annotation>, TypeChecker> realCheckCallback;
      if (checkCallback == null) {
        realCheckCallback = (definition, annotations, typeChecker) -> {
        };
      } else {
        realCheckCallback = requireNonNull(checkCallback);
      }

      BiConsumer<Definition, List<Annotation>> realApplyAstCallback;
      if (applyAstCallback != null) {
        realApplyAstCallback = (definition, annotations) -> {
          requireNonNull(applyAstCallback).accept(definition, annotations);
        };
      } else {
        realApplyAstCallback = (definition, annotations) -> {
        };
      }

      TriConsumer<vadl.viam.Definition, List<Annotation>, ViamLowering> realApplyViamCallback;
      if (applyViamCallback != null) {
        realApplyViamCallback = (definition, annotations, lowering
        ) -> {
          requireNonNull(applyViamCallback).accept(definition, annotations, lowering);
        };
      } else {
        realApplyViamCallback = (definition, annotations, lowering) -> {
        };
      }


      // Create a group
      var group = new AnnotationGroupProvider() {
        @Override
        public void check(Definition definition, List<Annotation> annotations,
                          TypeChecker typeChecker) {
          realCheckCallback.accept(definition, annotations, typeChecker);
        }

        @Override
        public void applyAst(Definition definition, List<Annotation> annotations) {
          realApplyAstCallback.accept(definition, annotations);
        }

        @Override
        public void applyViam(vadl.viam.Definition definition, List<Annotation> annotations,
                              ViamLowering lowering) {
          realApplyViamCallback.accept(definition, annotations, lowering);
        }
      };

      annotationFactories.computeIfAbsent(targetClass, k -> new java.util.HashMap<>());

      for (var pair : namedFactories) {
        var name = requireNonNull(pair.left());
        var annotationFactory = requireNonNull(pair.right());

        // Wrap the annotation factory in a lambda that sets the annotation name and group
        Supplier<Annotation> realAnnotationFactory = () -> {
          var annotation = requireNonNull(annotationFactory).get();
          annotation.name = requireNonNull(name);
          annotation.groupProvider = group;
          return annotation;
        };

        annotationFactories.get(targetClass).put(name, realAnnotationFactory);
      }
    }
  }
}

interface AnnotationGroupProvider {
  void check(Definition definition, List<Annotation> annotations, TypeChecker typeChecker);

  void applyAst(Definition definition, List<Annotation> annotations);

  void applyViam(vadl.viam.Definition definition, List<Annotation> annotations,
                 ViamLowering lowering);
}

/**
 * A Annotation in Vadl keeps state and knows how to resolve and type check itself. Further checks
 * can be defined on the {@link AnnotationGroupProvider} and who also knows how to apply the
 * annotation to the VIAM.
 *
 * <p>Every Annotation also has a group it belongs to, though it might be the only annotation in
 * the group.
 */
abstract class Annotation {
  @LazyInit
  String name;

  @LazyInit
  AnnotationGroupProvider groupProvider;

  @LazyInit
  AnnotationDefinition definition;

  public Annotation() {
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
}

/**
 * A simple annotation that just stores a boolean value, true by default but an optional argument
 * can be provided.
 *
 * <p>Examples for such annotations:
 * <pre>
 * [large]
 * [isThree : true]
 * [likesCoffee : 3 == 7]
 * constant flo = 3
 * </pre>
 */
class EnableAnnotation extends Annotation {
  boolean isEnabled = true;

  public EnableAnnotation() {
    super();
  }

  @Override
  void resolveName(AnnotationDefinition definition, SymbolTable.SymbolResolver resolver) {
    verifyValuesCntBetween(definition, 0, 1);
  }

  @Override
  void typeCheck(AnnotationDefinition definition, TypeChecker typeChecker) {

    // Only eval the argument if there is one
    if (definition.values.size() == 1) {
      var valueExpr = definition.values.getFirst();

      typeChecker.check(valueExpr);
      if (!valueExpr.type().equals(Type.bool())) {
        throw error("Enable annotation expects a boolean argument", valueExpr)
            .locationDescription(valueExpr, "Expected a boolean but got %s", valueExpr.type())
            .build();
      }

      isEnabled = typeChecker.constantEvaluator.eval(valueExpr).value().equals(BigInteger.ONE);
    }
  }
}

/**
 * A simple annotation that stores and evaluates a constant argument.
 *
 * <p>Examples for such annotations:
 * <pre>
 * [ alignment : 16 ]
 * stack pointer = X(1)
 * </pre>
 */
class ConstantAnnotation extends Annotation {
  @LazyInit
  ConstantValue constant;

  public ConstantAnnotation() {
    super();
  }

  @Override
  void resolveName(AnnotationDefinition definition, SymbolTable.SymbolResolver resolver) {
    verifyValuesCnt(definition, 1);
  }

  @Override
  void typeCheck(AnnotationDefinition definition, TypeChecker typeChecker) {
    var valueExpr = definition.values.getFirst();
    typeChecker.check(valueExpr);

    constant = typeChecker.constantEvaluator.eval(valueExpr);
  }
}

/**
 * A simple annotation that stores a single string
 *
 * <p>Examples for such annotations:
 * <pre>
 * [ commentString : "lava cake" ]
 * </pre>
 */
class StringAnnotation extends Annotation {
  @LazyInit
  String value;

  public StringAnnotation() {
    super();
  }

  @Override
  void resolveName(AnnotationDefinition definition, SymbolTable.SymbolResolver resolver) {
    verifyValuesCnt(definition, 1);
    var firstValue = definition.values.getFirst();

    if (!(firstValue instanceof StringLiteral)) {
      throw error("Invalid Annotation Argument", firstValue)
          .locationDescription(firstValue, "Expected a string but got %s",
              firstValue.getClass().getSimpleName())
          .build();
    }
  }

  @Override
  void typeCheck(AnnotationDefinition definition, TypeChecker typeChecker) {
    var valueExpr = definition.values.getFirst();
    typeChecker.check(valueExpr);
  }
}

/**
 * A annotation that holds a single expression. Used for more complex annotations.
 *
 * <p>Examples for such annotations:
 * <pre>
 * [ zero : X(0) ]
 * [ assert : VLIW.length <= 4 ]
 * [ ensure : (sf = 1) | (imm6(5) = 0) ]
 * </pre>
 */
class ExprAnnotation extends Annotation {
  @LazyInit
  Expr node;

  public ExprAnnotation() {
    super();
  }

  @Override
  void resolveName(AnnotationDefinition definition, SymbolTable.SymbolResolver resolver) {
    verifyValuesCnt(definition, 1);
    node = definition.values.getFirst();
    node.accept(resolver);
  }

  @Override
  void typeCheck(AnnotationDefinition definition, TypeChecker typeChecker) {
    node.accept(typeChecker);
  }
}
