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

package vadl.iss.passes;

import static vadl.iss.passes.TcgPassUtils.instrInfo;

import java.util.stream.Stream;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.extensions.InstrExecPlan.StrategyKind;
import vadl.pass.Pass;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * The pass all ISS (QEMU) passes extend from.
 * ISS template rendering passes do not extend from this class but from
 * {@link vadl.iss.template.IssTemplateRenderingPass}.
 */
public abstract class AbstractIssPass extends Pass {

  public AbstractIssPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public IssConfiguration configuration() {
    return (IssConfiguration) super.configuration();
  }

  public Stream<Instruction> allInstrs(Specification spec) {
    return spec.isa().get().ownInstructions().stream();
  }

  public Stream<Instruction> scalarTcgInstrs(Specification spec) {
    return allInstrs(spec)
        .filter(this::usesScalarTcgPipeline);
  }

  public Stream<Instruction> directGvecInstrs(Specification spec) {
    return allInstrs(spec)
        .filter(this::usesDirectGvecPlanningPath);
  }

  public Stream<Instruction> wholeHelperInstrs(Specification spec) {
    return allInstrs(spec)
        .filter(this::usesWholeHelperPath);
  }

  public Stream<Instruction> tcgInstrs(Specification spec) {
    return scalarTcgInstrs(spec);
  }

  public Stream<Instruction> helperInstrs(Specification spec) {
    return wholeHelperInstrs(spec);
  }

  private boolean usesScalarTcgPipeline(Instruction instruction) {
    var executionPlan = instrInfo(instruction).executionPlan();
    if (executionPlan != null) {
      return executionPlan.selectedStrategy() == StrategyKind.TCG_SCALAR;
    }
    return !instrInfo(instruction).asHelperCall();
  }

  private boolean usesDirectGvecPlanningPath(Instruction instruction) {
    var executionPlan = instrInfo(instruction).executionPlan();
    return executionPlan != null && executionPlan.selectedStrategy() == StrategyKind.DIRECT_GVEC;
  }

  private boolean usesWholeHelperPath(Instruction instruction) {
    var executionPlan = instrInfo(instruction).executionPlan();
    if (executionPlan != null) {
      return executionPlan.selectedStrategy() == StrategyKind.HELPER_CALL;
    }
    return instrInfo(instruction).asHelperCall();
  }
}
