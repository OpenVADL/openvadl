package vadl.iss.passes.tcgLowering;

import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.tcgLowering.nodes.TcgAddNode;
import vadl.iss.passes.tcgLowering.nodes.TcgGetRegFile;
import vadl.iss.passes.tcgLowering.nodes.TcgMoveNode;
import vadl.iss.passes.tcgLowering.nodes.TcgOpNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
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
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.passes.GraphProcessor;

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

    var addInsn = viam.isa().get().ownInstructions()
        .stream().filter(i -> i.identifier.simpleName().equals("ADD"))
        .findFirst().get();

    new TcgLoweringExecutor(addInsn)
        .run();

    return null;
  }
}


class TcgLoweringExecutor extends GraphProcessor {

  Instruction instruction;
  InstrEndNode insnEndNode;
  DirectionalNode lastNode;


  TcgLoweringExecutor(Instruction instruction) {
    this.instruction = instruction;
    this.lastNode = getSingleNode(instruction.behavior(), StartNode.class);
    this.insnEndNode = getSingleNode(instruction.behavior(), InstrEndNode.class);
  }

  protected void run() {
    processGraph(instruction.behavior(),
        n -> n instanceof SideEffectNode
    );

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
      // TODO: @jozott Make sense of this
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

    // TODO: @jozott Don't hardcode type!
    var width = TcgWidth.i64;
    return new TcgGetRegFile(toProcess.registerFile(), (ExpressionNode) addressRepl,
        TcgV.gen(width), width);
  }

  private Node process(WriteRegFileNode toProcess) {
    var valRepl = getResultOf(toProcess.value(), TcgOpNode.class);

    // TODO: @jozott Don't hardcode type!
    var width = TcgWidth.i64;
    // TODO: @jozott Don't hardcode result var!
    var res = TcgV.gen(width);
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
      // TODO: @jozott Don't hardcode width!
      var width = TcgWidth.i64;
      var res = TcgV.gen(width);
      return new TcgAddNode(res, args.get(0).res(), args.get(1).res(), width);
    } else {
      throw new ViamGraphError("built-in call not yet supported by tcg lowering")
          .addContext(call)
          .addContext(instruction);
    }
  }


}
