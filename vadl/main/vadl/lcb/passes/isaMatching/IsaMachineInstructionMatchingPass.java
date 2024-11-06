package vadl.lcb.passes.isaMatching;

import static vadl.types.BuiltInTable.ADD;
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
import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Counter;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyChildMatcher;
import vadl.viam.matching.impl.AnyNodeMatcher;
import vadl.viam.matching.impl.AnyReadMemMatcher;
import vadl.viam.matching.impl.AnyReadRegFileMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.FieldAccessRefMatcher;
import vadl.viam.matching.impl.IsReadRegMatcher;
import vadl.viam.matching.impl.WriteResourceMatcherForValue;
import vadl.viam.passes.functionInliner.FunctionInlinerPass;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * A {@link InstructionSetArchitecture} contains a {@link List} of {@link Instruction}.
 * One of the most important tasks of the LCB is to recognize the semantics of the machine
 * instruction and create a mapping from LLVM's SelectionDag to the machine instruction.
 * Most instructions in an instruction set architecture will be "simple" and
 * a lot of them are required in almost every instruction set architecture. The goal
 * of {@link IsaMachineInstructionMatchingPass} to label instructions which can be recognized.
 * Why is this useful?
 * At some places, we need to create machine instructions by hand in LLVM, and we need to
 * know which instructions are supported by instruction set. This labelling makes it much
 * easier to search for these instructions.
 */
public class IsaMachineInstructionMatchingPass extends Pass implements IsaMatchingUtils {
  public IsaMachineInstructionMatchingPass(LcbConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("IsaMachineInstructionMatchingPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {
    // The instruction matching happens on the uninlined graph
    // because the field accesses are uninlined.
    IdentityHashMap<Instruction, UninlinedGraph> uninlined =
        (IdentityHashMap<Instruction, UninlinedGraph>) passResults
            .lastResultOf(FunctionInlinerPass.class);
    Objects.requireNonNull(uninlined);
    HashMap<MachineInstructionLabel, List<Instruction>> matched = new HashMap<>();

    var isa = viam.isa().orElse(null);
    if (isa == null) {
      return matched;
    }

    // TODO: @kper : Support RegisterFileCounters
    ensure(isa.pc() instanceof Counter.RegisterCounter,
        () -> Diagnostic.error("Only counter to single registers are supported.",
            Objects.requireNonNull(isa.pc()).sourceLocation()));
    var pc = (Counter.RegisterCounter) isa.pc();

    isa.ownInstructions().forEach(instruction -> {
      // Get uninlined or the normal behavior if nothing was uninlined.
      var behavior = ensureNonNull(uninlined.get(instruction),
          () -> Diagnostic.error("Cannot find the uninlined graph of this instruction",
              instruction.sourceLocation()));

      // Some are typed and some aren't.
      // The reason is that most of the time we do not care because
      // the instruction selection will figure out the types anyway.
      // The raw cases where we need the type are typed like addition.
      if (findAdd32Bit(behavior)) {
        matched.put(MachineInstructionLabel.ADD_32, List.of(instruction));
      } else if (findAdd64Bit(behavior)) {
        matched.put(MachineInstructionLabel.ADD_64, List.of(instruction));
      } else if (findAddWithImmediate32Bit(behavior)) {
        matched.put(MachineInstructionLabel.ADDI_32, List.of(instruction));
      } else if (findAddWithImmediate64Bit(behavior)) {
        matched.put(MachineInstructionLabel.ADDI_64, List.of(instruction));
      } else if (findRR_OR_findRI(behavior, SUB)) {
        extend(matched, MachineInstructionLabel.SUB, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(SUBB, SUBSB))) {
        extend(matched, MachineInstructionLabel.SUBB, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(SUBC, SUBSC))) {
        extend(matched, MachineInstructionLabel.SUBC, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(AND, ANDS))) {
        extend(matched, MachineInstructionLabel.AND, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(OR, ORS))) {
        extend(matched, MachineInstructionLabel.OR, instruction);
      } else if (findRR(behavior, List.of(XOR, XORS))) {
        extend(matched, MachineInstructionLabel.XOR, instruction);
      } else if (findRI(behavior, List.of(XOR, XORS))) {
        // Here is an exception:
        // Usually, it is good enough to group RR and RI together.
        // However, when generating alternative patterns for conditionals,
        // then we need the XORI instruction. Therefore, we put it extra.
        extend(matched, MachineInstructionLabel.XORI, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(MUL, SMULL, SMULLS))) {
        extend(matched, MachineInstructionLabel.MUL, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(SDIV, SDIVS))) {
        extend(matched, MachineInstructionLabel.SDIV, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(UDIV, UDIVS))) {
        extend(matched, MachineInstructionLabel.UDIV, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(SMOD, SMODS))) {
        extend(matched, MachineInstructionLabel.SMOD, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(UMOD, UMODS))) {
        extend(matched, MachineInstructionLabel.UMOD, instruction);
      } else if (pc != null && findBranchWithConditional(behavior, EQU)) {
        extend(matched, MachineInstructionLabel.BEQ, instruction);
      } else if (pc != null && findBranchWithConditional(behavior, NEQ)) {
        extend(matched, MachineInstructionLabel.BNEQ, instruction);
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(SGEQ))) {
        extend(matched, MachineInstructionLabel.BSGEQ, instruction);
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(UGEQ))) {
        extend(matched, MachineInstructionLabel.BUGEQ, instruction);
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(SLEQ))) {
        extend(matched, MachineInstructionLabel.BSLEQ, instruction);
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(ULEQ))) {
        extend(matched, MachineInstructionLabel.BULEQ, instruction);
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(SLTH))) {
        extend(matched, MachineInstructionLabel.BSLTH, instruction);
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(ULTH))) {
        extend(matched, MachineInstructionLabel.BULTH, instruction);
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(SGTH))) {
        extend(matched, MachineInstructionLabel.BSGTH, instruction);
      } else if (pc != null
          && findBranchWithConditional(behavior, Set.of(UGTH))) {
        extend(matched, MachineInstructionLabel.BUGTH, instruction);
      } else if (findRR_OR_findRI(behavior, List.of(SLTH, ULTH))) {
        extend(matched, MachineInstructionLabel.LT, instruction);
      } else if (findWriteMem(behavior)) {
        extend(matched, MachineInstructionLabel.STORE_MEM, instruction);
      } else if (findLoadMem(behavior)) {
        extend(matched, MachineInstructionLabel.LOAD_MEM, instruction);
      } else if (pc != null && findJalr(behavior, pc)) {
        extend(matched, MachineInstructionLabel.JALR, instruction);
      } else if (pc != null && findJal(behavior, pc)) {
        extend(matched, MachineInstructionLabel.JAL, instruction);
      }
    });

    return matched;
  }

  @Override
  public void verification(Specification viam, @Nullable Object passResult) {
    ensureNonNull(passResult, "There must be a passResult");
    var isaMatched = (HashMap<MachineInstructionLabel, List<Instruction>>) passResult;

    var addi = isaMatched.get(MachineInstructionLabel.ADDI_64);
    if (addi == null) {
      addi = isaMatched.get(MachineInstructionLabel.ADDI_32);
    }

    ensure(addi != null && !addi.isEmpty(),
        () -> Diagnostic.error(
            "There must be an instruction (addition with immediate), but we haven't found any.",
            viam.sourceLocation()));
  }

  private boolean findLoadMem(UninlinedGraph graph) {
    var writesRegFile = graph.getNodes(WriteRegFileNode.class).toList().size();
    var writesReg = graph.getNodes(WriteRegNode.class).toList().size();

    if ((writesRegFile == 1) == (writesReg == 1)) {
      return false;
    }

    var matched = TreeMatcher.matches(
        graph.getNodes(WriteResourceNode.class).map(x -> x),
        new WriteResourceMatcherForValue(new AnyChildMatcher(new AnyReadMemMatcher())));

    return !matched.isEmpty();
  }

  private boolean findWriteMem(UninlinedGraph graph) {
    if (graph.getNodes(WriteMemNode.class).toList().size() != 1) {
      return false;
    }

    var matched = TreeMatcher.matches(graph.getNodes(WriteResourceNode.class).map(x -> x),
        new WriteResourceMatcherForValue(new AnyChildMatcher(new AnyReadRegFileMatcher())));

    return !matched.isEmpty();
  }

  private boolean findAdd32Bit(UninlinedGraph behavior) {
    return findAdd(behavior, 32);
  }

  private boolean findAdd64Bit(UninlinedGraph behavior) {
    return findAdd(behavior, 64);
  }

  private boolean findAdd(UninlinedGraph behavior, int bitWidth) {
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

  private boolean findAddWithImmediate32Bit(UninlinedGraph behavior) {
    return findAddWithImmediate(behavior, 32);
  }

  private boolean findAddWithImmediate64Bit(UninlinedGraph behavior) {
    return findAddWithImmediate(behavior, 64);
  }

  private boolean findAddWithImmediate(UninlinedGraph behavior, int bitWidth) {
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

  private boolean findBranchWithConditional(UninlinedGraph behavior,
                                            BuiltInTable.BuiltIn builtin) {
    return findBranchWithConditional(behavior, Set.of(builtin));
  }

  private boolean findBranchWithConditional(UninlinedGraph behavior,
                                            Set<BuiltInTable.BuiltIn> builtins) {
    var hasCondition =
        behavior.getNodes(IfNode.class)
            .anyMatch(
                x -> x.condition() instanceof BuiltInCall
                    && builtins.contains(((BuiltInCall) x.condition()).builtIn()));
    var writesPc = behavior.getNodes(WriteRegNode.class)
        .anyMatch(x -> x.staticCounterAccess() != null);

    return hasCondition && writesPc;
  }

  /**
   * Match Jump and Link Register when {@link Instruction} writes PC, writes
   * a register file and has an operation (ADD, SUB) where one input is a registerfile.
   */
  private boolean findJalr(UninlinedGraph behavior, Counter.RegisterCounter pcRegister) {
    var writesPc =
        behavior.getNodes(WriteRegNode.class)
            .filter(x -> x.register().equals(pcRegister.registerRef()))
            .toList();
    var writesRegFile = behavior.getNodes(WriteRegFileNode.class).toList();
    var inputRegister = TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
        new BuiltInMatcher(List.of(BuiltInTable.ADD, BuiltInTable.ADDS, SUB), List.of(
            new AnyChildMatcher(new AnyReadRegFileMatcher()),
            new AnyNodeMatcher()
        )));

    return writesPc.size() == 1 && writesRegFile.size() == 1 && !inputRegister.isEmpty();
  }

  /**
   * Match Jump and Link when {@link Instruction} writes PC, writes
   * a register file and has an operation (ADD, SUB) where one input is a PC.
   */
  private boolean findJal(UninlinedGraph behavior, Counter.RegisterCounter pcRegister) {
    var writesPc =
        behavior.getNodes(WriteRegNode.class)
            .filter(x -> x.register().equals(pcRegister.registerRef()))
            .toList();
    var writesRegFile = behavior.getNodes(WriteRegFileNode.class).toList();
    var inputRegister = TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
        new BuiltInMatcher(List.of(BuiltInTable.ADD, BuiltInTable.ADDS, SUB), List.of(
            new AnyChildMatcher(new IsReadRegMatcher(pcRegister.registerRef())),
            new AnyNodeMatcher()
        )));

    return writesPc.size() == 1 && writesRegFile.size() == 1 && !inputRegister.isEmpty();
  }
}
