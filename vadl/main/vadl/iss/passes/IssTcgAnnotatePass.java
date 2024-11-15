package vadl.iss.passes;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Counter;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.GraphProcessor;

/**
 * A pass that determines what nodes are turned into TCG nodes.
 * This is the case for nodes that are based on some memory/resource access
 * (in their dependency tree).
 */
public class IssTcgAnnotatePass extends Pass {

  public IssTcgAnnotatePass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS TCG Annotate Pass");
  }

  @Override
  public @Nonnull Result execute(PassResults passResults, Specification viam)
      throws IOException {
    var isa = viam.isa().get();
    var tcgNodes = isa.ownInstructions().stream()
        .flatMap(instr -> TcgAnnotator.runOn(instr.behavior(), requireNonNull(isa.pc())))
        .collect(Collectors.toSet());
    return new Result(tcgNodes);
  }

  /**
   * Represents the result of the ISS TCG annotate pass.
   * This result contains the set of nodes that are determined to be TCG nodes.
   */
  public record Result(
      Set<Node> tcgNodes
  ) {
  }
}

class TcgAnnotator extends GraphProcessor<Boolean> {

  Counter pc;

  public TcgAnnotator(Counter instruction) {
    this.pc = instruction;
  }

  public static Stream<Node> runOn(Graph graph, Counter pc) {
    var annotator = new TcgAnnotator(pc);
    annotator.processGraph(graph, (n) -> n.usageCount() == 0);
    return annotator.processedNodes.entrySet().stream()
        .filter(Map.Entry::getValue) // only annotated fields
        .map(Map.Entry::getKey) // map to node
        .distinct();
  }

  @Override
  protected Boolean processUnprocessedNode(Node toProcess) {
    // look at inputs first
    toProcess.visitInputs(this);

    if (toProcess instanceof ReadRegFileNode readRegFileNode) {
      // check that the address is not a tcg resource.
      // the address must be determined at "compile" time
      var addressRes = getResultOf(readRegFileNode.address(), Boolean.class);
      toProcess.ensure(!addressRes,
          "node's address is not allowed to be tcg time but compile time annotated: %s",
          readRegFileNode.address());
    }

    if (toProcess instanceof WriteRegFileNode writeResourceNode) {
      var addressRes = getResultOf(writeResourceNode.address(), Boolean.class);
      writeResourceNode.ensure(!addressRes,
          "node's address is not allowed to be tcg time but compile time annotated: %s",
          writeResourceNode.address()
      );
    }
    if (toProcess instanceof ReadResourceNode readResourceNode) {
      if (readResourceNode.resourceDefinition() == pc.registerResource()) {
        // pc registers are not lowered to tcg as they can be access directly using
        // ctx->base.pc_next
        return false;
      }
      return true;
    } else if (toProcess instanceof WriteResourceNode) {
      return true;
    } else {
      // In general a node is tcg if one of its inputs is tcg
      return toProcess.inputs()
          .anyMatch(n -> getResultOf(n, Boolean.class));
    }
  }
}
