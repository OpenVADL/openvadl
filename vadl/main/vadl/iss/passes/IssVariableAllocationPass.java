package vadl.iss.passes;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.configuration.GeneralConfiguration;
import vadl.iss.DataFlowAnalysis;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Instruction;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.WriteResourceNode;

public class IssVariableAllocationPass extends Pass {

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

    viam.isa().ifPresent(isa -> isa.ownInstructions()
        .forEach(instr -> {
              var res = new IssVariableAllocator(instr.behavior(), target_size)
                  .allocateVariables();
              result.varAssignments.put(instr, res);
            }
        ));
    return result;
  }
}

class IssVariableAllocator {

  Graph graph;
  Tcg_32_64 target_size;
  Map<Variable, Integer> registerAssignment;
  LivenessAnalysis livenessAnalysis;
  VariableTable variables;

  public IssVariableAllocator(Graph graph, Tcg_32_64 target_size) {
    this.graph = graph;
    this.target_size = target_size;
    this.variables = new VariableTable();
    this.livenessAnalysis = new LivenessAnalysis(variables);
    this.registerAssignment = new HashMap<>();
  }

  Map<DependencyNode, TcgV> allocateVariables() {

    // fill variable table with given graph
    variables.fillWith(graph);

    // perform liveness analysis
    livenessAnalysis.analyze(graph);

    var interferenceGraph = buildInterferenceGraph();
    allocateRegisters(interferenceGraph);

    return assignTcgV();
  }


  private Map<DependencyNode, TcgV> assignTcgV() {

    var regToTcgV = new HashMap<Integer, TcgV>();

    var partitionedAss = registerAssignment.entrySet().stream()
        .collect(Collectors.partitioningBy(e -> e.getKey().kind() == Variable.Kind.TMP));

    for (var regAss : partitionedAss.getOrDefault(false, List.of())) {
      // for each reg assignment of some register, we want to use it as
      // the chosen TCGv
      var var = regAss.getKey();
      var tcgV = TcgV.of(var.name(), target_size);
      regToTcgV.put(regAss.getValue(), tcgV);
    }

    var tmpCnt = 0;
    for (var tmpAss : partitionedAss.getOrDefault(true, List.of())) {
      // iterate through all temporary register assignments.
      var reg = tmpAss.getValue();

      if (!regToTcgV.containsKey(reg)) {
        // if there is no tcgV assignment yet, we create one.
        var newTcgV = TcgV.of("tmp_" + tmpCnt++, target_size);
        regToTcgV.put(reg, newTcgV);
      }
    }

    // final assignments
    var result = new HashMap<DependencyNode, TcgV>();

    // now we have all reg to tcgv assignments.
    // lets assign each a tcgV to each scheduled dependency that declares a variable
    graph.getNodes(ScheduledNode.class).forEach(s -> {
      var dep = s.node();
      // get declared variable
      var var = variables.definedVar(dep);
      if (var != null) {
        // get assigned register value
        var reg = registerAssignment.get(var);
        TcgV tcgV;
        // get mapped tcgV of register value
        tcgV = requireNonNull(regToTcgV.get(reg));
        result.put(dep, tcgV);
      }
    });

    return result;
  }

  /**
   * Builds the interference graph based on the liveness analysis results.
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
   * @param graph The interference graph to color.
   */
  private void allocateRegisters(InterferenceGraph graph) {
    // Use a simple heuristic graph coloring algorithm
    GraphColoring coloring = new GraphColoring(graph);
    coloring.colorGraph();

    // Store the register assignments
    registerAssignment.putAll(coloring.getVariableColors());
  }


}

/**
 * Implements liveness analysis to determine live variables at each point in the behavior.
 * This analysis is a backward may analysis that computes, for each node, the set of variables
 * that are live-in and live-out.
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

  private Set<Variable> defineVariables(ControlNode node) {
    if (!(node instanceof ScheduledNode scheduledNode)) {
      return Set.of();
    }
    var dep = scheduledNode.node();
    var definedVar = variableTable.definedVar(dep);
    return definedVar == null ? Set.of() : Set.of(definedVar);
  }

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
   * This method adds all variables that represent some index of a given registers file
   * to the given usedVars set.
   * We need this, as we can't be sure if some write before some read actually
   * writes to the same register.
   * So we are not allowed to assign them the same TCGv. By setting all
   * register file variables as used, we "lock" them, so they won't be assigned
   * to the same TCGv register.
   *
   * @param usedVars     used vars to add the refreshed reg file vars
   * @param registerFile that is read and must be refreshed.
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

class VariableTable {
  private static final Logger log = LoggerFactory.getLogger(VariableTable.class);
  Map<Pair<RegisterFile, ExpressionNode>, Variable> regFileVars = new HashMap<>();
  Map<Register, Variable> regVars = new HashMap<>();
  Map<ExpressionNode, Variable> tmpVars = new HashMap<>();


  public void fillWith(Graph graph) {
    graph.getNodes(ScheduledNode.class)
        .forEach(s -> {
          var dep = s.node();
          definedVar(dep);
          usedVars(dep);
        });
  }

  public Set<Variable> getRegVars() {
    return new HashSet<>(regVars.values());
  }

  public Set<Variable> getRegFileVars() {
    return new HashSet<>(regFileVars.values());
  }

  public Set<Variable> getTempVars() {
    return new HashSet<>(tmpVars.values());
  }


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