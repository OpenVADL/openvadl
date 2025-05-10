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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.types.Type;
import vadl.utils.Pair;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;
import vadl.utils.functionInterfaces.TriConsumer;
import vadl.viam.AssemblyDescription;
import vadl.viam.MemoryRegion;
import vadl.viam.Relocation;
import vadl.viam.annotations.AsmParserCaseSensitive;
import vadl.viam.annotations.AsmParserCommentString;

@SuppressWarnings({"UnusedMethod", "UnusedVariable"})
class AnnotationTable {
  private static final Map<Class<? extends Definition>, Map<String, Supplier<Annotation>>>
      annotationFactories = new java.util.HashMap<>();

  static {
    annotationOn(AsmDescriptionDefinition.class, "case sensitive", EnableAnnotation::new)
        .applyViam((def, annotation, lowering) -> {
          var asmDescription = (AssemblyDescription) def;
          asmDescription.addAnnotation(new AsmParserCaseSensitive(annotation.isEnabled));
        })
        .build();

    annotationOn(AsmDescriptionDefinition.class, "comment string", StringAnnotation::new)
        .applyViam((def, annotation, lowering) -> {
          var asmDescription = (AssemblyDescription) def;
          asmDescription.addAnnotation(new AsmParserCommentString(annotation.value));
        })
        .build();

    groupOn(CounterDefinition.class)
        .add("current", EnableAnnotation::new)
        .add("next", EnableAnnotation::new)
        .add("next next", EnableAnnotation::new)
        .check(GroupedAnnotationBuilder.GroupCheckContext::verifyOnlyOneOfGroup)
        // FIXME: Apply to AST
        .build();

    annotationOn(RegisterFileDefinition.class, "zero", ExprAnnotation::new)
        // FIXME: Typecheck
        // FIXME: Apply to VIAM
        .build();

    groupOn(RelocationDefinition.class)
        .add("global offset", EnableAnnotation::new)
        .add("relative", EnableAnnotation::new)
        .add("absolute", EnableAnnotation::new)
        .check(GroupedAnnotationBuilder.GroupCheckContext::verifyOnlyOneOfGroup)
        .applyViam(context -> {
          var mappings = Map.of(
              "global offset", Relocation.Kind.GLOBAL_OFFSET_TABLE,
              "relative", Relocation.Kind.RELATIVE,
              "absolute", Relocation.Kind.ABSOLUTE
          );

          var annotation = context.getOnly(Annotation.class).get();
          var relocation = (Relocation) context.targetDefinition;
          relocation.setKind(requireNonNull(mappings.get(annotation.name)));
        })
        .build();

    groupOn(CpuMemoryRegionDefinition.class)
        .add("firmware", EnableAnnotation::new)
        .add("base", ConstantAnnotation::new)
        .add("size", ConstantAnnotation::new)
        .check(ctx -> {
          ctx.verifyIfThenAlso("size", "base");
          ctx.verifyIfThenAlso("firmware", "base");
          ctx.get("base", ConstantAnnotation.class)
              .ifPresent(a -> a.verifyGreaterEqual(BigInteger.ZERO));
          ctx.get("size", ConstantAnnotation.class)
              .ifPresent(a -> a.verifyGreaterThan(BigInteger.ZERO));
        })
        .applyViam(ctx -> {
          var memReg = ctx.viamDef(MemoryRegion.class);
          ctx.get("firmware", EnableAnnotation.class).ifPresent(a -> {
            memReg.setHoldsFirmware(a.isEnabled);
          });
          ctx.get("base", ConstantAnnotation.class).ifPresent(a -> {
            memReg.setBase(a.constant.value());
          });
          ctx.get("size", ConstantAnnotation.class).ifPresent(a -> {
            memReg.setSize(a.constant.value().intValue());
          });
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
  static <D extends Definition, A extends Annotation> AnnotationBuilder<D, A> annotationOn(
      Class<D> klass, String name, Supplier<A> annotationFactory) {
    return new AnnotationBuilder<>(klass, name, annotationFactory);
  }

  /**
   * Create a new annotation group builder for a given {@link Definition} class.
   *
   * @param klass to which the annotations are bound.
   * @return a new annotation builder for the given class.
   */
  static <D extends Definition> GroupedAnnotationBuilder<D> groupOn(
      Class<D> klass) {
    return new GroupedAnnotationBuilder<D>(klass);
  }

  private static class AnnotationBuilder<D extends Definition, A extends Annotation> {
    private final Class<D> targetClass;

    private final String name;

    private final Supplier<A> annotationFactory;

    @Nullable
    private TriConsumer<D, A, TypeChecker> checkCallback;

    @Nullable
    private BiConsumer<D, A> applyAstCallback;

    @Nullable
    private TriConsumer<vadl.viam.Definition, A, ViamLowering> applyViamCallback;


    /**
     * Specifies an annotation name and an annotation factory.
     *
     * <p>The factory doesn't have to set the name, groupProvider or definition. These fiedls will
     * be set by the builder.
     *
     * @param targetClass       to which the annotation is bound.
     * @param name              of the annotation.
     * @param annotationFactory that creates the annotation.
     */
    AnnotationBuilder(Class<D> targetClass, String name, Supplier<A> annotationFactory) {
      this.targetClass = targetClass;
      this.name = name;
      this.annotationFactory = annotationFactory;
    }


    /**
     * Adds a check for arbitrary constraints on the annotation. The check is executed after
     * in the typechecker after the annotations itself have been checked and the definition they
     * are annotating. The check throws an {@link vadl.error.Diagnostic} if the check fails.
     *
     * @param checkCallback to be executed.
     * @return itself.
     */
    AnnotationBuilder<D, A> check(TriConsumer<D, A, TypeChecker> checkCallback) {
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
    AnnotationBuilder<D, A> applyAst(BiConsumer<D, A> applyCallback) {
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
    AnnotationBuilder<D, A> applyViam(
        TriConsumer<vadl.viam.Definition, A, ViamLowering> applyCallback) {
      if (this.applyViamCallback != null) {
        throw new IllegalStateException("Apply callback already set");
      }
      this.applyViamCallback = applyCallback;
      return this;
    }

    /**
     * Inserts the annotation into the annotationFactories table.
     */
    @SuppressWarnings("unchecked")
    void build() {
      if (name == null || annotationFactory == null) {
        throw new IllegalStateException("Not all required are fields set");
      }

      TriConsumer<Definition, List<Annotation>, TypeChecker> groupCheckCallback;
      if (checkCallback != null) {
        groupCheckCallback = (definition, annotations, typeChecker) -> {
          requireNonNull(checkCallback).accept((D) definition, (A) annotations.getFirst(),
              typeChecker);
        };
      } else {
        groupCheckCallback = (definition, annotations, typeChecker) -> {
        };
      }

      BiConsumer<Definition, List<Annotation>> groupApplyAstCallback;
      if (applyAstCallback != null) {
        groupApplyAstCallback = (definition, annotations) -> {
          requireNonNull(applyAstCallback).accept((D) definition, (A) annotations.getFirst());
        };
      } else {
        groupApplyAstCallback = (definition, annotations) -> {
        };
      }

      TriConsumer<vadl.viam.Definition, List<Annotation>, ViamLowering> groupApplyViamCallback;
      if (applyViamCallback != null) {
        groupApplyViamCallback = (definition, annotations, lowering
        ) -> {
          requireNonNull(applyViamCallback).accept(definition, (A) annotations.getFirst(),
              lowering);
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
        public void applyViam(Definition astDefinition, vadl.viam.Definition definition,
                              List<Annotation> annotations,
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

  private static class GroupedAnnotationBuilder<D extends Definition> {

    private final Class<D> targetClass;

    private final List<Pair<String, Supplier<Annotation>>> namedFactories = new ArrayList<>();

    @Nullable
    private Consumer<GroupCheckContext<D>> checkCallback;

    @Nullable
    private Consumer<GroupAstApplyContext<D>> applyAstCallback;

    @Nullable
    private Consumer<GroupViamApplyContext<D>> applyViamCallback;


    GroupedAnnotationBuilder(Class<D> targetClass) {
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
    GroupedAnnotationBuilder<D> add(String name, Supplier<Annotation> annotationFactory) {
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
    GroupedAnnotationBuilder<D> check(
        Consumer<GroupCheckContext<D>> checkCallback) {
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
    GroupedAnnotationBuilder<D> applyAst(
        Consumer<GroupAstApplyContext<D>> applyCallback) {
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
    GroupedAnnotationBuilder<D> applyViam(
        Consumer<GroupViamApplyContext<D>> applyCallback) {
      if (this.applyViamCallback != null) {
        throw new IllegalStateException("Apply callback already set");
      }
      this.applyViamCallback = applyCallback;
      return this;
    }

    /**
     * Inserts all annotation of the group into the annotationFactories table.
     */
    @SuppressWarnings("unchecked")
    void build() {
      // FIXME: apply should be optional.
      if (namedFactories.isEmpty()) {
        throw new IllegalStateException("Not all required are fields set");
      }

      Consumer<GroupCheckContext<D>> realCheckCallback;
      if (checkCallback == null) {
        realCheckCallback = (context) -> {
        };
      } else {
        realCheckCallback = requireNonNull(checkCallback);
      }

      Consumer<GroupAstApplyContext<D>> realApplyAstCallback;
      if (applyAstCallback != null) {
        realApplyAstCallback = (context) -> {
          requireNonNull(applyAstCallback).accept(context);
        };
      } else {
        realApplyAstCallback = (context) -> {
        };
      }

      Consumer<GroupViamApplyContext> realApplyViamCallback;
      if (applyViamCallback != null) {
        realApplyViamCallback = (context) -> {
          requireNonNull(applyViamCallback).accept(context);
        };
      } else {
        realApplyViamCallback = (context) -> {
        };
      }


      // Create a group
      var group = new AnnotationGroupProvider() {
        @Override
        public void check(Definition definition, List<Annotation> annotations,
                          TypeChecker typeChecker) {
          realCheckCallback.accept(
              new GroupCheckContext<>((D) definition, annotations, typeChecker));
        }

        @Override
        public void applyAst(Definition definition, List<Annotation> annotations) {
          realApplyAstCallback.accept(new GroupAstApplyContext<>((D) definition, annotations));
        }

        @Override
        public void applyViam(Definition astDefinition, vadl.viam.Definition definition,
                              List<Annotation> annotations,
                              ViamLowering lowering) {
          realApplyViamCallback.accept(
              new GroupViamApplyContext<>(astDefinition, definition, annotations, lowering));
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

    private static class GroupContext<D> {
      final D astTargetDef;
      // annotations set by the user
      final LinkedHashMap<String, Annotation> annotations;
      // holds the annotation factories of this group.
      // might be used to get declared annotations.
      private final Map<String, Supplier<Annotation>> factories;

      GroupContext(D astTargetDef, List<Annotation> annotations) {
        this.astTargetDef = astTargetDef;
        this.annotations = annotations.stream()
            .collect(Collectors.toMap(
                a -> a.name,
                Function.identity(),
                (a1, a2) -> a1,
                LinkedHashMap::new
            ));
        this.factories = requireNonNull(annotationFactories.get(astTargetDef.getClass()));
      }

      /**
       * Get the {@link Annotation} with the given name wrapped in an {@link Optional}.
       */
      Optional<Annotation> get(String anno) {
        return Optional.ofNullable(annotations.get(anno));
      }

      /**
       * Get the {@link Annotation} with the given name wrapped in an {@link Optional}.
       * It is automatically cast to the given annotation type.
       * If the found annotation is not of the given type, it will throw an
       * {@link IllegalStateException}, as the user always know the concrete type of the
       * annotation.
       *
       * @param anno      name of annotation to get
       * @param annoClass type to which a found annotation is cast to
       * @return an optional which is present if there was a annotation with the given name,
       *     otherwise it is empty
       */
      <A extends Annotation> Optional<A> get(String anno, Class<A> annoClass) {
        return get(anno).map(a -> {
          if (!annoClass.isInstance(a)) {
            throw new IllegalStateException(
                "Expected %s to be of annotation type %s but was %s".formatted(anno,
                    annoClass.getSimpleName(), a.getClass().getSimpleName()));
          }
          return annoClass.cast(a);
        });
      }

      /**
       * Returns an annotation of the given class.
       * This can be used if the user knows that there is only one annotation of the given class, it
       * may use this to retrieve it.
       *
       * @throws IllegalStateException if there were multiple annotations with the same type
       */
      <A extends Annotation> Optional<A> getOnly(Class<A> annoClass) {
        var all = annotations.values().stream()
            .filter(annoClass::isInstance)
            .map(annoClass::cast)
            .toList();
        if (all.size() > 1) {
          throw new IllegalStateException(
              "Expected to have at most one annotation of type %s".formatted(
                  annoClass.getSimpleName()));
        }
        return all.stream().findFirst();
      }

      // caches declarations accessed by #declaration(String).
      private final Map<String, AnnotationDeclaration> declarationCache = new HashMap<>();

      /**
       * Get the {@link AnnotationDeclaration} for a given name.
       * This is mostly used to get the {@link AnnotationDeclaration#usageString()}.
       */
      AnnotationDeclaration declaration(String name) {
        return declarationCache.computeIfAbsent(name, n -> {
          if (!factories.containsKey(name)) {
            throw new IllegalStateException("No annotation found with name " + name);
          }
          var result = annotations.get(name);
          if (result == null) {
            // produce new annotation object that represents the declared annotation
            return factories.get(name).get();
          }
          return result;
        });
      }

    }

    private static class GroupCheckContext<D> extends GroupContext<D> {
      final TypeChecker typeChecker;

      public GroupCheckContext(D targetDefinition, List<Annotation> annotations,
                               TypeChecker typeChecker) {
        super(targetDefinition, annotations);
        this.typeChecker = typeChecker;
      }

      /**
       * Verifies that only one annotation exists in the group. If more than one annotation is
       * present an error is thrown.
       *
       * @throws vadl.error.Diagnostic if more than one annotation is present in the group.
       */
      void verifyOnlyOneOfGroup() {
        if (annotations.size() > 1) {
          var diagnostic = error("Annotation clash", annotations.firstEntry().getValue())
              .locationDescription(annotations.firstEntry().getValue(),
                  "First defined here");
          for (Annotation annotation : annotations.values()) {
            diagnostic.locationDescription(annotation,
                "Conflicting defined here");
          }
          diagnostic.description("Only one of these annotations can be defined.");
          throw diagnostic.build();
        }
      }


      /**
       * Verifies that if an annotation is set, the user also sets other annotations.
       *
       * @param ifAnno        the annotation that is checked if it was set
       * @param thenAlsoAnnos the annotations that must also be set if {@code ifAnno} was set
       */
      void verifyIfThenAlso(String ifAnno, String... thenAlsoAnnos) {
        get(ifAnno).ifPresent(anno -> {
          for (var alsoAnno : thenAlsoAnnos) {
            var unused = get(alsoAnno).orElseThrow(() -> error("Missing annotation", anno)
                .locationDescription(anno, "Requires the %s annotation",
                    declaration(alsoAnno).usageString())
                .description("If %s was specified, the definition also requires %s.",
                    anno.usageString(),
                    declaration(alsoAnno).usageString())
                .build());
          }
        });
      }

    }

    private static class GroupAstApplyContext<D> extends GroupContext<D> {
      public GroupAstApplyContext(D targetDefinition, List<Annotation> annotations) {
        super(targetDefinition, annotations);
      }
    }

    private static class GroupViamApplyContext<D> extends GroupContext<D> {
      final vadl.viam.Definition targetDefinition;
      final ViamLowering lowering;

      public GroupViamApplyContext(D astTargetDef, vadl.viam.Definition viamTargetDef,
                                   List<Annotation> annotations,
                                   ViamLowering lowering) {
        super(astTargetDef, annotations);
        this.targetDefinition = viamTargetDef;
        this.lowering = lowering;
      }

      public <V extends vadl.viam.Definition> V viamDef(Class<V> defClass) {
        return defClass.cast(targetDefinition);
      }
    }
  }
}

interface AnnotationGroupProvider {
  void check(Definition definition, List<Annotation> annotations, TypeChecker typeChecker);

  void applyAst(Definition definition, List<Annotation> annotations);

  void applyViam(Definition astDef, vadl.viam.Definition definition, List<Annotation> annotations,
                 ViamLowering lowering);
}

/**
 * An interface representing the annotation declaration given by the
 * {@link AnnotationGroupProvider}.
 * Even though it is always a {@link Annotation} object, it does not always hold a state,
 * but serves as representation of what kind of annotation the provider specified.
 */
interface AnnotationDeclaration {

  /**
   * The name of the specified annotation.
   */
  String name();

  /**
   * The usage string of a given annotation.
   * E.g. a {@link EnumAnnotation} with the possible values {@code A, B, C}
   * and name {@code my option}, will return {@code [ my option: A, B, C ]}.
   * This is especially useful when writing an error message, when the concrete annotation
   * type/object is not known.
   */
  String usageString();

}

/**
 * A Annotation in Vadl keeps state and knows how to resolve and type check itself. Further checks
 * can be defined on the {@link AnnotationGroupProvider} and who also knows how to apply the
 * annotation to the VIAM.
 *
 * <p>Every Annotation also has a group it belongs to, though it might be the only annotation in
 * the group.
 */
abstract class Annotation implements AnnotationDeclaration, WithLocation {
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

  @Override
  public String usageString() {
    return "[ " + name + " ]";
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

  /**
   * Verify that the constant value is greater than the given value.
   */
  void verifyGreaterThan(BigInteger value) {
    if (constant.value().compareTo(value) <= 0) {
      var expr = definition.values.getFirst();
      throw error("Invalid annotation expression", expr)
          .locationDescription(expr,
              "Constant expression must be greater than %s, but was %s",
              value.toString(), constant.value().toString())
          .build();
    }
  }

  /**
   * Verify that the constant value is greater or equal to the given value.
   */
  void verifyGreaterEqual(BigInteger value) {
    if (constant.value().compareTo(value) < 0) {
      var expr = definition.values.getFirst();
      throw error("Invalid annotation expression", expr)
          .locationDescription(expr,
              "Constant expression must greater or equal to %s, but was %s",
              value.toString(), constant.value().toString())
          .build();
    }
  }

  @Override
  public String usageString() {
    return "[ " + name + " : <expr> ]";
  }
}

/**
 * A simple annotation that stores a single string.
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

  @Override
  public String usageString() {
    return "[ " + name + " : \"<str>\" ]";
  }
}

/**
 * A annotation that can take one of the specified fields.
 *
 * <p>Examples for such annotations:
 * <pre>
 * [ commentString : "lava cake" ]
 * </pre>
 */
class EnumAnnotation extends Annotation {
  List<String> possibleValues;

  @LazyInit
  String value;

  public EnumAnnotation(List<String> possibleValues) {
    super();
    this.possibleValues = possibleValues;
  }

  @Override
  void resolveName(AnnotationDefinition definition, SymbolTable.SymbolResolver resolver) {
    verifyValuesNonEmpty(definition);

    for (var value : definition.values) {
      if (!(value instanceof Identifier)) {
        throw error("Invalid Annotation Argument", value)
            .locationDescription(value, "Expected an identifier but got %s",
                value.getClass().getSimpleName())
            .build();
      }
    }

    value = definition.values.stream()
        .map(v -> ((Identifier) v).name)
        .collect(Collectors.joining(" "));

    if (!possibleValues.contains(value)) {
      throw error("Invalid Annotation Argument", definition)
          .locationDescription(definition, "Expected one of %s but got %s",
              String.join(", ", possibleValues), value)
          .build();
    }

    // Do not symbol resolve on purpose as the identifiers here aren't pointing to anything in the
    // AST.
  }

  @Override
  void typeCheck(AnnotationDefinition definition, TypeChecker typeChecker) {
    // Do nothing on purpose as the identifiers don't need to be checked.
  }

  @Override
  public String usageString() {
    var options = String.join(", ", possibleValues);
    return "[ " + name + " : " + options + " ]";
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

  @Override
  public String usageString() {
    return "[ " + name + " : <expr> ]";
  }
}
