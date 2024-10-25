package vadl.iss.passes.tcgLowering;

import static vadl.utils.GraphUtils.getSingleNode;
import static vadl.viam.ViamError.ensure;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.IssTcgAnnotatePass;
import vadl.iss.passes.tcgLowering.nodes.TcgAddNode;
import vadl.iss.passes.tcgLowering.nodes.TcgAddiNode;
import vadl.iss.passes.tcgLowering.nodes.TcgExtendNode;
import vadl.iss.passes.tcgLowering.nodes.TcgGetVar;
import vadl.iss.passes.tcgLowering.nodes.TcgLoadMemory;
import vadl.iss.passes.tcgLowering.nodes.TcgMoveNode;
import vadl.iss.passes.tcgLowering.nodes.TcgOpNode;
import vadl.iss.passes.tcgLowering.nodes.TcgSetRegFile;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.utils.Pair;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
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
        "ADDI"
        , "LB"
    );

    var tcgNodes = (IssTcgAnnotatePass.Result) passResults
        .lastResultOf(IssTcgAnnotatePass.class);

    viam.isa().get().ownInstructions()
        .stream()
        .filter(i -> supportedInstructions.contains(i.identifier.simpleName()))
        .forEach(i -> TcgLoweringExecutor.runOn(i, tcgNodes));

    return null;
  }
}


class TcgLoweringExecutor extends GraphProcessor<Node> {

  Instruction instruction;
  StartNode insnStartNode;
  InstrEndNode insnEndNode;
  DirectionalNode lastNode;

  Set<Pair<TcgV, WriteResourceNode>> destVars;
  Set<TcgV> tempVars;

  IssTcgAnnotatePass.Result tcgNodes;


  TcgLoweringExecutor(Instruction instruction,
                      IssTcgAnnotatePass.Result tcgNodes) {
    this.instruction = instruction;
    this.insnStartNode = getSingleNode(instruction.behavior(), StartNode.class);
    this.lastNode = insnStartNode;
    this.insnEndNode = getSingleNode(instruction.behavior(), InstrEndNode.class);
    this.destVars = new HashSet<>();
    this.tempVars = new HashSet<>();
    this.tcgNodes = tcgNodes;
  }

  static void runOn(Instruction instruction, IssTcgAnnotatePass.Result tcgNodes) {
    new TcgLoweringExecutor(instruction, tcgNodes).run();
  }

  protected void run() {
    processGraph(instruction.behavior(),
        n -> n instanceof SideEffectNode
    );

    // this pass inserts all TcgGetTemp nodes necessary
    insertTemporaryVariableRetrievals();

    // Remove original end node with side effects
    var newEndNode = instruction.behavior().add(
        new InstrEndNode(new NodeList<>())
    );
    insnEndNode.replaceAndDelete(newEndNode);
  }

  @Override
  protected Node processUnprocessedNode(Node toProcess) {
    toProcess.visitInputs(this);

    var node = callProcess(toProcess);
    if (node == null) {
      return lastNode;
    }
    if (!node.isActive()) {
      Objects.requireNonNull(toProcess.graph())
          .addWithInputs(node);
    }

    if (node instanceof TcgOpNode tcgOpNode) {
      if (lastNode != null) {
        lastNode.setNext(tcgOpNode);
      }

      tcgOpNode.setNext(insnEndNode);
      lastNode = tcgOpNode;
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
    } if (toProcess instanceof ReadMemNode readMemNode) {
      return process(readMemNode);
//    } else if (toProcess instanceof WriteMemNode writeMemNode) {
//      return process(writeMemNode);
    } else if (toProcess instanceof FieldRefNode) {
      return toProcess;
    } else if (toProcess instanceof ConstantNode) {
      // TODO: @jzottele Make sense of this
      return toProcess;
    } else if (toProcess instanceof SignExtendNode signExtendNode) {
      return process(signExtendNode);
    } else if (toProcess instanceof ZeroExtendNode) {
      return toProcess;
    } else if (toProcess instanceof TruncateNode) {
      return toProcess;
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

  private @Nullable Node process(WriteRegFileNode toProcess) {
    var valRepl = getResultOf(toProcess.value(), TcgOpNode.class);

    // TODO: @jzottele Don't hardcode type!
    var width = TcgWidth.i64;
    var res = TcgV.gen(width);
    destVars.add(Pair.of(res, toProcess));


    var prevOp = valRepl;
    if (toProcess.value().usageCount() <= 1) {
      replaceResultVar(valRepl, res);
    } else {
      // the value is used somewhere else, so we cannot directly write it here
      prevOp = lastNode.addAfter(
          new TcgMoveNode(
              res, valRepl.res(), width)
      );
      lastNode = prevOp;
    }

    return new TcgSetRegFile(toProcess.registerFile(), toProcess.address(), prevOp.res());
  }

  private TcgOpNode process(ReadMemNode toProcess) {
    // TODO: Not always a TcgOpNode
    var addrTcgRes = getResultOf(toProcess.address(), TcgOpNode.class);

    var readWidth = toProcess.type().bitWidth();
    var loadSize = Tcg_8_16_32_64.fromWidth(readWidth);

    // TODO: @jzottele Don't hardcode this
    var mode = TcgExtend.SIGN;

    // TODO: @jzottele Don't hardcode type!
    var width = TcgWidth.i64;
    var res = TcgV.gen(width);
    tempVars.add(res);


    return new TcgLoadMemory(loadSize, mode, res, addrTcgRes.res(), width);
  }

//  private TcgOpNode process(WriteMemNode toProcess) {
//    var valRes = getResultOf(toProcess.value(), TcgOpNode.class);
//    var addrRes = getResultOf(toProcess.address(), TcgOpNode.class);
//
//
//    return
//  }

  private Node process(BuiltInCall call) {
    return produceOpNode(call);
  }


  private TcgOpNode produceOpNode(BuiltInCall call) {
    var args = call.inputs()
        .map(processedNodes::get)
        .toList();

    // TODO: @jzottele Don't hardcode width!
    var width = TcgWidth.i64;
    var res = TcgV.gen(width);
    tempVars.add(res);

    if (call.builtIn() == BuiltInTable.ADD) {
      if (isBinaryImm(args)) {
        return new TcgAddiNode(res, asOp(args.get(0)).res(), (ExpressionNode) args.get(1), width);
      } else {
        // add result variable to tempVars
        return new TcgAddNode(res, asOp(args.get(0)).res(), asOp(args.get(1)).res(), width);
      }
    } else {
      throw new ViamGraphError("built-in call not yet supported by tcg lowering")
          .addContext(call)
          .addContext(instruction);
    }
  }

  private TcgOpNode process(SignExtendNode toProcess) {
    var argTcg = getResultOf(toProcess.value(), TcgOpNode.class);

    var size = Tcg_8_16_32.fromWidth(toProcess.value().type().asDataType().bitWidth());
    var resSize = TcgWidth.fromWidth(toProcess.type().bitWidth());
    var res = TcgV.gen(resSize);

    return new TcgExtendNode(size, TcgExtend.SIGN, res, argTcg.res());
  }

  private void process(LetNode node) {
    var prevRes = getResultOf(node.expression(), TcgOpNode.class);

    var var = prevRes.res();
    var.setName(node.letName().name());
  }

  private void insertTemporaryVariableRetrievals() {
    for (var tcgVar : tempVars) {
      var getTmp = new TcgGetVar.TcgGetTemp(tcgVar);
      insnStartNode.addAfter(getTmp);
      var beforeEnd = (DirectionalNode) insnEndNode.predecessor();
    }
    for (var pair : destVars) {
      var writeNode = pair.right();
      writeNode.ensure(writeNode instanceof WriteRegFileNode, "Write node not implemented yet");
      var writeRegNode = (WriteRegFileNode) writeNode;
      var getTmp = new TcgGetVar.TcgGetRegFile(
          writeRegNode.registerFile(),
          writeRegNode.address(),
          TcgGetVar.TcgGetRegFile.Kind.DEST,
          pair.left());
      insnStartNode.addAfter(getTmp);
    }
  }


  private void replaceResultVar(TcgOpNode opNode, TcgV newVar) {
    var oldRes = opNode.res();
    opNode.setRes(newVar);

    tempVars.remove(oldRes);
  }

  /**
   * Returns true if the args are indicate that the op is an immediate version.
   * If it returns false, it is a normal reg to reg version.
   *
   * <p>It will fail if the args are not 2, or are neither reg-reg nor reg-imm.
   */
  private boolean isBinaryImm(List<Node> args) {
    ensure(args.size() == 2, "Does not have two arguments: %s", args);
    if (args.get(0) instanceof TcgOpNode && args.get(1) instanceof ExpressionNode) {
      return true;
    } else if (args.get(0) instanceof TcgOpNode && args.get(1) instanceof TcgOpNode) {
      return false;
    } else {
      throw new ViamError("Invalid binary arguments")
          .addContext("args", args);
    }
  }

  private TcgOpNode asOp(Node node) {
    node.ensure(node instanceof TcgOpNode, "Not a TcgOpNode");
    return (TcgOpNode) node;
  }

}

