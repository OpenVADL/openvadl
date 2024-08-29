package vadl.lcb.passes.llvmLowering.strategies;

import static vadl.viam.ViamError.ensure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.LlvmMayLoadMemory;
import vadl.lcb.passes.llvmLowering.LlvmMayStoreMemory;
import vadl.lcb.passes.llvmLowering.LlvmSideEffectPatternIncluded;
import vadl.lcb.passes.llvmLowering.model.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.model.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.model.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.model.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.model.LlvmNodeReplaceable;
import vadl.lcb.passes.llvmLowering.model.MachineInstructionNode;
import vadl.lcb.passes.llvmLowering.model.MachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenPatternLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.impl.ReplaceWithLlvmSDNodesVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.ParameterIdentity;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionFrameRegisterOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionRegisterFileOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.visitors.LcbGraphNodeVisitor;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Register;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
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

  /**
   * Get the supported set of {@link InstructionLabel} which this strategy supports.
   */
  protected abstract Set<InstructionLabel> getSupportedInstructionLabels();

  /**
   * Checks whether the given {@link Instruction} is lowerable with this strategy.
   */
  public boolean isApplicable(InstructionLabel instructionLabel) {
    return getSupportedInstructionLabels().contains(instructionLabel);
  }

  /**
   * This returns an instance of the visitor which lowers graph into a lowerable pattern.
   * The default {@link ReplaceWithLlvmSDNodesVisitor} will reject any control flow like
   * if-conditions and mark them as not lowerable.
   */
  protected LcbGraphNodeVisitor getVisitorForPatternSelectorLowering() {
    return new ReplaceWithLlvmSDNodesVisitor();
  }

  /**
   * Flags indicate special properties of a machine instruction. This method checks the
   * machine instruction's behavior for those and returns them.
   *
   * @return the flags of an {@link UninlinedGraph}.
   */
  protected LlvmLoweringPass.Flags getFlags(UninlinedGraph uninlinedGraph) {
    var isTerminator = uninlinedGraph.getNodes(WriteRegNode.class)
        .anyMatch(node -> node.register() instanceof Register.Counter);
    var isBranch = isTerminator
        && uninlinedGraph.getNodes(Set.of(IfNode.class, LlvmBrCcSD.class, LlvmBrCondSD.class))
        .findFirst().isPresent();

    var isCall = false; //TODO
    var isReturn = false;
    var isPseudo = false; // This strategy always handles Instructions.
    var isCodeGenOnly = false;
    var mayLoad = uninlinedGraph.getNodes(LlvmMayLoadMemory.class).findFirst().isPresent();
    var mayStore =
        uninlinedGraph.getNodes(Set.of(WriteMemNode.class, LlvmMayStoreMemory.class)).findFirst()
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
   * Generate a lowering result for the given {@link Graph}.
   * If it is not lowerable then return {@link Optional#empty()}.
   */
  public Optional<LlvmLoweringPass.LlvmLoweringIntermediateResult> lower(
      Map<InstructionLabel, List<Instruction>> supportedInstructions,
      Instruction instruction,
      InstructionLabel instructionLabel,
      UninlinedGraph uninlinedBehavior) {
    var visitor = getVisitorForPatternSelectorLowering();
    var copy = (UninlinedGraph) uninlinedBehavior.copy();
    var instructionIdentifier = instruction.identifier;

    if (!checkIfNoControlFlow(copy) && !checkIfNotAllowedDataflowNodes(copy)) {
      logger.atWarn().log("Instruction '{}' is not lowerable and will be skipped",
          instructionIdentifier.toString());
      return Optional.empty();
    }

    // Continue with lowering of nodes
    for (var endNode : copy.getNodes(AbstractEndNode.class).toList()) {
      visitor.visit(endNode);

      if (!((TableGenPatternLowerable) visitor).isPatternLowerable()) {
        logger.atWarn().log("Instruction '{}' is not lowerable and will be skipped",
            instructionIdentifier.toString());
        return Optional.empty();
      }
    }

    var inputOperands = getTableGenInputOperands(copy);
    var outputOperands = getTableGenOutputOperands(copy);
    var registerUses = getRegisterUses(copy);
    var registerDefs = getRegisterDefs(copy);
    var flags = getFlags(copy);

    copy.deinitializeNodes();

    if (((TableGenPatternLowerable) visitor).isPatternLowerable()) {
      var patterns = generatePatterns(instruction,
          inputOperands,
          copy.getNodes(WriteResourceNode.class).toList());
      var alternativePatterns =
          generatePatternVariations(
              instruction,
              supportedInstructions,
              instructionLabel,
              copy,
              inputOperands,
              outputOperands,
              patterns);
      return Optional.of(new LlvmLoweringPass.LlvmLoweringIntermediateResult(copy,
          inputOperands,
          outputOperands,
          flags,
          Stream.concat(patterns.stream(), alternativePatterns.stream()).toList(),
          registerUses,
          registerDefs
      ));
    }

    logger.atWarn().log("Instruction '{}' is not lowerable", instructionIdentifier.toString());
    return Optional.empty();
  }

  /**
   * Get a list of {@link Register} which are written.
   */
  protected List<Register> getRegisterDefs(Graph behavior) {
    return behavior.getNodes(WriteRegNode.class)
        .map(WriteRegNode::register)
        .toList();
  }

  /**
   * Get a list of {@link Register} which are read.
   */
  protected List<Register> getRegisterUses(Graph behavior) {
    return behavior.getNodes(ReadRegNode.class)
        .map(ReadRegNode::register)
        .toList();
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
      Map<InstructionLabel, List<Instruction>> supportedInstructions,
      InstructionLabel instructionLabel,
      UninlinedGraph behavior,
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
   * Some dataflow nodes are not lowerable. This function checks whether the {@code behavior}
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
  protected List<TableGenInstructionOperand> getTableGenOutputOperands(Graph graph) {
    return getOutputOperands(graph)
        .stream().map(operand -> {
          var address = (FieldRefNode) operand.address();

          if (address == null || address.formatField() == null) {
            throw new ViamError("address must not be null");
          }

          return (TableGenInstructionOperand) new TableGenInstructionRegisterFileOperand(
              ParameterIdentity.from(operand, address),
              operand,
              address.formatField());
        })
        .toList();
  }

  /**
   * Extracts the input operands from the {@link Graph}.
   */
  protected List<TableGenInstructionOperand> getTableGenInputOperands(Graph graph) {
    return getInputOperands(graph)
        .stream()
        .map(LlvmInstructionLoweringStrategy::generateTableGenInputOutput)
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
    } else {
      throw new ViamError("Input operand not supported yet: " + operand);
    }
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
    var address = (FieldRefNode) node.address();
    return new TableGenInstructionFrameRegisterOperand(
        ParameterIdentity.from(node, address), node);
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(ReadRegFileNode node) {
    var address = (FieldRefNode) node.address();
    return new TableGenInstructionRegisterFileOperand(
        ParameterIdentity.from(node, address),
        node,
        address.formatField()
    );
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(
      LlvmFieldAccessRefNode node) {
    return new TableGenInstructionImmediateOperand(
        ParameterIdentity.from(node),
        node);
  }

  /**
   * Most instruction's behaviors have inputs. Those are the results which the instruction requires.
   */
  private static List<Node> getInputOperands(Graph graph) {
    return Stream.concat(graph.getNodes(ReadRegFileNode.class),
            graph.getNodes(FieldAccessRefNode.class))
        .map(x -> (Node) x).toList();
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
      var machineInstruction = getMachinePattern(instruction, inputOperands);
      patterns.add(
          new TableGenPattern(patternSelector, machineInstruction));
    });

    return patterns;
  }

  /**
   * Constructs from the given dataflow node a new graph which is the pattern selector.
   */
  @NotNull
  private static Graph getPatternSelector(WriteResourceNode sideEffectNode) {
    var graph = new Graph(sideEffectNode.id().toString() + ".selector.lowering");
    Node root = sideEffectNode instanceof LlvmSideEffectPatternIncluded ? sideEffectNode :
        sideEffectNode.value();
    root.clearUsages();
    graph.addWithInputs(root);
    return graph;
  }

  @NotNull
  private static Graph getMachinePattern(Instruction instruction,
                                         List<TableGenInstructionOperand> inputOperands) {
    var graph = new Graph(instruction.name() + ".machine.lowering");
    var params =
        inputOperands.stream()
            .map(MachineInstructionParameterNode::new)
            .toList();
    var node = new MachineInstructionNode(new NodeList<>(params), instruction);
    graph.addWithInputs(node);
    return graph;
  }

  protected <T extends Node & LlvmNodeReplaceable> void replaceNodeByParameterIdentity(
      List<T> selectorNodes,
      Graph machine,
      Function<T, Node> selectorNodeTransformation,
      BiFunction<MachineInstructionParameterNode, ParameterIdentity, TableGenInstructionOperand>
          machineNodeTransformation) {
    for (var node : selectorNodes) {
      // Something like `X:$rs1`
      var selectorParameter = node.parameterIdentity();

      // Updates the selector
      var newNode = selectorNodeTransformation.apply(node);
      node.replaceAndDelete(newNode);

      // Find the corresponding nodes in the machine graph because we know
      // the parameter identity `selectorParameter` in the selector graph.
      machine.getNodes(MachineInstructionParameterNode.class)
          .filter(candidate ->
              candidate.instructionOperand().origin() instanceof LlvmNodeReplaceable cast
                  && cast.parameterIdentity().equals(selectorParameter))
          .forEach(occurrence -> {
            var operand = machineNodeTransformation.apply(occurrence, selectorParameter);
            ensure(operand != occurrence.instructionOperand(),
                "The returned operand must be a new instance because it was modified");
            occurrence.setInstructionOperand(operand);
          });
    }
  }
}
