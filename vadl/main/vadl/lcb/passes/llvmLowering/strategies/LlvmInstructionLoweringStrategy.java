package vadl.lcb.passes.llvmLowering.strategies;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensurePresent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.LlvmMayLoadMemory;
import vadl.lcb.passes.llvmLowering.LlvmMayStoreMemory;
import vadl.lcb.passes.llvmLowering.LlvmSideEffectPatternIncluded;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmNodeReplaceable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUnlowerableSD;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbBranchEndNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbBuiltInCallNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbConstantNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbFieldAccessRefNodeByLlvmBasicBlockReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbFieldAccessRefNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbFuncCallReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbIfNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbInstrCallNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbInstrEndNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbLetNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbReadMemNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbReadRegFileNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbReadRegNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbReturnNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbSelectNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbSignExtendNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbSliceNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbTruncateNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbWriteMemNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbWriteRegFileNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbWriteRegNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbZeroExtendNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LlvmUnlowerableNodeReplacement;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenConstantOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionBareSymbolOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionFrameRegisterOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionImmediateLabelOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionIndexedRegisterFileOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionRegisterFileOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity.ParameterIdentity;
import vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity.ParameterTypeAndNameIdentity;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * Defines how a {@link Instruction} will be lowered to {@link TableGenInstruction}.
 */
public abstract class LlvmInstructionLoweringStrategy {
  private static final Logger logger = LoggerFactory.getLogger(
      LlvmInstructionLoweringStrategy.class);

  protected final ValueType architectureType;

  public LlvmInstructionLoweringStrategy(ValueType architectureType) {
    this.architectureType = architectureType;
  }

  /**
   * Get the supported set of {@link MachineInstructionLabel} which this strategy supports.
   */
  protected abstract Set<MachineInstructionLabel> getSupportedInstructionLabels();

  /**
   * Checks whether the given {@link Instruction} is lowerable with this strategy.
   */
  public boolean isApplicable(@Nullable MachineInstructionLabel machineInstructionLabel) {
    if (machineInstructionLabel == null) {
      return false;
    }

    return getSupportedInstructionLabels().contains(machineInstructionLabel);
  }

  protected List<GraphVisitor.NodeApplier
      <? extends Node, ? extends Node>> replacementHooksWithDefaultFieldAccessReplacement() {
    var hooks = new ArrayList<GraphVisitor.NodeApplier<? extends Node, ? extends Node>>();
    return replacementHooks(hooks, new LcbFieldAccessRefNodeReplacement(hooks, architectureType));
  }

  protected List<GraphVisitor.NodeApplier
      <? extends Node, ? extends Node>> replacementHooksWithFieldAccessWithBasicBlockReplacement() {
    var hooks = new ArrayList<GraphVisitor.NodeApplier<? extends Node, ? extends Node>>();
    return replacementHooks(hooks,
        new LcbFieldAccessRefNodeByLlvmBasicBlockReplacement(hooks, architectureType));
  }

  /**
   * Returns a list of applier which transform a {@link Graph}.
   */
  private List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacementHooks(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> hooks,
      GraphVisitor.NodeApplier<? extends Node, ? extends Node> fieldAccessRefNodeReplacement) {
    var v1 = new LcbBranchEndNodeReplacement(hooks);
    var v2 = new LcbBuiltInCallNodeReplacement(hooks);
    var v3 = new LcbConstantNodeReplacement(hooks);
    var v5 = new LcbFuncCallReplacement();
    var v6 = new LcbIfNodeReplacement();
    var v7 = new LcbInstrCallNodeReplacement(hooks);
    var v8 = new LcbInstrEndNodeReplacement(hooks);
    var v9 = new LcbLetNodeReplacement(hooks);
    var v10 = new LcbReadMemNodeReplacement(hooks);
    var v11 = new LcbReadRegFileNodeReplacement(hooks);
    var v12 = new LcbReadRegNodeReplacement(hooks);
    var v13 = new LcbReturnNodeReplacement(hooks);
    var v14 = new LcbSelectNodeReplacement(hooks);
    var v15 = new LcbSignExtendNodeReplacement(hooks);
    var v16 = new LcbSliceNodeReplacement(hooks);
    var v17 = new LcbTruncateNodeReplacement(hooks);
    var v18 = new LcbWriteMemNodeReplacement(hooks);
    var v19 = new LcbWriteRegFileNodeReplacement(hooks);
    var v20 = new LcbWriteRegNodeReplacement(hooks);
    var v21 = new LcbZeroExtendNodeReplacement(hooks);
    var v22 = new LlvmUnlowerableNodeReplacement(hooks);

    hooks.add(v1);
    hooks.add(v2);
    hooks.add(v3);
    hooks.add(fieldAccessRefNodeReplacement);
    hooks.add(v5);
    hooks.add(v6);
    hooks.add(v7);
    hooks.add(v8);
    hooks.add(v9);
    hooks.add(v10);
    hooks.add(v11);
    hooks.add(v12);
    hooks.add(v13);
    hooks.add(v14);
    hooks.add(v15);
    hooks.add(v16);
    hooks.add(v17);
    hooks.add(v18);
    hooks.add(v19);
    hooks.add(v20);
    hooks.add(v21);
    hooks.add(v22);

    return hooks;
  }

  protected abstract List<GraphVisitor.NodeApplier
      <? extends Node, ? extends Node>> replacementHooks();

  /**
   * Flags indicate special properties of a machine instruction. This method checks the
   * machine instruction's behavior for those and returns them.
   *
   * @return the flags of an {@link Graph}.
   */
  public static LlvmLoweringPass.Flags getFlags(Graph graph) {
    var isTerminator = graph.getNodes(WriteRegNode.class)
        .anyMatch(node -> node.staticCounterAccess() != null);

    var isBranch = isTerminator
        &&
        graph.getNodes(Set.of(IfNode.class, LlvmBrCcSD.class, LlvmBrCondSD.class, LlvmBrSD.class))
            .findFirst().isPresent();

    var isCall = false;
    var isReturn = false;
    var isPseudo = false; // This strategy always handles instructions.
    var isCodeGenOnly = false;
    var mayLoad = graph.getNodes(LlvmMayLoadMemory.class).findFirst().isPresent();
    var mayStore =
        graph.getNodes(Set.of(WriteMemNode.class, LlvmMayStoreMemory.class)).findFirst()
            .isPresent();

    return new LlvmLoweringPass.Flags(
        isTerminator,
        isBranch,
        isCall,
        isReturn,
        isPseudo,
        isCodeGenOnly,
        mayLoad,
        mayStore
    );
  }

  /**
   * Generate a lowering result for the given {@link Graph} for machine instructions.
   * If it is not lowerable then return {@link Optional#empty()}.
   *
   * @param labelledMachineInstructions   the instructions which have known semantics.
   * @param instruction                   is the machine instruction which should be lowered.
   * @param unmodifiedBehavior            is the uninlined graph in the case of {@link Instruction}.
   * @param unmodifiedAdditionalBehaviors are the uninlined graph in the case of {@link Instruction}
   *                                      which provide additional functionality. For example, a
   *                                      "less-than" instruction can be also
   *                                      lowered to "eq" or "neq". The behavior for "eq" and "neq"
   *                                      would be an additional behavior. It is easier to provide
   *                                      these high-level VIAM definitions then to create these
   *                                      patterns by hand in the strategy.
   */
  public Optional<LlvmLoweringRecord> lower(
      Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions,
      Instruction instruction,
      UninlinedGraph unmodifiedBehavior,
      @Nullable List<UninlinedGraph> unmodifiedAdditionalBehaviors) {
    return lowerInstruction(labelledMachineInstructions,
        instruction,
        unmodifiedBehavior,
        unmodifiedAdditionalBehaviors);
  }

  /**
   * Lower a pseudo instruction.
   */
  public Optional<LlvmLoweringRecord> lower(
      Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions,
      PseudoInstruction pseudoInstruction,
      Instruction instruction,
      Graph unmodifiedBehavior) {
    logger.atDebug().log("Lowering {} with {}", instruction.identifier.simpleName(),
        pseudoInstruction.identifier.simpleName());
    return lowerInstruction(labelledMachineInstructions,
        instruction,
        unmodifiedBehavior,
        Collections.emptyList());
  }

  /**
   * Generate a lowering result for the given {@link Graph} for pseudo instructions.
   * If it is not lowerable then return {@link Optional#empty()}.
   */
  protected Optional<LlvmLoweringRecord> lowerInstruction(
      Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions,
      Instruction instruction,
      Graph unmodifiedBehavior,
      @Nullable List<UninlinedGraph> unmodifiedAdditionalBehaviors) {
    var visitor = replacementHooksWithDefaultFieldAccessReplacement();
    var copy = unmodifiedBehavior.copy();

    if (!checkIfNoControlFlow(copy) && !checkIfNotAllowedDataflowNodes(copy)) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning("Instruction is not lowerable and will be skipped",
              instruction.sourceLocation()).build());
      return Optional.empty();
    }

    // Continue with lowering of nodes
    var isLowerable = true;
    for (var endNode : copy.getNodes(SideEffectNode.class).toList()) {
      visitReplacementHooks(visitor, endNode);

      if (!copy.getNodes(LlvmUnlowerableSD.class).toList().isEmpty()) {
        DeferredDiagnosticStore.add(
            Diagnostic.warning("Instruction is not lowerable and will be skipped",
                instruction.sourceLocation()).build());
        isLowerable = false;
      }
    }

    // If the behavior contains any registers then it is also not lowerable because LLVM's DAG
    // has no concept of register in the IR.
    if (copy.getNodes(ReadRegNode.class).findAny().isPresent()) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning(
              "Instruction is not lowerable because it tries to match fixed registers.",
              instruction.sourceLocation()).build());
      isLowerable = false;
    }

    var outputOperands = getTableGenOutputOperands(copy);
    var inputOperands = getTableGenInputOperands(outputOperands, copy);

    var registerUses = getRegisterUses(copy, inputOperands, outputOperands);
    var registerDefs = getRegisterDefs(copy, inputOperands, outputOperands);
    var flags = getFlags(copy);

    copy.deinitializeNodes();

    if (isLowerable) {
      var patterns = generatePatterns(instruction,
          inputOperands,
          copy.getNodes(WriteResourceNode.class).toList());
      var alternativePatterns =
          generatePatternVariations(
              instruction,
              labelledMachineInstructions,
              copy,
              inputOperands,
              outputOperands,
              patterns);
      return Optional.of(new LlvmLoweringRecord(copy,
          inputOperands,
          outputOperands,
          flags,
          Stream.concat(patterns.stream(), alternativePatterns.stream()).toList(),
          registerUses,
          registerDefs
      ));
    } else {
      return Optional.of(new LlvmLoweringRecord(copy,
          inputOperands,
          outputOperands,
          flags,
          Collections.emptyList(),
          registerUses,
          registerDefs
      ));
    }
  }

  protected void visitReplacementHooks(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> visitor,
      SideEffectNode sideEffect) {
    for (var v : visitor.stream().filter(x -> x.acceptable(sideEffect)).toList()) {
      v.visitApplicable(sideEffect);
    }
  }

  /**
   * Get a list of {@link RegisterRef} which are written. It is considered a
   * register definition when a {@link WriteRegNode} or a {@link WriteRegFileNode} with a
   * constant address exists. However, the only registers without any constraints on the
   * register file will be returned.
   *
   * @param behavior          of the {@link Instruction}.
   * @param filterConstraints whether registers with constraints should be considered.
   */
  private static List<RegisterRef> getRegisterDefs(Graph behavior, boolean filterConstraints) {
    return Stream.concat(behavior.getNodes(WriteRegNode.class)
                .map(WriteRegNode::register)
                .map(RegisterRef::new),
            behavior.getNodes(WriteRegFileNode.class)
                .filter(WriteRegFileNode::hasConstantAddress)
                .map(x -> new RegisterRef(x.registerFile(),
                    ((ConstantNode) x.address()).constant()))
        )
        // Register should not have any constraints. When it does then there is no
        // need that LLVM knows about it because it should not be a dependency.
        .filter(register -> filterConstraints || register.constraints().isEmpty())
        .toList();
  }

  /**
   * Get a list of {@link RegisterRef} which are written. It is considered a
   * register definition when a {@link WriteRegNode} or a {@link WriteRegFileNode} with a
   * constant address exists. However, the only registers without any constraints on the
   * register file will be returned.
   */
  public static List<RegisterRef> getRegisterDefs(Graph behavior,
                                                  List<TableGenInstructionOperand> inputOperands,
                                                  List<TableGenInstructionOperand> outputOperands) {
    // If a TableGen record has no input or output operands,
    // and no registers as def or use then it will throw an error.
    // Therefore, when input and output operands are empty then do not filter any
    // registers.
    var filterRegistersWithConstraints = inputOperands.isEmpty() && outputOperands.isEmpty();
    return getRegisterDefs(behavior, filterRegistersWithConstraints);
  }

  /**
   * Get a list of {@link RegisterRef} which are read. It is considered a
   * register usage when a {@link ReadRegNode} or a {@link ReadRegFileNode} with a
   * constant address exists. However, the only registers without any constraints on the
   * register file will be returned.
   *
   * @param behavior          of the {@link Instruction}.
   * @param filterConstraints whether registers with constraints should be considered.
   */
  private static List<RegisterRef> getRegisterUses(Graph behavior, boolean filterConstraints) {
    return Stream.concat(behavior.getNodes(ReadRegNode.class)
                .map(ReadRegNode::register)
                .map(RegisterRef::new),
            behavior.getNodes(ReadRegFileNode.class)
                .filter(ReadResourceNode::hasConstantAddress)
                .map(x -> new RegisterRef(x.registerFile(),
                    ((ConstantNode) x.address()).constant()))
        )
        // Register should not have any constraints. When it does then there is no
        // need that LLVM knows about it because it should not be a dependency.
        .filter(register -> filterConstraints || register.constraints().isEmpty())
        .toList();
  }

  /**
   * Get a list of {@link RegisterRef} which are read. It is considered a
   * register usage when a {@link ReadRegNode} or a {@link ReadRegFileNode} with a
   * constant address exists. However, the only registers without any constraints on the
   * register file will be returned.
   */
  public static List<RegisterRef> getRegisterUses(Graph behavior,
                                                  List<TableGenInstructionOperand> inputOperands,
                                                  List<TableGenInstructionOperand> outputOperands) {
    // If a TableGen record has no input or output operands,
    // and no registers as def or use then it will throw an error.
    // Therefore, when input and output operands are empty then do not filter any
    // registers.
    var filterRegistersWithConstraints = inputOperands.isEmpty() && outputOperands.isEmpty();
    return getRegisterUses(behavior, filterRegistersWithConstraints);
  }

  /**
   * Some {@link InstructionSetArchitecture} have not machine instructions for all LLVM Selection
   * DAG nodes or require additional patterns to match correctly. This method should generate
   * alternative patterns for these instructions.
   * For example, the RISC-V has only a machine instruction for the less-than comparison.
   * Other comparisons like greater-than-equal can be composed by the less-than operator.
   * This method will generate the patterns from the less-than comparison. But the opposite
   * direction should work as well. So when there is only a greater-than comparison
   * then this method should generate a pattern for the less-than.
   */
  protected abstract List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      Map<MachineInstructionLabel, List<Instruction>> supportedInstructions,
      Graph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns);

  /**
   * LLvm's TableGen cannot work with control flow. So if statements and other constructs are not
   * supported.
   *
   * @return {@code true} if the {@link Graph} is lowerable.
   */
  private boolean checkIfNoControlFlow(Graph behavior) {
    return behavior.getNodes(ControlNode.class)
        .allMatch(
            x -> x instanceof AbstractBeginNode || x instanceof AbstractEndNode); // exceptions
  }

  /**
   * Some dataflow nodes are not lowerable. This function checks whether the {@code behaviors}
   * contains these.
   *
   * @return {@code true} if the {@link Graph} is lowerable.
   */
  private boolean checkIfNotAllowedDataflowNodes(Graph behavior) {
    return behavior.getNodes(DependencyNode.class)
        .noneMatch(x -> x instanceof FuncParamNode);
  }

  /**
   * Extract the output parameters of {@link Graph}.
   */
  public static List<TableGenInstructionOperand> getTableGenOutputOperands(Graph graph) {
    return getOutputOperands(graph)
        .stream()
        .filter(operand -> {
          // Why?
          // Because LLVM cannot handle static registers in input or output operands.
          // They belong to defs and uses instead.
          return !operand.hasConstantAddress();
        })
        .map(LlvmInstructionLoweringStrategy::generateTableGenInputOutput)
        .toList();
  }

  /**
   * Extracts the input operands from the {@link Graph}. But it will skip nodes which are
   * already a {@link Node} in the {@code outputOperands}. Because if you have a
   * {@link PseudoInstruction} like {@code ADDI rd, rd, 1} then is the output and one input
   * the same which tablegen will not accept.
   */
  public static List<TableGenInstructionOperand> getTableGenInputOperands(
      List<TableGenInstructionOperand> outputOperands,
      Graph graph) {
    var setParameters =
        outputOperands.stream()
            .filter(x -> x instanceof TableGenInstructionIndexedRegisterFileOperand)
            .map(x -> {
              var mapped = (TableGenInstructionIndexedRegisterFileOperand) x;
              return mapped.parameter();
            })
            .collect(Collectors.toSet());
    var outputFields =
        outputOperands.stream()
            .filter(x -> x instanceof TableGenInstructionRegisterFileOperand)
            .map(x -> {
              var mapped = (TableGenInstructionRegisterFileOperand) x;
              return mapped.formatField();
            })
            .collect(Collectors.toSet());

    return getInputOperands(graph)
        .stream()
        .filter(node -> {
          // Why?
          // Because LLVM cannot handle static registers in input or output operands.
          // They belong to defs and uses instead.
          if (node instanceof ReadRegFileNode readRegFileNode) {
            return !readRegFileNode.hasConstantAddress();
          }
          return true;
        }).map(LlvmInstructionLoweringStrategy::generateTableGenInputOutput)
        .filter(
            // If the node is a fieldRefNode then it must not be in the outputs.
            // Otherwise, ok.
            node -> !(node instanceof TableGenInstructionIndexedRegisterFileOperand operand)
                || !setParameters.contains(operand.parameter()))
        .filter(
            // If the node is a fieldRefNode then it must not be in the outputs.
            // Otherwise, ok.
            node -> !(node instanceof TableGenInstructionRegisterFileOperand operand)
                || !outputFields.contains(operand.formatField()))
        .toList();
  }

  /**
   * Generate {@link TableGenInstructionOperand} which looks like "X:$lhs" for TableGen.
   */
  public static TableGenInstructionOperand generateTableGenInputOutput(Node operand) {
    if (operand instanceof LlvmFrameIndexSD node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof ReadRegFileNode node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof LlvmFieldAccessRefNode node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof FieldRefNode node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof LlvmBasicBlockSD node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof WriteRegFileNode node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof FuncParamNode node) {
      return generateInstructionOperand(node);
    } else {
      throw Diagnostic.error(
          "Cannot construct a tablegen instruction operand from the type.",
          operand.sourceLocation()).build();
    }
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(FuncParamNode node) {
    return new TableGenInstructionBareSymbolOperand(node,
        "bare_symbol",
        node.parameter().simpleName());
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(LlvmBasicBlockSD node) {
    return new TableGenInstructionImmediateLabelOperand(
        ParameterIdentity.fromBasicBlockToImmediateLabel(node),
        node);
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(FieldRefNode node) {
    return new TableGenInstructionOperand(node,
        ParameterIdentity.from(node));
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(LlvmFrameIndexSD node) {
    return new TableGenInstructionFrameRegisterOperand(
        ParameterIdentity.from(node, node.address()), node);
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(ReadRegFileNode node) {
    if (node.address() instanceof FieldRefNode field) {
      return new TableGenInstructionRegisterFileOperand(
          ParameterIdentity.from(node, field),
          node,
          field.formatField());
    } else if (node.address() instanceof FuncParamNode funcParamNode) {
      return new TableGenInstructionIndexedRegisterFileOperand(
          ParameterIdentity.from(node, funcParamNode),
          node,
          funcParamNode.parameter());
    } else if (node.address() instanceof ConstantNode constantNode) {
      // The register file has a constant as address.
      // This is ok as long as the value of the register file at the address is also constant.
      // For example, the X0 register in RISC-V which always has a constant value.
      var constraints = Arrays.stream(node.registerFile().constraints()).toList();
      var constraintValue = constraints.stream()
          .filter(x -> x.address().intValue() == constantNode.constant().asVal().intValue())
          .findFirst();
      var constRegisterValue = ensurePresent(constraintValue,
          () -> Diagnostic.error("Register file with constant index has no constant value.",
                  constantNode.sourceLocation())
              .help("Consider adding a constraint to register file for the given index."));
      // Update the type of the constant because it needs to be upcasted.
      // Heuristically, we take the type of the index because indices were also upcasted.
      var constantValue = constRegisterValue.value();
      constantValue.setType(constantNode.type());
      return new TableGenConstantOperand(constantNode, constantValue);
    } else {
      throw Diagnostic.error(
          "The compiler generator needs to generate a tablegen instruction operand from this "
              + "address for a field but it does not support it.",
          node.address().sourceLocation()).build();
    }
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(WriteRegFileNode node) {
    if (node.address() instanceof FieldRefNode field) {
      return new TableGenInstructionRegisterFileOperand(
          ParameterIdentity.from(node, field),
          node,
          field.formatField());
    } else if (node.address() instanceof FuncParamNode funcParamNode) {
      return new TableGenInstructionIndexedRegisterFileOperand(
          ParameterIdentity.from(node, funcParamNode),
          node,
          funcParamNode.parameter());
    } else {
      throw Diagnostic.error(
          "The compiler generator needs to generate a tablegen instruction operand from this "
              + "address for a field but it does not support it.",
          node.address().sourceLocation()).build();
    }
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(
      LlvmFieldAccessRefNode node) {
    if (node.usage() == LlvmFieldAccessRefNode.Usage.Immediate) {
      return new TableGenInstructionImmediateOperand(
          ParameterIdentity.from(node),
          node);
    } else if (node.usage() == LlvmFieldAccessRefNode.Usage.BasicBlock) {
      return new TableGenInstructionImmediateLabelOperand(
          ParameterIdentity.fromToImmediateLabel(node),
          node);
    } else {
      throw Diagnostic.error("Not supported usage", node.sourceLocation()).build();
    }
  }

  /**
   * Most instruction's behaviors have inputs. Those are the results which the instruction requires.
   */
  private static List<Node> getInputOperands(Graph graph) {
    // First, the registers
    var x = graph.getNodes(ReadRegFileNode.class);
    // Then, immediates
    var y = graph.getNodes(FieldAccessRefNode.class);
    // Then, the rest
    var z = graph.getNodes(FuncCallNode.class).flatMap(
        funcCallNode -> funcCallNode.function().behavior().getNodes(FuncParamNode.class));
    return Stream.concat(Stream.concat(x, y), z)
        .map(k -> (Node) k).toList();
  }

  /**
   * Most instruction's behaviors have outputs. Those are the results which the instruction emits.
   */
  private static List<WriteRegFileNode> getOutputOperands(Graph graph) {
    return graph.getNodes(WriteRegFileNode.class).toList();
  }

  protected List<TableGenPattern> generatePatterns(
      Instruction instruction,
      List<TableGenInstructionOperand> inputOperands,
      List<WriteResourceNode> sideEffectNodes) {
    ArrayList<TableGenPattern> patterns = new ArrayList<>();

    sideEffectNodes.forEach(sideEffectNode -> {
      var patternSelector = getPatternSelector(sideEffectNode);
      var machineInstruction = getOutputPattern(instruction, inputOperands);
      patterns.add(
          new TableGenSelectionWithOutputPattern(patternSelector, machineInstruction));
    });

    return patterns;
  }

  /**
   * Constructs from the given dataflow node a new graph which is the pattern selector.
   */
  @NotNull
  private static Graph getPatternSelector(WriteResourceNode sideEffectNode) {
    var graph = new Graph(sideEffectNode.id().toString() + ".selector.lowering");
    graph.setParentDefinition(Objects.requireNonNull(sideEffectNode.graph()).parentDefinition());

    Node root = sideEffectNode instanceof LlvmSideEffectPatternIncluded ? sideEffectNode.copy() :
        sideEffectNode.value().copy();
    root.clearUsages();
    graph.addWithInputs(root);
    return graph;
  }

  @NotNull
  private static Graph getOutputPattern(Instruction instruction,
                                        List<TableGenInstructionOperand> inputOperands) {
    var graph = new Graph(instruction.simpleName() + ".machine.lowering");
    graph.setParentDefinition(Objects.requireNonNull(instruction));

    var params =
        inputOperands.stream()
            .map(LcbMachineInstructionParameterNode::new)
            .toList();
    var node = new LcbMachineInstructionNode(new NodeList<>(params), instruction);
    graph.addWithInputs(node);
    return graph;
  }

  protected <T extends Node & LlvmNodeReplaceable> void replaceNodeByParameterIdentity(
      List<T> selectorNodes,
      Graph machine,
      Function<T, Node> selectorNodeTransformation,
      BiFunction<LcbMachineInstructionParameterNode,
          ParameterTypeAndNameIdentity,
          TableGenInstructionOperand>
          machineNodeTransformation) {
    for (var node : selectorNodes) {
      // Something like `X:$rs1`
      var selectorParameter = node.parameterIdentity();

      // Updates the selector
      var newNode = selectorNodeTransformation.apply(node);
      node.replaceAndDelete(newNode);

      // Find the corresponding nodes in the machine graph because we know
      // the parameter identity `selectorParameter` in the selector graph.
      machine.getNodes(LcbMachineInstructionParameterNode.class)
          .filter(candidate ->
              candidate.instructionOperand().origin() instanceof LlvmNodeReplaceable cast
                  && cast.parameterIdentity().equals(selectorParameter))
          .forEach(occurrence -> {
            var operand = machineNodeTransformation.apply(occurrence,
                (ParameterTypeAndNameIdentity) selectorParameter);
            ensure(!operand.equals(occurrence.instructionOperand()),
                "The returned operand must be a new instance because it was modified");
            occurrence.setInstructionOperand(operand);
          });
    }
  }
}
