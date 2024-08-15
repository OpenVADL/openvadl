package vadl.lcb.passes.isaMatching;

import static vadl.types.BuiltInTable.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
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
import vadl.viam.Parameter;
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
import vadl.viam.matching.impl.FieldAccessRefMatcher;

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
    // The instruction matching happens on the unlined graph
    // because the field accesses are unlined.
    IdentityHashMap<Instruction, Graph> unlined =
        (IdentityHashMap<Instruction, Graph>) passResults.get(new PassKey("FunctionInlinerPass"));
    ensureNonNull(unlined, "Inlining data must exist");
    HashMap<InstructionLabel, List<Instruction>> matched = new HashMap<>();

    viam.isas().forEach(isa -> isa.instructions().forEach(instruction -> {
      // Get unlined or the normal behavior if nothing was unlined.
      var behavior = unlined.getOrDefault(instruction, instruction.behavior());

      if (findAdd32Bit(behavior)) {
        matched.put(InstructionLabel.ADD_32, List.of(instruction));
      } else if (findAdd64Bit(behavior)) {
        matched.put(InstructionLabel.ADD_64, List.of(instruction));
      } else if (findAddWithImmediate32Bit(behavior)) {
        matched.put(InstructionLabel.ADDI_32, List.of(instruction));
      } else if (findAddWithImmediate64Bit(behavior)) {
        matched.put(InstructionLabel.ADDI_64, List.of(instruction));
      } else if (findRR(behavior, SUB)) {
        extend(matched, InstructionLabel.SUB, instruction);
      } else if (findRR(behavior, List.of(SUBB, SUBSB))) {
        extend(matched, InstructionLabel.SUBB, instruction);
      } else if (findRR(behavior, List.of(SUBC, SUBSC))) {
        extend(matched, InstructionLabel.SUBC, instruction);
      } else if (findRR(behavior, List.of(AND, ADDS))) {
        extend(matched, InstructionLabel.AND, instruction);
      } else if (findRR(behavior, List.of(OR, ORS))) {
        extend(matched, InstructionLabel.OR, instruction);
      } else if (findRR(behavior, List.of(XOR, XORS))) {
        extend(matched, InstructionLabel.XOR, instruction);
      } else if (findRR(behavior, List.of(MUL, SMULL, SMULLS))) {
        extend(matched, InstructionLabel.MUL, instruction);
      } else if (findRR(behavior, List.of(SDIV, SDIVS))) {
        extend(matched, InstructionLabel.SDIV, instruction);
      } else if (findRR(behavior, List.of(UDIV, UDIVS))) {
        extend(matched, InstructionLabel.UDIV, instruction);
      } else if (findRR(behavior, List.of(SMOD, SMODS))) {
        extend(matched, InstructionLabel.SMOD, instruction);
      } else if (findRR(behavior, List.of(UMOD, UMODS))) {
        extend(matched, InstructionLabel.UMOD, instruction);
      } else if (isa.pc() != null && findBeq(behavior, isa.pc())) {
        matched.put(InstructionLabel.BEQ, List.of(instruction));
      }
    }));

    return matched;
  }

  private void extend(HashMap<InstructionLabel, List<Instruction>> matched,
                      InstructionLabel instructionLabel, Instruction instruction) {
    matched.compute(instructionLabel, (k, v) -> {
      if (v == null) {
        return new ArrayList<>(List.of(instruction));
      } else {
        v.add(instruction);
        return v;
      }
    });
  }

  private boolean findRR(Graph behavior, BuiltIn builtin) {
    return findRR(behavior, List.of(builtin));
  }

  private boolean findRR(Graph behavior, List<BuiltIn> builtins) {
    var matched = TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
        new BuiltInMatcher(builtins, List.of(
            new AnyChildMatcher(new AnyReadRegFileMatcher()),
            new AnyChildMatcher(new AnyReadRegFileMatcher())
        )));

    return !matched.isEmpty() && writesExactlyOneRegisterClass(behavior);
  }


  private boolean findRI(Graph behavior, BuiltIn builtin) {
    var matched = TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
        new BuiltInMatcher(builtin, List.of(
            new AnyChildMatcher(new AnyReadRegFileMatcher()),
            new AnyChildMatcher(new FieldAccessRefMatcher())
        )));

    return !matched.isEmpty() && writesExactlyOneRegisterClass(behavior);
  }

  private boolean writesExactlyOneRegisterClass(Graph graph) {
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

    return true;
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
            new BuiltInMatcher(ADD, List.of(
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
            new BuiltInMatcher(List.of(ADD, ADDS),
                List.of(new AnyChildMatcher(new AnyReadRegFileMatcher()),
                    new AnyChildMatcher(new FieldAccessRefMatcher()))))
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
                    && ((BuiltInCall) x.condition).builtIn() == EQU);
    var writesPc = behavior.getNodes(WriteRegNode.class)
        .anyMatch(x -> x.register() == pc);

    return hasCondition && writesPc;
  }
}
