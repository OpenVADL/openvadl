package vadl.gcb.passes.relocation;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.error.Diagnostic;
import vadl.gcb.passes.relocation.model.LogicalRelocation;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Counter;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Relocation;
import vadl.viam.Specification;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.ReadRegNode;

/**
 * Generate {@link LogicalRelocation} from the specification.
 * First, every instruction that uses a {@link Relocation} must emit a {@link LogicalRelocation}.
 * Second, every format must emit a relative {@link LogicalRelocation} when
 * the one of its {@link Instruction#behavior()} reads from the
 * {@link InstructionSetArchitecture#pc()}.
 * Third, every format must emit an absolute {@link LogicalRelocation}.
 */
public class GenerateLogicalRelocationPass extends Pass {
  public GenerateLogicalRelocationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("generateLogicalRelocationPass");
  }

  @Nullable
  @Override
  public List<LogicalRelocation> execute(PassResults passResults, Specification viam)
      throws IOException {
    var immediates =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
            IdentifyFieldUsagePass.class);

    // Generate relocations based on the specified relocations.
    // The user can specify relocations in the vadl specification.
    var u = generateRelocationsBasedOnUsedRelocations(viam, immediates);
    var v = generateRelocationsBasedOnPseudoInstructions(viam, immediates);
    var x = generateAbsoluteRelocationsForEveryFormat(viam, immediates);
    var y = generateRelativeRelocations(viam, immediates);

    return Stream.concat(Stream.concat(Stream.concat(u, v), x), y)
        .sorted(Comparator.comparing(o -> o.identifier().lower()))
        .toList();
  }

  private Stream<LogicalRelocation> generateRelocationsBasedOnPseudoInstructions(
      Specification viam,
      IdentifyFieldUsagePass.ImmediateDetectionContainer immediates) {
    var logicalRelocations = new ArrayList<LogicalRelocation>();

    viam.isa().ifPresent((isa) -> {
      for (var pseudo : isa.ownPseudoInstructions()) {
        pseudo
            .behavior()
            .getNodes(FuncCallNode.class)
            .filter(funcCallNode -> funcCallNode.function() instanceof Relocation)
            .forEach(funcCallNode -> {
              var usages = funcCallNode.usages().toList();
              var relocation = (Relocation) funcCallNode.function();
              ensure(usages.size() == 1,
                  () -> Diagnostic.error("There must be usage for the relocation",
                      funcCallNode.sourceLocation()).build());
              var usage = (InstrCallNode) usages.get(0);
              ensureNonNull(usage, () -> Diagnostic.error("There must be usage for the relocation",
                  funcCallNode.sourceLocation()).build());

              // We have to find the field which the relocation is applied on.
              // We have two lists: paramFields and arguments
              // LUI{ rd = 1 as Bits5, imm = hi20( symbol ) }
              // paramFields: rd, imm
              // arguments:   1, hi20

              Streams.zip(usage.getParamFields().stream(), usage.arguments().stream(),
                      Pair::new)
                  .filter(pair -> pair.right() == funcCallNode)
                  .map(Pair::left)
                  .forEach(field -> {
                    // Now, we have the `field` for the relocation.
                    var instruction = usage.target();
                    var format = instruction.format();
                    var updateFunction =
                        BitMaskFunctionGenerator.generateUpdateFunction(format, field);
                    var cppConformRelocation =
                        CppTypeNormalizationPass.makeTypesCppConform(relocation);
                    logicalRelocations.add(
                        new LogicalRelocation(
                            relocation,
                            cppConformRelocation,
                            field,
                            format,
                            updateFunction));
                  });
            });
      }
    });

    return logicalRelocations.stream().distinct();
  }

  /**
   * This method generates relocations when a relocation is used in a machine instruction.
   */
  private Stream<LogicalRelocation> generateRelocationsBasedOnUsedRelocations(
      Specification viam, IdentifyFieldUsagePass.ImmediateDetectionContainer immediates) {
    var logicalRelocations = new ArrayList<LogicalRelocation>();

    viam.isa().ifPresent((isa) -> {
      for (var instruction : isa.ownInstructions()) {
        instruction.behavior().getNodes(FuncCallNode.class)
            .map(FuncCallNode::function)
            .filter(Relocation.class::isInstance)
            .map(Relocation.class::cast)
            .forEach(relocation -> {
              for (var entry : immediates.getFieldUsage(instruction.format()).entrySet()) {
                if (entry.getValue() == IdentifyFieldUsagePass.FieldUsage.IMMEDIATE) {
                  var field = entry.getKey();
                  var updateFunction =
                      BitMaskFunctionGenerator.generateUpdateFunction(instruction.format(), field);
                  var cppConformRelocation =
                      CppTypeNormalizationPass.makeTypesCppConform(relocation);
                  logicalRelocations.add(
                      new LogicalRelocation(
                          relocation,
                          cppConformRelocation,
                          field,
                          instruction.format(),
                          updateFunction));
                }
              }
            });
      }
    });

    // We do not need to emit a relocation for every instruction.
    // We just care about the formats.
    return logicalRelocations.stream().distinct();
  }

  private Stream<LogicalRelocation> generateAbsoluteRelocationsForEveryFormat(
      Specification viam,
      IdentifyFieldUsagePass.ImmediateDetectionContainer immediates) {
    return viam.isa()
        .map(isa -> isa.ownFormats().stream())
        .orElse(Stream.empty())
        .flatMap(format -> {
          var relocations = new ArrayList<LogicalRelocation>();
          for (var entry : immediates.getFieldUsage(format).entrySet()) {
            if (entry.getValue() == IdentifyFieldUsagePass.FieldUsage.IMMEDIATE) {
              var field = entry.getKey();
              var updateFunction =
                  BitMaskFunctionGenerator.generateUpdateFunction(format, field);
              relocations.add(
                  new LogicalRelocation(LogicalRelocation.Kind.ABSOLUTE, field, format,
                      updateFunction));
            }
          }
          return relocations.stream();
        });
  }

  /**
   * Generates relative relocations for formats when the instruction touches a {@link Counter}.
   */
  private Stream<LogicalRelocation> generateRelativeRelocations(
      Specification viam,
      IdentifyFieldUsagePass.ImmediateDetectionContainer immediates) {
    return viam.isa()
        .map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
        .filter(instruction -> instruction.behavior().getNodes(ReadRegNode.class)
            .anyMatch(x -> x.staticCounterAccess() != null))
        .map(Instruction::format)
        .distinct()
        .flatMap(format -> {
          var relocations = new ArrayList<LogicalRelocation>();
          for (var entry : immediates.getFieldUsage(format).entrySet()) {
            if (entry.getValue() == IdentifyFieldUsagePass.FieldUsage.IMMEDIATE) {
              var field = entry.getKey();
              var updateFunction =
                  BitMaskFunctionGenerator.generateUpdateFunction(format, field);
              relocations.add(
                  new LogicalRelocation(LogicalRelocation.Kind.RELATIVE, field, format,
                      updateFunction));
            }
          }
          return relocations.stream();
        });
  }
}
