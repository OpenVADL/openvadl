package vadl.lcb.passes.relocation;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.model.VariantKind;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.relocation.BitMaskFunctionGenerator;
import vadl.gcb.passes.relocation.model.AutomaticallyGeneratedRelocation;
import vadl.gcb.passes.relocation.model.CompilerRelocation;
import vadl.gcb.passes.relocation.model.Fixup;
import vadl.gcb.passes.relocation.model.ImplementedUserSpecifiedRelocation;
import vadl.gcb.passes.relocation.model.Modifier;
import vadl.gcb.passes.relocation.model.ModifierCtx;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldRefNode;

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
      List<VariantKind> variantKinds,
      List<Fixup> fixups,
      List<CompilerRelocation> elfRelocations,
      List<Pair<Modifier, VariantKind>> linkModifierToVariantKind
  ) {

  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    // The hierarchy is variant kind > fixup > relocation.
    var fieldUsages =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
            IdentifyFieldUsagePass.class);

    var variantKinds = new ArrayList<VariantKind>();
    var linkModifierToVariantKind = new ArrayList<Pair<Modifier, VariantKind>>();
    var compilerRelocations = new ArrayList<CompilerRelocation>();
    var fixups = new ArrayList<Fixup>();

    variantKinds.add(VariantKind.none());
    variantKinds.add(VariantKind.invalid());

    var relocations =
        viam.isa().map(isa -> isa.ownRelocations().stream()).orElseGet(Stream::empty).toList();

    // Variant Kinds
    for (var relocation : relocations) {
      var abs = VariantKind.absolute(relocation);
      var rel = VariantKind.relative(relocation);

      var ctx = ensureNonNull(relocation.extension(ModifierCtx.class),
          () -> Diagnostic.error(
              "Relocation has no generated modifier. It should have been automatically generated.",
              relocation.sourceLocation()));

      var absModifier = ctx.absoluteModifier();
      var relModifier = ctx.relativeModifier();

      // We need to store a link to map from a modifier to variant kind.
      linkModifierToVariantKind.add(Pair.of(absModifier, abs));
      linkModifierToVariantKind.add(Pair.of(relModifier, rel));

      variantKinds.add(abs);
      variantKinds.add(rel);
    }

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
          // The `updateFieldFunction` is the cpp function which tells the compiler how
          // to update the field when a relocation has to be done.
          var updateFieldFunction =
              BitMaskFunctionGenerator.generateUpdateFunction(format, field);
          var gcbRelocationFunction =
              CppTypeNormalizationPass.createGcbRelocationCppFunction(relocation);

          var liftedRelocation = new ImplementedUserSpecifiedRelocation(
              relocation,
              gcbRelocationFunction,
              format,
              field,
              updateFieldFunction
          );

          fixups.add(new Fixup(liftedRelocation));
          compilerRelocations.add(liftedRelocation);
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
        variantKinds.add(absoluteVariantKind);
        var updateFieldFunction =
            BitMaskFunctionGenerator.generateUpdateFunction(format, imm);
        var generated = AutomaticallyGeneratedRelocation.create(CompilerRelocation.Kind.ABSOLUTE,
            format,
            imm,
            updateFieldFunction);

        fixups.add(new Fixup(generated));
        compilerRelocations.add(generated);
      }

      // Relative
      for (var imm : immediateFields) {
        var relativeVariantKind = VariantKind.relative(imm);
        variantKinds.add(relativeVariantKind);
        var updateFieldFunction =
            BitMaskFunctionGenerator.generateUpdateFunction(format, imm);
        var generated = AutomaticallyGeneratedRelocation.create(CompilerRelocation.Kind.RELATIVE,
            format,
            imm,
            updateFieldFunction);

        fixups.add(new Fixup(generated));
        compilerRelocations.add(generated);
      }
    }

    /*
    // Immediates have variant kind as well because we emit them in the Pseudo Expansion
    var tableGenImmediateRecords = (List<TableGenImmediateRecord>) passResults.lastResultOf(
        GenerateTableGenImmediateRecordPass.class);
    var tableGenMachineRecords = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);
    var instructionsPerImmediateVariant = new IdentityHashMap<VariantKind, List<Instruction>>();

    for (var imm : tableGenImmediateRecords) {
      variantKinds.add(imm.variantKind());
      extend(variantKindMap, imm.fieldAccessRef().fieldRef(), imm.variantKind());

      for (var machine : tableGenMachineRecords) {
        if (machine.getInOperands().stream()
            .anyMatch(
                operand -> operandMatchesImmediate(imm.fieldAccessRef().fieldRef(), operand))) {
          extend(instructionsPerImmediateVariant, imm.variantKind(), machine.instruction());
        }
      }
    }
     */

    return new Output(
        variantKinds,
        fixups,
        compilerRelocations,
        linkModifierToVariantKind
    );
  }

  /**
   * Checks whether the origin of {@code operand} is equal to {@code imm}.
   */
  public static boolean operandMatchesImmediate(Format.Field imm,
                                                TableGenInstructionOperand operand) {
    var isFieldAccess =
        operand.origin() instanceof LlvmFieldAccessRefNode fieldAccessRefNode
            && fieldAccessRefNode.immediateOperand().fieldAccessRef().fieldRef().equals(imm);
    var isFieldRef = operand.origin() instanceof FieldRefNode fieldRefNode
        && fieldRefNode.formatField().equals(imm);
    var isLabel = operand.origin() instanceof LlvmBasicBlockSD basicBlockSD
        && basicBlockSD.fieldAccess().fieldRef().equals(imm);

    return isFieldAccess || isFieldRef || isLabel;
  }

  private <K, V> void extend(Map<K, List<V>> map,
                             K key,
                             V value) {
    map.compute(key, (k, v) -> {
      if (v == null) {
        return new ArrayList<>(List.of(value));
      } else {
        v.add(value);
        return v;
      }
    });
  }
}
