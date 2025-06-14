// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.lcb.passes.llvmLowering.strategies;

import static vadl.viam.ViamError.ensurePresent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.gcb.passes.pseudo.PseudoFuncParamNode;
import vadl.lcb.codegen.model.llvm.ValueType;
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
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbMulNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbMulhsNodeReplacement;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbMulhuNodeReplacement;
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
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenConstantOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionBareSymbolOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionFrameRegisterOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionIndexedRegisterFileOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionLabelOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionRegisterFileOperand;
import vadl.utils.Pair;
import vadl.viam.Abi;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.PrintableInstruction;
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
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplifier;
import vadl.viam.passes.behaviorRewrite.BehaviorRewritePass;
import vadl.viam.passes.behaviorRewrite.BehaviorRewriteSimplifier;
import vadl.viam.passes.canonicalization.Canonicalizer;

/**
 * Defines how a {@link Instruction} will be lowered to {@link TableGenInstruction}.
 */
public abstract class LlvmInstructionLoweringStrategy {
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
      <? extends Node, ? extends Node>> replacementHooksWithDefaultFieldAccessReplacement(
      PrintableInstruction printableInstruction) {
    var hooks = new ArrayList<GraphVisitor.NodeApplier<? extends Node, ? extends Node>>();
    return replacementHooks(
        hooks,
        new LcbFieldAccessRefNodeReplacement(printableInstruction, hooks, architectureType));
  }

  protected List<GraphVisitor.NodeApplier
      <? extends Node, ? extends Node>> replacementHooksWithFieldAccessWithBasicBlockReplacement(
      PrintableInstruction printableInstruction
  ) {
    var hooks = new ArrayList<GraphVisitor.NodeApplier<? extends Node, ? extends Node>>();
    return replacementHooks(
        hooks,
        new LcbFieldAccessRefNodeByLlvmBasicBlockReplacement(printableInstruction,
            hooks,
            architectureType));
  }

  /**
   * Returns a list of applier which transform a {@link Graph}.
   */
  private List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacementHooks(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> hooks,
      GraphVisitor.NodeApplier<? extends Node, ? extends Node> fieldAccessRefNodeReplacement) {
    var v1 = new LcbBranchEndNodeReplacement(hooks);
    var mul = new LcbMulNodeReplacement(hooks);
    var mulhs = new LcbMulhsNodeReplacement(hooks);
    var mulhu = new LcbMulhuNodeReplacement(hooks);
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
    hooks.add(mul);
    hooks.add(mulhs);
    hooks.add(mulhu);
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
      <? extends Node, ? extends Node>> replacementHooks(PrintableInstruction printableInstruction);

  /**
   * Flags indicate special properties of a machine instruction. This method checks the
   * machine instruction's behavior for those and returns them.
   *
   * @return the flags of an {@link Graph}.
   */
  protected LlvmLoweringPass.Flags getFlags(Graph graph) {
    var isTerminator = graph.getNodes(WriteRegTensorNode.class)
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
    var isBarrier = false;
    var isRemat = false;
    var isAsCheapAsMove = false;

    return new LlvmLoweringPass.Flags(
        isTerminator,
        isBranch,
        isCall,
        isReturn,
        isPseudo,
        isCodeGenOnly,
        mayLoad,
        mayStore,
        isBarrier,
        isRemat,
        isAsCheapAsMove
    );
  }

  /**
   * Lowers basic instruction information without patterns.
   */
  public LlvmLoweringPass.BaseInstructionInfo lowerBaseInfo(Graph behavior) {
    var outputOperands = getTableGenOutputOperands(behavior);
    var inputOperands = getTableGenInputOperands(outputOperands, behavior);

    var registerUses = getRegisterUses(behavior, inputOperands, outputOperands);
    var registerDefs = getRegisterDefs(behavior, inputOperands, outputOperands);
    var flags = getFlags(behavior);

    return new LlvmLoweringPass.BaseInstructionInfo(inputOperands,
        outputOperands,
        flags,
        registerUses,
        registerDefs);
  }

  /**
   * Generate a lowering result for the given {@link Graph} for pseudo instructions.
   * If it is not lowerable then return {@link Optional#empty()}.
   */
  public Optional<LlvmLoweringRecord.Machine> lowerInstruction(
      IsaMachineInstructionMatchingPass.Result labelledMachineInstructions,
      Instruction instruction,
      Graph unmodifiedBehavior,
      Abi abi) {
    var visitor = replacementHooksWithDefaultFieldAccessReplacement(instruction);
    var copy = unmodifiedBehavior.copy();

    if (!checkIfNoControlFlow(copy) && !checkIfNotAllowedDataflowNodes(copy)) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning("Instruction is not lowerable and will be skipped",
              instruction.location()).build());
      return Optional.empty();
    }

    // Continue with lowering of nodes
    for (var endNode : copy.getNodes(SideEffectNode.class).toList()) {
      visitReplacementHooks(visitor, endNode);
    }

    var isLowerable = !hasRedFlags(instruction, copy);
    var info = lowerBaseInfo(copy);
    copy.deinitializeNodes();


    if (isLowerable) {
      var additionalBehaviors = new ArrayList<Pair<Graph, List<TableGenInstructionOperand>>>();
      // This list stores the optimisations result which can be then displayed in the dump.
      var additionalBehaviorsBookkeeping = new ArrayList<DerivedGraphOptimisationResult>();

      var patterns = new ArrayList<TableGenPattern>();
      var alternatives = new ArrayList<TableGenPattern>();

      // The first behavior is always the modified main behavior.
      additionalBehaviors.add(Pair.of(copy, info.inputs()));
      var derivedBehaviors = deriveDifferentBehaviors(instruction, copy, info.inputs());
      additionalBehaviors.addAll(derivedBehaviors);

      // Iterate over all the constructed behaviors.
      for (var pair : additionalBehaviors) {
        var optimisationResult = optimise(pair.left());
        var behavior = optimisationResult.optimised;
        var inputOperands = pair.right();

        var localPatterns = generatePatterns(instruction,
            inputOperands,
            behavior.getNodes(WriteResourceNode.class).toList());
        var localAlternatives =
            generatePatternVariations(
                instruction,
                labelledMachineInstructions,
                behavior,
                inputOperands,
                info.outputs(),
                localPatterns,
                abi);

        patterns.addAll(localPatterns);
        alternatives.addAll(localAlternatives);
        additionalBehaviorsBookkeeping.add(optimisationResult);
      }

      return Optional.of(new LlvmLoweringRecord.Machine(
          instruction,
          info,
          Stream.concat(patterns.stream(), alternatives.stream()).toList(),
          additionalBehaviorsBookkeeping
      ));
    } else {
      return Optional.of(new LlvmLoweringRecord.Machine(
          instruction,
          info,
          Collections.emptyList(),
          Collections.emptyList()));
    }
  }

  /**
   * Helper class to capture the intermediate results between the optimisations.
   */
  public record DerivedGraphOptimisationResult(
      Graph optimised,
      Graph before,
      Graph canonicalized,
      Graph algebraicSimplified
  ) {
  }

  /**
   * Optimises the given graph by running {@link Canonicalizer}, {@link AlgebraicSimplifier} and
   * {@link BehaviorRewriteSimplifier}. This method modifies the given parameter and returns it.
   *
   * @param behavior is the graph which should be optimised.
   */
  private DerivedGraphOptimisationResult optimise(Graph behavior) {
    final var before = behavior.copy();
    Canonicalizer.canonicalize(behavior);
    var canonicalized = behavior.copy();
    new AlgebraicSimplifier(AlgebraicSimplificationPass.rules).run(behavior);
    var algebraicSimplified = behavior.copy();
    new BehaviorRewriteSimplifier(BehaviorRewritePass.rules).run(behavior);

    return new DerivedGraphOptimisationResult(
        behavior, before, canonicalized, algebraicSimplified
    );
  }

  /**
   * There are cases where an {@link Instruction} requires multiple patterns. The easiest
   * approach is to copy an existing behavior and generate the patterns from it.
   *
   * @param instruction              which "owns" the behavior.
   * @param copyBaseBehavior         is the graph which can be copied as a template for the derived
   *                                 patterns.
   * @param instructionInputOperands is the list of input operands of the tableGen record.
   * @return a list of graphs additionally to {@code copyBaseBehavior} which will be lowered. We
   *     also return a list of instruction input operands for each graph since machine patterns are
   *     built with those.
   */
  protected List<Pair<Graph, List<TableGenInstructionOperand>>> deriveDifferentBehaviors(
      Instruction instruction,
      Graph copyBaseBehavior,
      List<TableGenInstructionOperand> instructionInputOperands) {
    return Collections.emptyList();
  }

  /**
   * Check if some properties for the naive approach do not uphold.
   * Return {@code true} if it is not lowerable.
   */
  private boolean hasRedFlags(
      Instruction instruction,
      Graph graph) {
    if (!graph.getNodes(LlvmUnlowerableSD.class).toList().isEmpty()) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning("Instruction is not lowerable and will be skipped",
              instruction.location()).build());
      return true;
    }

    // If the behavior contains any registers then it is also not lowerable because LLVM's DAG
    // has no concept of register in the IR.
    if (graph.getNodes(ReadRegTensorNode.class)
        .anyMatch(n -> n.regTensor().isSingleRegister())) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning(
              "Instruction is not lowerable because it tries to match fixed registers.",
              instruction.location()).build());
      return true;
    }

    // If a sign extend node is right before a register file write then we cannot lower it.
    // This removes the patterns for ADDW, SLLW ...
    if (graph.getNodes(WriteRegTensorNode.class)
        .filter(n -> n.regTensor().isRegisterFile())
        .flatMap(Node::usages)
        .anyMatch(x -> x instanceof SignExtendNode)) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning(
              "Instruction is not lowerable because it tries to sign extend "
                  + "before writing a register file.",
              instruction.location()).build());
      return true;
    }

    return false;
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
   * register definition when a {@link WriteRegTensorNode} with a
   * constant address exists. However, the only registers without any constraints on the
   * register file will be returned. Also program containers are not part of a "Def".
   *
   * @param behavior          of the {@link Instruction}.
   * @param filterConstraints whether registers with constraints should be considered.
   */
  private static List<RegisterRef> getRegisterDefs(Graph behavior, boolean filterConstraints) {
    return behavior.getNodes(WriteRegTensorNode.class)
        .filter(node -> node.staticCounterAccess() == null)
        // all indices must be constant
        .filter(n -> n.indices().stream().allMatch(ExpressionNode::isConstant))
        .map(node -> {
          var reg = node.regTensor();
          reg.ensure(reg.indexDimensions().size() < 2,
              "Only register and register files supported");
          if (reg.isSingleRegister()) {
            return new RegisterRef(reg);
          } else {
            return new RegisterRef(reg, ((ConstantNode) node.indices().getFirst()).constant());
          }
        })
        // Register should not have any constraints. When it does then there is no
        // need that LLVM knows about it because it should not be a dependency.
        .filter(register -> filterConstraints || register.constraints().isEmpty())
        .toList();
  }

  /**
   * Get a list of {@link RegisterRef} which are written. It is considered a
   * register definition when a {@link WriteRegTensorNode} with a
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
   * register usage when a {@link ReadRegTensorNode} with a
   * constant address exists. However, the only registers without any constraints on the
   * register file will be returned. Also program containers are not part of a "Use".
   *
   * @param behavior          of the {@link Instruction}.
   * @param filterConstraints whether registers with constraints should be considered.
   */
  private static List<RegisterRef> getRegisterUses(Graph behavior, boolean filterConstraints) {
    return Stream.concat(behavior.getNodes(ReadRegTensorNode.class)
                .filter(node -> node.staticCounterAccess() == null
                    && node.regTensor().isSingleRegister())
                .map(ReadRegTensorNode::regTensor)
                .map(RegisterRef::new),
            behavior.getNodes(ReadRegTensorNode.class)
                .filter(node -> node.hasConstantAddress() && node.regTensor().isRegisterFile())
                .map(x -> new RegisterRef(x.regTensor(),
                    ((ConstantNode) x.address()).constant()))
        )
        // Register should not have any constraints. When it does then there is no
        // need that LLVM knows about it because it should not be a dependency.
        .filter(register -> filterConstraints || register.constraints().isEmpty())
        .toList();
  }

  /**
   * Get a list of {@link RegisterRef} which are read. It is considered a
   * register usage when a {@link ReadRegTensorNode} with a
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
      IsaMachineInstructionMatchingPass.Result supportedInstructions,
      Graph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns,
      Abi abi);

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


    return filterOutputs(outputOperands,
        getInputOperands(graph)
            .stream()
            .filter(node -> {
              // Why?
              // Because LLVM cannot handle static registers in input or output operands.
              // They belong to defs and uses instead.
              if (node instanceof ReadRegTensorNode readRegTensorNode
                  && readRegTensorNode.regTensor().isRegisterFile()) {
                return !readRegTensorNode.hasConstantAddress();
              }
              return true;
            })
            .map(LlvmInstructionLoweringStrategy::generateTableGenInputOutput))
        .toList();
  }

  /**
   * It is not allowed to have a {@link TableGenInstructionOperand} in the input list
   * when it is already in the output list. That's why we compute the {@code outputOperands}
   * first and then filter out the {@code stream} for elements which already present in
   * {@code outputOperands}.
   */
  protected static Stream<TableGenInstructionOperand> filterOutputs(
      List<TableGenInstructionOperand> outputOperands,
      Stream<TableGenInstructionOperand> stream) {
    /*
    pseudo instruction LA( rd: Index, symbol: Bits<32> ) =
    {
      LUI { rd = rd, imm = hi( symbol ) }
      ADDI { rd = rd, rs1 = rd, imm = lo( symbol ) }
    }

    Here ADDI has a destination `rd` and an input `rs1` which is the same register as the
    destination. For these cases, we do not want the operand in the inputs.
     */

    var visited =
        outputOperands.stream()
            .filter(x -> x instanceof TableGenInstructionRegisterFileOperand
                || x instanceof TableGenInstructionIndexedRegisterFileOperand)
            .collect(Collectors.toSet());

    return stream
        .filter(
            node -> !visited.contains(node));
  }

  /**
   * Generate {@link TableGenInstructionOperand} which looks like "X:$lhs" for TableGen.
   */
  public static TableGenInstructionOperand generateTableGenInputOutput(Node operand) {
    if (operand instanceof LlvmFrameIndexSD node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof ReadRegTensorNode node && node.regTensor().isRegisterFile()) {
      return generateInstructionOperandRegisterFile(node);
    } else if (operand instanceof LlvmFieldAccessRefNode node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof LlvmBasicBlockSD node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof WriteRegTensorNode node && node.regTensor().isRegisterFile()) {
      return generateInstructionOperandRegisterFile(node);
    } else if (operand instanceof FuncParamNode node) {
      return generateInstructionOperand(node);
    } else {
      throw Diagnostic.error(
          "Cannot construct a tablegen instruction operand from the type.",
          operand.location()).build();
    }
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(FuncParamNode node) {
    return new TableGenInstructionBareSymbolOperand(node,
        node.parameter().simpleName());
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(LlvmBasicBlockSD node) {
    return new TableGenInstructionLabelOperand(node);
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(LlvmFrameIndexSD node) {
    if (node.address() instanceof FieldRefNode fieldRefNode) {
      return new TableGenInstructionFrameRegisterOperand(node, fieldRefNode);
    } else if (node.address() instanceof FuncParamNode funcParamNode) {
      return new TableGenInstructionFrameRegisterOperand(node, funcParamNode);
    } else {
      throw Diagnostic.error("Node's address is not supported", node.address().location())
          .build();
    }
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperand(
      LlvmFieldAccessRefNode node) {
    if (node.usage() == LlvmFieldAccessRefNode.Usage.Immediate) {
      return new TableGenInstructionImmediateOperand(node);
    } else if (node.usage() == LlvmFieldAccessRefNode.Usage.BasicBlock) {
      return new TableGenInstructionLabelOperand(node);
    } else {
      throw Diagnostic.error("Not supported usage", node.location()).build();
    }
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperandRegisterFile(
      ReadRegTensorNode node) {
    if (node.address() instanceof FieldRefNode field) {
      return new TableGenInstructionRegisterFileOperand(node, field);
    } else if (node.address() instanceof FuncParamNode funcParamNode) {
      return new TableGenInstructionIndexedRegisterFileOperand(node, funcParamNode);
    } else if (node.address() instanceof ConstantNode constantNode) {
      // The register file has a constant as address.
      // This is ok as long as the value of the register file at the address is also constant.
      // For example, the X0 register in RISC-V which always has a constant value.
      var constraints = Arrays.stream(node.regTensor().constraints()).toList();
      var constraintValue = constraints.stream()
          .filter(
              x -> x.indices().getFirst().intValue() == constantNode.constant().asVal().intValue())
          .findFirst();
      var constRegisterValue = ensurePresent(constraintValue,
          () -> Diagnostic.error("Register file with constant index has no constant value.",
                  constantNode.location())
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
          node.address().location()).build();
    }
  }

  /**
   * Returns a {@link TableGenInstructionOperand} given a {@link Node}.
   */
  private static TableGenInstructionOperand generateInstructionOperandRegisterFile(
      WriteRegTensorNode node) {
    if (node.address() instanceof FieldRefNode field) {
      return new TableGenInstructionRegisterFileOperand(node, field);
    } else if (node.address() instanceof FuncParamNode funcParamNode) {
      return new TableGenInstructionIndexedRegisterFileOperand(node, funcParamNode);
    } else {
      throw Diagnostic.error(
          "The compiler generator needs to generate a tablegen instruction operand from this "
              + "address for a field but it does not support it.",
          node.address().location()).build();
    }
  }

  /**
   * Most instruction's behaviors have inputs. Those are the results which the instruction requires.
   */
  private static List<Node> getInputOperands(Graph graph) {
    // First, the registers
    var x = graph.getNodes(ReadRegTensorNode.class).filter(k -> k.regTensor().isRegisterFile());
    // Then, immediates
    var y = graph.getNodes(FieldAccessRefNode.class);
    // Then, the rest
    var z = graph.getNodes(FuncCallNode.class).flatMap(
        funcCallNode -> funcCallNode.function().behavior().getNodes(FuncParamNode.class));

    // We need this edge case for compiler and pseudo instructions.
    // However, we need to filter for `WriteResourceNode` and `ReadResourceNode`, so
    // the operand is not added twice.
    var u = graph.getNodes(PseudoFuncParamNode.class)
        .filter(k -> k.usages()
            .noneMatch(v -> v instanceof WriteResourceNode || v instanceof ReadResourceNode));

    return Stream.concat(Stream.concat(Stream.concat(x, y), z), u)
        .map(k -> (Node) k).toList();
  }

  /**
   * Most instruction's behaviors have outputs. Those are the results which the instruction emits.
   */
  private static List<WriteRegTensorNode> getOutputOperands(Graph graph) {
    return graph.getNodes(WriteRegTensorNode.class)
        .filter(e -> e.regTensor().isRegisterFile())
        .toList();
  }

  protected List<TableGenPattern> generatePatterns(
      Instruction instruction,
      List<TableGenInstructionOperand> inputOperands,
      List<WriteResourceNode> sideEffectNodes) {
    ArrayList<TableGenPattern> patterns = new ArrayList<>();

    sideEffectNodes.forEach(sideEffectNode -> {
      var patternSelector = generateSelectionPattern(sideEffectNode);
      var machineInstruction = generateMachinePattern(instruction, inputOperands);
      patterns.add(
          new TableGenSelectionWithOutputPattern(patternSelector, machineInstruction));
    });

    return patterns;
  }

  /**
   * Constructs from the given dataflow node a new graph which is the selection pattern.
   */
  @Nonnull
  protected Graph generateSelectionPattern(WriteResourceNode sideEffectNode) {
    var graph = new Graph(sideEffectNode.id().toString() + ".selector.lowering");
    graph.setParentDefinition(Objects.requireNonNull(sideEffectNode.graph()).parentDefinition());

    // Some patterns what that the side effect is included in the pattern.
    Node root = sideEffectNode instanceof LlvmSideEffectPatternIncluded ? sideEffectNode.copy() :
        sideEffectNode.value().copy();
    root.clearUsages();
    graph.addWithInputs(root);
    return graph;
  }

  /**
   * Constructs the pattern which is emitted during instruction selection. This method
   * constructs a graph with the given {@code instruction} and the {@code inputOperands} as
   * operands.
   */
  @Nonnull
  protected Graph generateMachinePattern(Instruction instruction,
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

  /*
  protected <T extends Node & LlvmNodeReplaceable> void replaceNodeByParameterIdentity(
      List<T> selectorNodes,
      Graph machine,
      Function<T, Node> selectorNodeTransformation,
      BiFunction<LcbMachineInstructionParameterNode,
          TableGenInstructionOperand,
          TableGenInstructionOperand>
          machineNodeTransformation) {
    for (var selectorNode : selectorNodes) {
      // selectorNode is something like `X:$rs1`

      // Updates the selector
      var newNode = selectorNodeTransformation.apply(selectorNode);
      selectorNode.replaceAndDelete(newNode);

      // Find the corresponding nodes in the machine graph because we know
      // the parameter identity `selectorParameter` in the selector graph.
      machine.getNodes(LcbMachineInstructionParameterNode.class)
          .filter(candidate ->
              candidate.instructionOperand().origin() instanceof LlvmNodeReplaceable cast
                  && cast.equals(selectorNode))
          .forEach(occurrence -> {
            var operand = machineNodeTransformation.apply(occurrence,
                selectorNode.operand());
            ensure(!operand.equals(occurrence.instructionOperand()),
                "The returned operand must be a new instance because it was modified");
            occurrence.setInstructionOperand(operand);
          });
    }
  } */
}
