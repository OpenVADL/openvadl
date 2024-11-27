package vadl.iss.passes.tcgLowering;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.configuration.GeneralConfiguration;
import vadl.iss.passes.IssVariableAllocationPass;
import vadl.iss.passes.safeResourceRead.nodes.ExprSaveNode;
import vadl.iss.passes.tcgLowering.nodes.TcgAddNode;
import vadl.iss.passes.tcgLowering.nodes.TcgExtendNode;
import vadl.iss.passes.tcgLowering.nodes.TcgGottoTbAbs;
import vadl.iss.passes.tcgLowering.nodes.TcgLoadMemory;
import vadl.iss.passes.tcgLowering.nodes.TcgMoveNode;
import vadl.iss.passes.tcgLowering.nodes.TcgNode;
import vadl.iss.passes.tcgLowering.nodes.TcgSetCond;
import vadl.iss.passes.tcgLowering.nodes.TcgShiftLeft;
import vadl.iss.passes.tcgLowering.nodes.TcgStoreMemory;
import vadl.iss.passes.tcgLowering.nodes.TcgTruncateNode;
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

public class TcgOpLoweringPass extends Pass {

  public TcgOpLoweringPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("TCG Operation Lowering");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var supportedInstructions = Set.of(
        "ADD",
        "ADDI",
        "LB",
        "SB",
        "ADDIW",
        "SLLI",
        "LUI",
        "BEQ"
    );

    var assignments = passResults.lastResultOf(IssVariableAllocationPass.class,
        IssVariableAllocationPass.Result.class);

    viam.isa().get().ownInstructions()
        .stream().filter(i -> supportedInstructions.contains(i.simpleName()))
        .forEach(i -> {
          new TcgOpLoweringExecutor(requireNonNull(assignments.varAssignments().get(i)))
              .runOn(i.behavior());
        });

    return null;
  }
}


@DispatchFor(
    value = DependencyNode.class,
    include = {"vadl.iss", "vadl.viam"})
class TcgOpLoweringExecutor implements CfgTraverser {

  Map<DependencyNode, TcgV> assignments;
  @LazyInit
  ScheduledNode toReplace;

  public TcgOpLoweringExecutor(Map<DependencyNode, TcgV> assignments) {
    this.assignments = assignments;
  }

  void runOn(Graph graph) {
    var start = getSingleNode(graph, StartNode.class);
    traverseBranch(start);
  }

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

  private void dispatch(DependencyNode node) {
    TcgOpLoweringExecutorDispatcher.dispatch(this, node);
  }

  private TcgV destOf(DependencyNode node) {
    var tcgV = assignments.get(node);
    node.ensure(tcgV != null, "Expected to be represented by a TCGv");
    return tcgV;
  }

  private void replaceCurrent(TcgNode... replacements) {
    if (replacements.length == 0) {
      toReplace.replaceByNothingAndDelete();
    } else {
      for (var i = 0; i < replacements.length - 1; i++) {
        // add all but the last replacement before the scheduled node
        addBeforeCurrent(replacements[i]);
      }
      // finally replace the scheduled node by the last replacement
      var last = replacements[replacements.length - 1];
      toReplace.replaceAndLinkAndDelete(last);
    }
  }

  private void addBeforeCurrent(TcgNode node) {
    toReplace.addBefore(node);
  }

  void handle(InstrExitNode node) {
    var pcWrite = node.pcWrite();
    // address jump to value
    node.replaceAndLinkAndDelete(
        new TcgGottoTbAbs(pcWrite.value())
    );
  }

  @Handler
  void handle(ExprSaveNode toHandle) {
    var destVar = destOf(toHandle);
    var srcVar = destOf(toHandle.value());
    replaceCurrent(new TcgMoveNode(destVar, srcVar));
  }

  @Handler
  void handle(TruncateNode toHandle) {
    var dest = destOf(toHandle);
    var src = destOf(toHandle.value());
    replaceCurrent(new TcgTruncateNode(dest, src, toHandle.type().bitWidth()));
  }

  @Handler
  void handle(ZeroExtendNode toHandle) {
    // Nothing to do? As we operate on zero extension all the time (constant size of registers)
    // TODO: Check if this is correct
    replaceCurrent();
  }

  @Handler
  void handle(SignExtendNode toHandle) {
    var dest = destOf(toHandle);
    var src = destOf(toHandle.value());
    var fromSize = Tcg_8_16_32.from(toHandle.value());
    replaceCurrent(new TcgExtendNode(fromSize, TcgExtend.SIGN, dest, src));
  }

  @Handler
  void handle(BuiltInCall toHandle) {
    var result = BuiltInTcgLoweringExecutor.lower(toHandle, assignments);
    replaceCurrent(result.replacements().toArray(TcgNode[]::new));
  }

  @Handler
  void handle(ReadRegNode toHandle) {
    // nothing to do as register reads are just TCGvs which are created at the
    // instruction's start
    replaceCurrent();
  }

  @Handler
  void handle(ReadRegFileNode toHandle) {
    // nothing to do as register file reads are just TCGvs which are created at the
    // instruction's start
    replaceCurrent();
  }

  @Handler
  void handle(ReadMemNode toHandle) {
    var dest = destOf(toHandle);
    var src = destOf(toHandle.address());
    var loadSize = Tcg_8_16_32_64.fromWidth(toHandle.type().bitWidth());

    // TODO: Don't hardcode this
    var mode = TcgExtend.SIGN;

    replaceCurrent(
        new TcgLoadMemory(loadSize, mode, dest, src)
    );
  }

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

  @Handler
  void handle(WriteMemNode toHandle) {
    var addr = destOf(toHandle.address());
    var value = destOf(toHandle.value());

    var storeSize = Tcg_8_16_32_64.from(toHandle.value());
    // TODO: @jozott Don't hardcode this
    var mode = TcgExtend.SIGN;

    replaceCurrent(
        new TcgStoreMemory(storeSize, mode, value, addr)
    );
  }


  //// Nodes that are not yet supported ////

  @Handler
  void handle(LetNode toHandle) {
    throw new UnsupportedOperationException("Type LetNode not yet implemented");
  }

  @Handler
  void handle(SelectNode toHandle) {
    throw new UnsupportedOperationException("Type SelectNode not yet implemented");
  }

  @Handler
  void handle(SliceNode toHandle) {
    throw new UnsupportedOperationException("Type SliceNode not yet implemented");
  }

  @Handler
  void handle(TupleGetFieldNode toHandle) {
    throw new UnsupportedOperationException("Type TupleGetFieldNode not yet implemented");
  }

  //// Nodes that should never be handled ////

  @Handler
  void handle(ParamNode toHandle) {
    throw failShouldNotHappen(toHandle);
  }

  @Handler
  void handle(ConstantNode toHandle) {
    throw failShouldNotHappen(toHandle);
  }

  @Handler
  void handle(TypeCastNode toHandle) {
    throw failShouldNotHappen(toHandle);
  }

  @Handler
  void handle(FuncCallNode toHandle) {
    throw failShouldNotHappen(toHandle);
  }


  private static ViamGraphError failShouldNotHappen(DependencyNode node) {
    return new ViamGraphError("%s should never be turned in to TCG operations",
        node.getClass().getSimpleName())
        .addContext(node);
  }

}

record BuiltInResult(
    List<TcgNode> replacements
) {
}


class BuiltInTcgLoweringExecutor {

  private static final Map<BuiltInTable.BuiltIn, Function<Context, BuiltInResult>>
      impls =
      Map.of(
          BuiltInTable.ADD, (ctx) -> out(
              new TcgAddNode(ctx.dest(), ctx.src(0), ctx.src(1))
          ),

          BuiltInTable.LSL, (ctx) -> out(
              new TcgShiftLeft(ctx.dest(), ctx.src(0), ctx.src(1))
          ),

          BuiltInTable.EQU, (ctx) -> out(
              new TcgSetCond(ctx.dest(), ctx.src(0), ctx.src(1), TcgCondition.EQ)
          )
      );


  public static BuiltInResult lower(BuiltInCall call, Map<DependencyNode, TcgV> assignments) {
    var context = new Context(assignments, call);
    var impl = impls.get(call.builtIn());

    call.ensure(impl != null, "No TCG lowering implementation for built-in %s found",
        call.builtIn());

    return impl.apply(context);
  }

  private static BuiltInResult out(TcgNode... nodes) {
    return new BuiltInResult(List.of(nodes));
  }

  private record Context(
      Map<DependencyNode, TcgV> assignments,
      BuiltInCall call
  ) {
    private TcgV dest() {
      var dest = assignments.get(call);
      call.ensure(dest != null, "Expected to be represented by a TCGv");
      return dest;
    }

    private TcgV src(int index) {
      call.ensure(call.arguments().size() > index, "Tried to access arg %s", index);
      var arg = call.arguments().get(index);
      var src = assignments.get(arg);
      arg.ensure(src != null, "Expected to be represented by a TCGv");
      return src;
    }

  }
}
