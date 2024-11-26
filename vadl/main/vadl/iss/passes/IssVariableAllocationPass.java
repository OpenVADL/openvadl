package vadl.iss.passes;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.iss.DataFlowAnalysis;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.iss.passes.tcgLowering.nodes.TcgFreeTemp;
import vadl.iss.passes.tcgLowering.nodes.TcgGetVar;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Instruction;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.ControlNode;
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
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

/**
 * A pass that performs variable allocation for the Instruction Set Simulator (ISS).
 *
 * <p>This pass is responsible for allocating variables used in the behavior of instructions.
 * It traverses the ISA specification, processes each instruction,
 * and assigns TCG (Tiny Code Generator)
 * variables to the scheduled nodes in the dependency graph of each instruction's behavior.
 *
 * <p>The allocation includes:
 * <ul>
 *   <li>Performing liveness analysis to determine variable lifetimes.
 *   <li>Building an interference graph to model conflicts between variables.
 *   <li>Assigning registers to variables using graph coloring.
 *   <li>Assigning TcgV instances to the nodes in the dependency graph for code generation.
 * </ul>
 */
public class IssVariableAllocationPass extends Pass {

  /**
   * Represents the result of the variable allocation pass.
   *
   * <p>Contains a mapping from each instruction to a mapping of its dependency nodes to their assigned TcgV variables.
   */
  public record Result(
      Map<Instruction, Map<DependencyNode, TcgV>> varAssignments
  ) {
  }

  public IssVariableAllocationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Variable Allocation");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    // TODO: Don't hardcode
    var target_size = Tcg_32_64.i64;

    var result = new Result(new HashMap<>());

    // Process each instruction in the ISA
    viam.isa().ifPresent(isa -> isa.ownInstructions()
        .forEach(instr -> {
              // Allocate variables for the instruction's behavior
              var res = new IssVariableAllocator(instr.behavior(), target_size)
                  .allocateVariables();
              // Store the variable assignments for the instruction
              result.varAssignments.put(instr, res);
            }
        ));
    return result;
  }
}

/**
 * Performs variable allocation for a single instruction's behavior.
 *
 * <p>This class encapsulates the logic required to allocate variables for an instruction's behavior.
 * It handles the following tasks:
 * <ul>
 *   <li>Filling the variable table with variables from the dependency graph.
 *   <li>Performing liveness analysis to determine variable lifetimes.
 *   <li>Building the interference graph to model variable conflicts.
 *   <li>Allocating by coloring the interference graph.
 *   <li>Assigning TcgV variables to the nodes in the dependency graph.
 * </ul>
 */
class IssVariableAllocator {

  Graph graph;
  StartNode startNode;
  InstrEndNode endNode;
  Tcg_32_64 target_size;
  // the integer just represents some non-specific TCGv
  Map<Variable, Integer> allocationMap;
  LivenessAnalysis livenessAnalysis;
  // a table of virtual variables (e.g. each scheduled expression has a variable assigned)
  VariableTable variables;

  /**
   * Constructs an IssVariableAllocator for the given dependency graph and target size.
   *
   * @param graph       The dependency graph of the instruction's behavior.
   * @param target_size The target size for TCG variables.
   */
  public IssVariableAllocator(Graph graph, Tcg_32_64 target_size) {
    this.graph = graph;
    this.startNode = getSingleNode(graph, StartNode.class);
    this.endNode = getSingleNode(graph, InstrEndNode.class);
    this.target_size = target_size;
    this.variables = new VariableTable();
    this.livenessAnalysis = new LivenessAnalysis(variables);
    this.allocationMap = new HashMap<>();
  }

  /**
   * Allocates variables for the instruction's behavior.
   *
   * <p>This method orchestrates the variable allocation process, which includes:
   * <ul>
   *   <li>Filling the variable table with variables from the dependency graph.
   *   <li>Performing liveness analysis to determine variable lifetimes.
   *   <li>Building the interference graph to model variable conflicts.
   *   <li>Allocating by coloring the interference graph.
   *   <li>Assigning TcgV variables to the nodes in the dependency graph.
   * </ul>
   *
   * @return A mapping of dependency nodes to their assigned TcgV variables.
   */
  Map<DependencyNode, TcgV> allocateVariables() {

    // fill variable table with given graph
    variables.fillWith(graph);

    // perform liveness analysis
    livenessAnalysis.analyze(graph);

    var interferenceGraph = buildInterferenceGraph();
    allocateAllocations(interferenceGraph);

    return assignTcgV();
  }

  /**
   * Assigns TcgV variables to nodes in the dependency graph.
   *
   * <p>This method is responsible for assigning TcgV (Tiny Code Generator Variables) instances to all
   * nodes within the dependency graph that are scheduled (as TCG operations).
   * The assignment process is crucial for code generation, as it
   * determines how variables in the intermediate representation map to variables used in the
   * generated code.
   *
   * <p>The method processes different types of nodes in a specific order to ensure
   * that variables are assigned consistently, especially when dealing
   * with registers and register files.
   * The steps are as follows:
   * <ol>
   *   <li>Initialize two mappings:
   *       <ul>
   *         <li>{@code regToTcgV}: A map from register identifiers (integers) to TcgV instances.
   *             This map keeps track of which TcgV instance corresponds to each register.
   *         <li>{@code nodeToTcgVAssignments}: A map from {@code DependencyNode} to TcgV instances.
   *             This map records the TcgV assignment for each node in the dependency graph.
   *       </ul>
   *   <li>Assign TcgV variables to register file write nodes by calling
   *       {@link #assignTcgVToRegFileWrites(Map, Map)}.
   *   <li>Assign TcgV variables to register file read nodes by calling
   *       {@link #assignTcgVToRegFileReads(Map)}. This one is special as at reg allocation
   *       time, reads and writes share the same reg assignment. However they do not share
   *       the same TCGv, as register writes potentially receive a constant TCGv if the
   *       register file has a constraint (e.g. {@code X(0)} in RISC-V). Thus, this method
   *       will not put its created TCGv in the regToTcgV as temporaries are not allowed to
   *       write it.
   *   <li>Assign TcgV variables to register read and write nodes by calling
   *       {@link #assignTcgVToRegReadAndWrites(Map, Map)}.
   *   <li>Assign TcgV variables to the remaining scheduled nodes (temporaries) by calling
   *       {@link #assignTcgVToRestOfScheduledNodes(Map, Map)}.
   * </ol>
   *
   * <p>By the end of this method,
   * every relevant node in the dependency graph will have a corresponding
   * TcgV instance,
   * and the mappings {@code regToTcgV} and {@code nodeToTcgVAssignments} will be fully
   * populated.
   * This mapping is essential for subsequent code generation steps.
   *
   * @return a mapping of {@code DependencyNode} to their assigned TcgV variables.
   */
  private Map<DependencyNode, TcgV> assignTcgV() {
    // holds the mapping of a allocation identifier to the corresponding created TCGv.
    // this is first filled by register file writes, and register accesses as those
    // are already existing TCGv of the CPU.
    // when assigning the temporary variables, they first check the existing of
    // such an alloc to tcgV assignment, if not existing, they will create one.
    var allocationToTcgV = new HashMap<Integer, TcgV>();
    // holds the final assignments of dependency nodes to their TCGv variables.
    var nodeToTcgVAssignments = new HashMap<DependencyNode, TcgV>();

    assignTcgVToRegFileWrites(allocationToTcgV, nodeToTcgVAssignments);
    assignTcgVToRegFileReads(nodeToTcgVAssignments);
    assignTcgVToRegReadAndWrites(allocationToTcgV, nodeToTcgVAssignments);

    assignTcgVToRestOfScheduledNodes(allocationToTcgV, nodeToTcgVAssignments);

    return nodeToTcgVAssignments;
  }

  private void assignTcgVToRegFileWrites(Map<Integer, TcgV> regToTcgV,
                                         Map<DependencyNode, TcgV> assignments) {
    graph.getNodes(ScheduledNode.class)
        .filter(s -> s.node() instanceof WriteRegFileNode)
        .forEach(s -> {
          var variable = requireNonNull(variables.definedVar(s.node()));
          var alloc = requireNonNull(allocationMap.get(variable));
          var tcgV = createTcgV(variable, true);
          regToTcgV.put(alloc, tcgV);
          assignments.put(s.node(), tcgV);
        });
  }

  private void assignTcgVToRegFileReads(Map<DependencyNode, TcgV> assignments) {
    // This won't put it created TCGv in the regToTcgV, as other nodes should not write
    // to the read-only reg file of this reg file.
    // (as it is potentially a constant, like X(0)).
    graph.getNodes(ScheduledNode.class)
        .filter(s -> s.node() instanceof ReadRegFileNode)
        .forEach(s -> {
          var variable = requireNonNull(variables.definedVar(s.node()));
          var tcgV = createTcgV(variable, false);
          assignments.put(s.node(), tcgV);
        });
  }

  private void assignTcgVToRegReadAndWrites(Map<Integer, TcgV> regToTcgV,
                                            Map<DependencyNode, TcgV> assignments) {
    graph.getNodes(ScheduledNode.class)
        .filter(s -> s.node() instanceof ReadRegNode || s.node() instanceof WriteRegNode)
        .forEach(s -> {
          var variable = requireNonNull(variables.definedVar(s.node()));
          var alloc = requireNonNull(allocationMap.get(variable));
          var tcgV = createTcgV(variable, false);
          regToTcgV.put(alloc, tcgV);
          assignments.put(s.node(), tcgV);
        });
  }

  private void assignTcgVToRestOfScheduledNodes(Map<Integer, TcgV> regToTcgV,
                                                Map<DependencyNode, TcgV> assignments) {
    graph.getNodes(ScheduledNode.class)
        .forEach(s -> {
          if (assignments.containsKey(s.node())) {
            // we already did an assignment for that one
            return;
          }
          var variable = variables.definedVar(s.node());
          if (variable == null) {
            return;
          }
          var alloc = requireNonNull(allocationMap.get(variable));
          TcgV tcgV;
          if (regToTcgV.containsKey(alloc)) {
            tcgV = regToTcgV.get(alloc);
          } else {
            tcgV = createTcgV(variable, false);
            regToTcgV.put(alloc, tcgV);
          }
          assignments.put(s.node(), tcgV);
        });
  }

  private int tmpCnt = 0;

  private TcgV createTcgV(Variable var, boolean isRegFileWrite) {
    // generate TCGv
    var tcgV = switch (var.kind()) {
      case TMP -> TcgV.tmp("tmp_" + tmpCnt++, target_size);
      case REG -> TcgV.reg(var.name(), target_size, (Register) requireNonNull(var.regOrFile()));
      case REG_FILE -> {
        var postfix = isRegFileWrite ? "_dest" : "";
        yield TcgV.regFile(var.name() + postfix, target_size,
            (RegisterFile) requireNonNull(var.regOrFile()),
            requireNonNull(var.fileAddr()), isRegFileWrite);
      }
    };

    // add TcgV creation start of instruction
    startNode.addAfter(TcgGetVar.from(tcgV));

    if (tcgV.kind() == TcgV.Kind.TMP) {
      // if the tcgv is temporary, we have to free it after the instruction
      endNode.addBefore(new TcgFreeTemp(tcgV));
    }

    return tcgV;
  }

  /**
   * Builds the interference graph based on the liveness analysis results.
   *
   * <p>The interference graph models conflicts between variables,
   * where each node represents a variable.
   * An edge between two nodes
   * indicates that the variables interfere with each other (i.e., they are
   * live at the same time and cannot share the same register).
   *
   * @return The interference graph representing variable interferences.
   */
  private InterferenceGraph buildInterferenceGraph() {
    InterferenceGraph infGraph = new InterferenceGraph();

    for (ScheduledNode node : graph.getNodes(ScheduledNode.class).toList()) {
      Set<Variable> liveOut = livenessAnalysis.getOutValue(node);

      // For each variable defined at this node, it interferes with all variables live-out
      var def = variables.definedVar(node.node());
      if (def != null) {
        for (Variable live : liveOut) {
          infGraph.addEdge(def, live);
        }
      }
    }

    return infGraph;
  }

  /**
   * Allocates registers by coloring the interference graph.
   *
   * <p>This method uses a graph coloring algorithm to assign registers to variables such that no two
   * interfering variables share the same register.
   *
   * @param graph The interference graph to color.
   */
  private void allocateAllocations(InterferenceGraph graph) {
    // Use a simple heuristic graph coloring algorithm
    GraphColoring coloring = new GraphColoring(graph);
    coloring.colorGraph();

    // Store the register assignments
    allocationMap.putAll(coloring.getVariableColors());
  }


}

/**
 * Implements liveness analysis to determine live variables at each point in the behavior.
 *
 * <p>This analysis is a backward may analysis that computes, for each node, the set of variables
 * that are live-in and live-out. It helps in building the interference graph by identifying variables
 * that are live simultaneously.
 */
class LivenessAnalysis extends DataFlowAnalysis<Set<Variable>> {

  private final VariableTable variableTable;

  public LivenessAnalysis(VariableTable variables) {
    this.variableTable = variables;
  }

  @Override
  protected Set<Variable> initialFlow() {
    return new HashSet<>();
  }

  @Override
  protected Set<Variable> meet(Set<Set<Variable>> values) {
    // union of all incoming sets
    var result = new HashSet<Variable>();
    for (var value : values) {
      result.addAll(value);
    }
    return result;
  }

  @Override
  protected Set<Variable> transferFunction(ControlNode node, Set<Variable> input) {
    if (node instanceof InstrEndNode) {
      return initialEndFlow();
    }

    var output = new HashSet<>(input);
    // Apply kill: remove variables that are defined in this node
    output.removeAll(defineVariables(node));
    // Apply gen: add variables that are used in this node
    output.addAll(usedVariables(node));
    return output;
  }

  /**
   * Provides the initial live-out variables at the end of the instruction.
   *
   * <p>Assumes that all registers and register files are live after the instruction to ensure
   * that their values are preserved.
   *
   * @return The set of variables assumed to be live at the end of the instruction.
   */
  private Set<Variable> initialEndFlow() {
    // we have to assume that all registers and register files are used
    // after this instruciton. Thus we have to set them used at the end of the instruction.
    var readResourceVars = new HashSet<Variable>();
    readResourceVars.addAll(variableTable.getRegVars());
    readResourceVars.addAll(variableTable.getRegFileVars());
    return readResourceVars;
  }

  @Override
  protected boolean isForward() {
    // backward analysis
    return false;
  }

  @Override
  protected boolean isMayAnalysis() {
    // may analysis
    return true;
  }

  /**
   * Retrieves the set of variables defined at the given node.
   *
   * @param node The control node to analyze.
   * @return The set of variables defined at the node.
   */
  private Set<Variable> defineVariables(ControlNode node) {
    if (!(node instanceof ScheduledNode scheduledNode)) {
      return Set.of();
    }
    var dep = scheduledNode.node();
    var definedVar = variableTable.definedVar(dep);
    return definedVar == null ? Set.of() : Set.of(definedVar);
  }

  /**
   * Retrieves the set of variables used at the given node.
   *
   * @param node The control node to analyze.
   * @return The set of variables used at the node.
   */
  private Set<Variable> usedVariables(ControlNode node) {
    if ((node instanceof ScheduledNode scheduledNode)) {
      // uses the dependency's inputs
      var dep = scheduledNode.node();
      var used = new HashSet<Variable>(variableTable.usedVars(dep));
      if (dep instanceof ReadRegFileNode readRegFileNode) {
        // if we read from a register file we must ensure that we "lock" all
        // variables of the register file, as we can't be sure if other writes
        // are potentially writing to the same index
        refreshRegFileUsage(used, readRegFileNode.registerFile());
      }
      return used;
    } else {
      // if not a scheduled node, we just use the variables defined by
      // any inputs
      return node.inputs()
          .filter(DependencyNode.class::isInstance)
          .map(i -> variableTable.definedVar((DependencyNode) i))
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
    }
  }

  // TODO: Write test for such a scenario

  /**
   * Adds all variables that represent some index of a given register file to the used variables set.
   *
   * <p>This method is necessary because when reading from a register file, we cannot be certain
   * whether other writes are potentially writing to the same index. By adding all variables of the
   * register file to the used set, we prevent assigning them the same TcgV register,
   * avoiding conflicts.
   *
   * @param usedVars     The set of used variables to add to.
   * @param registerFile The register file being read, which must be refreshed.
   */
  private void refreshRegFileUsage(Set<Variable> usedVars, RegisterFile registerFile) {
    var regFileVars = variableTable.getRegFileVars().stream()
        .filter(v -> v.regOrFile() == registerFile)
        .collect(Collectors.toSet());

    usedVars.addAll(regFileVars);
  }
}

/**
 * Represents an interference graph where nodes are variables and edges represent interference between variables.
 *
 * <p>An interference graph models conflicts between variables where each node is a variable, and an edge indicates
 * that two variables interfere with each other (i.e., they are live at the same time and cannot share a register).
 */
class InterferenceGraph {

  private Map<Variable, Set<Variable>> adjacencyList;

  /**
   * Constructs an empty interference graph.
   */
  public InterferenceGraph() {
    this.adjacencyList = new HashMap<>();
  }

  /**
   * Adds an edge between two variables, indicating they interfere with each other.
   *
   * @param v1 The first variable.
   * @param v2 The second variable.
   */
  public void addEdge(Variable v1, Variable v2) {
    adjacencyList.computeIfAbsent(v1, k -> new HashSet<>()).add(v2);
    adjacencyList.computeIfAbsent(v2, k -> new HashSet<>()).add(v1);
  }

  /**
   * Retrieves the set of variables that interfere with the given variable.
   *
   * @param variable The variable to query.
   * @return The set of interfering variables.
   */
  public Set<Variable> getInterferences(Variable variable) {
    return adjacencyList.getOrDefault(variable, new HashSet<>());
  }

  /**
   * Retrieves all variables in the interference graph.
   *
   * @return A set of all variables in the graph.
   */
  public Set<Variable> getVariables() {
    return adjacencyList.keySet();
  }
}

/**
 * Performs graph coloring on an interference graph to allocate registers.
 */
class GraphColoring {

  private InterferenceGraph graph;
  private Map<Variable, Integer> variableColors;
  private int numRegisters;

  /**
   * Constructs a GraphColoring instance for the given interference graph.
   *
   * @param graph The interference graph to color.
   */
  public GraphColoring(InterferenceGraph graph) {
    this.graph = graph;
    this.variableColors = new HashMap<>();
    this.numRegisters = 0;
  }

  /**
   * Colors the graph using a simple heuristic algorithm.
   */
  public void colorGraph() {
    Set<Variable> uncoloredVariables = new HashSet<>(graph.getVariables());

    while (!uncoloredVariables.isEmpty()) {
      Variable variable = selectVariable(uncoloredVariables);
      assignColor(variable);
      uncoloredVariables.remove(variable);
    }
  }

  /**
   * Selects the next variable to color.
   * <p>
   * This implementation selects an arbitrary variable.
   * It can be improved by using heuristics like choosing the variable with the highest degree.
   *
   * @param uncoloredVariables The set of uncolored variables.
   * @return The selected variable.
   */
  private Variable selectVariable(Set<Variable> uncoloredVariables) {
    return uncoloredVariables.iterator().next();
  }

  /**
   * Assigns a color (register number) to the given variable.
   *
   * @param variable The variable to color.
   */
  private void assignColor(Variable variable) {
    Set<Integer> usedColors = new HashSet<>();

    // Collect colors of interfering variables
    for (Variable neighbor : graph.getInterferences(variable)) {
      Integer color = variableColors.get(neighbor);
      if (color != null) {
        usedColors.add(color);
      }
    }

    // Find the smallest color not used by neighbors
    int color = 0;
    while (usedColors.contains(color)) {
      color++;
    }

    variableColors.put(variable, color);

    // Update the number of registers used
    if (color + 1 > numRegisters) {
      numRegisters = color + 1;
    }
  }

  /**
   * Retrieves the mapping of variables to their assigned colors (register numbers).
   *
   * @return The variable to color mapping.
   */
  public Map<Variable, Integer> getVariableColors() {
    return variableColors;
  }

  /**
   * Retrieves the total number of registers used.
   *
   * @return The number of registers used.
   */
  public int getNumRegisters() {
    return numRegisters;
  }
}

/**
 * Represents a variable in the intermediate representation of the instruction's behavior.
 *
 * <p>A variable can be of different kinds:
 * <ul>
 *   <li>Temporary variables (TMP)
 *   <li>Registers (REG)
 *   <li>Register files (REG_FILE)
 * </ul>
 */
record Variable(
    Kind kind,
    @Nullable Resource regOrFile,
    @Nullable ExpressionNode fileAddr,
    String name
) {
  enum Kind {
    TMP,
    REG,
    REG_FILE
  }

  @Override
  public String toString() {
    return name;
  }
}

/**
 * Manages the variables used in the instruction's behavior.
 *
 * <p>The variable table keeps track of all variables, including temporaries, registers, and register files.
 * It provides methods to retrieve or create variables associated with nodes in the dependency graph.
 */
class VariableTable {
  /**
   * Mapping from register files and their indices to variables.
   */
  Map<Pair<RegisterFile, ExpressionNode>, Variable> regFileVars = new HashMap<>();

  /**
   * Mapping from registers to variables.
   */
  Map<Register, Variable> regVars = new HashMap<>();

  /**
   * Mapping from expression nodes (temporaries) to variables.
   */
  Map<ExpressionNode, Variable> tmpVars = new HashMap<>();

  /**
   * Fills the variable table with variables from the dependency graph.
   *
   * <p>This method processes the graph and ensures that all variables used or defined in the graph
   * are present in the variable table.
   *
   * @param graph The dependency graph to process.
   */
  public void fillWith(Graph graph) {
    graph.getNodes(Set.of(ScheduledNode.class, InstrExitNode.class))
        .forEach(s ->
            s.inputs().forEach(dep -> {
                  definedVar((DependencyNode) dep);
                  usedVars((DependencyNode) dep);
                }
            ));
  }

  /**
   * Retrieves all register variables.
   *
   * @return A set of all register variables.
   */
  public Set<Variable> getRegVars() {
    return new HashSet<>(regVars.values());
  }

  /**
   * Retrieves all register file variables.
   *
   * @return A set of all register file variables.
   */
  public Set<Variable> getRegFileVars() {
    return new HashSet<>(regFileVars.values());
  }

  /**
   * Retrieves all temporary variables.
   *
   * @return A set of all temporary variables.
   */
  public Set<Variable> getTempVars() {
    return new HashSet<>(tmpVars.values());
  }

  /**
   * Retrieves or creates the variable defined by the given dependency node.
   *
   * <p>If the node defines a variable, this method returns the corresponding Variable instance.
   * If the variable does not exist in the table, it is created and added.
   *
   * @param dep The dependency node.
   * @return The variable defined by the node, or null if the node does not define a variable.
   */
  public @Nullable Variable definedVar(DependencyNode dep) {
    if (dep.usages().noneMatch(ScheduledNode.class::isInstance)) {
      // only scheduled nodes have some variable defined
      return null;
    }

    if (dep instanceof ReadRegNode regRead) {
      // even though we do not really define it, we declare it here.
      // as we know that the reg is not used before, we can do this safely.
      return getOrCreateRegVar(regRead.register());
    } else if (dep instanceof ReadRegFileNode regFileRead) {
      // same as for ReadRegNode
      return getOrCreateRegFileVar(regFileRead.registerFile(), regFileRead.address());
    } else if (dep instanceof ExpressionNode expr) {
      return getOrCreateTmpVar(expr);
    } else if (dep instanceof WriteRegNode regWrite) {
      return getOrCreateRegVar(regWrite.register());
    } else if (dep instanceof WriteRegFileNode regFileWrite) {
      return getOrCreateRegFileVar(regFileWrite.registerFile(), regFileWrite.address());
    } else if (dep instanceof WriteMemNode) {
      // mems are no var/registers
      return null;
    } else {
      throw new ViamGraphError("Unexpected scheduled dependency")
          .addContext(dep);
    }
  }

  /**
   * Retrieves the set of variables used by the given dependency node.
   *
   * @param dep The dependency node.
   * @return The set of variables used by the node.
   */
  public Set<Variable> usedVars(DependencyNode dep) {

    if (dep instanceof ReadRegNode regRead) {
      return Set.of(getOrCreateRegVar(regRead.register()));
    } else if (dep instanceof ReadRegFileNode regFileRead) {
      return Set.of(getOrCreateRegFileVar(regFileRead.registerFile(), regFileRead.address()));
    } else if (dep instanceof ExpressionNode expr) {
      // find all free variables by searching for inputs that are scheduled
      var freeVariablesUsed = expr.inputs().filter(e -> e
          .usages().anyMatch(ScheduledNode.class::isInstance)
      ).map(i -> requireNonNull(definedVar((ExpressionNode) i)));

      return freeVariablesUsed.collect(Collectors.toSet());
    } else if (dep instanceof WriteResourceNode resWrite) {

      var valueToWrite = resWrite.value();
      var usedVars = new HashSet<Variable>();
      if (valueToWrite.usages().anyMatch(ScheduledNode.class::isInstance)) {
        // if value to write is schedule, the node reads it
        usedVars.add(requireNonNull(definedVar(valueToWrite)));
      }

      if (resWrite.hasAddress()) {
        if (resWrite.address().usages().anyMatch(ScheduledNode.class::isInstance)) {
          // if value to write is schedule, the node reads it
          usedVars.add(requireNonNull(definedVar(resWrite.address())));
        }
      }
      return usedVars;
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
  private Variable getOrCreateTmpVar(ExpressionNode expr) {
    return tmpVars.computeIfAbsent(expr, n ->
        new Variable(
            Variable.Kind.TMP,
            null,
            null,
            "tmp_n" + expr.id
        )
    );
  }

  /**
   * Retrieves or creates a variable for the given register.
   *
   * @param reg The register.
   * @return The variable corresponding to the register.
   */
  private Variable getOrCreateRegVar(Register reg) {
    return regVars.computeIfAbsent(reg, n ->
        new Variable(
            Variable.Kind.REG,
            reg,
            null,
            "reg_" + reg.simpleName().toLowerCase()
        )
    );
  }

  /**
   * Retrieves or creates a variable for the given register file and index.
   *
   * @param regFile The register file.
   * @param index   The index expression node.
   * @return The variable corresponding to the register file at the given index.
   */
  private Variable getOrCreateRegFileVar(RegisterFile regFile, ExpressionNode index) {
    var key = Pair.of(regFile, index);
    return regFileVars.computeIfAbsent(key, n ->
        new Variable(
            Variable.Kind.REG_FILE,
            regFile,
            index,
            "regFile_" + regFile.simpleName().toLowerCase() + "_n" + index.id
        )
    );
  }


}