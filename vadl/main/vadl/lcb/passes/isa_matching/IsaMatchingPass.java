package vadl.lcb.passes.isa_matching;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Register;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.EndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyConstantValueMatcher;
import vadl.viam.matching.impl.AnyReadRegFileMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.TypcastMatcher;

/**
 * A {@link InstructionSetArchitecture} contains a {@link List} of {@link Instruction}.
 * One of the most important tasks of the LCB is to recognize the semantics of the machine
 * instruction and create a mapping from LLVM's SelectionDag to the machine instruction.
 * Most instructions in an instruction set architecture will be "simple" and
 * a lot of them are required in almost every instruction set architecture. The goal
 * of {@link IsaMatchingPass} to label instructions which can be recognized.
 * Why is this useful?
 * At some places, we need to create machine instructions by hand in LLVM, and we need to
 * know which instructions are supported by instruction set. This labelling makes it much
 * easier to search for these instructions.
 */
public class IsaMatchingPass extends Pass {
  @Override
  public PassName getName() {
    return new PassName("IsaMatchingPass");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {
    HashMap<InstructionLabel, Instruction> matched = new HashMap<>();

    viam.isas().forEach(isa -> isa.instructions().forEach(instruction -> {
      if (findUnsignedAdd32Bit(instruction.behavior())) {
        matched.put(InstructionLabel.ADD_U_32, instruction);
      } else if (findSignedAdd32Bit(instruction.behavior())) {
        matched.put(InstructionLabel.ADD_S_32, instruction);
      } else if (findAddWithImmediate32Bit(instruction.behavior())) {
        matched.put(InstructionLabel.ADDI_32, instruction);
      } else if (isa.pc() != null && findBeq(instruction.behavior(), isa.pc())) {
        matched.put(InstructionLabel.BEQ, instruction);
      }
    }));

    return matched;
  }

  private boolean writesExactlyOneRegisterClassWithType(Graph graph, Type resultType) {
    var writes = graph.getNodes(WriteRegFileNode.class).toList();

    if (writes.size() > 1) {
      return false;
    }

    return writes.get(0).registerFile().resultType() == resultType;
  }

  private boolean findUnsignedAdd32Bit(Graph behavior) {
    var matched = TreeMatcher.matches(behavior.getNodes(),
            new BuiltInMatcher(BuiltInTable.ADD, List.of(
                new TypcastMatcher(Type.unsignedInt(32), new AnyReadRegFileMatcher()),
                new TypcastMatcher(Type.unsignedInt(32), new AnyReadRegFileMatcher())
            )))
        .stream()
        .map(x -> ((BuiltInCall) x).type())
        .filter(ty -> ty instanceof UIntType && ((UIntType) ty).bitWidth() == 32)
        .findFirst();

    return matched.isPresent() &&
        writesExactlyOneRegisterClassWithType(behavior, Type.unsignedInt(32));
  }

  private boolean findAddWithImmediate32Bit(Graph behavior) {
    var matched = TreeMatcher.matches(behavior.getNodes(),
            new BuiltInMatcher(List.of(BuiltInTable.ADD, BuiltInTable.ADDS),
                List.of(new AnyConstantValueMatcher())))
        .stream()
        .map(x -> ((BuiltInCall) x).type())
        .filter(ty -> ty instanceof BitsType && ((BitsType) ty).bitWidth() == 32)
        .findFirst();

    return matched.isPresent() &&
        writesExactlyOneRegisterClassWithType(behavior, Type.signedInt(32));
  }

  private boolean findSignedAdd32Bit(Graph behavior) {
    var matched = TreeMatcher.matches(behavior.getNodes(),
            new BuiltInMatcher(List.of(BuiltInTable.ADD, BuiltInTable.ADDS), List.of(
                new TypcastMatcher(Type.unsignedInt(32), new AnyReadRegFileMatcher()),
                new TypcastMatcher(Type.unsignedInt(32), new AnyReadRegFileMatcher())
            )))
        .stream()
        .map(x -> ((BuiltInCall) x).type())
        .filter(ty -> ty instanceof SIntType && ((SIntType) ty).bitWidth() == 32)
        .findFirst();

    return matched.isPresent() &&
        writesExactlyOneRegisterClassWithType(behavior, Type.signedInt(32));
  }

  private boolean findBeq(Graph behavior, Register.Counter pc) {
    var hasCondition =
        behavior.getNodes(IfNode.class)
            .anyMatch(
                x -> x.condition instanceof BuiltInCall
                    && ((BuiltInCall) x.condition).builtIn() == BuiltInTable.EQU);
    var writesPc = behavior.getNodes(WriteRegNode.class)
        .anyMatch(x -> x.register() == pc);

    return hasCondition && writesPc;
  }
}
