package vadl.iss.passes.tcgLowering;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;
import static vadl.viam.ViamError.ensure;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.IssTcgAnnotatePass;
import vadl.iss.passes.tcgLowering.nodes.TcgAddNode;
import vadl.iss.passes.tcgLowering.nodes.TcgAddiNode;
import vadl.iss.passes.tcgLowering.nodes.TcgBr;
import vadl.iss.passes.tcgLowering.nodes.TcgBrCondImm;
import vadl.iss.passes.tcgLowering.nodes.TcgConstantNode;
import vadl.iss.passes.tcgLowering.nodes.TcgExtendNode;
import vadl.iss.passes.tcgLowering.nodes.TcgLabelLabel;
import vadl.iss.passes.tcgLowering.nodes.TcgGetVar;
import vadl.iss.passes.tcgLowering.nodes.TcgGottoTbAbs;
import vadl.iss.passes.tcgLowering.nodes.TcgLoadMemory;
import vadl.iss.passes.tcgLowering.nodes.TcgMoveNode;
import vadl.iss.passes.tcgLowering.nodes.TcgNode;
import vadl.iss.passes.tcgLowering.nodes.TcgOpNode;
import vadl.iss.passes.tcgLowering.nodes.TcgSetCond;
import vadl.iss.passes.tcgLowering.nodes.TcgSetIsJmp;
import vadl.iss.passes.tcgLowering.nodes.TcgSetLabel;
import vadl.iss.passes.tcgLowering.nodes.TcgSetReg;
import vadl.iss.passes.tcgLowering.nodes.TcgSetRegFile;
import vadl.iss.passes.tcgLowering.nodes.TcgShiftLeft;
import vadl.iss.passes.tcgLowering.nodes.TcgStoreMemory;
import vadl.iss.passes.tcgLowering.nodes.TcgTruncateNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.utils.Pair;
import vadl.viam.Constant;
import vadl.viam.Counter;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.BeginNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.passes.GraphProcessor;


/**
 * Represents a transformation pass specifically for TCG (Tiny Code Generator) lowering within
 * the ISS (Instruction Set Simulator).
 * This class extends {@link AbstractIssPass} and executes a lowering operation
 * on all instructions of the ISA.
 */
public class TcgLoweringPass extends AbstractIssPass {

  public TcgLoweringPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("TCG Lowering Pass");
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

    var tcgNodes = (IssTcgAnnotatePass.Result) passResults
        .lastResultOf(IssTcgAnnotatePass.class);

    var isa = viam.isa().get();
    isa.ownInstructions()
        .stream()
        .filter(i -> supportedInstructions.contains(i.identifier.simpleName()))
        .forEach(i -> TcgLoweringExecutor.runOn(i, requireNonNull(isa.pc()), tcgNodes));

    return null;
  }
}


class TcgLoweringExecutor extends GraphProcessor<Node> {

  Instruction instruction;
  StartNode insnStartNode;
  InstrEndNode insnEndNode;
  InstrEndNode newInsnEndNode;
  DirectionalNode lastNode;

  Counter pc;
  Set<Pair<TcgV, WriteResourceNode>> destVars;
  Set<TcgV> tempVars;

  IssTcgAnnotatePass.Result tcgNodes;

  // indicates a `ctx->base.is_jmp = DISAS_NORETURN;` is required
  // if the instruction resets the PC
  TcgSetIsJmp.Type instrJmpType;


  TcgLoweringExecutor(Instruction instruction,
                      Counter pc,
                      IssTcgAnnotatePass.Result tcgNodes) {
    this.instruction = instruction;
    this.insnStartNode = getSingleNode(instruction.behavior(), StartNode.class);
    this.lastNode = insnStartNode;
    this.insnEndNode = getSingleNode(instruction.behavior(), InstrEndNode.class);
    this.newInsnEndNode = instruction.behavior().add(new InstrEndNode(new NodeList<>()));
    this.destVars = new HashSet<>();
    this.tempVars = new HashSet<>();
    this.tcgNodes = tcgNodes;
    this.pc = pc;
    this.instrJmpType = TcgSetIsJmp.Type.NEXT;
  }

  static void runOn(Instruction instruction, Counter pc, IssTcgAnnotatePass.Result tcgNodes) {
    new TcgLoweringExecutor(instruction, pc, tcgNodes).run();
  }

  protected void run() {
    processGraph(instruction.behavior(),
        n -> n instanceof StartNode
    );

    // this pass inserts all TcgGetTemp nodes necessary
    insertTemporaryVariableRetrievals();

    if (instrJmpType != TcgSetIsJmp.Type.NEXT) {
      // everything except for next must be set
      addLast(new TcgSetIsJmp(instrJmpType));
    }

    // remove all control nodes from the initial graph
    instruction.behavior().getNodes(ControlNode.class)
        .forEach(n -> {
          if (n.id().numericId() > insnEndNode.id().numericId() || n.isDeleted()) {
            return;
          }
          recusiveDeletion(n);
        });
  }

  private void recusiveDeletion(Node node) {
    node.usages().toList().forEach(this::recusiveDeletion);
    var pred = node.predecessor();
    if (pred != null) {
      recusiveDeletion(pred);
    }
    if (!node.isDeleted()) {
      node.safeDelete();
    }
  }

  @Override
  protected Node processUnprocessedNode(Node toProcess) {
    if (toProcess instanceof DirectionalNode castedNode) {
      return processDirectional(castedNode);
    } else if (toProcess instanceof AbstractEndNode castedNode) {
      return processEndNode(castedNode);
    } else if (toProcess instanceof IfNode castedNode) {
      return processIf(castedNode);
    } else if (toProcess instanceof DependencyNode castedNode) {
      return processDependencyNodes(castedNode);
    } else {
      throw new ViamGraphError("node not yet supported by tcg lowering")
          .addContext(toProcess);
    }
  }

  /*
   * Any directional node does not have any side effects and just passes the processing
   * to its successor.
   */
  protected Node processDirectional(DirectionalNode dirNode) {
    processNode(dirNode.next());
    return dirNode;
  }

  /*
   * Processes side effect nodes. But does nothing more.
   */
  protected Node processEndNode(AbstractEndNode node) {
    node.sideEffects().forEach(this::processNode);
    return node;
  }

  /*
   * Just removes the node and calls the next node
   */
  protected Node processBeginNode(BeginNode node) {
    node.replaceByNothingAndDelete();
    return lastNode;
  }


  /*
   * Firstly emits the TCG for the condition, than emits the else branch
   * if a branch forward to the end of the statement and lastly produces the if branch.
   */
  protected Node processIf(IfNode dirNode) {
    var ifLabel = genLabelObj("if_label");
    var endLabel = genLabelObj("end_label");


    // create tcg if and end labels
    addLast(new TcgLabelLabel(ifLabel));
    addLast(new TcgLabelLabel(endLabel));


    // TODO: here we have potential to optimize the branch by using a branch condition
    // instead of the result of the condition.
    var condition = getResultOf(dirNode.condition(), TcgOpNode.class);

    // produce 0 value node to compare to
    var zeroValue = requireNonNull(dirNode.graph())
        .add(new ConstantNode(Constant.Value.of(0, Type.bits(condition.dest().width.width))));

    // check if true by check if value is not 0.
    // if true, we branch to the ifLabel.
    addLast(new TcgBrCondImm(condition.dest(), zeroValue, TcgCondition.NE, ifLabel));

    processNode(dirNode.falseBranch());

    // unconditionally jump to endLabel
    addLast(new TcgBr(endLabel));

    // set branch of ifLabel to mark start of ifBranch
    addLast(new TcgSetLabel(ifLabel));

    // produce the true branch
    processNode(dirNode.trueBranch());

    // set end label and return it just to have some value in graph processor
    return addLast(new TcgSetLabel(endLabel));
  }

  protected Node processDependencyNodes(DependencyNode toProcess) {
    toProcess.visitInputs(this);

    var node = callProcess(toProcess);
    if (node == null) {
      return lastNode;
    }
    if (!node.isActive()) {
      requireNonNull(toProcess.graph())
          .addWithInputs(node);
    }

    if (node instanceof TcgNode tcgOpNode) {
      addLast(tcgOpNode);
    }

    return node;
  }

  protected @Nullable Node callProcess(Node toProcess) {
    if (!tcgNodes.tcgNodes().contains(toProcess)) {
      // if not a tcg node, we just return it unprocessed
      return toProcess;
    }

    if (toProcess instanceof BuiltInCall call) {
      return process(call);
    } else if (toProcess instanceof ReadRegFileNode regFileRead) {
      return process(regFileRead);
    } else if (toProcess instanceof WriteRegFileNode regFileWrite) {
      return process(regFileWrite);
    } else if (toProcess instanceof ReadRegNode castedNode) {
      return process(castedNode);
    } else if (toProcess instanceof WriteRegNode castedNode) {
      return process(castedNode);
    } else if (toProcess instanceof ReadMemNode readMemNode) {
      return process(readMemNode);
    } else if (toProcess instanceof WriteMemNode writeMemNode) {
      return process(writeMemNode);
    } else if (toProcess instanceof FieldRefNode) {
      return toProcess;
    } else if (toProcess instanceof ConstantNode) {
      return toProcess;
    } else if (toProcess instanceof SignExtendNode signExtendNode) {
      return process(signExtendNode);
    } else if (toProcess instanceof ZeroExtendNode) {
      return null;
    } else if (toProcess instanceof TruncateNode truncateNode) {
      return process(truncateNode);
    } else if (toProcess instanceof LetNode letNode) {
      process(letNode);
      return null;
    } else {
      throw new ViamGraphError("node not yet supported by tcg lowering")
          .addContext(toProcess)
          .addContext(instruction);
    }
  }

  private Node process(ReadRegFileNode toProcess) {
    var addressRepl = processedNodes.get(toProcess.address());
    toProcess.ensure(addressRepl instanceof ExpressionNode, "unexpected TctNode as input");

    // TODO: @jzottele Don't hardcode type!
    var width = TcgWidth.i64;
    var tcgVar = TcgV.gen(width);
    return new TcgGetVar.TcgGetRegFile(
        toProcess.registerFile(), (ExpressionNode) addressRepl,
        TcgGetVar.TcgGetRegFile.Kind.SRC,
        tcgVar
    );
  }

  private Node process(ReadRegNode toProcess) {

    if (toProcess.register() == pc.registerResource()) {
      // if we read the pc register we are doing this via
      // ctx->base.pc_next
      // TODO: Make a custom node (TcgReadPC)
      return toProcess;
    }

    // TODO: @jzottele Don't hardcode type!
    var width = TcgWidth.i64;
    var tcgVar = TcgV.gen(width);
    return new TcgGetVar.TcgGetReg(
        toProcess.register(),
        tcgVar
    );
  }

  private @Nullable Node process(WriteRegNode toProcess) {

    if (toProcess.register() == pc.registerResource()) {
      var pcDest = getResultOf(toProcess.value(), ExpressionNode
          .class);
      // TODO: Determine if chain or noReturn. Chain only if end might be reached.
      instrJmpType = TcgSetIsJmp.Type.CHAIN;
      return new TcgGottoTbAbs(pcDest);
    }

    var valRes = getResultOf(toProcess.value(), Node.class);

    // TODO: Don't hardcode this
    var width = TcgWidth.i64;
    var destVar = TcgV.gen(width);

    if (valRes instanceof ExpressionNode valueImm) {
      // if the value is an immediate (not from a tcg operation)
      // we produce an immediate node
      addLast(new TcgConstantNode(destVar, valueImm));
    } else {
      var prevOp = ((TcgOpNode) valRes);
      // get destVar from register destination
      destVars.add(Pair.of(destVar, toProcess));
      if (toProcess.value().usageCount() <= 1) {
        // if value is used by only this node, we can directly write the result to the register
        replaceResultVar(prevOp, destVar);
      } else {
        // the value is used somewhere else, so we cannot directly write it here
        addLast(new TcgMoveNode(destVar, prevOp.dest()));
      }
    }


    return new TcgSetReg(toProcess.register(), destVar);

  }

  private @Nullable Node process(WriteRegFileNode toProcess) {
    var valRes = getResultOf(toProcess.value(), Node.class);

    // TODO: Don't hardcode this
    var width = TcgWidth.i64;
    var destVar = TcgV.gen(width);

    if (valRes instanceof ExpressionNode valueImm) {
      // if the value is an immediate (not from a tcg operation)
      // we produce an immediate node
      addLast(new TcgConstantNode(destVar, valueImm));
    } else {
      var prevOp = ((TcgOpNode) valRes);
      // get destVar from register destination
      destVars.add(Pair.of(destVar, toProcess));
      if (toProcess.value().usageCount() <= 1) {
        // if value is used by only this node, we can directly write the result to the register
        replaceResultVar(prevOp, destVar);
      } else {
        // the value is used somewhere else, so we cannot directly write it here
        addLast(new TcgMoveNode(destVar, prevOp.dest()));
      }
    }

    return new TcgSetRegFile(toProcess.registerFile(), toProcess.address(), destVar);
  }

  private TcgNode process(ReadMemNode toProcess) {
    // TODO: Not always a TcgNode
    var addrTcgRes = getResultOf(toProcess.address(), TcgOpNode.class);

    var readWidth = toProcess.type().bitWidth();
    var loadSize = Tcg_8_16_32_64.fromWidth(readWidth);

    // TODO: @jzottele Don't hardcode this
    var mode = TcgExtend.SIGN;

    // TODO: @jzottele Don't hardcode type!
    var width = TcgWidth.i64;
    var res = TcgV.gen(width);
    tempVars.add(res);


    return new TcgLoadMemory(loadSize, mode, res, addrTcgRes.dest());
  }

  private TcgNode process(WriteMemNode toProcess) {
    // TODO: Not always a TcgNode
    var addrTcgRes = getResultOf(toProcess.address(), TcgOpNode.class);
    var valTcgRes = getResultOf(toProcess.value(), TcgOpNode.class);

    var writeWidth = toProcess.value().type().asDataType().bitWidth();
    var storeSize = Tcg_8_16_32_64.fromWidth(writeWidth);

    // TODO: @jzottele Don't hardcode this
    var mode = TcgExtend.SIGN;

    return new TcgStoreMemory(storeSize, mode, valTcgRes.dest(), addrTcgRes.dest());
  }

  private TcgNode process(SignExtendNode toProcess) {
    var argTcg = getResultOf(toProcess.value(), TcgOpNode.class);

    var size = Tcg_8_16_32.fromWidth(toProcess.value().type().asDataType().bitWidth());
    var resSize = TcgWidth.fromWidth(toProcess.type().bitWidth());
    var res = TcgV.gen(resSize);

    return new TcgExtendNode(size, TcgExtend.SIGN, res, argTcg.dest());
  }

  private TcgNode process(TruncateNode toProcess) {
    var argTcg = getResultOf(toProcess.value(), TcgOpNode.class);

    var resultWidth = toProcess.type().asDataType().bitWidth();

    // TODO: Don't hardcode this
    var res = TcgV.gen(TcgWidth.i64);
    tempVars.add(res);

    return new TcgTruncateNode(res, argTcg.dest(), resultWidth);
  }

  private void process(LetNode node) {
    var prevRes = getResultOf(node.expression(), TcgOpNode.class);

    var var = prevRes.dest();
    var.setName(node.letName().name());
  }

  private Node process(BuiltInCall call) {
    return produceOpNode(call);
  }


  private TcgNode produceOpNode(BuiltInCall call) {
    var args = call.inputs()
        .map(processedNodes::get)
        .toList();

    // TODO: @jzottele Don't hardcode width!
    var width = TcgWidth.i64;
    var res = TcgV.gen(width);
    tempVars.add(res);

    if (call.builtIn() == BuiltInTable.ADD) {
      if (isBinaryImm(args)) {
        return new TcgAddiNode(res, asOp(args.get(0)).dest(), (ExpressionNode) args.get(1));
      } else {
        // add result variable to tempVars
        return new TcgAddNode(res, asOp(args.get(0)).dest(), asOp(args.get(1)).dest());
      }
    } else if (call.builtIn() == BuiltInTable.LSL && isBinaryImm(args)) {
      return new TcgShiftLeft(res, asOp(args.get(0)).dest(), (ExpressionNode) args.get(1));
    } else if (call.builtIn() == BuiltInTable.EQU && !isBinaryImm(args)) {
      return new TcgSetCond(res, asOp(args.get(0)).dest(), asOp(args.get(1)).dest(),
          TcgCondition.EQ);
    } else {
      throw new ViamGraphError("built-in call not yet supported by tcg lowering")
          .addContext(call)
          .addContext(instruction);
    }
  }

  private void insertTemporaryVariableRetrievals() {
    for (var tcgVar : tempVars) {
      var getTmp = new TcgGetVar.TcgGetTemp(tcgVar);
      insnStartNode.addAfter(getTmp);
    }
    for (var pair : destVars) {
      var writeNode = pair.right();
      if (writeNode instanceof WriteRegNode writeRegNode) {
        var getTmp = new TcgGetVar.TcgGetReg(
            writeRegNode.register(),
            pair.left()
        );
        insnStartNode.addAfter(getTmp);
      } else if (writeNode instanceof WriteRegFileNode writeRegFileNode) {
        var getTmp = new TcgGetVar.TcgGetRegFile(
            writeRegFileNode.registerFile(),
            writeRegFileNode.address(),
            TcgGetVar.TcgGetRegFile.Kind.DEST,
            pair.left());
        insnStartNode.addAfter(getTmp);
      } else {
        writeNode.ensure(false, "Write node not implemented yet");
      }
    }
  }


  private void replaceResultVar(TcgOpNode opNode, TcgV newVar) {
    var oldRes = opNode.dest();
    opNode.setDest(newVar);

    tempVars.remove(oldRes);
  }

  private <T extends DirectionalNode> T addLast(T opNode) {
    var node = lastNode.addAfter(opNode);
    lastNode = opNode;
    lastNode.setNext(newInsnEndNode);
    return node;
  }

  /**
   * Returns true if the args are indicate that the op is an immediate version.
   * If it returns false, it is a normal reg to reg version.
   *
   * <p>It will fail if the args are not 2, or are neither reg-reg nor reg-imm.
   */
  private boolean isBinaryImm(List<Node> args) {
    ensure(args.size() == 2, "Does not have two arguments: %s", args);
    if (args.get(0) instanceof TcgNode && args.get(1) instanceof ExpressionNode) {
      return true;
    } else if (args.get(0) instanceof TcgNode && args.get(1) instanceof TcgNode) {
      return false;
    } else {
      throw new ViamError("Invalid binary arguments")
          .addContext("args", args);
    }
  }

  private TcgOpNode asOp(Node node) {
    node.ensure(node instanceof TcgOpNode, "Not a TcgNode");
    return (TcgOpNode) node;
  }

  private int labelCnt = 0;

  private TcgLabel genLabelObj(@Nullable String namePrefix) {
    var prefix = "l";
    if (namePrefix != null) {
      prefix = namePrefix + "_";
    }
    return new TcgLabel(prefix + labelCnt++);
  }

}

