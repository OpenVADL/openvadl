package vadl.gcb.passes.relocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.gcb.passes.relocation.model.LogicalRelocation;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Counter;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Relocation;
import vadl.viam.Specification;
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
        (DetectImmediatePass.ImmediateDetectionContainer) passResults.lastResultOf(
            DetectImmediatePass.class);

    // Generate relocations based on the specified relocations.
    // The user can specify relocations in the vadl specification.
    var u = generateRelocationsBasedOnSpecifiedRelocation(viam, immediates);
    var x = generateAbsoluteRelocationsForEveryFormat(viam, immediates);
    var y = generateRelativeRelocations(viam, immediates);

    return Stream.concat(Stream.concat(u, x), y)
        .sorted(Comparator.comparing(o -> o.name().value()))
        .toList();
  }

  private Stream<LogicalRelocation> generateRelocationsBasedOnSpecifiedRelocation(
      Specification viam, DetectImmediatePass.ImmediateDetectionContainer immediates) {
    var logicalRelocations = new ArrayList<LogicalRelocation>();
    var isa = viam.isa().orElse(null);
    if (isa == null) {
      return Stream.empty();
    }

    for (var instruction : isa.ownInstructions()) {
      instruction.behavior().getNodes(FuncCallNode.class)
          .map(FuncCallNode::function)
          .filter(Relocation.class::isInstance)
          .map(Relocation.class::cast)
          .forEach(relocation -> {
            for (var entry : immediates.get(instruction.format()).entrySet()) {
              if (entry.getValue() == DetectImmediatePass.FieldUsage.IMMEDIATE) {
                var field = entry.getKey();
                var updateFunction =
                    BitMaskFunctionGenerator.generateUpdateFunction(instruction.format(), field);
                logicalRelocations.add(
                    new LogicalRelocation(relocation,
                        field,
                        instruction.format(),
                        updateFunction));
              }
            }
          });
    }

    // We do not need to emit a relocation for every instruction.
    // We just care about the formats.
    return logicalRelocations.stream().distinct();
  }

  private Stream<LogicalRelocation> generateAbsoluteRelocationsForEveryFormat(
      Specification viam,
      DetectImmediatePass.ImmediateDetectionContainer immediates) {
    return viam.isa()
        .map(isa -> isa.ownFormats().stream())
        .orElse(Stream.empty())
        .flatMap(format -> {
          var relocations = new ArrayList<LogicalRelocation>();
          for (var entry : immediates.get(format).entrySet()) {
            if (entry.getValue() == DetectImmediatePass.FieldUsage.IMMEDIATE) {
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
      DetectImmediatePass.ImmediateDetectionContainer immediates) {
    return viam.isa()
        .map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
        .filter(instruction -> instruction.behavior().getNodes(ReadRegNode.class)
            .anyMatch(x -> x.staticCounterAccess() != null))
        .map(Instruction::format)
        .distinct()
        .flatMap(format -> {
          var relocations = new ArrayList<LogicalRelocation>();
          for (var entry : immediates.get(format).entrySet()) {
            if (entry.getValue() == DetectImmediatePass.FieldUsage.IMMEDIATE) {
              var field = entry.getKey();
              var updateFunction =
                  BitMaskFunctionGenerator.generateUpdateFunction(format, field);
              relocations.add(
                  new LogicalRelocation(LogicalRelocation.Kind.RELATIVE, field, format,
                      updateFunction));
            }
          }
          return relocations.stream();
        })
        .distinct();
  }
}
