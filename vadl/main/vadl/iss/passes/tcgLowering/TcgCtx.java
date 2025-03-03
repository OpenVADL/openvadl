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

package vadl.iss.passes.tcgLowering;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.iss.passes.TcgPassUtils;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.types.TupleType;
import vadl.utils.Pair;
import vadl.utils.Triple;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Instruction;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.WriteStageOutputNode;

/**
 * The TCG context is associated with an instruction.
 * It holds all necessary information required across multiple passes.
 * Most importantly the {@link Assignment}.
 */
public class TcgCtx extends DefinitionExtension<Instruction> {

  private final Graph graph;
  private final Tcg_32_64 targetSize;

  private final Assignment assignment;

  /**
   * Constructs a new TCG context with a new assignment.
   */
  public TcgCtx(Graph graph, Tcg_32_64 targetSize) {
    this.graph = graph;
    this.targetSize = targetSize;
    this.assignment = new Assignment();
  }

  public Assignment assignment() {
    return assignment;
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Definition.class;
  }

  /**
   * The assignment is a mapping of dependency nodes to TCG variable reference nodes.
   * Passes, like the {@link TcgOpLoweringPass} require the assignment to produce
   * sequential code.
   * Most of those assignments are not fixed, but temporary.
   * So if a node does not yet have an assignment when it is requested,
   * a TCGv is created and a reference node added to the graph.
   * There are multiple kinds of TCGv, namely {@code reg,regfile,tmp,const}.
   * The kind of the TCGv depends on the dependency node passed to the request.
   */
  @DispatchFor(value = DependencyNode.class,
      include = {"vadl.iss", "vadl.viam"},
      returnType = List.class
  )
  public class Assignment {
    private final HashMap<DependencyNode, List<TcgVRefNode>> assignments;
    // the tcgV cache stores assignments not necessarily specifc to nodes
    // but to register/reigsterFile+index, ...
    private final HashMap<Object, List<TcgVRefNode>> tcgVCache;

    private Assignment() {
      this.assignments = new HashMap<>();
      this.tcgVCache = new HashMap<>();
    }


    /**
     * Retrieve the TCGv reference node for the given dependency node.
     * If the dependency node has multiple return values (e.i. a tuple), this
     * method will throw an exception.
     */
    public TcgVRefNode singleDestOf(DependencyNode node) {
      var dest = destOf(node);
      node.ensure(dest.size() == 1, "Expected exactly one destination variable, got %s", dest);
      return dest.get(0);
    }

    /**
     * Get all tcg variables used in the assignment.
     */
    public Stream<TcgVRefNode> tcgVariables() {
      return assignments.values().stream().flatMap(Collection::stream).distinct();
    }

    /**
     * Retrieve the TCGv reference nodes for the given dependency node.
     * As a node may result in a tuple, it may have multiple destination TCGvs.
     * Therefore, the method returns a list of TCGv reference nodes.
     *
     * <p>If you know that there must be exactly one destination TCGv, use the
     * {@link #singleDestOf(DependencyNode)} instead.
     */
    public List<TcgVRefNode> destOf(DependencyNode node) {
      return AssignmentDispatcher.dispatch(this, node);
    }

    @Handler
    List<TcgVRefNode> destOf(TcgVRefNode toHandle) {
      return assignments.computeIfAbsent(toHandle, n -> List.of(toHandle));
    }

    @Handler
    List<TcgVRefNode> destOf(WriteMemNode toHandle) {
      return List.of();
    }

    @Handler
    List<TcgVRefNode> destOf(WriteRegNode toHandle) {
      return assignments.computeIfAbsent(toHandle,
          n -> createRegVar(toHandle.register(), true));
    }

    @Handler
    List<TcgVRefNode> destOf(WriteRegFileNode toHandle) {
      return assignments.computeIfAbsent(toHandle,
          n -> createRegFileVar(toHandle.registerFile(), toHandle.address(), true));
    }

    @Handler
    List<TcgVRefNode> destOf(ReadRegNode toHandle) {
      return assignments.computeIfAbsent(toHandle,
          n -> createRegVar(toHandle.register(), false));
    }

    @Handler
    List<TcgVRefNode> destOf(ReadRegFileNode toHandle) {
      return assignments.computeIfAbsent(toHandle,
          n -> createRegFileVar(toHandle.registerFile(), toHandle.address(), false));
    }

    @Handler
    List<TcgVRefNode> destOf(ExpressionNode expr) {
      return assignments.computeIfAbsent(expr, v -> {
        var isTcg = expr.usages().anyMatch(u -> u instanceof ScheduledNode);
        if (isTcg) {
          return createTempExprVar(expr);
        } else {
          return List.of(createConstExprVar(expr));
        }
      });
    }

    @Handler
    List<TcgVRefNode> handle(WriteStageOutputNode toHandle) {
      throw new UnsupportedOperationException("Type WriteStageOutputNode not yet implemented");
    }

    private TcgVRefNode toNode(TcgV tcgV) {
      return toNode(tcgV, null);
    }

    private TcgVRefNode toNode(TcgV tcgV, @Nullable ExpressionNode dependency) {
      return graph.addWithInputs(new TcgVRefNode(tcgV, dependency));
    }

    private TcgVRefNode createConstExprVar(ExpressionNode expr) {
      return toNode(TcgV.constant(
          "const_" + TcgPassUtils.exprVarName(expr) + "_n" + expr.id,
          targetSize, expr
      ), expr);
    }

    private List<TcgVRefNode> createTempExprVar(ExpressionNode expr) {
      // determine number of return values from tuple
      var numberOfResults = expr.type() instanceof TupleType tupleType ? tupleType.size() : 1;

      return IntStream.range(0, numberOfResults).boxed()
          .map(i -> toNode(
              TcgV.tmp(
                  "tmp_" + TcgPassUtils.exprVarName(expr) + "_" + i,
                  targetSize
              )
          ))
          .toList();
    }

    /**
     * Creates a variable for the given register.
     *
     * @param reg The register.
     * @return The variable corresponding to the register.
     */
    private List<TcgVRefNode> createRegVar(Register reg, boolean isDest) {
      var key = Pair.of(reg, isDest);
      var dest = isDest ? "_dest" : "";
      return tcgVCache.computeIfAbsent(key, k -> List.of(toNode(TcgV.reg(
          "reg_" + reg.simpleName().toLowerCase() + dest,
          targetSize,
          reg
      ))));
    }

    /**
     * Creates a variable for the given register file and index.
     *
     * @param regFile The register file.
     * @param index   The index expression node.
     * @return The variable corresponding to the register file at the given index.
     */
    private List<TcgVRefNode> createRegFileVar(RegisterFile regFile, ExpressionNode index,
                                               boolean isDest) {
      var key = Triple.of(regFile, index, isDest);
      var dest = isDest ? "_dest" : "";
      return tcgVCache.computeIfAbsent(key, k -> {
        var regFileVar = TcgV.regFile("regfile_" + regFile.simpleName().toLowerCase()
                + "_" + TcgPassUtils.exprVarName(index) + dest,
            targetSize, regFile, index, isDest);

        // add index as dependency to var reference node
        return List.of(toNode(regFileVar, index));
      });
    }
  }

}
