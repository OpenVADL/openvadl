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
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.graph.dependency.ParamNode;

/**
 * Provides extended information and capabilities for ISA instruction definitions.
 * E.g. it defines whether an instruction is rendered as a helper call or not.
 */
public class InstrInfo extends DefinitionExtension<Instruction> {


  /**
   * Backend execution strategy for an instruction.
   */
  public enum ExecStrategy {
    /**
     * Instruction can be lowered to direct TCG translation.
     */
    DIRECT_TCG,
    /**
     * Instruction must be translated as helper call.
     */
    HELPER_CALL
  }

  @Nullable
  private ExecStrategy execStrategy = null;

  List<Function> extractedFunctions = new ArrayList<>();

  /**
   * Determines if the instruction is rendered as a helper call to
   * a C implementation of this instruction.
   */
  public boolean asHelperCall() {
    return execStrategy() == ExecStrategy.HELPER_CALL;
  }

  /**
   * Gets the execution strategy used by code generation.
   */
  public ExecStrategy execStrategy() {
    if (execStrategy == null) {
      execStrategy = computeFallbackExecStrategy();
    }
    return execStrategy;
  }

  /**
   * Sets the execution strategy as computed by the strategy classifier pass.
   */
  public void setExecStrategy(ExecStrategy execStrategy) {
    this.execStrategy = execStrategy;
  }

  private ExecStrategy computeFallbackExecStrategy() {
    var hasHelperOnlyReads = instr().behavior().getNodes(IssReadRegNode.class)
        .anyMatch(n -> regInfo(n.regTensor()).execClass() == RegInfo.ExecClass.HELPER_ONLY);
    var hasHelperOnlyWrites = instr().behavior().getNodes(IssWriteRegNode.class)
        .anyMatch(n -> regInfo(n.regTensor()).execClass() == RegInfo.ExecClass.HELPER_ONLY);
    return hasHelperOnlyReads || hasHelperOnlyWrites
        ? ExecStrategy.HELPER_CALL
        : ExecStrategy.DIRECT_TCG;
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

  public Stream<ParamNode> helperFormatParamOrder() {
    return instr().behavior().getNodes(ParamNode.class)
        .sorted(Comparator.comparing((a) -> a.definition().simpleName()));
  }

  public void addExtractedFunction(Function function) {
    extractedFunctions.add(function);
  }

  public List<Function> extractedFunctions() {
    return extractedFunctions;
  }

}
