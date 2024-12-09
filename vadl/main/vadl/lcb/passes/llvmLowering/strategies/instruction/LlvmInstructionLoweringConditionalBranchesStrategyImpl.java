package vadl.lcb.passes.llvmLowering.strategies.instruction;

import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BEQ;
import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BNEQ;
import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BSGEQ;
import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BSGTH;
import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BSLEQ;
import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BSLTH;
import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BUGEQ;
import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BUGTH;
import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BULEQ;
import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BULTH;
import static vadl.viam.ViamError.ensurePresent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmCondCode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSetCondSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTypeCastSD;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LoweringStrategyUtils;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.RegisterFile;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * Lowering conditional branch instructions into TableGen patterns.
 */
public class LlvmInstructionLoweringConditionalBranchesStrategyImpl
    extends LlvmInstructionLoweringStrategy {
  public LlvmInstructionLoweringConditionalBranchesStrategyImpl(ValueType architectureType) {
    super(architectureType);
  }

  @Override
  protected Set<MachineInstructionLabel> getSupportedInstructionLabels() {
    return Set.of(BEQ, BNEQ, BSGEQ, BSLEQ, BSLTH, BSGTH, BUGEQ, BULEQ, BULTH, BUGTH);
  }

  @Override
  protected List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacementHooks() {
    return replacementHooksWithFieldAccessWithBasicBlockReplacement();
  }

  @Override
  public Optional<LlvmLoweringRecord> lower(
      Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions,
      Instruction instruction,
      Graph uninlinedBehavior,
      DummyAbi abi) {
    var visitor = replacementHooks();
    var copy = uninlinedBehavior.copy();

    for (var node : copy.getNodes(SideEffectNode.class).toList()) {
      visitReplacementHooks(visitor, node);
    }

    copy.deinitializeNodes();
    return Optional.of(
        createIntermediateResult(labelledMachineInstructions, instruction, copy, abi));
  }

  private LlvmLoweringRecord createIntermediateResult(
      Map<MachineInstructionLabel, List<Instruction>> supportedInstructions,
      Instruction instruction,
      Graph visitedGraph,
      DummyAbi dummyAbi) {

    var outputOperands = getTableGenOutputOperands(visitedGraph);
    var inputOperands = getTableGenInputOperands(outputOperands, visitedGraph);
    var flags = getFlags(visitedGraph);

    var writes = visitedGraph.getNodes(WriteResourceNode.class).toList();
    var patterns = generatePatterns(instruction, inputOperands, writes);
    var alternatives =
        generatePatternVariations(instruction,
            supportedInstructions,
            visitedGraph,
            inputOperands,
            outputOperands,
            patterns,
            dummyAbi);

    var allPatterns = Stream.concat(patterns.stream(), alternatives.stream())
        .map(LoweringStrategyUtils::replaceBasicBlockByLabelImmediateInMachineInstruction)
        .toList();

    var uses = getRegisterUses(visitedGraph, inputOperands, outputOperands);
    var defs = getRegisterDefs(visitedGraph, inputOperands, outputOperands);

    return new LlvmLoweringRecord(
        visitedGraph,
        inputOperands,
        outputOperands,
        flags,
        allPatterns,
        uses,
        defs
    );
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      Map<MachineInstructionLabel, List<Instruction>> supportedInstructions,
      Graph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns,
      DummyAbi abi) {
    var flipped = LlvmLoweringPass.flipIsaMatchingMachineInstructions(supportedInstructions);
    var label = flipped.get(instruction);

    ArrayList<TableGenPattern> alternatives = new ArrayList<>();
    var swapped = generatePatternsForSwappedOperands(patterns);
    alternatives.addAll(swapped);
    alternatives.addAll(
        generateBrCondFromBrCc(Stream.concat(patterns.stream(), swapped.stream()).toList()));

    if (label == BNEQ) {
      alternatives.add(generateBrCondWithRegister(instruction, behavior));
    }

    return alternatives;
  }

  private TableGenPattern generateBrCondWithRegister(Instruction instruction, Graph behavior) {
    /*
     Generate the following pattern:

     def : Pat<(brcond X:$cond, bb:$imm12), (BNE X:$cond, X0, bb:$imm12)>;
     */

    var brcc = ensurePresent(
        behavior.getNodes(LlvmBrCcSD.class)
            .findFirst(), () -> Diagnostic.error("Cannot find a comparison in the behavior",
            instruction.sourceLocation()));
    var register = (ReadRegFileNode) brcc.first();
    var llvmRegisterNode =
        new LlvmReadRegFileNode(register.registerFile(), (ExpressionNode) register.address().copy(),
            register.type(), register.staticCounterAccess());
    var registerFile = register.registerFile();
    var zeroRegister = getZeroRegister(registerFile);

    var selector = new Graph("selector");
    selector.addWithInputs(new LlvmBrCondSD(llvmRegisterNode,
        (ExpressionNode) brcc.immOffset().copy()));
    var machine = new Graph("machine");
    machine.addWithInputs(new LcbMachineInstructionNode(new NodeList<>(
        (ExpressionNode) llvmRegisterNode.copy(),
        zeroRegister,
        (ExpressionNode) brcc.immOffset().copy()
    ), instruction));

    return new TableGenSelectionWithOutputPattern(selector, machine);
  }

  private ConstantNode getZeroRegister(RegisterFile registerFile) {
    var zeroConstraint =
        ensurePresent(
            Arrays.stream(registerFile.constraints()).filter(x -> x.value().intValue() == 0)
                .findFirst(),
            () -> Diagnostic.error("Cannot find zero constraint", registerFile.sourceLocation()));
    var constant =
        new Constant.Str(registerFile.simpleName() + zeroConstraint.address().intValue());
    return new ConstantNode(constant);
  }

  private List<TableGenPattern> generatePatternsForSwappedOperands(List<TableGenPattern> patterns) {
    /*
      When we have a pattern with SEGE.

      def : Pat<(brcc SETGE, X:$rs1, X:$rs2, bb:$imm),
            (BGE X:$rs1, X:$rs2, RV32I_Btype_ImmediateB_immediateAsLabel:$imm)>;

      // Then swap the operands and replace the condCode with SETLE.

      def : Pat<(brcc SETLE, X:$rs2, X:$rs1, bb:$imm),
            (BGE X:$rs1, X:$rs2, RV32I_Btype_ImmediateB_immediateAsLabel:$imm)>;

      Of course, it might be the case that there is already such an instruction which covers that.
      But it is better to be sure.
     */
    ArrayList<TableGenPattern> alternatives = new ArrayList<>();

    for (var pattern : patterns) {
      if (pattern instanceof TableGenSelectionWithOutputPattern outputPattern) {
        // We only need to consider the selector pattern.
        // There is no change required for the machine pattern.
        var copy = pattern.selector().copy();
        copy.getNodes(LlvmBrCcSD.class).forEach(llvmBrCcSD -> {
          var newCondCode = LlvmCondCode.inverse(llvmBrCcSD.condition());
          llvmBrCcSD.swapOperands(newCondCode);
        });
        alternatives.add(
            new TableGenSelectionWithOutputPattern(copy, outputPattern.machine().copy()));
      }
    }

    return alternatives;
  }

  private List<TableGenPattern> generateBrCondFromBrCc(
      List<TableGenPattern> patterns) {
    ArrayList<TableGenPattern> alternatives = new ArrayList<>();

    // Generate brcond patterns from brcc
    /*
    def : Pat<(brcc SETEQ, X:$rs1, X:$rs2, bb:$imm),
          (BEQ X:$rs1, X:$rs2, RV32I_Btype_ImmediateB_immediateAsLabel:$imm)>;

    to

    def : Pat<(brcond (i32 (seteq X:$rs1, X:$rs2)), bb:$imm12),
        (BEQ X:$rs1, X:$rs2, RV32I_Btype_ImmediateB_immediateAsLabel:$imm)>;
     */

    for (var pattern : patterns.stream()
        .filter(x -> x instanceof TableGenSelectionWithOutputPattern)
        .map(x -> (TableGenSelectionWithOutputPattern) x).toList()) {
      var selector = pattern.selector().copy();
      var machine = pattern.machine().copy();

      // Replace BrCc with BrCond
      var hasChanged = false;
      for (var node : selector.getNodes(LlvmBrCcSD.class).toList()) {
        // For `brcc` we have Setcc code, so need to see if we have a suitable
        // instruction for that.
        var builtin = LlvmCondCode.from(node.condition());
        var builtinCall =
            new LlvmSetCondSD(builtin, new NodeList<>(node.first(), node.second()),
                node.first().type());

        // We also extend the result of the condition to i32 or i64.
        var typeCast = new LlvmTypeCastSD(builtinCall, node.immOffset().type());
        var brCond = new LlvmBrCondSD(typeCast, node.immOffset());
        node.replaceAndDelete(brCond);
        hasChanged = true;
      }

      // If nothing had changed, then it makes no sense to add it.
      if (hasChanged) {
        alternatives.add(new TableGenSelectionWithOutputPattern(selector, machine));
      }
    }

    return alternatives;
  }
}
