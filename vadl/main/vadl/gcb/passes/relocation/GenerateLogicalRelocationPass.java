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
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Register;
import vadl.viam.Register.Counter;
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
    // Generate relocations based on the specified relocations.
    // The user can specify relocations in the vadl specification.
    var u = generateRelocationsBasedOnSpecifiedRelocation(viam);
    var v = generateRelocationsBasedOnSpecifiedRelocationPseudoInstructions(viam);
    var x = generateAbsoluteRelocationsForEveryFormat(viam);
    var y = generateRelativeRelocations(viam);

    return Stream.concat(Stream.concat(Stream.concat(u, v), x), y)
        .sorted(Comparator.comparing(o -> o.name().value()))
        .toList();
  }

  private Stream<LogicalRelocation> generateRelocationsBasedOnSpecifiedRelocation(
      Specification viam) {
    var logicalRelocations = new ArrayList<LogicalRelocation>();
    for (var isa : viam.isas().toList()) {
      var pc = isa.pc();
      for (var instruction : isa.ownInstructions()) {
        instruction.behavior().getNodes(FuncCallNode.class)
            .map(FuncCallNode::function)
            .filter(Relocation.class::isInstance)
            .map(Relocation.class::cast)
            .forEach(relocation -> {
              logicalRelocations.add(new LogicalRelocation(pc, relocation, instruction.format()));
            });
      }
    }

    // We do not need to emit a relocation for every instruction.
    // We just care about the formats.
    return logicalRelocations.stream().distinct();
  }

  private Stream<LogicalRelocation> generateRelocationsBasedOnSpecifiedRelocationPseudoInstructions(
      Specification viam) {
    var logicalRelocations = new ArrayList<LogicalRelocation>();
    for (var isa : viam.isas().toList()) {
      var pc = isa.pc();
      for (var instruction : isa.ownPseudoInstructions()) {
        /* We would like to generate relocations for every pseudo instruction.
           However, the problem is that pseudo instruction has no relocation itself
           because it contains references to other instructions.
           Example:

          pseudo instruction CALL( symbol : Bits<32> ) =
          {
              LUI{ rd = 1, imm20 = hi20( symbol ) }
              JALR{ rd = 1, rs1 = 1, imm = lo12( symbol ) }
          }

           The pseudo instruction CALL will have two InstrCallNode nodes which have the
           references to the instructions LUI and JALR.
           We check both machine instructions whether the have relocations. If they have then
           we generate relocations for it.
         */

        instruction.behavior().getNodes(FuncCallNode.class)
            .map(FuncCallNode::function)
            .filter(Relocation.class::isInstance)
            .map(Relocation.class::cast)
            .forEach(relocation -> {
              //logicalRelocations.add(new LogicalRelocation(pc, relocation, instruction. ()));
            });
      }
    }

    // We do not need to emit a relocation for every instruction.
    // We just care about the formats.
    return logicalRelocations.stream().distinct();
  }

  private Stream<LogicalRelocation> generateAbsoluteRelocationsForEveryFormat(Specification viam) {
    return viam.isas()
        .flatMap(isa -> isa.ownFormats().stream())
        .map(format -> new LogicalRelocation(LogicalRelocation.Kind.ABSOLUTE, format));
  }

  /**
   * Generates relative relocations for formats when the instruction touches a {@link Counter}.
   */
  private Stream<LogicalRelocation> generateRelativeRelocations(Specification viam) {
    return viam.isas()
        .flatMap(isa -> isa.ownInstructions().stream())
        .filter(instruction -> instruction.behavior().getNodes(ReadRegNode.class)
            .map(ReadRegNode::register)
            .anyMatch(Register.Counter.class::isInstance))
        .map(Instruction::format)
        .distinct()
        .map(format -> new LogicalRelocation(LogicalRelocation.Kind.RELATIVE, format))
        .distinct();
  }
}
