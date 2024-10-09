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
import vadl.iss.passes.tcgLowering.nodes.TcgAddNode;
import vadl.iss.passes.tcgLowering.nodes.TcgGetVar;
import vadl.iss.passes.tcgLowering.nodes.TcgMoveNode;
import vadl.iss.passes.tcgLowering.nodes.TcgOpNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.utils.Pair;
import vadl.viam.Instruction;
import vadl.viam.Specification;
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
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
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
        "ADD"
        , "ADDI"
    );

    viam.isa().get().ownInstructions()
        .stream()
        .filter(i -> supportedInstructions.contains(i.identifier.simpleName()))
        .forEach(TcgLoweringExecutor::runOn);

    return null;
  }
}


class TcgLoweringExecutor extends GraphProcessor {

  Instruction instruction;
  StartNode insnStartNode;
  InstrEndNode insnEndNode;
  DirectionalNode lastNode;

  Set<Pair<TcgV, WriteResourceNode>> destVars;
  Set<TcgV> tempVars;


  TcgLoweringExecutor(Instruction instruction) {
    this.instruction = instruction;
    this.insnStartNode = getSingleNode(instruction.behavior(), StartNode.class);
    this.lastNode = insnStartNode;
    this.insnEndNode = getSingleNode(instruction.behavior(), InstrEndNode.class);
    this.destVars = new HashSet<>();
    this.tempVars = new HashSet<>();
  }

  static void runOn(Instruction instruction) {
    new TcgLoweringExecutor(instruction).run();
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

  protected Node callProcess(Node toProcess) {
    if (toProcess instanceof BuiltInCall call) {
      return process(call);
    } else if (toProcess instanceof ReadRegFileNode regFileRead) {
      return process(regFileRead);
    } else if (toProcess instanceof WriteRegFileNode regFileWrite) {
      return process(regFileWrite);
    } else if (toProcess instanceof FieldRefNode) {
      return toProcess;
    } else if (toProcess instanceof ConstantNode) {
      // TODO: @jzottele Make sense of this
      return toProcess;
    } else if (toProcess instanceof SignExtendNode) {
      return toProcess;
    } else if (toProcess instanceof ZeroExtendNode) {
      return toProcess;
    } else if (toProcess instanceof TruncateNode) {
      return toProcess;
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

  private Node process(WriteRegFileNode toProcess) {
    var valRepl = getResultOf(toProcess.value(), TcgOpNode.class);

    // TODO: @jzottele Don't hardcode type!
    var width = TcgWidth.i64;
    var res = TcgV.gen(width);
    destVars.add(Pair.of(res, toProcess));
    return new TcgMoveNode(
        res, valRepl.res(), width);
  }

  private Node process(BuiltInCall call) {
    return produceOpNode(call);
  }


  private TcgOpNode produceOpNode(BuiltInCall call) {
    var args = call.inputs()
        .map(processedNodes::get)
        .map(n -> (TcgOpNode) n)
        .toList();

    if (call.builtIn() == BuiltInTable.ADD) {
      // TODO: @jzottele Don't hardcode width!
      var width = TcgWidth.i64;
      var res = TcgV.gen(width);
      // add result variable to tempVars
      tempVars.add(res);
      return new TcgAddNode(res, args.get(0).res(), args.get(1).res(), width);
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

}

