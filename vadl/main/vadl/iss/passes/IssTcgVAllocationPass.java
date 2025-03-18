// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.iss.passes;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.DataFlowAnalysis;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgCtx;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.nodes.TcgGetVar;
import vadl.iss.passes.tcgLowering.nodes.TcgNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.StartNode;

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
 *   <li>Updating the TCGv assignments on TcgNodes based on the allocation.
 * </ul>
 *
 * <p>From paper: To minimize the number of temporary TCG variables,
 * a variable allocation pass is applied to the TCG CFG.
 * First, the live ranges of previously created temporary variables are determined.
 * Then, graph coloring is used to compute an optimized TCG variable assignment.
 * The primary goal is to maximize the reuse of written registers,
 * reducing unnecessary temporary allocations.</p>
 */
public class IssTcgVAllocationPass extends AbstractIssPass {

  public IssTcgVAllocationPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Variable Allocation");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var skipOptimization = configuration().isSkip(IssConfiguration.IssOptsToSkip.OPT_VAR_ALLOC);

    // Process each instruction in the ISA
    viam.isa().ifPresent(isa -> isa.ownInstructions()
        .forEach(instr ->
            // Allocate variables for the instruction's behavior
            new IssVariableAllocator(instr.behavior(),
                instr.expectExtension(TcgCtx.class).assignment()
            )
                .assignFinalVariables(!skipOptimization)
        ));
    return null;
  }
}

/**
 * Performs variable allocation for a single instruction's behavior.
 *
 * <p>This class encapsulates the logic required to allocate variables
 * for an instruction's behavior.
 * It handles the following tasks:
 * <ul>
 *   <li>Performing liveness analysis to determine variable lifetimes.
 *   <li>Building the interference graph to model variable conflicts.
 *   <li>Allocating by coloring the interference graph.
 *   <li>Updating the TCGv assignments on TcgNodes based on the allocation.
 * </ul>
 */
class IssVariableAllocator {

  private final Graph graph;
  private final StartNode startNode;
  // the integer just represents some non-specific TCGv
  private final Map<TcgVRefNode, Integer> allocationMap;
  private final LivenessAnalysis livenessAnalysis;

  /**
   * Constructs an IssVariableAllocator for the given dependency graph and target size.
   *
   * @param graph The dependency graph of the instruction's behavior.
   */
  public IssVariableAllocator(Graph graph,
                              TcgCtx.Assignment ssaAssignments) {
    this.graph = graph;
    this.startNode = getSingleNode(graph, StartNode.class);
    this.livenessAnalysis = new LivenessAnalysis(ssaAssignments);
    this.allocationMap = new HashMap<>();
  }

  /**
   * Allocates variables for the instruction's behavior.
   *
   * <p>This method orchestrates the variable allocation process, which includes:
   * <ul>
   *   <li>Performing liveness analysis to determine variable lifetimes.
   *   <li>Building the interference graph to model variable conflicts.
   *   <li>Allocating by coloring the interference graph.
   *   <li>Updating the TCGv assignments on TcgNodes based on the allocation.
   * </ul>
   */
  void assignFinalVariables(boolean optimizeAllocation) {

    if (!optimizeAllocation) {
      // if we don't optimize allocation, we just init all variables
      initializeFixedVariables(true);
      return;
    }

    // this schedules non temporary variables, e.i.:
    // reg, regfile, const
    initializeFixedVariables(false);

    // perform liveness analysis
    livenessAnalysis.analyze(graph);

    var interferenceGraph = buildInterferenceGraph();
    allocateColoring(interferenceGraph);

    updateFinalAssignments();
  }

  /**
   * Initializes all variable at start of instruction.
   * This is required to build a valid inference graph.
   *
   * @param initTmps indicates if temporary variables should be initialized.
   */
  private void initializeFixedVariables(boolean initTmps) {
    graph.getNodes(TcgVRefNode.class)
        .filter(v -> initTmps || v.var().kind() != TcgV.Kind.TMP)
        .sorted(Comparator.comparing(v -> v.var().kind()))
        .forEach(v -> startNode.addAfter(TcgGetVar.from(v)));
  }

  private void updateFinalAssignments() {
    var coloringToTcgV = getColoringToTcgV();

    var usedTcgVs = coloringToTcgV.values();
    var varsToReplace = allocationMap.keySet().stream()
        .filter(v -> !usedTcgVs.contains(v))
        .collect(Collectors.toSet());
    for (var var : varsToReplace) {
      var color = allocationMap.get(var);
      var replacement = coloringToTcgV.get(color);
      var.replaceAndDelete(requireNonNull(replacement));
    }

    insertTmpTcgVGetters(new HashSet<>(coloringToTcgV.values()));
  }

  private void insertTmpTcgVGetters(Set<TcgVRefNode> tmps) {
    for (var ref : tmps) {
      if (requireNonNull(ref.var().kind()) == TcgV.Kind.TMP) {
        startNode.addAfter(TcgGetVar.from(ref));
      }
    }
  }

  private Map<Integer, TcgVRefNode> getColoringToTcgV() {
    var colorAssignments = new HashMap<Integer, TcgVRefNode>();
    allocationMap.entrySet().stream()
        // first assign all non-temps with their original variable
        .filter(e -> e.getKey().var().kind() != TcgV.Kind.TMP)
        .forEach(e -> {
          var var = e.getKey();
          var color = e.getValue();
          if (colorAssignments.containsKey(color)) {
            throw new ViamError("Two non-sharable variables are sharing the same color %d", color)
                .addContext("var1", colorAssignments.get(color))
                .addContext("var2", var);
          }
          colorAssignments.put(color, var);
        });

    allocationMap.entrySet().stream()
        // now assign the rest of the colorings
        .filter(e -> e.getKey().var().kind() == TcgV.Kind.TMP)
        .forEach(e -> {
          var var = e.getKey();
          var color = e.getValue();
          if (!colorAssignments.containsKey(color)) {
            colorAssignments.put(color, var);
          }
        });


    return colorAssignments;
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

    for (var node : graph.getNodes(TcgNode.class).toList()) {
      Set<TcgVRefNode> liveOut = livenessAnalysis.getOutValue(node);

      // For each variable defined at this node, it interferes with all variables live-out
      var defs = node.definedVars();
      for (var def : defs) {
        for (var live : liveOut) {
          infGraph.addEdge(def, live);
        }
      }
    }

    return infGraph;
  }

  /**
   * Allocates registers by coloring the interference graph.
   *
   * <p>This method uses a graph coloring algorithm to assign registers to
   * variables such that no two interfering variables share the same register.
   *
   * @param graph The interference graph to color.
   */
  private void allocateColoring(InterferenceGraph graph) {
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
 * that are live-in and live-out.
 * It helps in building the interference graph by identifying variables
 * that are live simultaneously.
 */
// TODO: Check if the tempassignmets is enough here (look at IssMulhNode)
class LivenessAnalysis extends DataFlowAnalysis<Set<TcgVRefNode>> {

  private final TcgCtx.Assignment tempAssignments;
  private final Map<RegisterFile, List<TcgVRefNode>> registerFileVars;

  public LivenessAnalysis(TcgCtx.Assignment tempAssignments) {
    this.tempAssignments = tempAssignments;
    // collect all registerFileVariables to their respective registerFile
    this.registerFileVars = tempAssignments.tcgVariables()
        .filter(v -> v.var().kind() == TcgV.Kind.REG_FILE)
        .collect(Collectors.groupingBy(
            v -> (RegisterFile) requireNonNull(v.var().registerOrFile())));
  }

  @Override
  protected Set<TcgVRefNode> initialFlow() {
    return new HashSet<>();
  }

  @Override
  protected Set<TcgVRefNode> meet(Set<Set<TcgVRefNode>> values) {
    // union of all incoming sets
    var result = new HashSet<TcgVRefNode>();
    for (var value : values) {
      result.addAll(value);
    }
    return result;
  }

  @Override
  protected Set<TcgVRefNode> transferFunction(ControlNode node, Set<TcgVRefNode> input) {
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
  private Set<TcgVRefNode> initialEndFlow() {
    // We have to assume that all registers and register files are used
    // after this instruciton. Thus we have to set them used at the end of the instruction.
    // We also use constants at the end, so they can't be reassigned
    var endFlow = tempAssignments.tcgVariables()
        .filter(v ->
            v.var().kind() == TcgV.Kind.REG
                || v.var().kind() == TcgV.Kind.REG_FILE
                || v.var().kind() == TcgV.Kind.CONST
        ).collect(Collectors.toSet());
    return endFlow;
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
  private List<TcgVRefNode> defineVariables(ControlNode node) {
    if (!(node instanceof TcgNode tcgNode)) {
      return List.of();
    }
    return tcgNode.definedVars();
  }

  /**
   * Retrieves the set of variables used at the given node.
   *
   * @param node The control node to analyze.
   * @return The set of variables used at the node.
   */
  private List<TcgVRefNode> usedVariables(ControlNode node) {
    if (!(node instanceof TcgNode tcgNode)) {
      return List.of();
    }

    // get variables
    var directlyUsedVars = new HashSet<>(tcgNode.usedVars());
    for (var usedVar : directlyUsedVars.stream().toList()) {
      if (usedVar.var().kind() == TcgV.Kind.REG_FILE) {
        var regFile = (RegisterFile) usedVar.var().registerOrFile();
        directlyUsedVars.addAll(registerFileVars.get(regFile));
      }
    }


    // however, if those variables are register files
    // all variables of the same register file must be considered also used
    // as the concrete register file index is not known

    return directlyUsedVars.stream().toList();
  }

}

/**
 * Represents an interference graph where nodes are variables and edges represent
 * interference between variables.
 *
 * <p>An interference graph models conflicts between variables where each node is a variable,
 * and an edge indicates that two variables interfere with each other
 * (i.e., they are live at the same time and cannot share a register).
 */
class InterferenceGraph {

  private Map<TcgVRefNode, Set<TcgVRefNode>> adjacencyList;

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
  public void addEdge(TcgVRefNode v1, TcgVRefNode v2) {
    adjacencyList.computeIfAbsent(v1, k -> new HashSet<>()).add(v2);
    adjacencyList.computeIfAbsent(v2, k -> new HashSet<>()).add(v1);
  }

  /**
   * Retrieves the set of variables that interfere with the given variable.
   *
   * @param variable The variable to query.
   * @return The set of interfering variables.
   */
  public Set<TcgVRefNode> getInterferences(TcgVRefNode variable) {
    return adjacencyList.getOrDefault(variable, new HashSet<>());
  }

  /**
   * Retrieves all variables in the interference graph.
   *
   * @return A set of all variables in the graph.
   */
  public Set<TcgVRefNode> getVariables() {
    return adjacencyList.keySet();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<TcgVRefNode, Set<TcgVRefNode>> entry : adjacencyList.entrySet()) {
      sb.append(entry.getKey()).append(" -> ")
          .append(entry.getValue()).append("\n");
    }
    return sb.toString();
  }

}

/**
 * Performs graph coloring on an interference graph to allocate registers.
 */
class GraphColoring {

  private InterferenceGraph graph;
  private Map<TcgVRefNode, Integer> variableColors;
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
    var uncoloredVariables = new HashSet<>(graph.getVariables());

    while (!uncoloredVariables.isEmpty()) {
      var variable = selectVariable(uncoloredVariables);
      assignColor(variable);
      uncoloredVariables.remove(variable);
    }
  }

  /**
   * Selects the next variable to color.
   *
   * <p>This implementation selects an arbitrary variable.
   * It can be improved by using heuristics like choosing the variable with the highest degree.
   *
   * @param uncoloredVariables The set of uncolored variables.
   * @return The selected variable.
   */
  private TcgVRefNode selectVariable(Set<TcgVRefNode> uncoloredVariables) {
    return uncoloredVariables.iterator().next();
  }

  /**
   * Assigns a color (register number) to the given variable.
   *
   * @param variable The variable to color.
   */
  private void assignColor(TcgVRefNode variable) {
    Set<Integer> usedColors = new HashSet<>();

    // Collect colors of interfering variables
    for (TcgVRefNode neighbor : graph.getInterferences(variable)) {
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
  public Map<TcgVRefNode, Integer> getVariableColors() {
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
