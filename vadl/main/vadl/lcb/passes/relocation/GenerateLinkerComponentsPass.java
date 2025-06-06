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
import java.util.Optional;
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
import vadl.gcb.passes.relocation.model.RelocationsBeforeElfExpansion;
import vadl.gcb.passes.relocation.model.UserSpecifiedRelocation;
import vadl.gcb.valuetypes.VariantKind;
import vadl.lcb.passes.llvmLowering.CreateFunctionsFromImmediatesPass;
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
      List<AutomaticallyGeneratedRelocation> automaticallyGeneratedRelocations,
      List<ImplementedUserSpecifiedRelocation> userSpecifiedRelocations,
      List<Pair<Modifier, VariantKind>> linkModifierToVariantKind,
      VariantKindStore variantKindStore,
      List<RelocationsBeforeElfExpansion> relocationsBeforeElfExpansion
  ) {

    /**
     * Get all elf relocations. These are all {@link ImplementedUserSpecifiedRelocation}
     * and all {@link AutomaticallyGeneratedRelocation}.
     */
    public List<HasRelocationComputationAndUpdate> elfRelocations() {
      return Stream.concat(userSpecifiedRelocations.stream(),
              automaticallyGeneratedRelocations.stream())
          .map(HasRelocationComputationAndUpdate.class::cast)
          .toList();
    }

  }

  /**
   * This data structure keeps track of the variant kind for relocations and immediates.
   */
  public record VariantKindStore(
      Map<RelocationsBeforeElfExpansion, VariantKind> relocationVariantKinds,
      Map<Format.FieldAccess, VariantKind> decodeVariantKinds
  ) {
    public static VariantKindStore empty() {
      return new VariantKindStore(new IdentityHashMap<>(), new IdentityHashMap<>());
    }

    /**
     * Store the variant {@link VariantKind} of an {@link RelocationsBeforeElfExpansion}.
     */
    public void addUserDefined(RelocationsBeforeElfExpansion relocation,
                               VariantKind kind) {
      relocationVariantKinds.put(relocation, kind);
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
    var immediates = (CreateFunctionsFromImmediatesPass.Output) passResults.lastResultOf(
        CreateFunctionsFromImmediatesPass.class);

    final var modifiers = new ArrayList<Modifier>();
    final var variantKinds = new ArrayList<VariantKind>();
    final var linkModifierToVariantKind = new ArrayList<Pair<Modifier, VariantKind>>();

    final var variantStore = VariantKindStore.empty();
    final var userSpecifiedRelocations =
        new ArrayList<ImplementedUserSpecifiedRelocation>();
    final var automaticallyGeneratedRelocations =
        new ArrayList<AutomaticallyGeneratedRelocation>();
    final var fixups = new ArrayList<Fixup>();
    final var relocationsBeforeElfExpansion = new ArrayList<RelocationsBeforeElfExpansion>();

    variantKinds.add(VariantKind.none());
    var pltVariantKind = VariantKind.plt();
    variantKinds.add(pltVariantKind);

    var relocations =
        viam.isa().map(isa -> isa.ownRelocations().stream()).orElseGet(Stream::empty).toList();

    var instructions =
        viam.isa().map(isa -> isa.ownInstructions().stream()).orElseGet(Stream::empty).toList();

    // Fixups and relocations for user defined relocations
    var candidates = new ArrayList<Pair<UserSpecifiedRelocation, Format.Field>>();
    for (var relocation : relocations) {

      // Create one variant per user defined relocation
      var variantKind = VariantKind.forUserDefinedRelocation(relocation);
      variantKinds.add(variantKind);

      // Create one modifier per user defined relocation
      var modifier = Modifier.from(relocation);
      modifiers.add(modifier);

      var gcbRelocationFunction =
          AutomaticallyGeneratedRelocation.createGcbRelocationCppFunction(relocation);

      var userSpecifiedRelocation =
          new UserSpecifiedRelocation(relocation.identifier, modifier, variantKind,
              gcbRelocationFunction, relocation);
      relocationsBeforeElfExpansion.add(userSpecifiedRelocation);

      variantStore.addUserDefined(userSpecifiedRelocation, variantKind);

      for (var encoding : immediates.encodings().keySet()) {
        candidates.add(Pair.of(userSpecifiedRelocation, encoding.fieldAccessRef().fieldRef()));
      }
    }

    for (var candidate : candidates.stream().distinct().toList()) {
      var userSpecifiedRelocation = candidate.left();
      var field = candidate.right();
      var format = field.format();

      // The `updateFieldFunction` is the cpp function which tells the compiler how
      // to update the field when a relocation has to be done.
      var updateFieldFunction =
          BitMaskFunctionGenerator.generateUpdateFunction(format, field);

      var liftedRelocation = new ImplementedUserSpecifiedRelocation(
          userSpecifiedRelocation.relocation(),
          userSpecifiedRelocation.variantKind(),
          userSpecifiedRelocation.modifier(),
          userSpecifiedRelocation.valueRelocation(),
          format,
          field,
          updateFieldFunction
      );

      var fixup = new Fixup(liftedRelocation);
      liftedRelocation.setFixup(fixup);
      fixups.add(fixup);

      userSpecifiedRelocations.add(liftedRelocation);
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
      genAbs(imm, format, fixups,
          modifiers, variantKinds, variantStore, linkModifierToVariantKind,
          automaticallyGeneratedRelocations, relocationsBeforeElfExpansion);

      // Relative
      genRelative(imm, format, fixups,
          modifiers, variantKinds, variantStore, linkModifierToVariantKind,
          automaticallyGeneratedRelocations, relocationsBeforeElfExpansion);
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

    var modifier = new Modifier("MO_PLT", CompilerRelocation.Kind.ABSOLUTE, Optional.empty());
    modifiers.add(modifier);

    // Needs to be the last one.
    variantKinds.add(VariantKind.invalid());
    linkModifierToVariantKind.add(Pair.of(modifier, pltVariantKind));

    return new Output(
        modifiers,
        variantKinds,
        fixups,
        automaticallyGeneratedRelocations,
        userSpecifiedRelocations,
        linkModifierToVariantKind,
        variantStore,
        relocationsBeforeElfExpansion
    );
  }

  private static void genRelative(Format.Field imm,
                                  Format format,
                                  List<Fixup> fixups,
                                  List<Modifier> modifiers,
                                  List<VariantKind> variantKinds,
                                  VariantKindStore variantStore,
                                  List<Pair<Modifier, VariantKind>> linkModifierToVariantKind,
                                  List<AutomaticallyGeneratedRelocation> compilerRelocations,
                                  List<RelocationsBeforeElfExpansion> relocationsBeforeExpansion) {
    var modifier = Modifier.relative(imm);
    modifiers.add(modifier);

    var variantKind = VariantKind.relative(imm);
    variantKinds.add(variantKind);
    linkModifierToVariantKind.add(Pair.of(modifier, variantKind));

    var updateFieldFunction =
        BitMaskFunctionGenerator.generateUpdateFunction(format, imm);
    var generated = AutomaticallyGeneratedRelocation.create(CompilerRelocation.Kind.RELATIVE,
        modifier,
        variantKind,
        format,
        imm,
        updateFieldFunction);

    var fixup = new Fixup(generated);
    generated.setFixup(fixup);
    fixups.add(fixup);

    compilerRelocations.add(generated);
    relocationsBeforeExpansion.add(generated);
    variantStore.relocationVariantKinds.put(generated, variantKind);
  }

  private static void genAbs(Format.Field imm,
                             Format format,
                             List<Fixup> fixups,
                             List<Modifier> modifiers,
                             List<VariantKind> variantKinds,
                             VariantKindStore variantStore,
                             List<Pair<Modifier, VariantKind>> linkModifierToVariantKind,
                             List<AutomaticallyGeneratedRelocation> compilerRelocations,
                             List<RelocationsBeforeElfExpansion> relocationsBeforeExpansion) {

    var modifier = Modifier.absolute(imm);
    modifiers.add(modifier);

    var variantKind = VariantKind.absolute(imm);
    variantKinds.add(variantKind);
    linkModifierToVariantKind.add(Pair.of(modifier, variantKind));

    var updateFieldFunction =
        BitMaskFunctionGenerator.generateUpdateFunction(format, imm);
    var generated = AutomaticallyGeneratedRelocation.create(CompilerRelocation.Kind.ABSOLUTE,
        modifier,
        variantKind,
        format,
        imm,
        updateFieldFunction);

    var fixup = new Fixup(generated);
    generated.setFixup(fixup);
    fixups.add(fixup);

    compilerRelocations.add(generated);
    relocationsBeforeExpansion.add(generated);
    variantStore.relocationVariantKinds.put(generated, variantKind);
  }
}
