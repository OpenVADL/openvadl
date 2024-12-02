package vadl.iss.passes;

import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.iss.passes.tcgLowering.nodes.TcgGetVar;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Triple;
import vadl.viam.Instruction;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;

/**
 * IssSsaVarAssignment is a specific type of Pass that performs a temporary variable
 * assignment during the ISS generation process.
 *
 * <p>It assigns a TCGv variable to every expression that is scheduled for TCG generation.
 * Those assignments are not the final ones, but just an SSA assignment.
 * The finalized assignment is done by the {@link IssTcgVAllocationPass}.
 * The pass also adds the register variable getters ({@link TcgGetVar}), as those
 * are required by the later allocation pass and will not be removed at a later point.
 */
public class IssVarSsaAssignment extends Pass<IssConfiguration> {

  /**
   * Represents the result of a temporary variable assignment during the ISS generation process.
   * This record holds a mapping of instructions to their corresponding variable assignments.
   *
   * <p>The mapping associates each instruction with another mapping that links dependency nodes
   * to TcgVRefNodes. This structure is used in the context of TCG (Tiny Code Generator) variable
   * allocation, where each instruction's expressions are assigned temporary variables.
   *
   * <p>The assignments documented here are in SSA (Single Static Assignment) form,
   * meaning that each variable is assigned exactly once.
   * The final variable assignment is performed in a separate pass.
   *
   * @param varAssignments A mapping from instructions to their variable assignments, where each
   *                       instruction is associated with a map from dependency
   *                       nodes to TcgVRefNodes.
   */
  public record Result(
      Map<Instruction, Map<DependencyNode, TcgVRefNode>> varAssignments
  ) {
  }

  public IssVarSsaAssignment(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Temp (SSA) Var Assignment");
  }

  @Override
  public Result execute(PassResults passResults, Specification viam)
      throws IOException {

    var targetSize = configuration().targetSize();

    var result = new Result(new HashMap<>());

    // Process each instruction in the ISA
    viam.isa().ifPresent(isa -> isa.ownInstructions()
        .forEach(instr -> {
              // Allocate variables for the instruction's behavior
              var res = new IssTempVarAssigner(targetSize, instr.behavior()).run();
              // Store the variable assignments for the instruction
              result.varAssignments.put(instr, res);
            }
        ));

    return result;
  }
}

class IssTempVarAssigner {

  Tcg_32_64 targetSize;
  // key to tcgV cache to avoid duplicated TCGv
  Map<Object, TcgVRefNode> tcgVCache = new HashMap<>();
  Map<DependencyNode, TcgVRefNode> ssaAssignments = new HashMap<>();
  Graph graph;

  IssTempVarAssigner(Tcg_32_64 targetSize, Graph graph) {
    this.targetSize = targetSize;
    this.graph = graph;
  }

  Map<DependencyNode, TcgVRefNode> run() {
    graph.getNodes(Set.of(ScheduledNode.class, InstrEndNode.class))
        .forEach(s -> s.inputs().forEach(dep -> assignDest((DependencyNode) dep)));

    insertRegisterTcgVGetters();
    return ssaAssignments;
  }

  private void insertRegisterTcgVGetters() {
    var startNode = getSingleNode(graph, StartNode.class);

    for (var ref : ssaAssignments.values()) {
      switch (ref.var().kind()) {
        case REG, REG_FILE -> startNode.addAfter(TcgGetVar.from(ref));
        default -> {
        }
      }
    }
  }

  private void assignDest(DependencyNode dep) {
    if (dep.usages().noneMatch(ScheduledNode.class::isInstance)) {
      // only scheduled nodes have some variable defined
      return;
    }

    var dest = destOf(dep);
    if (dest == null) {
      return;
    }
    ssaAssignments.put(dep, dest);
  }

  private @Nullable TcgVRefNode destOf(DependencyNode dep) {
    if (dep instanceof ReadRegNode regRead) {
      // even though we do not really define it, we declare it here.
      // as we know that the reg is not used before, we can do this safely.
      return getOrCreateRegVar(regRead.register());
    } else if (dep instanceof ReadRegFileNode regFileRead) {
      // same as for ReadRegNode
      return getOrCreateRegFileVar(regFileRead.registerFile(), regFileRead.address(), false);
    } else if (dep instanceof ExpressionNode expr) {
      return getOrCreateTmpVar(expr);
    } else if (dep instanceof WriteRegNode regWrite) {
      return getOrCreateRegVar(regWrite.register());
    } else if (dep instanceof WriteRegFileNode regFileWrite) {
      return getOrCreateRegFileVar(regFileWrite.registerFile(), regFileWrite.address(), true);
    } else if (dep instanceof WriteMemNode) {
      // mems are no var/registers
      return null;
    } else {
      throw new ViamGraphError("Unexpected scheduled dependency")
          .addContext(dep);
    }
  }

  /**
   * Retrieves or creates a temporary variable for the given expression node.
   *
   * @param expr The expression node.
   * @return The variable corresponding to the expression node.
   */
  private TcgVRefNode getOrCreateTmpVar(ExpressionNode expr) {
    return tcgVCache.computeIfAbsent(expr, k -> toNode(TcgV.tmp(
        "tmp_" + TcgPassUtils.exprVarName(expr),
        targetSize
    )));
  }

  /**
   * Retrieves or creates a variable for the given register.
   *
   * @param reg The register.
   * @return The variable corresponding to the register.
   */
  private TcgVRefNode getOrCreateRegVar(Register reg) {
    return tcgVCache.computeIfAbsent(reg, k -> toNode(TcgV.reg(
        "reg_" + reg.simpleName().toLowerCase(),
        targetSize,
        reg
    )));
  }

  /**
   * Retrieves or creates a variable for the given register file and index.
   *
   * @param regFile The register file.
   * @param index   The index expression node.
   * @return The variable corresponding to the register file at the given index.
   */
  private TcgVRefNode getOrCreateRegFileVar(RegisterFile regFile, ExpressionNode index,
                                            boolean isDest) {
    var key = Triple.of(regFile, index, isDest);
    var dest = isDest ? "_dest" : "";
    return tcgVCache.computeIfAbsent(key, k -> toNode(TcgV.regFile(
        "regfile_" + regFile.simpleName().toLowerCase() + "_" + TcgPassUtils.exprVarName(index)
            + dest,
        targetSize,
        regFile,
        index,
        isDest
    )));
  }

  private TcgVRefNode toNode(TcgV tcgV) {
    return graph.addWithInputs(new TcgVRefNode(tcgV));
  }

}
