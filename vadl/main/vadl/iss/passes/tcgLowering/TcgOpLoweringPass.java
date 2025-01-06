package vadl.iss.passes.tcgLowering;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;
import static vadl.utils.GraphUtils.hasUser;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.IssVarSsaAssignment;
import vadl.iss.passes.nodes.IssStaticPcRegNode;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.safeResourceRead.nodes.ExprSaveNode;
import vadl.iss.passes.tcgLowering.nodes.TcgAddNode;
import vadl.iss.passes.tcgLowering.nodes.TcgAndNode;
import vadl.iss.passes.tcgLowering.nodes.TcgExtendNode;
import vadl.iss.passes.tcgLowering.nodes.TcgGottoTbAbs;
import vadl.iss.passes.tcgLowering.nodes.TcgLoadMemory;
import vadl.iss.passes.tcgLowering.nodes.TcgLookupAndGotoPtr;
import vadl.iss.passes.tcgLowering.nodes.TcgMoveNode;
import vadl.iss.passes.tcgLowering.nodes.TcgNode;
import vadl.iss.passes.tcgLowering.nodes.TcgNotNode;
import vadl.iss.passes.tcgLowering.nodes.TcgOrNode;
import vadl.iss.passes.tcgLowering.nodes.TcgSarNode;
import vadl.iss.passes.tcgLowering.nodes.TcgSetCond;
import vadl.iss.passes.tcgLowering.nodes.TcgSetIsJmp;
import vadl.iss.passes.tcgLowering.nodes.TcgShlNode;
import vadl.iss.passes.tcgLowering.nodes.TcgShrNode;
import vadl.iss.passes.tcgLowering.nodes.TcgStoreMemory;
import vadl.iss.passes.tcgLowering.nodes.TcgSubNode;
import vadl.iss.passes.tcgLowering.nodes.TcgTruncateNode;
import vadl.iss.passes.tcgLowering.nodes.TcgUnaryNopNode;
import vadl.iss.passes.tcgLowering.nodes.TcgXorNode;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ParamNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TupleGetFieldNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.passes.CfgTraverser;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

/**
 * A pass that lowers high-level operations to TCG (Tiny Code Generator) operations.
 * It processes instructions and transforms them into TCG nodes,
 * which can be used for code generation.
 */
public class TcgOpLoweringPass extends Pass {

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

  @Override
  public IssConfiguration configuration() {
    return (IssConfiguration) super.configuration();
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

    var assignments = passResults.lastResultOf(IssVarSsaAssignment.class,
        IssVarSsaAssignment.Result.class);

    viam.isa().get().ownInstructions()
        .forEach(i ->
            new TcgOpLoweringExecutor(requireNonNull(assignments.varAssignments().get(i)),
                configuration().targetSize())
                .runOn(i.behavior()));

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
  Map<DependencyNode, TcgVRefNode> assignments;

  /**
   * The scheduled node currently being processed.
   */
  @LazyInit
  ScheduledNode toReplace;

  Tcg_32_64 targetSize;

  /**
   * Constructs a new {@code TcgOpLoweringExecutor} with the given variable assignments.
   *
   * @param assignments The map of dependency nodes to their assigned TCG variables.
   */
  public TcgOpLoweringExecutor(Map<DependencyNode, TcgVRefNode> assignments, Tcg_32_64 targetSize) {
    this.assignments = assignments;
    this.targetSize = targetSize;
  }

  /**
   * Runs the lowering process on the given graph.
   *
   * @param graph The graph to process.
   */
  void runOn(Graph graph) {
    // first set jump, as later the info isn't available anymore
    setJmp(graph);

    // lower all nodes
    var start = getSingleNode(graph, StartNode.class);
    traverseBranch(start);
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
      handle(instrExitNode);
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
   *
   * @param node The dependency node.
   * @return The assigned TCG variable.
   */
  private TcgVRefNode destOf(DependencyNode node) {
    var tcgV = assignments.get(node);
    node.ensure(tcgV != null, "Expected to be represented by a TCGv");
    return tcgV;
  }

  private boolean isTcg(DependencyNode node) {
    return assignments.containsKey(node);
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
      toReplace.replaceByNothingAndDelete();
    } else {
      for (var i = 0; i < replacements.length - 1; i++) {
        // Add all but the last replacement before the scheduled node
        addBeforeCurrent(replacements[i]);
      }
      // Finally replace the scheduled node by the last replacement
      var last = replacements[replacements.length - 1];
      toReplace.replaceAndLinkAndDelete(last);
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
  void handle(InstrExitNode node) {
    if (isTcg(node.pcWrite())) {
      // if the pc is not statically defined, we must jump to the current PC
      node.replaceAndLinkAndDelete(
          new TcgLookupAndGotoPtr()
      );
    } else {
      var pcWrite = node.pcWrite();
      // Address jump to value
      node.replaceAndLinkAndDelete(
          new TcgGottoTbAbs(pcWrite.value()));
    }
  }

  // Handler methods for different node types

  /**
   * Handles the {@link ExprSaveNode} by generating a TCG move operation.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(ExprSaveNode toHandle) {
    var destVar = destOf(toHandle);
    var srcVar = destOf(toHandle.value());
    replaceCurrent(new TcgMoveNode(destVar, srcVar));
  }

  /**
   * Handles the {@link TruncateNode} by generating a TCG truncate operation.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(TruncateNode toHandle) {
    var dest = destOf(toHandle);
    var src = destOf(toHandle.value());
    replaceCurrent(new TcgTruncateNode(dest, src, toHandle.type().bitWidth()));
  }

  /**
   * Handles the {@link ZeroExtendNode}. Currently does nothing as zero extension is implied.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(ZeroExtendNode toHandle) {
    var dest = destOf(toHandle);
    var src = destOf(toHandle.value());
    // TODO: This must be either optimized earlier, or be fixed.
    //   the current solution isn't good.
    // Nothing to do; zero extension is implied in TCG operations
    replaceCurrent(new TcgUnaryNopNode(dest, src));
  }

  /**
   * Handles the {@link SignExtendNode} by generating a TCG sign extension operation.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(SignExtendNode toHandle) {
    var dest = destOf(toHandle);
    var src = destOf(toHandle.value());
    var fromSize = toHandle.value().type().asDataType().bitWidth();
    replaceCurrent(new TcgExtendNode(fromSize, TcgExtend.SIGN, dest, src));
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

  /**
   * Handles the {@link ReadRegNode}. Currently does nothing as TCG variables
   * represent register reads.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(ReadRegNode toHandle) {
    // Nothing to do; register reads are TCG variables created at instruction start
    replaceCurrent();
  }

  /**
   * Handles the {@link ReadRegFileNode}. Currently does nothing as TCG variables
   * represent register file reads.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(ReadRegFileNode toHandle) {
    // Nothing to do; register file reads are TCG variables created at instruction start
    replaceCurrent();
  }

  /**
   * Handles the {@link ReadMemNode} by generating a TCG load memory operation.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(ReadMemNode toHandle) {
    var dest = destOf(toHandle);
    var src = destOf(toHandle.address());
    var loadSize = Tcg_8_16_32_64.fromWidth(toHandle.type().bitWidth());

    // TODO: Don't hardcode this
    var mode = TcgExtend.ZERO;

    replaceCurrent(
        new TcgLoadMemory(loadSize, mode, dest, src)
    );
  }

  /**
   * Handles the {@link WriteRegFileNode} by generating a TCG move operation if necessary.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(WriteRegFileNode toHandle) {
    var destVar = destOf(toHandle);
    var srcVar = destOf(toHandle.value());
    if (destVar.equals(srcVar)) {
      replaceCurrent();
    } else {
      replaceCurrent(new TcgMoveNode(destVar, srcVar));
    }
  }

  /**
   * Handles the {@link WriteRegNode} by generating a TCG move operation if necessary.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(WriteRegNode toHandle) {
    var destVar = destOf(toHandle);
    var srcVar = destOf(toHandle.value());
    if (destVar.width() != srcVar.width()) {
      replaceCurrent();
    } else {
      replaceCurrent(new TcgMoveNode(destVar, srcVar));
    }
  }

  /**
   * Handles the {@link WriteMemNode} by generating a TCG store memory operation.
   *
   * @param toHandle The node to handle.
   */
  @Handler
  void handle(WriteMemNode toHandle) {
    var addr = destOf(toHandle.address());
    var value = destOf(toHandle.value());

    var storeSize = Tcg_8_16_32_64.from(toHandle.value());
    // TODO: Don't hardcode this
    var mode = TcgExtend.SIGN;

    replaceCurrent(
        new TcgStoreMemory(storeSize, mode, value, addr)
    );
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

  /**
   * Handles the {@link SelectNode}. Currently not implemented.
   *
   * @param toHandle The node to handle.
   * @throws UnsupportedOperationException Always thrown.
   */
  @Handler
  void handle(SelectNode toHandle) {
    throw new UnsupportedOperationException("Type SelectNode not yet implemented");
  }

  /**
   * Handles the {@link SliceNode}. Currently not implemented.
   *
   * @param toHandle The node to handle.
   * @throws UnsupportedOperationException Always thrown.
   */
  @Handler
  void handle(SliceNode toHandle) {
    throw new ViamGraphError("Type SliceNode not yet implemented")
        .addContext(toHandle);
  }

  /**
   * Handles the {@link TupleGetFieldNode}. Currently not implemented.
   *
   * @param toHandle The node to handle.
   * @throws UnsupportedOperationException Always thrown.
   */
  @Handler
  void handle(TupleGetFieldNode toHandle) {
    throw new UnsupportedOperationException("Type TupleGetFieldNode not yet implemented");
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
   * Handles the {@link TypeCastNode}. Should never happen.
   *
   * @throws ViamGraphError Always thrown.
   */
  @Handler
  void handle(TypeCastNode toHandle) {
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
        .set(BuiltInTable.ADD, (ctx) -> out(
            new TcgAddNode(ctx.dest(), ctx.src(0), ctx.src(1))
        ))


        .set(BuiltInTable.SUB, (ctx) -> out(
            new TcgSubNode(ctx.dest(), ctx.src(0), ctx.src(1))
        ))

        //// Logical ////

        .set(BuiltInTable.NOT, (ctx) -> out(
            new TcgNotNode(ctx.dest(), ctx.src(0), ctx.src(1))
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
            new TcgExtendNode(ctx.argWidth(0), TcgExtend.SIGN, ctx.tmp(0), ctx.src(0)),
            new TcgSarNode(ctx.dest(), ctx.tmp(0), ctx.src(1))
        ))

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
                                    Map<DependencyNode, TcgVRefNode> assignments,
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
      Map<DependencyNode, TcgVRefNode> assignments,
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
      var dest = assignments.get(call);
      call.ensure(dest != null, "Expected to be represented by a TCGv");
      return dest;
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
      var src = assignments.get(arg);
      arg.ensure(src != null, "Expected to be represented by a TCGv");
      return src;
    }

    /**
     * Returns a temporary tcgV ref node for the given local index id.
     * If a temp for {@code i} does not exist yet, it creates one and returns it.
     *
     * @param i refers to a specific temporary
     * @return a new temp tcgV
     */
    private TcgVRefNode tmp(int i) {
      return localTmps.computeIfAbsent(i, (k) -> {
        var name = "tmp_" + call.id + "_" + k;
        return graph().addWithInputs(new TcgVRefNode(TcgV.tmp(name, targetSize)));
      });
    }

    private int argWidth(int i) {
      return call.arguments().get(i).type().asDataType().bitWidth();
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