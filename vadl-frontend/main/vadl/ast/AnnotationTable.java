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

import static java.util.Objects.requireNonNull;
import static vadl.error.Diagnostic.ensure;
import static vadl.error.Diagnostic.error;
import static vadl.viam.ViamError.ensurePresent;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.ast.nodes.AbiSequenceDefinition;
import vadl.ast.nodes.AliasDefinition;
import vadl.ast.nodes.AnnotationDefinition;
import vadl.ast.nodes.AsmDescriptionDefinition;
import vadl.ast.nodes.CallIndexExpr;
import vadl.ast.nodes.CounterDefinition;
import vadl.ast.nodes.CpuMemoryRegionDefinition;
import vadl.ast.nodes.Definition;
import vadl.ast.nodes.DerivedFormatField;
import vadl.ast.nodes.EncodingDefinition;
import vadl.ast.nodes.Expr;
import vadl.ast.nodes.FloatTypeDefinition;
import vadl.ast.nodes.FormatField;
import vadl.ast.nodes.GroupDefinition;
import vadl.ast.nodes.Identifier;
import vadl.ast.nodes.InstructionDefinition;
import vadl.ast.nodes.MemoryDefinition;
import vadl.ast.nodes.Node;
import vadl.ast.nodes.OperationDefinition;
import vadl.ast.nodes.ProcessorDefinition;
import vadl.ast.nodes.RegisterDefinition;
import vadl.ast.nodes.RelocationDefinition;
import vadl.ast.nodes.StringLiteral;
import vadl.ast.nodes.TypedNode;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticBuilder;
import vadl.gcb.annotations.OnlyNegativeNumbersAnnotation;
import vadl.gcb.annotations.RelocationSyntaxAnnotation;
import vadl.gcb.annotations.SkipPruningAnnotation;
import vadl.gcb.annotations.StatusRegisterAnnotation;
import vadl.types.BitsType;
import vadl.types.FloatEncoding;
import vadl.types.Type;
import vadl.utils.Pair;
import vadl.utils.functionInterfaces.QuadConsumer;
import vadl.utils.functionInterfaces.TriConsumer;
import vadl.viam.Abi;
import vadl.viam.ArtificialResource;
import vadl.viam.AssemblyDescription;
import vadl.viam.Constant;
import vadl.viam.Counter;
import vadl.viam.Encoding;
import vadl.viam.Endianness;
import vadl.viam.FloatExceptionFlag;
import vadl.viam.Format;
import vadl.viam.Group;
import vadl.viam.Instruction;
import vadl.viam.Memory;
import vadl.viam.MemoryRegion;
import vadl.viam.RegisterResource;
import vadl.viam.RegisterTensor;
import vadl.viam.Relocation;
import vadl.viam.annotations.AlignmentAnnotation;
import vadl.viam.annotations.AsmGenerateRulesAnno;
import vadl.viam.annotations.AsmParserCaseSensitive;
import vadl.viam.annotations.AsmParserCommentString;
import vadl.viam.annotations.AssertAnnotation;
import vadl.viam.annotations.DefineOperandAnnotation;
import vadl.viam.annotations.EnableHtifAnno;
import vadl.viam.annotations.InstructionUndefinedAnno;
import vadl.viam.annotations.PcOffsetAnnotation;
import vadl.viam.annotations.StopAnnotation;
import vadl.viam.annotations.TbStateRegisterAnnotation;

/**
 * The annotation table defines how {@link Annotation} can be used for different elements in
 * VADL.
 */
@SuppressWarnings({"UnusedMethod", "UnusedVariable"})
public class AnnotationTable {
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
          var strLit = (StringLiteral) annotation.definition.values.getFirst();
          asmDescription.addAnnotation(new AsmParserCommentString(strLit.value));
        })
        .build();

    annotationOn(AsmDescriptionDefinition.class, "generate rules", EnableAnnotation::new)
        .applyViam((def, annotation, lowering) -> {
          var asmDescription = (AssemblyDescription) def;
          asmDescription.addAnnotation(new AsmGenerateRulesAnno(annotation.isEnabled));
        })
        .build();

    groupOn(CounterDefinition.class)
        .add("current", EnableAnnotation::new)
        .add("next", EnableAnnotation::new)
        .add("next next", EnableAnnotation::new)
        .check(GroupedAnnotationBuilder.GroupCheckContext::verifyOnlyOneOfGroup)
        .applyViam(ctx -> {
          var reg = ((Counter) ctx.targetDefinition).registerTensor();
          BiConsumer<Annotation, Integer> addAnn = (ann, offset) -> {
            var offsetAnn = new PcOffsetAnnotation(offset);
            offsetAnn.setSourceLocation(ann.location());
            reg.addAnnotation(offsetAnn);
          };
          ctx.get("next").ifPresent(ann -> addAnn.accept(ann, 1));
          ctx.get("next next").ifPresent(ann -> addAnn.accept(ann, 2));
        })
        .build();

    groupOn(AliasDefinition.class)
        .add("current", EnableAnnotation::new)
        .add("next", EnableAnnotation::new)
        .add("next next", EnableAnnotation::new)
        .check(ctx -> {
          var alias = ctx.astTargetDef;
          ensure(alias.kind.equals(AliasDefinition.AliasKind.PROGRAM_COUNTER),
              () -> error("Invalid annotation target", alias)
                  .locationDescription(alias,
                      "Program counter annotations can only be applied on program counters "
                          + "and program counter aliases"));
          ctx.verifyOnlyOneOfGroup();
        })
        .applyViam(ctx -> {
          var reg = ((Counter) ctx.targetDefinition).registerTensor();
          BiConsumer<Annotation, Integer> addAnn = (ann, offset) -> {
            var offsetAnn = new PcOffsetAnnotation(offset);
            offsetAnn.setSourceLocation(ann.location());
            reg.addAnnotation(offsetAnn);
          };
          ctx.get("next").ifPresent(ann -> addAnn.accept(ann, 1));
          ctx.get("next next").ifPresent(ann -> addAnn.accept(ann, 2));
        })
        .build();

    annotationOn(RegisterDefinition.class, "zero", ZeroConstraintAnnotation::new)
        .applyViam((def, annotation, lowering) -> {
          var viamDef = (RegisterTensor) def;
          var indices = annotation.indices.stream().map(ConstantValue::toViamConstant).toList();
          var zero = Constant.Value.of(0, viamDef.resultType(indices.size()));
          viamDef.addConstraint(new RegisterResource.Constraint(indices, zero));
        })
        .build();

    annotationOn(RegisterDefinition.class, "alignment", ConstantAnnotation::new)
        .applyViam((def, annotation, lowering) -> {
          var viamDef = (RegisterTensor) def;
          viamDef.addAnnotation(
              new AlignmentAnnotation(new Abi.Alignment(annotation.constant.value().intValue())));
        })
        .build();

    annotationOn(AliasDefinition.class, "zero", ZeroConstraintAnnotation::new)
        .check((def, annotation, lowering) -> {
          ensure(def.computedTarget instanceof RegisterDefinition,
              () -> error("Invalid annotation target", annotation)
                  .locationDescription(annotation,
                      "Zero annotation can only be applied on register aliases"));
        })
        .applyViam((def, annotation, lowering) -> {
          var viamDef = (ArtificialResource) def;
          var indices = annotation.indices.stream().map(ConstantValue::toViamConstant).toList();
          var zero = Constant.Value.of(0, viamDef.resultType(indices.size()));
          viamDef.addConstraint(new RegisterResource.Constraint(indices, zero));
        })
        // this handled in the VIAM lowering when constructing the ArtificialResource
        .build();

    annotationOn(RegisterDefinition.class, "execution state", ExecutionStateAnnotation::new)
        .check((def, annotation, lowering) -> annotation.typeCheckTarget(def))
        .applyAst((def, annotation) -> annotation.calcBitSlice(def))
        .applyViam((def, annotation, lowering) -> {
          var viamDef = (RegisterTensor) def;
          if (viamDef.hasAnnotation(TbStateRegisterAnnotation.class)) {
            viamDef.expectAnnotation(TbStateRegisterAnnotation.class).addSlice(annotation.slice);
          } else {
            viamDef.addAnnotation(
                new TbStateRegisterAnnotation(viamDef.totalWidth(), annotation.slice));
          }
        })
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

    annotationOn(EncodingDefinition.class, "select when", EncodingConstraintAnnotation::new)
        .check((def, annotation, tc) -> annotation.verifyExprType(tc, Type.bool()))
        .applyViam((def, annotation, lowering) -> {
          // The actual formular checks are done in the VdtEncodingConstraintValidationPass.
          var encoding = (Encoding) def;
          var graph = new BehaviorLowering(lowering).getFunctionGraph(annotation.expr,
              encoding.simpleName() + " Constraint");
          graph.setParentDefinition(encoding);
          encoding.setConstraint(graph);
        })
        .build();

    annotationOn(InstructionDefinition.class, "undefined when", InstructionUndefinedAnnotation::new)
        .check((def, annotation, tc) -> annotation.verifyExprType(tc, Type.bool()))
        .applyViam((def, annotation, lowering) -> {
          var instr = (Instruction) def;
          var graph = new BehaviorLowering(lowering).getFunctionGraph(annotation.expr,
              instr.simpleName() + " Constraint");
          graph.setParentDefinition(instr);
          var anno = new InstructionUndefinedAnno(graph);
          anno.check();
          instr.addAnnotation(anno);
        })
        .build();

    annotationOn(InstructionDefinition.class, "operation",
        () -> new IdentifersAnnotation(OperationDefinition.class))
        .check((def, annotation, lowering) -> {
          // Add the definition to the operation defined
          annotation.identifiers.forEach(identifier -> {
            var operation = (OperationDefinition) requireNonNull(identifier.target());
            operation.instructions.add(def);
          });
        })
        .build();

    annotationOn(AliasDefinition.class, "overwrite source",
        () -> new EnumAnnotation(List.of("zero", "sign")))
        .check((def, annotation, lowering) -> {
          annotation.verifyValuesCnt(annotation.definition, 1);
        })
        .build();

    groupOn(MemoryDefinition.class)
        .add("big endian", OptExprAnnotation::new)
        .add("little endian", OptExprAnnotation::new)
        .check(ctx -> {
          ctx.verifyOnlyOneOfGroup();
          ctx.getOnly(OptExprAnnotation.class)
              .ifPresent(ann -> ann.verifyExprType(Type.bool()));
        })
        .applyViam(ctx -> {
          var memDef = ctx.viamDef(Memory.class);

          BiConsumer<OptExprAnnotation, Endianness> apply = (ann, endianness) -> {
            memDef.setEndianness(endianness);
            if (ann.expr != null) {
              var graph = new BehaviorLowering(ctx.lowering).getFunctionGraph(
                  ann.expr,
                  memDef.simpleName() + " Bi Endian Condition"
              );
              graph.setParentDefinition(memDef);
              memDef.setBiEndianCondition(graph);
            }
          };

          ctx.get("big endian", OptExprAnnotation.class)
              .ifPresent(ann -> apply.accept(ann, Endianness.BIG));
          ctx.get("little endian", OptExprAnnotation.class)
              .ifPresent(ann -> apply.accept(ann, Endianness.LITTLE));
        }).build();

    annotationOn(GroupDefinition.class, "assert", () -> new ExprAnnotation(true))
        .check((def, annotation, tc) -> annotation.verifyExprType(tc, Type.bool()))
        .applyViam((def, annotation, lowering) -> {
          var group = (Group) def;
          var graph = new BehaviorLowering(lowering)
              .getFunctionGraph(annotation.expr, "Assert " + group.simpleName());
          graph.setParentDefinition(group);
          group.addAnnotation(new AssertAnnotation(graph));
        })
        .build();

    annotationOn(GroupDefinition.class, "stop", () -> new ExprAnnotation(true))
        .check((def, annotation, tc) -> annotation.verifyExprType(tc, Type.bool()))
        .applyViam((def, annotation, lowering) -> {
          var group = (Group) def;
          var graph = new BehaviorLowering(lowering)
              .getFunctionGraph(annotation.expr, "Stop " + group.simpleName());
          graph.setParentDefinition(group);
          group.addAnnotation(new StopAnnotation(graph));
        })
        .build();

    /// FLOAT RELATED ///

    annotationOn(FloatTypeDefinition.class, "IEEE", IEEEFloatFormatAnnotation::new)
        .applyAst((def, annotation) -> {
          var size = annotation.constant.value().intValue();
          ensure(FloatEncoding.isValidIEEESize(size),
              () -> error("Invalid IEEE encoding size", annotation)
                  .description("The following sizes are supported: %s",
                      Arrays.stream(FloatEncoding.values())
                          .map(e -> Integer.toString(e.size)).collect(Collectors.joining(", ")))
          );
          def.encoding = FloatEncoding.ieee(size);
        })
        .build();

    QuadConsumer<RegisterTensor, FloatFlagAnnotation, Boolean, FloatExceptionFlag>
        applyViamFloatFlag;
    applyViamFloatFlag = (reg, annotation, sticky, flag) -> {
      var idx = annotation.index;
      if (reg.hasAnnotation(vadl.viam.annotations.FloatFlagAnnotation.class)) {
        var ann = reg.expectAnnotation(vadl.viam.annotations.FloatFlagAnnotation.class);
        var setFlag = ann.get(idx);
        ensure(setFlag == null, () -> error(
            "Bit already mapped as " + (ann.isSticky(idx) ? "" : "non ")
                + "sticky " + requireNonNull(setFlag).name + " flag",
            annotation
        ));
        ann.set(idx, sticky, flag);
      } else {
        var ann = new vadl.viam.annotations.FloatFlagAnnotation();
        ann.set(idx, sticky, flag);
        reg.addAnnotation(ann);
      }
    };

    for (var flag : FloatExceptionFlag.values()) {
      annotationOn(RegisterDefinition.class, "fe flag " + flag.name, FloatFlagAnnotation::new)
          .check((def, annotation, lowering) -> annotation.typeCheckTarget(def))
          .applyViam((def, annotation, lowering) ->
              applyViamFloatFlag.accept((RegisterTensor) def, annotation, false, flag))
          .build();

      annotationOn(RegisterDefinition.class, "sticky fe flag " + flag.name,
          FloatFlagAnnotation::new)
          .check((def, annotation, lowering) -> annotation.typeCheckTarget(def))
          .applyViam((def, annotation, lowering) ->
              applyViamFloatFlag.accept((RegisterTensor) def, annotation, true, flag))
          .build();
    }

    /// PROCESSOR RELATED ///

    annotationOn(ProcessorDefinition.class, "htif", EnableAnnotation::new)
        .applyViam((def, annotation, lowering) -> {
          if (annotation.isEnabled) {
            def.addAnnotation(new EnableHtifAnno());
          }
        }).build();

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

    /// Compiler RELATED ///

    annotationOn(InstructionDefinition.class, "skip pruning", EnableAnnotation::new)
        .applyViam((def, annotation, lowering) -> {
          if (annotation.isEnabled) {
            def.addAnnotation(new SkipPruningAnnotation());
          }
        }).build();

    annotationOn(RegisterDefinition.class, "negative status register", EnableAnnotation::new)
        .applyViam((def, annotation, lowering) -> {
          if (annotation.isEnabled) {
            def.addAnnotation(new StatusRegisterAnnotation.NegativeStatusRegisterAnnotation());
            // The annotation does not check subtyping, so we have to add the parent class as
            // well.
            def.addAnnotation(new StatusRegisterAnnotation());
          }
        }).build();

    annotationOn(RegisterDefinition.class, "zero status register", EnableAnnotation::new)
        .applyViam((def, annotation, lowering) -> {
          if (annotation.isEnabled) {
            def.addAnnotation(new StatusRegisterAnnotation.ZeroStatusRegisterAnnotation());
            // The annotation does not check subtyping, so we have to add the parent class as
            // well.
            def.addAnnotation(new StatusRegisterAnnotation());
          }
        }).build();

    annotationOn(RegisterDefinition.class, "carry status register", EnableAnnotation::new)
        .applyViam((def, annotation, lowering) -> {
          if (annotation.isEnabled) {
            def.addAnnotation(new StatusRegisterAnnotation.CarryStatusRegisterAnnotation());
            // The annotation does not check subtyping, so we have to add the parent class as
            // well.
            def.addAnnotation(new StatusRegisterAnnotation());
          }
        }).build();

    annotationOn(RegisterDefinition.class, "overflow status register", EnableAnnotation::new)
        .applyViam((def, annotation, lowering) -> {
          if (annotation.isEnabled) {
            def.addAnnotation(new StatusRegisterAnnotation.OverflowStatusRegisterAnnotation());
            // The annotation does not check subtyping, so we have to add the parent class as
            // well.
            def.addAnnotation(new StatusRegisterAnnotation());
          }
        }).build();

    annotationOn(AbiSequenceDefinition.class, "only negative", EnableAnnotation::new)
        .applyViam((def, annotation, lowering) -> {
          if (annotation.isEnabled) {
            def.addAnnotation(new OnlyNegativeNumbersAnnotation());
          }
        }).build();

    annotationOn(InstructionDefinition.class, "add operands",
        () -> new IdentifersAnnotation(Definition.class))
        .applyViam((def, annotation, lowering) -> {
          var fields =
              annotation.identifiers.stream()
                  .map(x -> (Format.Field) ensurePresent(
                      lowering.fetch((FormatField) requireNonNull(x.target())),
                      () -> error("Cannot find field", x.location()))).toList();

          def.addAnnotation(new DefineOperandAnnotation(fields));
        }).build();

    annotationOn(RelocationDefinition.class, "syntax", StringAnnotation::new)
        .applyViam((def, annotation, lowering) -> {
          var lit = (StringLiteral) annotation.definition.values.getFirst();
          def.addAnnotation(new RelocationSyntaxAnnotation(lit.value));
        }).build();
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

  static List<AnnotationDeclaration> availableAnnotationDeclarations(
      Class<? extends Definition> klass) {
    return annotationFactories.getOrDefault(klass, Map.of())
        .values().stream().map(Supplier::get)
        .map(annotation -> (AnnotationDeclaration) annotation)
        .toList();
  }

  static List<String> availableAnnotationNames(Class<? extends Definition> klass) {
    return annotationFactories.getOrDefault(klass, Map.of()).keySet().stream().toList();
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
    return groupings(definition.annotations.stream().map(d -> d.annotation));
  }

  private static Map<AnnotationGroupProvider, List<Annotation>> groupings(
      Stream<Annotation> annotations) {
    return annotations.collect(Collectors.groupingBy(a -> a.groupProvider));
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



// ---------- GENERAL ANNOTATION CLASSES ----------

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
 * An annotation that can be applied to registers of type {@link FormatType}. It can be used
 * to reference one format field of the type. The bit-size of the format field must be 1.
 *
 * <p>Usage examples:
 * <pre>
 * [ sticky fe flag overflow : ov ]
 * register reg : Format
 * format Format : Bits<8> { ov [7], ... }
 * </pre>
 */
class FloatFlagAnnotation extends FormatFieldAnnotation {

  @LazyInit
  Identifier field;

  @LazyInit
  int index;

  @Override
  void typeCheck(AnnotationDefinition definition, TypeChecker typeChecker) {
    super.typeCheck(definition, typeChecker);
    verifyValuesCnt(definition, 1);
    field = (Identifier) definition.values.getFirst();
  }

  @Override
  void typeCheckTarget(TypedNode target) {
    super.typeCheckTarget(target);
    var format = ((FormatType) target.type()).format;
    var range = requireNonNull(format.getFieldRange(field.name));
    Diagnostic.ensure(range.from() == range.to(), () ->
        error("Float flag can only be one bit", field));
    index = range.from();
  }

  @Override
  String annotationName() {
    return "Float exception flag annotation";
  }

  @Override
  public String usageString() {
    return "[ " + name + " : <format-field> ]";
  }
}

/**
 * An annotation that can be applied to registers of type {@link BitsType}. If the register's
 * type is a {@link FormatType}, then this annotation can be used to reference its format fields.
 *
 * <p>Usage examples:
 * <pre>
 * [ execution state ]
 * register reg : Bits<8>
 *
 * [ execution state : f0, f1 ]
 * register reg : Format
 * format Format : Bits<8> { f0 [7], f1 [6], ... }
 * </pre>
 */
class ExecutionStateAnnotation extends FormatFieldAnnotation {

  void calcBitSlice(TypedNode target) {
    if (fields.isEmpty()) {
      var width = ((BitsType) target.type()).bitWidth();
      slice = Constant.BitSlice.of(width - 1, 0);
      return;
    }
    var format = requireNonNull((FormatType) target.type()).format;
    slice = new Constant.BitSlice(
        fields.stream()
            .map(field -> requireNonNull(format.getFieldRange(field.name)))
            .map(range -> new Constant.BitSlice.Part(range.from(), range.to()))
            .toArray(Constant.BitSlice.Part[]::new)
    );
  }

  @Override
  String annotationName() {
    return "Execution state annotation";
  }

  @Override
  public String usageString() {
    return "[ " + name + " : <ident>, ... ]";
  }
}

/**
 * An annotation that can be applied to anything that has a type which is a {@link BitsType}.
 * If the target's type is a {@link FormatType}, then this annotation can be used to reference
 * its format fields.
 *
 * <p>Examples for such annotations:
 * <pre>
 * [ whole ]
 * register reg : Bits<8>
 *
 * [ formatFields : f0, f1 ]
 * register reg : Format
 * format Format : Bits<8> { f0 [7], f1 [6], ... }
 * </pre>
 */
abstract class FormatFieldAnnotation extends Annotation {

  @LazyInit
  List<Identifier> fields;

  @LazyInit
  Constant.BitSlice slice;

  @Override
  void resolveName(AnnotationDefinition definition, SymbolTable.SymbolResolver resolver) {
  }

  @Override
  void typeCheck(AnnotationDefinition definition, TypeChecker typeChecker) {
    definition.values.forEach(def -> {
      Diagnostic.ensure(def instanceof Identifier, () -> error("Invalid annotation value", def)
          .description("An identifier was expected.")
      );
    });

    fields = definition.values.stream().map(def -> (Identifier) def).toList();
  }

  void typeCheckTarget(TypedNode target) {
    if (fields.isEmpty()) {
      Diagnostic.ensure(target.type() instanceof BitsType,
          () -> error("Annotation target has invalid type", this).description("""
              %s can only be applied to simple register \
              definitions (no register files or tensors)""", annotationName()));
      return;
    }
    Diagnostic.ensure(target.type() instanceof FormatType,
        () -> error("Annotation target has invalid type", this).description("""
            %s with format fields can only be applied \
            to register definitions with a format type""", annotationName()));

    var format = ((FormatType) target.type()).format;
    fields.forEach(field -> {
      Function<String, DiagnosticBuilder> errBuilder = (String err) ->
          error(err, field).description("Must be one of: %s",
              format.fields.stream()
                  .filter(f -> !(f instanceof DerivedFormatField))
                  .map(f -> f.identifier().name)
                  .collect(Collectors.joining(", "))
          );
      Diagnostic.ensure(format.hasField(field.name),
          () -> errBuilder.apply("Unknown field name"));
      Diagnostic.ensure(!(format.getField(field.name) instanceof DerivedFormatField),
          () -> errBuilder.apply("Cannot annotate derived field"));
    });
  }

  abstract String annotationName();
}

/**
 * Marker interface for all annotations that set the {@link FloatTypeDefinition#encoding} field.
 */
interface FloatEncodingAnnotation {
}

/**
 * An annotation which makes a {@link FloatTypeDefinition} use IEEE-754 encoding with a given
 * size (32 or 64 bit).
 */
class IEEEFloatFormatAnnotation extends ConstantAnnotation implements FloatEncodingAnnotation {
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
    definition.values.getFirst().accept(resolver);
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
              firstValue.nodeName())
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
                value.nodeName())
            .build();
      }
    }

    value = definition.values.stream()
        .map(v -> ((Identifier) v).name)
        .collect(Collectors.joining(" "));

    if (!possibleValues.contains(value)) {
      throw error("Invalid Annotation Argument", definition)
          .locationDescription(definition, "Expected one of %s but got `%s`",
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
 * A simple annotation that stores a one or more Identifiers.
 * The creator can also specify the node class to which the identifiers have to point.
 * The identifiers have to point to some node in the AST and will be resolved.
 *
 * <p>Examples for such annotations:
 * <pre>
 * [ relatedInstr : ADDI ADDU ]
 * </pre>
 */
class IdentifersAnnotation extends Annotation {

  /**
   * If set to true, the annotation only allows one argument.
   */
  private boolean singleMode = false;

  List<Identifier> identifiers = new ArrayList<>();

  @Nullable
  final Class<? extends Definition> targetClass;

  public IdentifersAnnotation() {
    super();
    targetClass = null;
  }

  public IdentifersAnnotation(Class<? extends Definition> targetClass) {
    super();
    this.targetClass = targetClass;
  }

  /**
   * Creates an IdentifersAnnotation that only allows one argument.
   */
  public static IdentifersAnnotation single() {
    var annotation = new IdentifersAnnotation();
    annotation.singleMode = true;
    return annotation;
  }

  /**
   * Creates an IdentifersAnnotation that only allows one argument.
   */
  public static IdentifersAnnotation single(Class<? extends Definition> targetClass) {
    var annotation = new IdentifersAnnotation(targetClass);
    annotation.singleMode = true;
    return annotation;
  }

  @Override
  void resolveName(AnnotationDefinition definition, SymbolTable.SymbolResolver resolver) {
    if (singleMode) {
      verifyValuesCnt(definition, 1);
    } else {
      verifyValuesNonEmpty(definition);
    }

    for (var value : definition.values) {
      if (!(value instanceof Identifier identifier)) {
        throw error("Invalid Annotation Argument", value)
            .locationDescription(value, "Expected an identifier but got %s",
                value.nodeName())
            .build();
      }
      identifiers.add(identifier);

      if (targetClass != null) {
        definition.symbolTable().requireAs(identifier, targetClass);
      } else {
        definition.symbolTable().requireAs(identifier, Node.class);
      }
    }
  }

  @Override
  void typeCheck(AnnotationDefinition definition, TypeChecker typeChecker) {
    // Only typecheck expressions
    if (targetClass != null && Expr.class.isAssignableFrom(targetClass)) {
      identifiers.forEach(typeChecker::check);
    }
  }

  @Override
  public String usageString() {
    if (singleMode) {
      return "[ " + name + " : <Id>]";
    }
    return "[ " + name + " : <Id>... ]";
  }
}

/**
 * An annotation that holds a single optional expression.
 *
 * <p>Examples for such annotations:
 * <pre>
 * [ little endian ]
 * [ little endian : MSR.le = 1 ]
 * </pre>
 */
class OptExprAnnotation extends Annotation {
  @LazyInit
  @Nullable
  Expr expr;

  @Override
  void resolveName(AnnotationDefinition definition, SymbolTable.SymbolResolver resolver) {
    verifyValuesCntBetween(definition, 0, 1);
    if (!definition.values.isEmpty()) {
      expr = definition.values.getFirst();
      expr.accept(resolver);
    }
  }

  @Override
  void typeCheck(AnnotationDefinition definition, TypeChecker typeChecker) {
    if (expr != null) {
      expr.accept(typeChecker);
    }
  }

  @Override
  public String usageString() {
    return "[ " + name + " : <expr> ]";
  }

  public void verifyExprType(Type type) {
    if (expr != null) {
      var presentExpr = expr;
      Diagnostic.ensure(
          presentExpr.type() == type,
          () -> error("Invalid annotation presentExpression", presentExpr)
              .locationDescription(presentExpr, "Expression must be a %s", type)
      );
    }
  }
}

/**
 * An annotation that holds a single expression. Used for more complex annotations.
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
  Expr expr;

  public ExprAnnotation() {
    super();
  }

  public ExprAnnotation(boolean allowMultiple) {
    super(allowMultiple);
  }

  @Override
  void resolveName(AnnotationDefinition definition, SymbolTable.SymbolResolver resolver) {
    verifyValuesCnt(definition, 1);
    expr = definition.values.getFirst();
    expr.accept(resolver);
  }

  @Override
  void typeCheck(AnnotationDefinition definition, TypeChecker typeChecker) {
    expr.accept(typeChecker);
  }

  @Override
  public String usageString() {
    return "[ " + name + " : <expr> ]";
  }

  public void verifyExprType(final TypeChecker tc, final Type type) {

    // Suppress these generic error messages, in case more specific errors already exist.
    // (this assumes that existing errors are related)
    final var hasError =  !tc.getErrors().isEmpty() || DeferredDiagnosticStore.hasError();

    Diagnostic.ensure(hasError || expr.type != null,
        () -> error("Invalid annotation expression", expr)
            .locationDescription(expr, "Unable to determine type"));
    Diagnostic.ensure(hasError || expr.type() == type,
        () -> error("Invalid annotation expression", expr)
            .locationDescription(expr, "Expression must be a %s", type));
  }
}

// ---------- SPECIFIC ANNOTATION CLASSES ----------

/**
 * The {@code [ zero : <register>(<expr>) ]} annotation.
 * This is its own class, as the typechecking is rather complex and determines
 * new properties.
 * <pre>{@code
 * [ zero : X(0) ]
 * register X: Bits<5> -> Bits<64>
 * }</pre>
 */
class ZeroConstraintAnnotation extends ExprAnnotation {
  @LazyInit
  List<ConstantValue> indices;

  @Override
  void typeCheck(AnnotationDefinition definition, TypeChecker typeChecker) {
    super.typeCheck(definition, typeChecker);
    var def = definition.target;

    if (!(expr instanceof CallIndexExpr callExpr)) {
      throw error("Invalid zero annotation", this)
          .locationDescription(this, "Zero annotation must be of form %s.", usageString())
          .build();
    }
    if (callExpr.computedTarget() != def) {
      throw error("Invalid zero annotation", callExpr.target)
          .locationDescription(callExpr.target,
              "Zero annotation target must be the annotated register.")
          .locationNote(def, "This is the register to target.")
          .build();
    }


    var args = callExpr.argsIndices.stream().flatMap(a -> a.values.stream()).toList();
    // FIXME: Ones we have multi dimensional registers,
    //   we must loose this restriction, so that there can be multiple indices set.
    if (args.size() != 1) {
      throw error("Invalid zero annotation", callExpr)
          .locationDescription(callExpr,
              "Exactly one register index was expected, but %s were provided.",
              args.size())
          .note("In the future it will be possible to have constraints on multiple dimensions.")
          .build();
    }

    this.indices = args.stream().map(expr -> {
      try {
        return typeChecker.constantEvaluator.eval(expr);
      } catch (EvaluationError e) {
        throw error("Invalid zero annotation", expr)
            .locationDescription(expr, "Index must be a constant expression.")
            .locationNote(def, "%s", requireNonNull(e.getMessage()))
            .build();
      }
    }).toList();

  }

  @Override
  public String usageString() {
    return "[ " + name + " : " + "<register>( <expr> ) ]";
  }
}

class EncodingConstraintAnnotation extends ExprAnnotation {

  @Override
  void resolveName(AnnotationDefinition definition, SymbolTable.SymbolResolver resolver) {
    var format = requireNonNull(((EncodingDefinition) definition.target).formatNode);
    // Extend annotation's symbol table by the symbol table of the encoding's format.
    definition.symbolTable().extendBy(format.symbolTable());
    super.resolveName(definition, resolver);
  }
}

class InstructionUndefinedAnnotation extends ExprAnnotation {

  @Override
  void resolveName(AnnotationDefinition definition, SymbolTable.SymbolResolver resolver) {
    var format = requireNonNull(((InstructionDefinition) definition.target).formatNode);
    // Extend annotation's symbol table by the symbol table of the encoding's format.
    definition.symbolTable().extendBy(format.symbolTable());
    super.resolveName(definition, resolver);
  }
}
