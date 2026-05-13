// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.cppCodeGen.CppTypeMap;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.gcb.passes.DetermineRegisterUsesAndDefsPass;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.gcb.passes.RenamedFieldRefNode;
import vadl.gcb.passes.operands.GenerateInstructionOperandsPass;
import vadl.gcb.passes.operands.InstructionOperandsCtx;
import vadl.gcb.passes.operands.model.GcbConstantOperand;
import vadl.gcb.passes.operands.model.GcbInstructionBareSymbolOperand;
import vadl.gcb.passes.operands.model.GcbInstructionImmediateOperand;
import vadl.gcb.passes.operands.model.GcbInstructionIndexedRegisterFileOperand;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionRegisterFileOperand;
import vadl.gcb.valuetypes.ValueType;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.LlvmMayLoadMemory;
import vadl.lcb.passes.llvmLowering.LlvmMayStoreMemory;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.LlvmSideEffectPatternIncluded;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadArtificialResourceNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUnlowerableSD;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbNodeReplacementHandler;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbNodeReplacementHandlerDispatcher;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionConstraint;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionFrameRegisterOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionLabelOperand;
import vadl.lcb.passes.operands.TableGenInstructionImmediateOperand;
import vadl.utils.Pair;
import vadl.viam.Abi;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.PrintableInstruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
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
  protected final ValueType smallestRegisterClassType;

  public LlvmInstructionLoweringStrategy(ValueType architectureType,
                                         ValueType smallestRegisterClassType) {
    this.architectureType = architectureType;
    this.smallestRegisterClassType = smallestRegisterClassType;
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
    var hasSideEffects = false;

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
        isAsCheapAsMove,
        hasSideEffects
    );
  }

  /**
   * Lowers basic instruction information without patterns.
   */
  public LlvmLoweringPass.BaseInstructionInfo lowerBaseInfo(
      Instruction instruction,
      Graph behavior,
      DetermineRegisterUsesAndDefsPass.Info info) {
    var ctx = instruction.expectExtension(InstructionOperandsCtx.class);
    var outputOperands = transformOutputOperands(instruction, behavior, ctx.outputs());
    var inputOperands = transformInputOperands(instruction, behavior, ctx.inputs());

    var flags = getFlags(behavior);

    return new LlvmLoweringPass.BaseInstructionInfo(inputOperands,
        outputOperands,
        flags,
        info.uses(),
        info.defs());
  }

  /**
   * The operand detection in {@link GenerateInstructionOperandsPass} was done on an
   * unmodified graph. But after the lowering, nodes might have changed and different
   * strategies might want to update the operands.
   */
  protected List<GcbInstructionOperand> transformOutputOperands(
      Instruction instruction,
      Graph behavior,
      List<GcbInstructionOperand> operands) {
    return replaceOperands(instruction, behavior, operands);
  }

  /**
   * The operand detection in {@link GenerateInstructionOperandsPass} was done on an
   * unmodified graph. But after the lowering, nodes might have changed and different
   * strategies might want to update the operands.
   */
  protected List<GcbInstructionOperand> transformInputOperands(
      Instruction instruction,
      Graph behavior,
      List<GcbInstructionOperand> operands) {
    return replaceOperands(instruction, behavior, operands);
  }

  protected List<GcbInstructionOperand> replaceOperands(
      Instruction instruction,
      Graph behavior,
      List<GcbInstructionOperand> operands) {
    var fieldAccesses = behavior.getNodes(LlvmFieldAccessRefNode.class).collect(Collectors.toMap(
        FieldAccessRefNode::fieldAccess, x -> x));
    var basicBlocks = behavior.getNodes(LlvmBasicBlockSD.class).collect(Collectors.toMap(
        FieldAccessRefNode::fieldAccess, x -> x));
    var frameIndices = behavior.getNodes(LlvmFrameIndexSD.class).collect(Collectors.toMap(
        LlvmFrameIndexSD::origin, x -> x));

    // Replace generic field accesses by more specialized.
    for (int i = 0; i < operands.size(); i++) {
      var operand = operands.get(i);
      if (operand instanceof GcbInstructionImmediateOperand immediateOperand
          && fieldAccesses.containsKey(immediateOperand.fieldAccess())) {
        var llvmNode = ensureNonNull(fieldAccesses.get(immediateOperand.fieldAccess()),
            () -> Diagnostic.error("There is no lowered field access",
                instruction.location().join(immediateOperand.fieldAccess().location())));

        var llvmNodeBitwidth = llvmNode.type().asDataType().bitWidth();
        var llvmType = llvmNodeBitwidth < this.smallestRegisterClassType.getBitwidth()
            ? this.smallestRegisterClassType
            : ValueType.from(llvmNode.type().asDataType()).get();
        llvmNode =
            new LlvmFieldAccessRefNode(
                instruction,
                immediateOperand.fieldAccess(),
                immediateOperand.fieldAccess().type(),
                llvmType,
                LlvmFieldAccessRefNode.Usage.Immediate);

        operands.set(i, new TableGenInstructionImmediateOperand(llvmNode));
      } else if (operand instanceof GcbInstructionImmediateOperand immediateOperand
          && basicBlocks.containsKey(immediateOperand.fieldAccess())) {
        var llvmNode = ensureNonNull(basicBlocks.get(immediateOperand.fieldAccess()),
            () -> Diagnostic.error("There is no lowered field access",
                instruction.location().join(immediateOperand.fieldAccess().location())));

        operands.set(i, new TableGenInstructionLabelOperand(llvmNode));
      } else if (operand instanceof GcbInstructionImmediateOperand immediateOperand
          && !(operand instanceof TableGenInstructionLabelOperand)
          && !fieldAccesses.containsKey(immediateOperand.fieldAccess())
          && !basicBlocks.containsKey(immediateOperand.fieldAccess())) {
        // This branch is taken when the field access was removed from the instruction's behavior
        // because of optimisations. However, we still need to replace this operand.
        var fieldAccess = immediateOperand.fieldAccess();
        var upcastedType =
            (ValueType) ValueType.from(CppTypeMap.upcast(fieldAccess.accessFunction().returnType()))
                .orElseThrow(
                    () -> Diagnostic.error("Cannot cast type", fieldAccess.location()).build());
        upcastedType = upcastedType.getBitwidth() < this.smallestRegisterClassType.getBitwidth()
            ? this.smallestRegisterClassType
            : upcastedType;
        var llvmNode =
            new LlvmFieldAccessRefNode(instruction,
                fieldAccess,
                fieldAccess.type(),
                upcastedType,
                LlvmFieldAccessRefNode.Usage.Immediate);
        operands.set(i, new TableGenInstructionImmediateOperand(llvmNode));
      } else if (operand instanceof GcbInstructionRegisterFileOperand registerFileOperand
          && registerFileOperand.origin() instanceof ReadRegTensorNode readNode) {
        if (frameIndices.containsKey(readNode)) {
          var llvmNode = frameIndices.get(readNode);
          if (llvmNode.address() instanceof FieldRefNode fieldRefNode) {
            operands.set(i, new TableGenInstructionFrameRegisterOperand(llvmNode, fieldRefNode));
          } else if (llvmNode.address() instanceof FuncParamNode funcParamNode) {
            operands.set(i, new TableGenInstructionFrameRegisterOperand(llvmNode, funcParamNode));
          } else {
            throw Diagnostic.error("Node's address is not supported", llvmNode.address().location())
                .build();
          }
        }
      }
    }

    return operands;
  }

  protected void replaceNode(PrintableInstruction instruction, Node node) {
    LcbNodeReplacementHandlerDispatcher.dispatch(getReplacementHandler(instruction), node);
  }

  protected LcbNodeReplacementHandler getReplacementHandler(PrintableInstruction instruction) {
    return new LcbNodeReplacementHandler(instruction, architectureType,
        this.smallestRegisterClassType);
  }

  /**
   * Generate a lowering result for the given {@link Graph} for pseudo instructions.
   * If it is not lowerable then return {@link Optional#empty()}.
   */
  public Optional<LlvmLoweringRecord.Machine> lowerInstruction(
      IsaMachineInstructionMatchingPass.Result labelledMachineInstructions,
      Instruction instruction,
      Graph unmodifiedBehavior,
      Abi abi,
      DetermineRegisterUsesAndDefsPass.Info registerDefsUses,
      boolean generatePatterns) {
    var copy = unmodifiedBehavior.copy();

    if (!checkIfNoControlFlow(copy) && !checkIfNotAllowedDataflowNodes(copy)) {
      return Optional.empty();
    }

    var constraints = generateConstraints(copy);
    lowerNodes(instruction, copy);

    var isLowerable = !hasRedFlags(instruction, copy);
    var info = lowerBaseInfo(instruction, copy, registerDefsUses);

    if (isLowerable) {
      var additionalBehaviors = new ArrayList<Pair<Graph, List<GcbInstructionOperand>>>();
      // This list stores the optimisations result which can be then displayed in the dump.
      var additionalBehaviorsBookkeeping = new ArrayList<DerivedGraphOptimisationResult>();

      var patterns = new ArrayList<TableGenPattern>();
      var alternatives = new ArrayList<TableGenPattern>();

      if (generatePatterns) {
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
              behavior,
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
      }

      return Optional.of(new LlvmLoweringRecord.Machine(
          instruction,
          info,
          Stream.concat(patterns.stream(), alternatives.stream()).toList(),
          additionalBehaviorsBookkeeping,
          constraints
      ));
    } else {
      return Optional.of(new LlvmLoweringRecord.Machine(
          instruction,
          info,
          Collections.emptyList(),
          Collections.emptyList(),
          constraints));
    }
  }

  /**
   * Return the fields which require a constraint.
   */
  protected List<TableGenInstructionConstraint> generateConstraints(Graph copy) {
    return copy.getNodes(RenamedFieldRefNode.class)
        .map(renamedFieldRefNode -> new TableGenInstructionConstraint(
            renamedFieldRefNode.originalField(), renamedFieldRefNode.replaced()))
        .toList();
  }

  private void lowerNodes(Instruction instruction, Graph copy) {
    // Replaces nodes along side effects.
    for (var endNode : copy.getNodes(SideEffectNode.class).toList()) {
      replaceNode(instruction, endNode);
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
  protected List<Pair<Graph, List<GcbInstructionOperand>>> deriveDifferentBehaviors(
      Instruction instruction,
      Graph copyBaseBehavior,
      List<GcbInstructionOperand> instructionInputOperands) {
    return Collections.emptyList();
  }

  /**
   * Check if some properties for the naive approach do not uphold.
   * Return {@code true} if it is not lowerable.
   */
  protected boolean hasRedFlags(
      Instruction instruction,
      Graph graph) {
    if (hasUnlowerableSDNode(graph)
        || readsSingleRegister(graph)
        || hasSignExtensionInGraph(instruction, graph)
        || hasMultipleOutputs(graph)
        || rejectWhenReadingFromMemory(graph)
        || rejectWhenWritingToMemory(graph)
        || hasUnreplacedBuiltins(graph)
        || hasSelectNodes(graph)
        || hasNotAllOperandsUsed(instruction, graph)
        || hasMultipleUnaryNodes(graph)) {
      return true;
    }

    /*
      Edge case where only one read node is left.

      instruction CSELALX : CondSelectFormat =   let result = if true then
        X(rn)
      else
        X(rm) in
      X(rd) := result as Bits<Size::XSize> as BitsX

      Here we essentially have X(rd) = X(rn) which creates the pattern:

      def : Pat<S:$rn,
        (CSELALX S:$rn, S:$rm)>;

      We don't want that!
     */

    return graph.getNodes(WriteResourceNode.class).limit(2).count() == 1
        && graph.getNodes(ReadResourceNode.class).limit(2).count() == 1
        && graph.getNodes(WriteResourceNode.class)
        .anyMatch(writeResourceNode -> writeResourceNode.value() instanceof ReadResourceNode);
  }

  /**
   * TableGen doesn't like it when they are multiple sext or zext nodes.
   */
  private boolean hasMultipleUnaryNodes(Graph graph) {
    var sext = graph.getNodes(SignExtendNode.class).count();
    var zext = graph.getNodes(ZeroExtendNode.class).count();

    return sext + zext > 1;
  }

  /**
   * TableGen requires that all the operands are used in the pattern. If it doesn't then the
   * instruction is not lowerable.
   */
  private boolean hasNotAllOperandsUsed(Instruction instruction, Graph graph) {
    var ctx = instruction.expectExtension(InstructionOperandsCtx.class);
    var operands = ctx.inputs();

    for (var operand : operands) {
      if (operand instanceof GcbInstructionRegisterFileOperand registerFileOperand) {
        var name = registerFileOperand.name();
        var match = graph.getNodes(FieldRefNode.class)
            .anyMatch(fieldRefNode -> fieldRefNode.formatField().simpleName().equals(name));

        // If no format field is using the operand then we can't lower.
        if (!match) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * If there are any {@link SelectNode} in the graph.
   */
  private boolean hasSelectNodes(Graph graph) {
    return graph.getNodes(SelectNode.class).limit(2).count() == 1;
  }

  /**
   * If there is a {@link BuiltInCall} which is not {@link LlvmNodeLowerable} and thus
   * wasn't replaced.
   */
  protected boolean hasUnreplacedBuiltins(Graph graph) {
    return graph.getNodes(BuiltInCall.class).anyMatch(x -> !(x instanceof LlvmNodeLowerable));
  }

  /**
   * If a sign extend node is right before a register file write then we cannot lower it.
   * This removes the patterns for ADDW, SLLW ...
   */
  private boolean hasSignExtensionInGraph(Instruction instruction, Graph graph) {
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

  /**
   * If the {@link Graph} it has nodes which writes to memory then we cannot lower it.
   */
  protected boolean rejectWhenWritingToMemory(Graph graph) {
    return graph.getNodes(WriteMemNode.class).findAny().isPresent();
  }

  /**
   * If the {@link Graph} it has nodes which read from memory then we cannot lower it.
   */
  protected boolean rejectWhenReadingFromMemory(Graph graph) {
    return graph.getNodes(ReadMemNode.class).findAny().isPresent();
  }

  /**
   * If the {@link Graph} it has multiple writes then we cannot lower it.
   */
  protected boolean hasMultipleOutputs(Graph graph) {
    return graph.getNodes(WriteResourceNode.class).limit(2).count() > 1;
  }

  /**
   * If the behavior contains any registers then it is also not lowerable because LLVM's DAG
   * has no concept of register in the IR.
   */
  protected boolean readsSingleRegister(Graph graph) {
    return graph.getNodes(ReadRegTensorNode.class)
        .anyMatch(n -> n.regTensor().isSingleRegister());
  }

  protected boolean hasUnlowerableSDNode(Graph graph) {
    return graph.getNodes(LlvmUnlowerableSD.class).findAny().isPresent();
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
      List<GcbInstructionOperand> inputOperands,
      List<GcbInstructionOperand> outputOperands,
      List<TableGenPattern> patterns,
      Abi abi);

  /**
   * LLvm's TableGen cannot work with control flow. So if statements and other constructs are not
   * supported.
   *
   * @return {@code true} if the {@link Graph} is lowerable.
   */
  protected boolean checkIfNoControlFlow(Graph behavior) {
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
  protected boolean checkIfNotAllowedDataflowNodes(Graph behavior) {
    return behavior.getNodes(DependencyNode.class)
        .noneMatch(x -> x instanceof FuncParamNode);
  }

  /**
   * Generate {@link GcbInstructionOperand} which looks like "X:$lhs" for TableGen.
   */
  public static GcbInstructionOperand generateTableGenInputOutput(Node operand) {
    if (operand instanceof LlvmFrameIndexSD node) {
      return generateInstructionOperand(node);
    } else if (operand instanceof LlvmReadArtificialResourceNode node) {
      return generateInstructionOperandRegisterFile(node);
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
   * Returns a {@link GcbInstructionOperand} given a {@link Node}.
   */
  private static GcbInstructionOperand generateInstructionOperand(FuncParamNode node) {
    return new GcbInstructionBareSymbolOperand(node,
        node.parameter().simpleName());
  }

  /**
   * Returns a {@link GcbInstructionOperand} given a {@link Node}.
   */
  private static GcbInstructionOperand generateInstructionOperand(LlvmBasicBlockSD node) {
    return new TableGenInstructionLabelOperand(node);
  }

  /**
   * Returns a {@link GcbInstructionOperand} given a {@link Node}.
   */
  private static GcbInstructionOperand generateInstructionOperand(LlvmFrameIndexSD node) {
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
   * Returns a {@link GcbInstructionOperand} given a {@link Node}.
   */
  private static GcbInstructionOperand generateInstructionOperand(
      LlvmFieldAccessRefNode node) {
    if (node.usage() == LlvmFieldAccessRefNode.Usage.Immediate) {
      return new TableGenInstructionImmediateOperand(node);
    } else if (node.usage() == LlvmFieldAccessRefNode.Usage.BasicBlock) {
      return new TableGenInstructionLabelOperand(node);
    } else {
      throw Diagnostic.error("Not supported usage", node.location()).build();
    }
  }

  private static GcbInstructionOperand generateInstructionOperandRegisterFile(
      ReadArtificialResNode node) {
    if (node.address() instanceof FieldRefNode field) {
      return new GcbInstructionRegisterFileOperand(node, field.formatField());
    } else if (node.address() instanceof FuncParamNode funcParamNode) {
      return new GcbInstructionIndexedRegisterFileOperand(node, funcParamNode);
    } else {
      throw Diagnostic.error(
          "The compiler generator needs to generate a tablegen instruction operand from this "
              + "address for a field but it does not support it.",
          node.address().location()).build();
    }
  }

  /**
   * Returns a {@link GcbInstructionOperand} given a {@link Node}.
   */
  private static GcbInstructionOperand generateInstructionOperandRegisterFile(
      ReadRegTensorNode node) {
    if (node.address() instanceof FieldRefNode field) {
      return new GcbInstructionRegisterFileOperand(node, field.formatField());
    } else if (node.address() instanceof FuncParamNode funcParamNode) {
      return new GcbInstructionIndexedRegisterFileOperand(node, funcParamNode);
    } else if (node.address() instanceof ConstantNode constantNode) {
      // The register file has a constant as address.
      // This is ok as long as the value of the register file at the address is also constant.
      // For example, the X0 register in RISC-V which always has a constant value.
      var constraints = node.regTensor().constraints();
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
      return new GcbConstantOperand(constantNode, constantValue);
    } else {
      throw Diagnostic.error(
          "The compiler generator needs to generate a tablegen instruction operand from this "
              + "address for a field but it does not support it.",
          node.address().location()).build();
    }
  }

  /**
   * Returns a {@link GcbInstructionOperand} given a {@link Node}.
   */
  private static GcbInstructionOperand generateInstructionOperandRegisterFile(
      WriteRegTensorNode node) {
    if (node.address() instanceof FieldRefNode field) {
      return new GcbInstructionRegisterFileOperand(node, field);
    } else if (node.address() instanceof FuncParamNode funcParamNode) {
      return new GcbInstructionIndexedRegisterFileOperand(node, funcParamNode);
    } else {
      throw Diagnostic.error(
          "The compiler generator needs to generate a tablegen instruction operand from this "
              + "address for a field but it does not support it.",
          node.address().location()).build();
    }
  }

  protected List<TableGenPattern> generatePatterns(
      Instruction instruction,
      Graph behavior,
      List<GcbInstructionOperand> inputOperands,
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
                                         List<GcbInstructionOperand> inputOperands) {
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
