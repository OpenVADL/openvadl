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

package vadl.iss.passes;

import static vadl.iss.passes.TcgPassUtils.instrInfo;
import static vadl.iss.passes.TcgPassUtils.regInfo;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.extensions.InstrInfo;
import vadl.iss.passes.extensions.RegInfo;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.TensorNode;

/**
 * Computes and stores ISS backend execution strategies for instructions.
 *
 * <p>This pass centralizes strategy classification so code generation only consumes the strategy
 * and does not infer backend choices ad-hoc.</p>
 */
public class IssExecStrategyPass extends AbstractIssPass {

  public IssExecStrategyPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Exec Strategy Classification");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam) throws IOException {
    if (viam.isa().isEmpty()) {
      return null;
    }

    var isa = viam.isa().get();
    isa.ownInstructions().forEach(instr -> {
      var hasHelperOnlyReads = instr.behavior().getNodes(IssReadRegNode.class)
          .anyMatch(n -> regInfo(n.regTensor()).execClass() == RegInfo.ExecClass.HELPER_ONLY);
      var hasHelperOnlyWrites = instr.behavior().getNodes(IssWriteRegNode.class)
          .anyMatch(n -> regInfo(n.regTensor()).execClass() == RegInfo.ExecClass.HELPER_ONLY);

      // TODO: Eventually this must be supported by TCG as well
      var anyTensorExpr = instr.behavior().getNodes(ForallNode.class).findAny().isPresent()
          || instr.behavior().getNodes(TensorNode.class).findAny().isPresent()
          || instr.behavior().getNodes(FoldNode.class).findAny().isPresent();

      var strategy = hasHelperOnlyReads || hasHelperOnlyWrites || anyTensorExpr
          ? InstrInfo.ExecStrategy.HELPER_CALL
          : InstrInfo.ExecStrategy.DIRECT_TCG;

      instrInfo(instr).setExecStrategy(strategy);
    });
    return null;
  }
}
