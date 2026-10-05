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

package vadl.iss.passes.common;

import java.io.IOException;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.passes.statusBuiltInInlinePass.SaturatedArithmeticInliner;

/**
 * TCG has no scalar saturated arithmetic operations.
 * This pass replaces the non-status saturated built-ins ({@link BuiltInTable#SSATADD},
 * {@link BuiltInTable#USATADD}, {@link BuiltInTable#SSATSUB}, {@link BuiltInTable#USATSUB})
 * in instructions that use the normal TCG pipeline by the equivalent wrapping arithmetic
 * and a select of the saturation value.
 * Instructions executed by a helper call keep the built-ins, as the C++ code generator
 * supports them directly.
 *
 * <p>The status variants of the saturated built-ins are already inlined by the
 * {@link vadl.viam.passes.statusBuiltInInlinePass.StatusBuiltInInlinePass}.</p>
 */
public class IssSaturatedArithmeticLoweringPass extends AbstractIssPass {

  private static final Set<BuiltInTable.BuiltIn> SATURATED_BUILT_INS = Set.of(
      BuiltInTable.SSATADD,
      BuiltInTable.USATADD,
      BuiltInTable.SSATSUB,
      BuiltInTable.USATSUB
  );

  public IssSaturatedArithmeticLoweringPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Saturated Arithmetic Lowering");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam) throws IOException {
    normalTcgInstrs(viam)
        .forEach(instr -> instr.behavior()
            .getNodes(BuiltInCall.class)
            .filter(call -> SATURATED_BUILT_INS.contains(call.builtIn()))
            .toList()
            .forEach(SaturatedArithmeticInliner::lower));
    return null;
  }
}
