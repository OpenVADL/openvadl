// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.extensions;

import static vadl.iss.passes.TcgPassUtils.regInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ParamNode;

/**
 * Provides extended information and capabilities for ISA instruction definitions.
 * E.g. it defines whether an instruction is rendered as a helper call or not.
 */
public class InstrInfo extends DefinitionExtension<Instruction> {

  @Nullable
  private InstrExecPlan executionPlan = null;

  List<Function> extractedFunctions = new ArrayList<>();

  /**
   * Determines if the instruction is rendered as a helper call to
   * a C implementation of this instruction.
   */
  public boolean asHelperCall() {
    return usesWholeHelperPath();
  }

  /**
   * Returns the execution plan computed for this instruction, if any.
   */
  public @Nullable InstrExecPlan executionPlan() {
    return executionPlan;
  }

  /**
   * Stores the execution plan computed for this instruction.
   */
  public void setExecutionPlan(InstrExecPlan executionPlan) {
    this.executionPlan = executionPlan;
  }

  /**
   * Returns the selected direct-gvec plan for this instruction, if any.
   */
  public @Nullable VectorTensorPlan directGvecPlan() {
    if (executionPlan == null) {
      return null;
    }
    return executionPlan.directGvecPlan();
  }

  /**
   * Returns whether the instruction uses the shared non-helper lowering path.
   */
  public boolean usesNormalTcgPath() {
    if (executionPlan != null) {
      return executionPlan.usesNormalTcgPath();
    }
    return computeFallbackExecutionPath() == InstrExecPlan.ExecutionPath.NORMAL_TCG;
  }

  /**
   * Returns whether the instruction must still execute as a whole-instruction helper call.
   */
  public boolean usesWholeHelperPath() {
    if (executionPlan != null) {
      return executionPlan.usesWholeHelperPath();
    }
    return computeFallbackExecutionPath() == InstrExecPlan.ExecutionPath.HELPER_CALL;
  }

  /**
   * Returns whether the execution plan retained a viable direct-gvec lowering subplan.
   */
  public boolean hasViableDirectGvecPlan() {
    return directGvecPlan() != null;
  }

  private InstrExecPlan.ExecutionPath computeFallbackExecutionPath() {
    var hasCpuVectorReads = instr().behavior().getNodes(IssReadRegNode.class)
        .anyMatch(n -> regInfo(n.regTensor()).execClass() == RegInfo.ExecClass.CPU_VECTOR);
    var hasCpuVectorWrites = instr().behavior().getNodes(IssWriteRegNode.class)
        .anyMatch(n -> regInfo(n.regTensor()).execClass() == RegInfo.ExecClass.CPU_VECTOR);
    return hasCpuVectorReads || hasCpuVectorWrites
        ? InstrExecPlan.ExecutionPath.HELPER_CALL
        : InstrExecPlan.ExecutionPath.NORMAL_TCG;
  }

  /**
   * Determines if the instruction's loops should be unrolled.
   */
  public boolean unrollLoops() {
    // TODO: decide this based on the instruction's behavior.
    return true;
  }

  /**
   * Generates a lowercase representation of the instruction's simple name.
   */
  @SuppressWarnings("MethodName")
  public String cIdentName() {
    return instr().simpleName().toLowerCase();
  }

  @SuppressWarnings("MethodName")
  public String cCpuStateName() {
    return "CPU" + instr().simpleName().toUpperCase() + "State";
  }

  public String helperName() {
    return cIdentName() + "_instr";
  }

  public Instruction instr() {
    return extendingDef();
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Instruction.class;
  }

  /**
   * Returns helper parameter order matching the instruction behavior's first-seen parameter names.
   */
  public Stream<ParamNode> helperFormatParamOrder() {
    var params = new LinkedHashMap<String, ParamNode>();
    instr().behavior().getNodes(ParamNode.class)
        .forEach(p -> params.putIfAbsent(p.definition().simpleName(), p));

    Stream.concat(
            instr().behavior().getNodes(IssReadRegNode.class)
                .flatMap(n -> n.accessorIndices().stream()),
            instr().behavior().getNodes(IssWriteRegNode.class)
                .flatMap(n -> n.accessorIndices().stream())
        )
        .forEach(expr -> collectParamNodes(expr)
            .forEach(p -> params.putIfAbsent(p.definition().simpleName(), p)));

    return params.values().stream()
        .sorted(Comparator.comparing((a) -> a.definition().simpleName()));
  }

  private List<ParamNode> collectParamNodes(ExpressionNode expr) {
    var out = new ArrayList<ParamNode>();
    if (expr instanceof ParamNode param) {
      out.add(param);
    }
    expr.collectInputsWithChildren(out, ParamNode.class);
    return out;
  }

  public void addExtractedFunction(Function function) {
    extractedFunctions.add(function);
  }

  public List<Function> extractedFunctions() {
    return extractedFunctions;
  }

}
