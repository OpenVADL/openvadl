package vadl.lcb.passes.llvmLowering.strategies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.model.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.model.MachineInstructionNode;
import vadl.lcb.passes.llvmLowering.visitors.ReplaceWithLlvmSDNodesVisitor;
import vadl.lcb.tablegen.model.TableGenInstruction;
import vadl.lcb.tablegen.model.TableGenInstructionOperand;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.EndNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Defines how a {@link Instruction} will be lowered to {@link TableGenInstruction}.
 */
public abstract class LlvmLoweringStrategy {
  private static final Logger logger = LoggerFactory.getLogger(
      LlvmLoweringStrategy.class);

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
   * Generate a lowering result for the given {@link Graph}.
   * If it is not lowerable then return {@link Optional#empty()}.
   */
  public Optional<LlvmLoweringPass.LlvmLoweringIntermediateResult> lower(
      HashMap<InstructionLabel, List<Instruction>> supportedInstructions,
      Instruction instruction,
      InstructionLabel instructionLabel,
      Graph uninlinedBehavior) {
    var visitor = new ReplaceWithLlvmSDNodesVisitor();
    var copy = uninlinedBehavior.copy();
    var nodes = copy.getNodes().toList();
    var instructionIdentifier = instruction.identifier;

    if (!checkIfNoControlFlow(copy) && !checkIfNotAllowedDataflowNodes(copy)) {
      logger.atWarn().log("Instruction '{}' is not lowerable and will be skipped",
          instructionIdentifier.toString());
      return Optional.empty();
    }

    // Continue with lowering of nodes
    for (var node : nodes) {
      visitor.visit(node);

      if (!visitor.isPatternLowerable()) {
        logger.atWarn().log("Instruction '{}' is not lowerable and wil be skipped",
            instructionIdentifier.toString());
        break;
      }
    }

    var inputOperands = getTableGenInputOperands(copy);
    var outputOperands = getTableGenOutputOperands(copy);

    copy.deinitializeNodes();

    if (visitor.isPatternLowerable()) {
      var patterns = generatePatterns(instruction,
          inputOperands,
          copy.getNodes(WriteResourceNode.class).toList());
      var alternativePatterns =
          generatePatternVariations(supportedInstructions,
              instructionLabel,
              copy,
              inputOperands,
              outputOperands,
              patterns);
      return Optional.of(new LlvmLoweringPass.LlvmLoweringIntermediateResult(copy,
          inputOperands, outputOperands,
          Stream.concat(patterns.stream(), alternativePatterns.stream()).toList()));
    }

    logger.atWarn().log("Instruction '{}' is not lowerable", instructionIdentifier.toString());
    return Optional.empty();
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
  protected abstract List<LlvmLoweringPass.LlvmLoweringTableGenPattern> generatePatternVariations(
      HashMap<InstructionLabel, List<Instruction>> supportedInstructions,
      InstructionLabel instructionLabel,
      Graph copy,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<LlvmLoweringPass.LlvmLoweringTableGenPattern> patterns);

  /**
   * LLvm's TableGen cannot work with control flow. So if statements and other constructs are not
   * supported.
   *
   * @return {@code true} if the {@link Graph} is lowerable.
   */
  private boolean checkIfNoControlFlow(Graph behavior) {
    return behavior.getNodes(ControlNode.class)
        .allMatch(x -> x instanceof AbstractBeginNode || x instanceof EndNode); // exceptions
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
  private List<TableGenInstructionOperand> getTableGenOutputOperands(Graph graph) {
    return getOutputOperands(graph)
        .stream().map(operand -> {
          var address = (FieldRefNode) operand.address();

          if (address == null || address.formatField() == null) {
            throw new ViamError("address must not be null");
          }

          return new TableGenInstructionOperand(operand.registerFile().name(),
              address.formatField().identifier.simpleName());
        })
        .toList();
  }

  /**
   * Extracts the input operands from the {@link Graph}.
   */
  private List<TableGenInstructionOperand> getTableGenInputOperands(Graph graph) {
    return getInputOperands(graph)
        .stream()
        .map(LlvmLoweringStrategy::generateTableGenInputOutput)
        .toList();
  }

  /**
   * Generate {@link TableGenInstructionOperand} which looks like "X:$lhs" for TableGen.
   */
  public static TableGenInstructionOperand generateTableGenInputOutput(Node operand) {
    if (operand instanceof ReadRegFileNode node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof ReadRegNode node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof LlvmFieldAccessRefNode node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof FuncCallNode node) {
      return generateInstructionOperand(node);
    } else {
      throw new ViamError("Input operand not supported yet: " + operand);
    }
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(ReadRegNode node) {
    return new TableGenInstructionOperand(node.register().name(),
        node.register().identifier.simpleName());
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(ReadRegFileNode node) {
    var address = (FieldRefNode) node.address();
    return new TableGenInstructionOperand(node.registerFile().name(),
        address.formatField().identifier.simpleName());
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(
      LlvmFieldAccessRefNode node) {
    return new TableGenInstructionOperand(node.immediateOperand().getFullName(),
        node.fieldAccess().identifier.simpleName());
  }

  /**
   * Returns an {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(FuncCallNode node) {
    return new TableGenInstructionOperand(node.function().identifier.lower(),
        node.function().identifier.simpleName());
  }

  /**
   * Most instruction's behaviors have inputs. Those are the results which the instruction requires.
   */
  private static List<Node> getInputOperands(Graph graph) {
    return Stream.concat(Stream.concat(graph.getNodes(ReadResourceNode.class),
            graph.getNodes(FieldAccessRefNode.class)), graph.getNodes(FuncCallNode.class))
        .map(x -> (Node) x).toList();
  }

  /**
   * Most instruction's behaviors have outputs. Those are the results which the instruction emits.
   */
  private static List<WriteRegFileNode> getOutputOperands(Graph graph) {
    return graph.getNodes(WriteRegFileNode.class).toList();
  }

  protected List<LlvmLoweringPass.LlvmLoweringTableGenPattern> generatePatterns(
      Instruction instruction,
      List<TableGenInstructionOperand> inputOperands,
      List<WriteResourceNode> sideEffectNodes) {
    ArrayList<LlvmLoweringPass.LlvmLoweringTableGenPattern> patterns = new ArrayList<>();

    sideEffectNodes.forEach(sideEffectNode -> {
      var patternSelector = getPatternSelector(sideEffectNode);
      var machineInstruction = getMachinePattern(instruction, inputOperands);
      patterns.add(
          new LlvmLoweringPass.LlvmLoweringTableGenPattern(patternSelector, machineInstruction));
    });

    return patterns;
  }

  @NotNull
  private static Graph getPatternSelector(WriteResourceNode sideEffectNode) {
    var graph = new Graph(sideEffectNode.id().toString() + ".selector.lowering");
    var root = sideEffectNode.value();
    root.clearUsages();
    graph.addWithInputs(root);
    return graph;
  }

  @NotNull
  private static Graph getMachinePattern(Instruction instruction,
                                         List<TableGenInstructionOperand> inputOperands) {
    var graph = new Graph(instruction.name() + ".machine.lowering");
    var params = new NodeList<>(
        inputOperands.stream()
            .map(operand -> (ExpressionNode) new ConstantNode(new Constant.Str(operand.render())))
            .toList());
    var node = new MachineInstructionNode(params, instruction);
    graph.addWithInputs(node);
    return graph;
  }
}
