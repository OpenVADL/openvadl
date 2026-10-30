// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.common.builtInLowering;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.VadlBuiltInEmptyNoStatusDispatcher;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;

/**
 * Lowers built-ins that have no corresponding TCG operation to an equivalent expression
 * of built-ins that do.
 * E.g., TCG has no scalar saturated arithmetic operations, so the
 * {@link vadl.types.BuiltInTable#SSATADD} built-in is replaced by a wrapping addition
 * and a select of the saturation value (see {@link SaturatedArithmeticLowering}).
 *
 * <p>Only instructions that use the normal TCG pipeline are lowered.
 * Instructions executed by a helper call keep the built-ins, as the C++ code generator
 * supports them directly.
 * The pass runs after the {@link vadl.iss.passes.vector.IssDirectGvecLoweringPass},
 * so vector lowering still sees the original built-ins, and before the
 * {@link vadl.iss.passes.common.opDecomposition.IssOpDecompositionPass} and the
 * {@link vadl.iss.passes.common.IssNormalizationPass}, so they only have to handle
 * the lowered operations.</p>
 */
public class IssBuiltInLoweringPass extends AbstractIssPass {

  public IssBuiltInLoweringPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Built-In Lowering");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam) throws IOException {
    var lowerer = new BuiltInLowerer();
    normalTcgInstrs(viam)
        .forEach(instr -> instr.behavior()
            .getNodes(BuiltInCall.class)
            .toList()
            .forEach(call -> lowerer.dispatch(call, call.builtIn())));
    return null;
  }
}

/**
 * Dispatches the built-in calls to their lowering.
 * Built-ins without a handler are kept as they are.
 */
class BuiltInLowerer implements VadlBuiltInEmptyNoStatusDispatcher<BuiltInCall> {

  @Override
  public void handleSSATADD(BuiltInCall input) {
    SaturatedArithmeticLowering.lowerSsatAdd(input);
  }

  @Override
  public void handleUSATADD(BuiltInCall input) {
    SaturatedArithmeticLowering.lowerUsatAdd(input);
  }

  @Override
  public void handleSSATSUB(BuiltInCall input) {
    SaturatedArithmeticLowering.lowerSsatSub(input);
  }

  @Override
  public void handleUSATSUB(BuiltInCall input) {
    SaturatedArithmeticLowering.lowerUsatSub(input);
  }
}
