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

package vadl.iss.passes.tcgLowering;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;
import static vadl.utils.GraphUtils.intU;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.TcgPassUtils;
import vadl.iss.passes.nodes.IssConstExtractNode;
import vadl.iss.passes.nodes.IssGhostCastNode;
import vadl.iss.passes.nodes.IssLoadNode;
import vadl.iss.passes.nodes.IssSelectNode;
import vadl.iss.passes.nodes.IssStaticPcRegNode;
import vadl.iss.passes.nodes.IssStoreNode;
import vadl.iss.passes.nodes.IssValExtractNode;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.opDecomposition.nodes.IssMul2Node;
import vadl.iss.passes.opDecomposition.nodes.IssMulhNode;
import vadl.iss.passes.safeResourceRead.nodes.ExprSaveNode;
import vadl.iss.passes.tcgLowering.nodes.TcgAddNode;
import vadl.iss.passes.tcgLowering.nodes.TcgAndNode;
import vadl.iss.passes.tcgLowering.nodes.TcgClzNode;
import vadl.iss.passes.tcgLowering.nodes.TcgConstSelectNode;
import vadl.iss.passes.tcgLowering.nodes.TcgDepositNode;
import vadl.iss.passes.tcgLowering.nodes.TcgDivNode;
import vadl.iss.passes.tcgLowering.nodes.TcgExtractNode;
import vadl.iss.passes.tcgLowering.nodes.TcgGenException;
import vadl.iss.passes.tcgLowering.nodes.TcgGottoTb;
import vadl.iss.passes.tcgLowering.nodes.TcgLoadMemory;
import vadl.iss.passes.tcgLowering.nodes.TcgLookupAndGotoPtr;
import vadl.iss.passes.tcgLowering.nodes.TcgMovCondNode;
import vadl.iss.passes.tcgLowering.nodes.TcgMoveNode;
import vadl.iss.passes.tcgLowering.nodes.TcgMul2Node;
import vadl.iss.passes.tcgLowering.nodes.TcgMulNode;
import vadl.iss.passes.tcgLowering.nodes.TcgNegNode;
import vadl.iss.passes.tcgLowering.nodes.TcgNode;
import vadl.iss.passes.tcgLowering.nodes.TcgNotNode;
import vadl.iss.passes.tcgLowering.nodes.TcgOrNode;
import vadl.iss.passes.tcgLowering.nodes.TcgRemNode;
import vadl.iss.passes.tcgLowering.nodes.TcgRotlNode;
import vadl.iss.passes.tcgLowering.nodes.TcgRotrNode;
import vadl.iss.passes.tcgLowering.nodes.TcgSarNode;
import vadl.iss.passes.tcgLowering.nodes.TcgSetCond;
import vadl.iss.passes.tcgLowering.nodes.TcgSetIsJmp;
import vadl.iss.passes.tcgLowering.nodes.TcgShlNode;
import vadl.iss.passes.tcgLowering.nodes.TcgShrNode;
import vadl.iss.passes.tcgLowering.nodes.TcgStoreMemory;
import vadl.iss.passes.tcgLowering.nodes.TcgSubNode;
import vadl.iss.passes.tcgLowering.nodes.TcgXorNode;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.ExceptionDef;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.LabelNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ParamNode;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TupleGetFieldNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteStageOutputNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.passes.CfgTraverser;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

/**
 * A pass that lowers high-level operations to TCG (Tiny Code Generator) operations.
 * It processes instructions and transforms them into TCG nodes,
 * which can be used for code generation.
 *
 * <p>From paper: The scheduled dependency nodes are lowered into control nodes,
 * each corresponding to one or more TCG operations.
 * During this process, a node retrieves its destination and input TCG variables from a context
 * that generates variables on demand and attaches them to the dependency node.
 * Once lowering is complete, all dependency nodes are removed from the graph.
 * The resulting structure is a CFG consisting of TCG op nodes in SSA form.</p>
 */
public class TcgOpLoweringPass extends AbstractIssPass {

  /**
   * Constructs a new {@code TcgOpLoweringPass} with the specified configuration.
   *
   * @param configuration The general configuration for this pass.
   */
  public TcgOpLoweringPass(IssConfiguration configuration) {
    super(configuration);
  }

  /**
   * Returns the name of the pass.
   *
   * @return The pass name "TCG Operation Lowering".
   */
  @Override
  public PassName getName() {
    return PassName.of("TCG Operation Lowering");
  }

  /**
   * Executes the TCG operation lowering pass on the given specification.
   * It processes instructions and lowers them to TCG operations.
   *
   * @param passResults The results from previous passes.
   * @param viam        The VIAM specification.
   * @return {@code null}
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    // only if we skip the jmp slot optimization, we set it to false
    var optJmpSlot = !configuration().isSkip(IssConfiguration.IssOptsToSkip.OPT_JMP_SLOTS);

    viam.isa().get().ownInstructions()
        .forEach(i ->
            new TcgOpLoweringExecutor(i.expectExtension(TcgCtx.class).assignment(),
                configuration().targetSize())
                .runOn(i.behavior(), optJmpSlot));

    return null;
  }
}

@DispatchFor(
    value = DependencyNode.class,
    include = {"vadl.iss", "vadl.viam"})
class TcgOpLoweringExecutor implements CfgTraverser {

  /**
   * Map of dependency nodes to their assigned TCG variables.
   */
  TcgCtx.Assignment assignments;

  /**
   * The scheduled node currently being processed.
   */
  @LazyInit
  ScheduledNode toReplace;
  @LazyInit
  Graph graph;

  Tcg_32_64 targetSize;

  // indicates whether jump slot 1 is already
  // used by some branch (instr exit)
  boolean isJumpSlotTaken = false;
  // indicates if we want to optimize jumps with jump slots.
  // only false if `--skip opt-jmp-slots` was passed.
  boolean optJumpSlot = true;

  /**
   * Constructs a new {@code TcgOpLoweringExecutor} with the given variable assignments.
   *
   * @param assignments The map of dependency nodes to their assigned TCG variables.
   */
  public TcgOpLoweringExecutor(TcgCtx.Assignment assignments,
                               Tcg_32_64 targetSize) {
    this.assignments = assignments;
    this.targetSize = targetSize;
  }

  /**
   * Runs the lowering process on the given graph.
   *
   * @param graph The graph to process.
   */
  void runOn(Graph graph, boolean optJumpSlot) {
    this.optJumpSlot = optJumpSlot;
    this.graph = graph;
    // first set jump, as later the info isn't available anymore
    setJmp(graph);

    // lower all nodes
    var start = getSingleNode(graph, StartNode.class);
    traverseBranch(start);

    // remove side effects from the end node (no longer needed there).
    // it just makes the graph bloated.
    var instrEndNode = getSingleNode(graph, InstrEndNode.class);
    instrEndNode.replaceAndDelete(new InstrEndNode(new NodeList<>()));

    // finally, delete all scheduled nodes
    graph.deleteDanglingControlNodes();
  }

  /**
   * Set {@code ctx->is_jmp} to {@code DISAS_CHAIN} if there are InstrExits in the instruction
   * that are not in the default branch.
   * This allows chaining of instructions.
   */
  private void setJmp(Graph graph) {
    var instrEnd = getSingleNode(graph, InstrEndNode.class);

    var containsJmps = graph.getNodes(InstrExitNode.class).findAny().isPresent();
    if (!containsJmps) {
      // if there are no jumps, we don't have to chain any instructions
      return;
    }

    // check if there is an unconditional jump (InstrExit) at the default branch.
    // this is the case if there is some side effect of at the instrEnd that
    // is used by some InstrExit node.
    var unconditionalJump = instrEnd.sideEffects().stream()
        .anyMatch(s -> s.usages().anyMatch(u -> u instanceof InstrExitNode));

    if (!unconditionalJump) {
      // if there is no unconditional jump, we must chain the instruction with the next one
      // by setting the jmp type to chain.
      // the tcg_stop_tb method will take care about the instruction chaining.
      instrEnd.addBefore(new TcgSetIsJmp(TcgSetIsJmp.Type.CHAIN));
    } else {
      // if the jump is unconditional we must exit the tb loop anyway
      instrEnd.addBefore(new TcgSetIsJmp(TcgSetIsJmp.Type.NORETURN));
    }
  }

  /**
   * Overrides the traversal of directional nodes in the CFG.
   * Processes scheduled nodes by dispatching them to appropriate handlers.
   *
   * @param dirNode The directional node to traverse.
   * @return The next control node in the traversal.
   */
  @Override
  public ControlNode traverseDirectional(DirectionalNode dirNode) {
    var next = dirNode.next();
    if ((dirNode instanceof ScheduledNode scheduledNode)) {
      toReplace = scheduledNode;
      dispatch(scheduledNode.node());
    } else if ((dirNode instanceof InstrExitNode instrExitNode)) {
      switch (instrExitNode) {
        case InstrExitNode.PcChange pcChange -> handle(pcChange);
        case InstrExitNode.Raise raise -> handle(raise);
      }
    }
    return next;
  }

  /**
   * Dispatches the given dependency node to the appropriate handler method.
   *
   * @param node The dependency node to dispatch.
   */
  private void dispatch(DependencyNode node) {
    TcgOpLoweringExecutorDispatcher.dispatch(this, node);
  }

  /**
   * Retrieves the TCG variable assigned to the given dependency node.
   * It requires that it has only a single destination.
   *
   * @param node The dependency node.
   * @return The assigned TCG variable.
   */
  private TcgVRefNode singleDestOf(DependencyNode node) {
    return assignments.singleDestOf(node);
  }

  /**
   * Retrieves all TCG variables assigned to the given dependency node.
   *
   * @param node The dependency node.
   * @return The assigned TCG variable.
   */
  private List<TcgVRefNode> allDestOf(DependencyNode node) {
    var tcgVs = assignments.destOf(node);
    node.ensure(!tcgVs.isEmpty(),
        "Expected to be represented by at least one TCGv, but got %s", tcgVs);
    return tcgVs;
  }

  // TODO: Refactor to use context instead
  private final Map<Integer, TcgVRefNode> localTmps = new HashMap<>();
  private int tmpCounter = 0;

  private TcgVRefNode tmp(int i) {
    return localTmps.computeIfAbsent(i, k -> {
      var name = "tmp_l" + k + "_" + tmpCounter++;
      return graph.addWithInputs(new TcgVRefNode(TcgV.tmp(name, targetSize), new NodeList<>()));
    });
  }

  @SuppressWarnings("UnusedMethod")
  private TcgVRefNode constant(Constant.Value value) {
    var constNode = graph.add(new ConstantNode(value));
    return assignments.singleDestOf(constNode);
  }


  private boolean isTcg(DependencyNode node) {
    return TcgPassUtils.isTcg(node);
  }

  /**
   * Replaces the current scheduled node with the given TCG nodes.
   * If multiple replacements are provided, all but the last are added before the current node,
   * and the last replaces the current node.
   *
   * @param replacements The TCG nodes to replace with.
   */
  private void replaceCurrent(TcgNode... replacements) {
    if (replacements.length == 0) {
      // we don't delete it yet, so the isTcg still works as expected
      toReplace.replaceByNothing();
    } else {
      for (var i = 0; i < replacements.length - 1; i++) {
        // Add all but the last replacement before the scheduled node
        addBeforeCurrent(replacements[i]);
      }
      // Finally replace the scheduled node by the last replacement
      var last = replacements[replacements.length - 1];
      toReplace.replaceAndLink(last);
    }
  }

  /**
   * Adds the given TCG node before the current scheduled node.
   *
   * @param node The TCG node to add.
   */
  private void addBeforeCurrent(TcgNode node) {
    toReplace.addBefore(node);
  }

  /**
   * Handles the {@link InstrExitNode} by replacing it with a TCG goto operation.
   *
   * @param node The instruction exit node to handle.
   */
  void handle(InstrExitNode.PcChange node) {
    if (isTcg(node.cause())) {
      // if the pc is not statically defined, we must jump to the current PC
      node.replaceAndLink(
          new TcgLookupAndGotoPtr()
      );
    } else {
      var pcWrite = node.cause();

      var jmpSlot = TcgGottoTb.JmpSlot.LOOK_UP;
      if (!this.isJumpSlotTaken && optJumpSlot) {
        // if the jumpslot (1) is not yet taken, we take it.
        // TODO: We could use heuristic to find the best slot assignment.
        //  (other than just the first one)
        this.isJumpSlotTaken = true;
        jmpSlot = TcgGottoTb.JmpSlot.BRANCH_OUT;
      }

      // Address jump to value
      node.replaceAndLink(
          new TcgGottoTb(pcWrite.value(), jmpSlot));
    }
  }

  /**
   * Handles the {@link InstrExitNode.Raise} by replacing it with an exception generation
   * operation.
   *
   * @param node The instruction exit node to handle.
   */
  void handle(InstrExitNode.Raise node) {
    var args = node.cause().arguments().stream()
        .map(this::singleDestOf)
        .collect(Collectors.toCollection(NodeList::new));
    node.replaceAndLink(
        new TcgGenException(
            (ExceptionDef) node.cause().procedure(),
            args
        ));
  }

  // Handler methods for different node types

  /**
   * Handles the {@link ExprSaveNode} by generating a TCG move operation.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(ExprSaveNode toHandle) {
    var destVar = singleDestOf(toHandle);
    var srcVar = singleDestOf(toHandle.value());
    if (destVar.equals(srcVar)) {
      replaceCurrent();
    } else {
      replaceCurrent(new TcgMoveNode(destVar, srcVar));
    }
  }

  /**
   * Handles the {@link IssConstExtractNode} by generating a {@code (s)extract} operation.
   */
  @Handler
  void handle(IssConstExtractNode toHandle) {
    var dest = singleDestOf(toHandle);
    var src = singleDestOf(toHandle.value());
    var ofs = intU(0, 32).toNode();
    var len = intU(toHandle.fromWidth(), 32).toNode();

    if (toHandle.extendMode() == TcgExtend.ZERO || toHandle.toWidth() == targetSize.width) {
      // if extend mode is zero or toWidth is target size, we can use a single extract node
      replaceCurrent(
          new TcgExtractNode(dest, src, ofs, len, toHandle.extendMode()));
    } else {
      // we have to sign extend and then truncate
      // sextract t0,   src, 0, len
      // extract  dest, t0,  0, toWidth

      var t0 = tmp(0);
      var toWidth = intU(toHandle.fromWidth(), 32).toNode();
      replaceCurrent(
          new TcgExtractNode(t0, src, ofs, len, TcgExtend.SIGN),
          new TcgExtractNode(dest, t0, ofs, toWidth, TcgExtend.ZERO)
      );
    }

  }

  /**
   * Handles the {@link IssValExtractNode} by generating a {@code (s)extract} operation.
   */
  @Handler
  void handle(IssValExtractNode toHandle) {
    toHandle.ensure(!isTcg(toHandle.ofs()) && !isTcg(toHandle.len()),
        "Offset or length of node is TCG scheduled, but offset and length must be "
            + "translation time constant. Node should never been constructed!");
    var dest = singleDestOf(toHandle);
    var src = singleDestOf(toHandle.value());
    replaceCurrent(
        new TcgExtractNode(dest, src, toHandle.ofs(), toHandle.len(), toHandle.extendMode()));
  }

  /**
   * Handles the {@link TruncateNode} by generating a TCG truncate operation.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(TruncateNode toHandle) {
    toHandle.fail(
        "Shouldn't exist at this point. The ExtractNormalizationPass should have replaced it.");
  }

  /**
   * Handles the {@link ZeroExtendNode}. Currently does nothing as zero extension is implied.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(ZeroExtendNode toHandle) {
    toHandle.fail(
        "Shouldn't exist at this point. The ExtractNormalizationPass should have replaced it.");
  }

  /**
   * Handles the {@link SignExtendNode} by generating a TCG sign extension operation.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(SignExtendNode toHandle) {
    toHandle.fail(
        "Shouldn't exist at this point. The ExtractNormalizationPass should have replaced it.");
  }

  /**
   * Translates the IssMul2 built-in into a TCG mul2 (unsigned or signed).
   * It is special, as mul2 returns two results.
   * The upper half and the lower half.
   */
  @Handler
  void handle(IssMul2Node toHandle) {
    var dests = allDestOf(toHandle);
    var loDest = dests.get(0);
    var hiDest = dests.get(1);

    var arg1 = singleDestOf(toHandle.arg1());
    var arg2 = singleDestOf(toHandle.arg2());

    replaceCurrent(new TcgMul2Node(toHandle.kind(), loDest, hiDest, arg1, arg2));
  }

  @Handler
  void handle(IssMulhNode toHandle) {
    var hiDest = singleDestOf(toHandle);
    var loTmpTest = tmp(0);
    var arg1 = singleDestOf(toHandle.arg1());
    var arg2 = singleDestOf(toHandle.arg2());
    replaceCurrent(new TcgMul2Node(toHandle.kind(), loTmpTest, hiDest, arg1, arg2));
  }

  /**
   * Handles the {@link BuiltInCall} by lowering it using the built-in TCG lowering executor.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(BuiltInCall toHandle) {
    var result = BuiltInTcgLoweringExecutor.lower(toHandle, assignments, targetSize);
    replaceCurrent(result.replacements().toArray(TcgNode[]::new));
  }

  @Handler
  void handle(ReadArtificialResNode toHandle) {
    throw new IllegalStateException("Should not reach here, should be inlined.");
  }

  @Handler
  void handle(ReadRegTensorNode toHandle) {
    // Nothing to do; register reads are TCG variables created at instruction start
    replaceCurrent();
  }

  /**
   * Handles the {@link IssLoadNode}, which was created from a {@link ReadMemNode}
   * in the {@link vadl.iss.passes.IssMemoryAccessTransformationPass}.
   */
  @Handler
  void handle(IssLoadNode toHandle) {
    var dest = singleDestOf(toHandle);
    var src = singleDestOf(toHandle.address());

    replaceCurrent(
        new TcgLoadMemory(toHandle.loadSize(), toHandle.tcgExtend(), dest, src)
    );
  }

  /**
   * Handles the {@link WriteRegTensorNode} by generating a TCG move operation if necessary.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(WriteRegTensorNode toHandle) {
    var destVar = singleDestOf(toHandle);
    var srcVar = singleDestOf(toHandle.value());
    if (destVar.equals(srcVar)) {
      replaceCurrent();
    } else {
      replaceCurrent(new TcgMoveNode(destVar, srcVar));
    }
  }

  /**
   * Handles the {@link IssStoreNode}, which was created from a {@link WriteMemNode}
   * in the {@link vadl.iss.passes.IssMemoryAccessTransformationPass}.
   */
  @Handler
  void handle(IssStoreNode toHandle) {
    var addr = singleDestOf(toHandle.address());
    var value = singleDestOf(toHandle.value());

    var storeSize = toHandle.storeSize();
    // doesn't matter (hopefully)
    var mode = TcgExtend.SIGN;

    replaceCurrent(
        new TcgStoreMemory(storeSize, mode, value, addr)
    );
  }

  @Handler
  void handle(WriteArtificialResNode toHandle) {
    throw new UnsupportedOperationException("Type WriteArtificialResNode not yet implemented");
  }

  /// / Nodes that are already considered lowered ////

  @Handler
  void handle(IssStaticPcRegNode node) {
    // nothing to do
  }

  //// Nodes that are not yet supported ////

  /**
   * Handles the {@link LetNode}. Currently not implemented.
   *
   * @param toHandle The node to handle.
   * @throws UnsupportedOperationException Always thrown.
   */
  @Handler
  void handle(LetNode toHandle) {
    throw new UnsupportedOperationException("Type LetNode not yet implemented");
  }

  @Handler
  void handle(SelectNode toHandle) {
    // this should have been turned into an IssSelectNode (see below)
    throw failShouldNotHappen(toHandle);
  }

  // TODO: This should be moved to the branch lowering pass.
  //   Currently all expressions are executed even if they are not required.
  //   The question is: How complex must the expression be so that jumps are less expensive.
  @Handler
  void handle(IssSelectNode toHandle) {
    var dest = singleDestOf(toHandle);
    var trueSrc = singleDestOf(toHandle.trueCase());
    var falseSrc = singleDestOf(toHandle.falseCase());

    var isTcgCondition = isTcg(toHandle.c1()) || isTcg(toHandle.c2());
    if (!isTcgCondition) {
      // if the condition (both c1 and c2) are not TCG, we can produce an optimized
      // TcgConstSelectNode that does only emit a mov instead of a movCond.
      replaceCurrent(new TcgConstSelectNode(dest, toHandle.conditionExpr(), trueSrc, falseSrc));
      return;
    }

    var c1 = singleDestOf(toHandle.c1());
    var c2 = singleDestOf(toHandle.c2());

    replaceCurrent(new TcgMovCondNode(
        dest,
        c1,
        c2,
        trueSrc,
        falseSrc,
        toHandle.condition()
    ));
  }

  /**
   * Handles the {@link SliceNode}.
   * This is done by depositing each part of the slice in the destination variable.
   *
   * @param toHandle The node to handle.
   * @throws UnsupportedOperationException Always thrown.
   */
  @Handler
  void handle(SliceNode toHandle) {
    var destVar = singleDestOf(toHandle);
    var srcVar = singleDestOf(toHandle.value());
    var bitSlice = toHandle.bitSlice();
    if (bitSlice.isContinuous()) {
      // if the slice is continues we only perform a single extract operation
      var pos = intU(bitSlice.lsb(), 32);
      var len = intU(bitSlice.bitSize(), 32);
      var node = new TcgExtractNode(destVar, srcVar, pos.toNode(), len.toNode(), TcgExtend.ZERO);
      replaceCurrent(node);
      return;
    }

    // TODO: Test if it is more efficient to shift + or instead of deposit.
    // TODO: Calling a helper function for performing complex slice
    //  operations might be more efficient.
    // constructed result
    var res = tmp(0);
    // holds value that is deposit next
    var bit = tmp(1);
    var destOffset = 0;
    var ops = new ArrayList<TcgNode>();
    for (var part : bitSlice.parts().toList().reversed()) {
      var len = part.size();
      var offset = part.lsb();
      // extract value to set
      ops.add(new TcgExtractNode(bit, srcVar, offset, len, TcgExtend.ZERO));
      ops.add(new TcgDepositNode(res, res, bit, destOffset, len));
      destOffset += len;
    }
    ops.add(new TcgMoveNode(destVar, res));

    replaceCurrent(ops.toArray(new TcgNode[0]));
  }

  /**
   * Handles the {@link TupleGetFieldNode}. Currently not implemented.
   *
   * @param toHandle The node to handle.
   * @throws UnsupportedOperationException Always thrown.
   */
  @Handler
  void handle(TupleGetFieldNode toHandle) {
    var dest = singleDestOf(toHandle);
    var srcs = allDestOf(toHandle.expression());
    toHandle.ensure(toHandle.index() < srcs.size(), "Get tuple index out of bounds.");
    var src = srcs.get(toHandle.index());

    if (dest.equals(src)) {
      // if dest equals src, we don't emit move
      replaceCurrent();
    } else {
      replaceCurrent(new TcgMoveNode(dest, src));
    }
  }

  //// Nodes that should never be handled ////

  /**
   * Handles the {@link ParamNode}. Should never happen.
   *
   * @throws ViamGraphError Always thrown.
   */
  @Handler
  void handle(ParamNode toHandle) {
    throw failShouldNotHappen(toHandle);
  }

  /**
   * Handles the {@link ConstantNode}. Should never happen.
   *
   * @throws ViamGraphError Always thrown.
   */
  @Handler
  void handle(ConstantNode toHandle) {
    throw failShouldNotHappen(toHandle);
  }

  /**
   * Handles the {@link ReadMemNode}. Should be replaced by a {@link IssLoadNode} in the
   * {@link vadl.iss.passes.IssMemoryAccessTransformationPass}.
   */
  @Handler
  void handle(ReadMemNode toHandle) {
    throw failShouldNotHappen(toHandle);
  }

  /**
   * Handles the {@link WriteMemNode}. Should be replaced by a {@link IssStoreNode} in the
   * {@link vadl.iss.passes.IssMemoryAccessTransformationPass}.
   */
  @Handler
  void handle(WriteMemNode toHandle) {
    throw failShouldNotHappen(toHandle);
  }

  /**
   * Handles the {@link FuncCallNode}. Should never happen.
   *
   * @throws ViamGraphError Always thrown.
   */
  @Handler
  void handle(FuncCallNode toHandle) {
    throw failShouldNotHappen(toHandle);
  }

  @Handler
  void handle(TcgVRefNode toHandle) {
    throw failShouldNotHappen(toHandle);
  }

  @Handler
  void handle(AsmBuiltInCall toHandle) {
    throw failShouldNotHappen(toHandle);
  }

  @Handler
  void handle(ReadStageOutputNode toHandle) {
    throw failShouldNotHappen(toHandle);
  }

  @Handler
  void handle(WriteStageOutputNode toHandle) {
    throw failShouldNotHappen(toHandle);
  }

  @Handler
  void handle(ProcCallNode toHandle) {
    throw failShouldNotHappen(toHandle);
  }

  @Handler
  void handle(LabelNode toHandle) {
    throw failShouldNotHappen(toHandle);
  }

  /**
   * The {@link IssGhostCastNode} is removed in the {@link vadl.iss.passes.IssTcgSchedulingPass}
   * if it had been scheduled.
   * So it cannot occur during op lowering.
   */
  @Handler
  void handle(IssGhostCastNode toHandle) {
    throw failShouldNotHappen(toHandle);
  }

  /**
   * Throws a {@link ViamGraphError} indicating that the node should not be handled.
   *
   * @return A {@link ViamGraphError} exception.
   */
  private static ViamGraphError failShouldNotHappen(DependencyNode node) {
    return new ViamGraphError("%s should never be turned into TCG operations",
        node.getClass().getSimpleName())
        .addContext(node);
  }

}


/**
 * Represents the result of lowering a built-in function call to TCG nodes.
 *
 * @param replacements The list of TCG nodes that replace the built-in call.
 */
record BuiltInResult(
    List<TcgNode> replacements
) {
}

/**
 * Lowers built-in function calls to TCG nodes.
 * Provides implementations for specific built-in functions.
 */
class BuiltInTcgLoweringExecutor {

  /**
   * Holds the implementations of the different built-ins.
   */
  private static final Map<BuiltInTable.BuiltIn, Function<Context, BuiltInResult>> impls;

  static {
    impls = new MapBuilder<BuiltInTable.BuiltIn, Function<Context, BuiltInResult>>(new HashMap<>())

        //// Arithmetic ////
        .set(BuiltInTable.NEG, (ctx) -> out(
            new TcgNegNode(ctx.dest(), ctx.src(0))
        ))

        .set(BuiltInTable.ADD, (ctx) -> out(
            new TcgAddNode(ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        .set(BuiltInTable.SUB, (ctx) -> out(
            new TcgSubNode(ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        .set(BuiltInTable.MUL, (ctx) -> out(
            new TcgMulNode(ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        .set(BuiltInTable.SMULL, (ctx) -> {
          ctx.call.ensure(ctx.call.type().asDataType().bitWidth() <= ctx.targetSize.width,
              "Result does not fit. Should be decomposed before.");
          return out(
              new TcgMulNode(ctx.dest(), ctx.src(0), ctx.src(1))
          );
        })

        .set(BuiltInTable.UMULL, (ctx) -> {
          ctx.call.ensure(ctx.call.type().asDataType().bitWidth() <= ctx.targetSize.width,
              "Result does not fit. Should be decomposed before.");
          return out(
              new TcgMulNode(ctx.dest(), ctx.src(0), ctx.src(1))
          );
        })

        .set(BuiltInTable.SUMULL, (ctx) -> {
          ctx.call.ensure(ctx.call.type().asDataType().bitWidth() <= ctx.targetSize.width,
              "Result does not fit. Should be decomposed before.");
          return out(
              new TcgMulNode(ctx.dest(), ctx.src(0), ctx.src(1))
          );
        })

        .set(BuiltInTable.SDIV, (ctx) -> out(
            new TcgDivNode(true, ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        .set(BuiltInTable.UDIV, (ctx) -> out(
            new TcgDivNode(false, ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        .set(BuiltInTable.SMOD, (ctx) -> out(
            new TcgRemNode(true, ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        .set(BuiltInTable.UMOD, (ctx) -> out(
            new TcgRemNode(false, ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        //// Logical ////

        .set(BuiltInTable.NOT, (ctx) -> out(
            new TcgNotNode(ctx.dest(), ctx.src(0))
        ))

        .set(BuiltInTable.AND, (ctx) -> out(
            new TcgAndNode(ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        .set(BuiltInTable.OR, (ctx) -> out(
            new TcgOrNode(ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        .set(BuiltInTable.XOR, (ctx) -> out(
            new TcgXorNode(ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        .set(BuiltInTable.CLZ, (ctx) -> {
          // the second argument is the fallback value in case of value being 0
          var valSize = ctx.call.arguments().getFirst().type().asDataType().bitWidth();
          var valSizeConst = Constant.Value.of(valSize, Type.bits(ctx.targetSize.width));
          return out(
              new TcgClzNode(ctx.dest(), ctx.src(0), ctx.constant(valSizeConst))
          );
        })

        //// Comparison ////

        .set(BuiltInTable.EQU, (ctx) -> out(
            new TcgSetCond(ctx.dest(), ctx.src(0), ctx.src(1), TcgCondition.EQ)
        ))

        .set(BuiltInTable.NEQ, (ctx) -> out(
            new TcgSetCond(ctx.dest(), ctx.src(0), ctx.src(1), TcgCondition.NE)
        ))

        .set(BuiltInTable.SLTH, (ctx) -> out(
            new TcgSetCond(ctx.dest(), ctx.src(0), ctx.src(1), TcgCondition.LT)
        ))

        .set(BuiltInTable.ULTH, (ctx) -> out(
            new TcgSetCond(ctx.dest(), ctx.src(0), ctx.src(1), TcgCondition.LTU)
        ))

        .set(BuiltInTable.SLEQ, (ctx) -> out(
            new TcgSetCond(ctx.dest(), ctx.src(0), ctx.src(1), TcgCondition.LE)
        ))

        .set(BuiltInTable.ULEQ, (ctx) -> out(
            new TcgSetCond(ctx.dest(), ctx.src(0), ctx.src(1), TcgCondition.LEU)
        ))

        .set(BuiltInTable.SGTH, (ctx) -> out(
            new TcgSetCond(ctx.dest(), ctx.src(0), ctx.src(1), TcgCondition.GT)
        ))

        .set(BuiltInTable.UGTH, (ctx) -> out(
            new TcgSetCond(ctx.dest(), ctx.src(0), ctx.src(1), TcgCondition.GTU)
        ))

        .set(BuiltInTable.SGEQ, (ctx) -> out(
            new TcgSetCond(ctx.dest(), ctx.src(0), ctx.src(1), TcgCondition.GE)
        ))

        .set(BuiltInTable.UGEQ, (ctx) -> out(
            new TcgSetCond(ctx.dest(), ctx.src(0), ctx.src(1), TcgCondition.GEU)
        ))

        //// Shifts/Rotates ////

        .set(BuiltInTable.LSL, (ctx) -> out(
            new TcgShlNode(ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        .set(BuiltInTable.LSR, (ctx) -> out(
            new TcgShrNode(ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        // when doing an arithmatic shift right, we must sign extend the source value
        .set(BuiltInTable.ASR, (ctx) -> out(
            new TcgSarNode(ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        .set(BuiltInTable.ROR, (ctx) -> out(
            new TcgRotrNode(ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        .set(BuiltInTable.ROL, (ctx) -> out(
            new TcgRotlNode(ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        //// MISC ////

        .set(BuiltInTable.CONCATENATE_BITS, (ctx) -> {
          // we use a deposit of the lhs operand into the rhs operand,
          // so the pos is the width of the rhs,
          // and the len is targetWidth - pos,
          // which makes the operation potentially faster (and is still correct)
          var rhs = ctx.call.arguments().get(1);
          var pos = rhs.type().asDataType().bitWidth();
          var len = ctx.targetSize.width - pos;
          return out(
              new TcgDepositNode(ctx.dest(), ctx.src(1), ctx.src(0),
                  intU(pos, 32).toNode(), intU(len, 32).toNode())
          );
        })

        .build();
  }

  /**
   * Lowers a built-in function call to TCG nodes.
   *
   * @param call        The built-in function call.
   * @param assignments The map of dependency nodes to their assigned TCG variables.
   * @return A {@link BuiltInResult} containing the TCG nodes that replace the built-in call.
   */
  public static BuiltInResult lower(BuiltInCall call,
                                    TcgCtx.Assignment assignments,
                                    Tcg_32_64 targetSize) {
    var context = new Context(assignments, call, targetSize, new HashMap<>());
    var impl = impls.get(call.builtIn());

    call.ensure(impl != null, "No TCG lowering implementation for built-in %s found",
        call.builtIn());

    return impl.apply(context);
  }

  /**
   * Helper method to create a {@link BuiltInResult} from TCG nodes.
   *
   * @param nodes The TCG nodes.
   * @return A {@link BuiltInResult} containing the nodes.
   */
  private static BuiltInResult out(TcgNode... nodes) {
    return new BuiltInResult(List.of(nodes));
  }

  /**
   * Context for lowering a built-in function call.
   */
  private record Context(
      TcgCtx.Assignment assignments,
      BuiltInCall call,
      Tcg_32_64 targetSize,
      HashMap<Integer, TcgVRefNode> localTmps
  ) {
    /**
     * Retrieves the destination TCG variable assigned to the built-in call.
     *
     * @return The destination TCG variable.
     */
    private TcgVRefNode dest() {
      return assignments.singleDestOf(call);
    }

    /**
     * Retrieves the source TCG variable for the argument at the given index.
     *
     * @param index The index of the argument.
     * @return The source TCG variable.
     */
    private TcgVRefNode src(int index) {
      call.ensure(call.arguments().size() > index, "Tried to access arg %s", index);
      var arg = call.arguments().get(index);
      return assignments.singleDestOf(arg);
    }

    /**
     * Returns a temporary tcgV ref node for the given local index id.
     * If a temp for {@code i} does not exist yet, it creates one and returns it.
     *
     * @param i refers to a specific temporary
     * @return a new temp tcgV
     */
    @SuppressWarnings("UnusedMethod")
    private TcgVRefNode tmp(int i) {
      return localTmps.computeIfAbsent(i, (k) -> {
        var name = "tmp_" + call.id + "_" + k;
        return graph().addWithInputs(new TcgVRefNode(TcgV.tmp(name, targetSize), new NodeList<>()));
      });
    }

    private TcgVRefNode constant(Constant.Value value) {
      return assignments.singleDestOf(graph().add(new ConstantNode(value)));
    }


    private Graph graph() {
      return requireNonNull(call.graph());
    }
  }


  private record MapBuilder<K, V>(Map<K, V> map) {
    MapBuilder<K, V> set(K key, V value) {
      map.put(key, value);
      return this;
    }

    Map<K, V> build() {
      return map;
    }
  }
}