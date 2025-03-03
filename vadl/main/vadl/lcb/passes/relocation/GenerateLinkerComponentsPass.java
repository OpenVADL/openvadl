package vadl.lcb.passes.relocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.relocation.BitMaskFunctionGenerator;
import vadl.gcb.passes.relocation.model.AutomaticallyGeneratedRelocation;
import vadl.gcb.passes.relocation.model.CompilerRelocation;
import vadl.gcb.passes.relocation.model.Fixup;
import vadl.gcb.passes.relocation.model.HasRelocationComputationAndUpdate;
import vadl.gcb.passes.relocation.model.ImplementedUserSpecifiedRelocation;
import vadl.gcb.passes.relocation.model.Modifier;
import vadl.gcb.valuetypes.VariantKind;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Specification;

/**
 * This pass generates variant kinds, fixups and relocations.
 */
public class GenerateLinkerComponentsPass extends Pass {
  public GenerateLinkerComponentsPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateLinkerComponentsPass");
  }

  /**
   * Output for this pass.
   */
  public record Output(
      List<Modifier> modifiers,
      List<VariantKind> variantKinds,
      List<Fixup> fixups,
      List<HasRelocationComputationAndUpdate> elfRelocations,
      List<Pair<Modifier, VariantKind>> linkModifierToVariantKind,
      List<Pair<Modifier, HasRelocationComputationAndUpdate>> linkModifierToRelocation,
      VariantKindStore variantKindStore
  ) {

  }

  /**
   * Data structure to find {@link VariantKind} by relocation and {@link Format.Field}.
   */
  public record RelocationStore<T>(
      IdentityHashMap<Pair<T, Format.Field>, VariantKind> absolute,
      IdentityHashMap<Pair<T, Format.Field>, VariantKind> relative) {
    public static <X> RelocationStore<X> empty() {
      return new RelocationStore<>(new IdentityHashMap<>(), new IdentityHashMap<>());
    }
  }

  /**
   * This data structure keeps track of the variant kind for relocations and immediates.
   */
  public record VariantKindStore(
      RelocationStore<ImplementedUserSpecifiedRelocation> userDefined,
      RelocationStore<AutomaticallyGeneratedRelocation> automaticallyGenerated
  ) {
    public static VariantKindStore empty() {
      return new VariantKindStore(RelocationStore.empty(), RelocationStore.empty());
    }

    /**
     * Store the variant {@link VariantKind} of an {@link ImplementedUserSpecifiedRelocation}.
     */
    public void addUserDefined(ImplementedUserSpecifiedRelocation relocation,
                               Format.Field field,
                               VariantKind kind) {
      if (relocation.kind() == CompilerRelocation.Kind.ABSOLUTE) {
        this.userDefined.absolute.put(Pair.of(relocation, field), kind);
      } else {
        this.userDefined.relative.put(Pair.of(relocation, field), kind);
      }
    }

    /**
     * Store the {@link VariantKind} of an {@link AutomaticallyGeneratedRelocation}.
     */
    public void addAutomaticallyGeneratedRelocation(AutomaticallyGeneratedRelocation relocation,
                                                    Format.Field field,
                                                    VariantKind kind) {
      if (relocation.kind() == CompilerRelocation.Kind.ABSOLUTE) {
        this.automaticallyGenerated.absolute.put(Pair.of(relocation, field), kind);
      } else {
        this.automaticallyGenerated.relative.put(Pair.of(relocation, field), kind);
      }
    }

    /**
     * Get the absolute variant kinds for a given {@link Format.Field} when we know that it has
     * been automatically generated. It is expected that only one variant kind will be returned.
     * However, it might be possible that an instruction has multiple relocations in one and that's
     * why return a list.
     */
    public List<VariantKind> absoluteVariantKindsByAutomaticGeneratedRelocationAndField(
        Format.Field field) {
      return this.automaticallyGenerated.absolute
          .entrySet()
          .stream()
          .filter(x -> x.getKey().right().equals(field))
          .map(Map.Entry::getValue)
          .toList();
    }
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    // The hierarchy is variant kind > fixup > relocation.
    var fieldUsages =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
            IdentifyFieldUsagePass.class);

    var modifiers = new ArrayList<Modifier>();
    var variantKinds = new ArrayList<VariantKind>();
    var linkModifierToVariantKind = new ArrayList<Pair<Modifier, VariantKind>>();
    var linkModifierToRelocation =
        new ArrayList<Pair<Modifier, HasRelocationComputationAndUpdate>>();
    var variantStore = VariantKindStore.empty();
    var compilerRelocations = new ArrayList<HasRelocationComputationAndUpdate>();
    var fixups = new ArrayList<Fixup>();

    variantKinds.add(VariantKind.none());
    variantKinds.add(VariantKind.invalid());

    var relocations =
        viam.isa().map(isa -> isa.ownRelocations().stream()).orElseGet(Stream::empty).toList();

    var formats =
        viam.isa().map(isa -> isa.ownFormats().stream()).orElseGet(Stream::empty).toList();
    // Fixups and relocations for user defined relocations
    for (var relocation : relocations) {
      for (var format : formats) {
        // We cannot use all the fields of a format because not all are immediates.
        // That's why we need the `fieldUsages`.
        var immediateFields = fieldUsages.getImmediates(format);

        // Generate a relocation for every immediate in the format.
        // However, usually, it should be just one.
        for (var field : immediateFields) {
          // Create a variant kind for every format immediate field.
          var variantKind = relocation.isAbsolute() ? VariantKind.absolute(relocation, field)
              : VariantKind.relative(relocation, field);
          var modifier = Modifier.from(relocation, field);
          variantKinds.add(variantKind);

          // The `updateFieldFunction` is the cpp function which tells the compiler how
          // to update the field when a relocation has to be done.
          var updateFieldFunction =
              BitMaskFunctionGenerator.generateUpdateFunction(format, field);
          var gcbRelocationFunction =
              CppTypeNormalizationPass.createGcbRelocationCppFunction(relocation);

          var liftedRelocation = new ImplementedUserSpecifiedRelocation(
              relocation,
              variantKind,
              modifier,
              gcbRelocationFunction,
              format,
              field,
              updateFieldFunction
          );

          modifiers.add(modifier);

          fixups.add(new Fixup(liftedRelocation));
          compilerRelocations.add(liftedRelocation);
          linkModifierToRelocation.add(Pair.of(modifier, liftedRelocation));
          variantStore.addUserDefined(liftedRelocation, field, variantKind);
          linkModifierToVariantKind.add(Pair.of(modifier, variantKind));
        }
      }
    }

    // Next, we need to generate relocations for every immediate in an instruction.
    for (var format : formats) {
      // We cannot use all the fields of a format because not all are immediates.
      // That's why we need the `fieldUsages`.
      var immediateFields = fieldUsages.getImmediates(format);
      // Generate a relocation for every immediate in the format.
      // However, usually, it should be just one.

      // Absolute
      for (var imm : immediateFields) {
        var absoluteVariantKind = VariantKind.absolute(imm);
        var modifier = Modifier.absolute(imm);
        modifiers.add(modifier);

        variantKinds.add(absoluteVariantKind);
        var updateFieldFunction =
            BitMaskFunctionGenerator.generateUpdateFunction(format, imm);
        var generated = AutomaticallyGeneratedRelocation.create(CompilerRelocation.Kind.ABSOLUTE,
            absoluteVariantKind,
            format,
            imm,
            updateFieldFunction);


        fixups.add(new Fixup(generated));
        compilerRelocations.add(generated);
        variantStore.addAutomaticallyGeneratedRelocation(generated, imm, absoluteVariantKind);
        linkModifierToRelocation.add(Pair.of(modifier, generated));
      }

      // Relative
      for (var imm : immediateFields) {
        var relativeVariantKind = VariantKind.relative(imm);
        var modifier = Modifier.relative(imm);

        variantKinds.add(relativeVariantKind);
        modifiers.add(modifier);

        var updateFieldFunction =
            BitMaskFunctionGenerator.generateUpdateFunction(format, imm);
        var generated = AutomaticallyGeneratedRelocation.create(CompilerRelocation.Kind.RELATIVE,
            relativeVariantKind,
            format,
            imm,
            updateFieldFunction);


        fixups.add(new Fixup(generated));
        compilerRelocations.add(generated);
        variantStore.addAutomaticallyGeneratedRelocation(generated, imm, relativeVariantKind);
        linkModifierToRelocation.add(Pair.of(modifier, generated));
      }
    }

    return new Output(
        modifiers,
        variantKinds,
        fixups,
        compilerRelocations,
        linkModifierToVariantKind,
        linkModifierToRelocation,
        variantStore
    );
  }
}
