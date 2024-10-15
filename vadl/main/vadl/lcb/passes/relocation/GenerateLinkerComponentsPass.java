package vadl.lcb.passes.relocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.model.VariantKind;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.gcb.passes.relocation.BitMaskFunctionGenerator;
import vadl.gcb.passes.relocation.IdentifyFieldUsagePass;
import vadl.gcb.passes.relocation.model.CompilerRelocation;
import vadl.gcb.passes.relocation.model.ConcreteLogicalRelocation;
import vadl.gcb.passes.relocation.model.GeneratedRelocation;
import vadl.gcb.passes.relocation.model.RelocationLowerable;
import vadl.gcb.passes.relocation.model.Fixup;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.immediates.GenerateTableGenImmediateRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldRefNode;

public class GenerateLinkerComponentsPass extends Pass {
  public GenerateLinkerComponentsPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateLinkerComponentsPass");
  }

  public record Output(
      List<VariantKind> variantKinds,
      List<Fixup> fixups,
      List<CompilerRelocation> elfRelocations,
      Map<Format.Field, List<VariantKind>> variantKindMap,
      Map<CompilerRelocation, List<Instruction>> instructionsPerCompilerRelocation,
      Map<VariantKind, List<Instruction>> instructionPerImmediateVariant,
      Map<Format.Field, List<Fixup>> fixupsByField
  ) {

  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    // The hierarchy is variant kind > fixup > relocation.
    var immediates =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
            IdentifyFieldUsagePass.class);

    var variantKindMap = new IdentityHashMap<Format.Field, List<VariantKind>>();
    var variantKinds = new ArrayList<VariantKind>();
    var compilerRelocations = new ArrayList<CompilerRelocation>();
    var fixups = new ArrayList<Fixup>();
    var relocationPerFormat = new IdentityHashMap<Format, List<CompilerRelocation>>();
    var instructionsPerCompilerRelocation =
        new IdentityHashMap<CompilerRelocation, List<Instruction>>();
    var instructionsPerImmediateVariant = new IdentityHashMap<VariantKind, List<Instruction>>();
    var fixupsByField =
        new IdentityHashMap<Format.Field, List<Fixup>>();

    variantKinds.add(VariantKind.None());
    variantKinds.add(VariantKind.Invalid());

    // User defined relocations
    viam.isa().map(isa -> isa.ownRelocations().stream()).orElseGet(Stream::empty)
        .forEach(relocation -> {
          var variantKind = new VariantKind(relocation);
          var alreadySeen = new HashSet<Format.Field>();

          // Create a concrete relocations for each user defined + immediate field combination.
          // The reason is that it might exist a pseudo instruction which sets a relocation.
          viam.isa().map(isa -> isa.ownInstructions().stream()).orElseGet(Stream::empty)
              .forEach(instruction -> {
                var imms = immediates.getImmediates(instruction.format());

                for (var field : imms) {
                  var format = instruction.format();
                  var updateFieldFunction =
                      BitMaskFunctionGenerator.generateUpdateFunction(format, field);
                  var cppConformRelocation =
                      CppTypeNormalizationPass.makeTypesCppConform(relocation);

                  if (!alreadySeen.contains(field)) {
                    var concrete =
                        new ConcreteLogicalRelocation(relocation, cppConformRelocation,
                            format,
                            field,
                            updateFieldFunction,
                            variantKind);
                    var fixup = new Fixup(concrete);
                    compilerRelocations.add(concrete);
                    fixups.add(fixup);
                    extend(relocationPerFormat, format, concrete);
                    extend(variantKindMap, field, variantKind);
                    extend(instructionsPerCompilerRelocation, concrete, instruction);
                    alreadySeen.add(field);
                  }
                }
              });

          // Bookkeeping
          variantKinds.add(variantKind);
        });

    var absoluteVariantKind = VariantKind.Absolute();
    variantKinds.add(absoluteVariantKind);
    viam.isa().map(isa -> isa.ownFormats().stream()).orElseGet(Stream::empty)
        .forEach(format -> {
          var imms = immediates.getImmediates(format);
          for (var imm : imms) {
            var updateFieldFunction =
                BitMaskFunctionGenerator.generateUpdateFunction(format, imm);
            var generated = GeneratedRelocation.create(CompilerRelocation.Kind.ABSOLUTE,
                format,
                imm,
                updateFieldFunction,
                absoluteVariantKind
            );
            var fixup = new Fixup(generated);
            compilerRelocations.add(generated);
            extend(variantKindMap, imm, absoluteVariantKind);
            extend(fixupsByField, imm, fixup);
            fixups.add(fixup);
          }
        });

    var relativeRelocation = VariantKind.Relative();
    variantKinds.add(relativeRelocation);
    viam.isa().map(isa -> isa.ownFormats().stream()).orElseGet(Stream::empty)
        .forEach(format -> {
          var imms = immediates.getImmediates(format);
          for (var imm : imms) {
            var updateFieldFunction =
                BitMaskFunctionGenerator.generateUpdateFunction(format, imm);
            var generated = GeneratedRelocation.create(CompilerRelocation.Kind.RELATIVE,
                format,
                imm,
                updateFieldFunction,
                relativeRelocation
            );
            var fixup = new Fixup(generated);
            compilerRelocations.add(generated);
            extend(variantKindMap, imm, relativeRelocation);
            extend(fixupsByField, imm, fixup);
            fixups.add(fixup);
          }
        });

    // Immediates have variant kind as well because we emit them in the Pseudo Expansion
    var tableGenImmediateRecords = (List<TableGenImmediateRecord>) passResults.lastResultOf(
        GenerateTableGenImmediateRecordPass.class);
    var tableGenMachineRecords = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);

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

    return new Output(
        variantKinds,
        fixups,
        compilerRelocations,
        variantKindMap,
        instructionsPerCompilerRelocation,
        instructionsPerImmediateVariant,
        fixupsByField
    );
  }

  public static boolean operandMatchesImmediate(Format.Field imm,
                                                TableGenInstructionOperand operand) {
    var isFieldAccess =
        operand.origin() instanceof LlvmFieldAccessRefNode fieldAccessRefNode
            && fieldAccessRefNode.immediateOperand().fieldAccessRef().fieldRef() ==
            imm;
    var isFieldRef = operand.origin() instanceof FieldRefNode fieldRefNode
        && fieldRefNode.formatField() == imm;
    var isLabel = operand.origin() instanceof LlvmBasicBlockSD basicBlockSD
        && basicBlockSD.fieldAccess().fieldRef() == imm;

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
