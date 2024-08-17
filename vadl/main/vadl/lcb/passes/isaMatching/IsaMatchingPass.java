package vadl.lcb.passes.isaMatching;

import static vadl.types.BuiltInTable.ADD;
import static vadl.types.BuiltInTable.ADDC;
import static vadl.types.BuiltInTable.ADDS;
import static vadl.types.BuiltInTable.AND;
import static vadl.types.BuiltInTable.ANDS;
import static vadl.types.BuiltInTable.EQU;
import static vadl.types.BuiltInTable.MUL;
import static vadl.types.BuiltInTable.NEQ;
import static vadl.types.BuiltInTable.OR;
import static vadl.types.BuiltInTable.ORS;
import static vadl.types.BuiltInTable.SDIV;
import static vadl.types.BuiltInTable.SDIVS;
import static vadl.types.BuiltInTable.SGEQ;
import static vadl.types.BuiltInTable.SGTH;
import static vadl.types.BuiltInTable.SLEQ;
import static vadl.types.BuiltInTable.SLTH;
import static vadl.types.BuiltInTable.SMOD;
import static vadl.types.BuiltInTable.SMODS;
import static vadl.types.BuiltInTable.SMULL;
import static vadl.types.BuiltInTable.SMULLS;
import static vadl.types.BuiltInTable.SUB;
import static vadl.types.BuiltInTable.SUBB;
import static vadl.types.BuiltInTable.SUBC;
import static vadl.types.BuiltInTable.SUBSB;
import static vadl.types.BuiltInTable.SUBSC;
import static vadl.types.BuiltInTable.UDIV;
import static vadl.types.BuiltInTable.UDIVS;
import static vadl.types.BuiltInTable.UGEQ;
import static vadl.types.BuiltInTable.UGTH;
import static vadl.types.BuiltInTable.ULEQ;
import static vadl.types.BuiltInTable.ULTH;
import static vadl.types.BuiltInTable.UMOD;
import static vadl.types.BuiltInTable.UMODS;
import static vadl.types.BuiltInTable.XOR;
import static vadl.types.BuiltInTable.XORS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyChildMatcher;
import vadl.viam.matching.impl.AnyReadMemMatcher;
import vadl.viam.matching.impl.AnyReadRegFileMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.FieldAccessRefMatcher;
import vadl.viam.matching.impl.WriteResourceMatcher;

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
    // The instruction matching happens on the uninlined graph
    // because the field accesses are uninlined.
    IdentityHashMap<Instruction, Graph> uninlined =
        (IdentityHashMap<Instruction, Graph>) passResults.get(new PassKey("FunctionInlinerPass"));
    ensureNonNull(uninlined, "Inlining data must exist");
    HashMap<InstructionLabel, List<Instruction>> matched = new HashMap<>();

    viam.isas().forEach(isa -> isa.instructions().forEach(instruction -> {
      // Get uninlined or the normal behavior if nothing was uninlined.
      var behavior = uninlined.getOrDefault(instruction, instruction.behavior());

      // Some are typed and some aren't.
      // The reason is that most of the time we do not care because
      // the instruction selection will figure out the types anyway.
      // The raw cases where we need the type are typed like addition.
      if (findAdd32Bit(behavior)) {
        matched.put(InstructionLabel.ADD_32, List.of(instruction));
      } else if (findAdd64Bit(behavior)) {
        matched.put(InstructionLabel.ADD_64, List.of(instruction));
      } else if (findAddWithImmediate32Bit(behavior)) {
        matched.put(InstructionLabel.ADDI_32, List.of(instruction));
      } else if (findAddWithImmediate64Bit(behavior)) {
        matched.put(InstructionLabel.ADDI_64, List.of(instruction));
      } else if (findRR_OR_findRI(behavior, SUB)) {
        extend(matched, InstructionLabel.SUB, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(SUBB, SUBSB))) {
        extend(matched, InstructionLabel.SUBB, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(SUBC, SUBSC))) {
        extend(matched, InstructionLabel.SUBC, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(AND, ANDS))) {
        extend(matched, InstructionLabel.AND, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(OR, ORS))) {
        extend(matched, InstructionLabel.OR, instruction);
      } else if (findRR(behavior, List.of(XOR, XORS))) {
        extend(matched, InstructionLabel.XOR, instruction);
      } else if (findRI(behavior, List.of(XOR, XORS))) {
        // Here is an exception:
        // Usually, it is good enough to group RR and RI together.
        // However, when generating alternative patterns for conditionals,
        // then we need the XORI instruction. Therefore, we put it extra.
        extend(matched, InstructionLabel.XORI, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(MUL, SMULL, SMULLS))) {
        extend(matched, InstructionLabel.MUL, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(SDIV, SDIVS))) {
        extend(matched, InstructionLabel.SDIV, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(UDIV, UDIVS))) {
        extend(matched, InstructionLabel.UDIV, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(SMOD, SMODS))) {
        extend(matched, InstructionLabel.SMOD, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(UMOD, UMODS))) {
        extend(matched, InstructionLabel.UMOD, instruction);
      } else if (isa.pc() != null && findBranchWithConditional(behavior, isa.pc(), EQU)) {
        extend(matched, InstructionLabel.BEQ, instruction);
      } else if (isa.pc() != null && findBranchWithConditional(behavior, isa.pc(), NEQ)) {
        extend(matched, InstructionLabel.BNEQ, instruction);
      } else if (isa.pc() != null
          && findBranchWithConditional(behavior, isa.pc(), Set.of(SGEQ, UGEQ))) {
        extend(matched, InstructionLabel.BGEQ, instruction);
      } else if (isa.pc() != null
          && findBranchWithConditional(behavior, isa.pc(), Set.of(SLEQ, ULEQ))) {
        extend(matched, InstructionLabel.BLEQ, instruction);
      } else if (isa.pc() != null
          && findBranchWithConditional(behavior, isa.pc(), Set.of(SLTH, ULTH))) {
        extend(matched, InstructionLabel.BLTH, instruction);
      } else if (isa.pc() != null
          && findBranchWithConditional(behavior, isa.pc(), Set.of(SGTH, UGTH))) {
        extend(matched, InstructionLabel.BGTH, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(SLTH, ULTH))) {
        extend(matched, InstructionLabel.LT, instruction);
      } else if (findWriteMem(behavior)) {
        extend(matched, InstructionLabel.STORE_MEM, instruction);
      } else if (findLoadMem(behavior)) {
        extend(matched, InstructionLabel.LOAD_MEM, instruction);
      } else if (isa.pc() != null && findJalr(behavior, isa.pc())) {
        extend(matched, InstructionLabel.JALR, instruction);
      }
    }));

    return matched;
  }

  private boolean findLoadMem(Graph graph) {
    var writesRegFile = graph.getNodes(WriteRegFileNode.class).toList().size();
    var writesReg = graph.getNodes(WriteRegNode.class).toList().size();

    if ((writesRegFile == 1) == (writesReg == 1)) {
      return false;
    }

    var matched = TreeMatcher.matches(
        graph.getNodes(WriteResourceNode.class).map(x -> x),
        new WriteResourceMatcher(new AnyChildMatcher(new AnyReadMemMatcher())));

    return !matched.isEmpty();
  }

  private boolean findWriteMem(Graph graph) {
    if (graph.getNodes(WriteMemNode.class).toList().size() != 1) {
      return false;
    }

    var matched = TreeMatcher.matches(graph.getNodes(WriteResourceNode.class).map(x -> x),
        new WriteResourceMatcher(new AnyChildMatcher(new AnyReadRegFileMatcher())));

    return !matched.isEmpty();
  }

  /**
   * The {@code matched} hashmap contains a list of {@link Instruction} as value.
   * This value extends this list with the given {@link Instruction} when the key is matched.
   */
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

  private boolean findRR_OR_findRI(Graph behavior, BuiltInTable.BuiltIn builtin) {
    return findRR(behavior, List.of(builtin)) || findRI(behavior, List.of(builtin));
  }

  private boolean findRR_OR_findRI(Graph behavior, List<BuiltInTable.BuiltIn> builtins) {
    return findRR(behavior, builtins) || findRI(behavior, builtins);
  }

  /**
   * Find register-registers instructions when it matches one of the given
   * {@link BuiltInTable.BuiltIn}.
   * Also, it must only write one register result.
   */
  private boolean findRR(Graph behavior, List<BuiltInTable.BuiltIn> builtins) {
    var matched = TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
        new BuiltInMatcher(builtins, List.of(
            new AnyChildMatcher(new AnyReadRegFileMatcher()),
            new AnyChildMatcher(new AnyReadRegFileMatcher())
        )));

    return !matched.isEmpty() && writesExactlyOneRegisterClass(behavior);
  }

  /**
   * Find register-immediate instructions when it matches one of the given
   * {@link BuiltInTable.BuiltIn}.
   * Also, it must only write one register result.
   */
  private boolean findRI(Graph behavior, List<BuiltInTable.BuiltIn> builtins) {
    var matched = TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
        new BuiltInMatcher(builtins, List.of(
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

  private boolean findBranchWithConditional(Graph behavior,
                                            Register.Counter pc,
                                            BuiltInTable.BuiltIn builtin) {
    return findBranchWithConditional(behavior, pc, Set.of(builtin));
  }

  private boolean findBranchWithConditional(Graph behavior,
                                            Register.Counter pc,
                                            Set<BuiltInTable.BuiltIn> builtins) {
    var hasCondition =
        behavior.getNodes(IfNode.class)
            .anyMatch(
                x -> x.condition instanceof BuiltInCall
                    && builtins.contains(((BuiltInCall) x.condition).builtIn()));
    var writesPc = behavior.getNodes(WriteRegNode.class)
        .anyMatch(x -> x.register() == pc);

    return hasCondition && writesPc;
  }

  /**
   * Match Jump and Link Register when {@link Instruction} writes PC and writes
   * a register file.
   */
  private boolean findJalr(Graph behavior, Register.Counter pcRegister) {
    var writesPc =
        behavior.getNodes(WriteRegNode.class).filter(x -> x.register().equals(pcRegister))
            .toList();
    var writesRegFile = behavior.getNodes(WriteRegFileNode.class).toList();

    // Idea: if this check is not sufficient in the future
    // then check whether the regfile node also reads from PC.
    return writesPc.size() == 1 && writesRegFile.size() == 1;
  }
}
