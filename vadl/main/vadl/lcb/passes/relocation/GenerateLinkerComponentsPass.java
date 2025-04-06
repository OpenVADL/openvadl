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

package vadl.lcb.passes.relocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
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
import vadl.viam.Relocation;
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
      RelocationStore<AutomaticallyGeneratedRelocation> automaticallyGenerated,
      Map<Format.FieldAccess, VariantKind> decodeVariantKinds
  ) {
    public static VariantKindStore empty() {
      return new VariantKindStore(RelocationStore.empty(), RelocationStore.empty(),
          new IdentityHashMap<>());
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

    /**
     * Get the decode variant kinds for a given {@link Format.Field}. There is a distinct
     * {@link VariantKind} for every {@link Format.FieldAccess} of a field.
     */
    public List<VariantKind> decodeVariantKindsByField(Format.Field field) {
      return this.decodeVariantKinds
          .entrySet()
          .stream()
          .filter(x -> x.getKey().fieldRef().equals(field))
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

    final var modifiers = new ArrayList<Modifier>();
    final var variantKinds = new ArrayList<VariantKind>();
    final var linkModifierToVariantKind = new ArrayList<Pair<Modifier, VariantKind>>();
    final var linkModifierToRelocation =
        new ArrayList<Pair<Modifier, HasRelocationComputationAndUpdate>>();
    final var variantStore = VariantKindStore.empty();
    final var compilerRelocations = new ArrayList<HasRelocationComputationAndUpdate>();
    final var fixups = new ArrayList<Fixup>();

    variantKinds.add(VariantKind.none());
    variantKinds.add(VariantKind.invalid());

    var relocations =
        viam.isa().map(isa -> isa.ownRelocations().stream()).orElseGet(Stream::empty).toList();

    var instructions =
        viam.isa().map(isa -> isa.ownInstructions().stream()).orElseGet(Stream::empty).toList();

    // Fixups and relocations for user defined relocations
    var candidates = new ArrayList<Pair<Relocation, Format.Field>>();
    for (var relocation : relocations) {
      for (var instruction : instructions) {
        // We cannot use all the fields of a format because not all are immediates.
        // That's why we need the `fieldUsages`.
        var immediateFields = fieldUsages.getImmediates(instruction);
        for (var imm : immediateFields) {
          candidates.add(new Pair<>(relocation, imm));
        }
      }
    }

    for (var candidate : candidates.stream().distinct().toList()) {
      var relocation = candidate.left();
      var field = candidate.right();
      var format = field.format();

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
          AutomaticallyGeneratedRelocation.createGcbRelocationCppFunction(relocation);

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

    // Next, we need to generate relocations for every immediate in an instruction.
    var candidatesAuto = new ArrayList<Pair<Format, Format.Field>>();
    for (var instruction : instructions) {
      // We cannot use all the fields of a format because not all are immediates.
      // That's why we need the `fieldUsages`.
      var immediateFields = fieldUsages.getImmediates(instruction);
      for (var imm : immediateFields) {
        candidatesAuto.add(new Pair<>(instruction.format(), imm));
      }
    }

    for (var candidate : candidatesAuto.stream().distinct().toList()) {
      var format = candidate.left();
      var imm = candidate.right();

      // Absolute
      genAbs(imm, modifiers, variantKinds, format, fixups, compilerRelocations, variantStore,
          linkModifierToRelocation);

      // Relative
      genRelative(imm, variantKinds, modifiers, format, fixups, compilerRelocations, variantStore,
          linkModifierToRelocation);
    }


    // Finally, we need to generate variant kinds
    // to apply the correct decode function after pseudo expansion.
    var fieldAccesses = viam.isa().map(isa -> isa.ownFormats().stream()).orElseGet(
        Stream::empty).flatMap(formats -> formats.fieldAccesses().stream()).toList();

    for (var fieldAccess : fieldAccesses) {
      var variantKind = VariantKind.decode(fieldAccess);
      variantKinds.add(variantKind);
      variantStore.decodeVariantKinds.put(fieldAccess, variantKind);
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

  private static void genRelative(Format.Field imm, List<VariantKind> variantKinds,
                                  List<Modifier> modifiers, Format format,
                                  List<Fixup> fixups,
                                  List<HasRelocationComputationAndUpdate> compilerRelocations,
                                  VariantKindStore variantStore,
                                  List<Pair<Modifier, HasRelocationComputationAndUpdate>>
                                      linkModifierToRelocation) {
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

  private static void genAbs(Format.Field imm, List<Modifier> modifiers,
                             List<VariantKind> variantKinds, Format format,
                             List<Fixup> fixups,
                             List<HasRelocationComputationAndUpdate> compilerRelocations,
                             VariantKindStore variantStore,
                             List<Pair<Modifier, HasRelocationComputationAndUpdate>>
                                 linkModifierToRelocation) {
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
}
