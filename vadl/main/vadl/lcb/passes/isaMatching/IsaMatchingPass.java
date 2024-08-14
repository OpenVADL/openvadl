package vadl.lcb.passes.isaMatching;

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
import vadl.types.Type;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Register;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyChildMatcher;
import vadl.viam.matching.impl.AnyReadRegFileMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.FieldRefNodeMatcher;

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
      if (findAdd32Bit(instruction.behavior())) {
        matched.put(InstructionLabel.ADD_32, instruction);
      } else if (findAdd64Bit(instruction.behavior())) {
        matched.put(InstructionLabel.ADD_64, instruction);
      } else if (findAddWithImmediate32Bit(instruction.behavior())) {
        matched.put(InstructionLabel.ADDI_32, instruction);
      } else if (findAddWithImmediate64Bit(instruction.behavior())) {
        matched.put(InstructionLabel.ADDI_64, instruction);
      } else if (isa.pc() != null && findBeq(instruction.behavior(), isa.pc())) {
        matched.put(InstructionLabel.BEQ, instruction);
      }
    }));

    return matched;
  }

  private boolean writesExactlyOneRegisterClassWithType(Graph graph, Type resultType) {
    var writesRegFiles = graph.getNodes(WriteRegFileNode.class).toList();
    var writesReg = graph.getNodes(WriteRegNode.class).toList();
    var writesMem = graph.getNodes(WriteMemNode.class).toList();
    var readMem = graph.getNodes(ReadMemNode.class).toList();

    if (writesRegFiles.size() != 1
        || !writesReg.isEmpty()
        || !writesMem.isEmpty()
        || !readMem.isEmpty()) {
      return false;
    }

    return writesRegFiles.get(0).registerFile().resultType() == resultType;
  }

  private boolean findAdd32Bit(Graph behavior) {
    return findAdd(behavior, 32);
  }

  private boolean findAdd64Bit(Graph behavior) {
    return findAdd(behavior, 64);
  }

  private boolean findAdd(Graph behavior, int bitWidth) {
    var matched = TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
            new BuiltInMatcher(BuiltInTable.ADD, List.of(
                new AnyChildMatcher(new AnyReadRegFileMatcher()),
                new AnyChildMatcher(new AnyReadRegFileMatcher())
            )))
        .stream()
        .map(x -> ((BuiltInCall) x).type())
        .filter(ty -> ty instanceof BitsType bi && bi.bitWidth() == bitWidth)
        .findFirst();

    return matched.isPresent()
        && writesExactlyOneRegisterClassWithType(behavior, Type.bits(bitWidth));
  }

  private boolean findAddWithImmediate32Bit(Graph behavior) {
    return findAddWithImmediate(behavior, 32);
  }

  private boolean findAddWithImmediate64Bit(Graph behavior) {
    return findAddWithImmediate(behavior, 64);
  }

  private boolean findAddWithImmediate(Graph behavior, int bitWidth) {
    var matched = TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
            new BuiltInMatcher(List.of(BuiltInTable.ADD, BuiltInTable.ADDS),
                List.of(new AnyChildMatcher(new AnyReadRegFileMatcher()),
                    new AnyChildMatcher(new FieldRefNodeMatcher()))))
        .stream()
        .map(x -> ((BuiltInCall) x).type())
        .filter(ty -> ty instanceof BitsType && ((BitsType) ty).bitWidth() == bitWidth)
        .findFirst();

    return matched.isPresent()
        && writesExactlyOneRegisterClassWithType(behavior, Type.bits(bitWidth));
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
