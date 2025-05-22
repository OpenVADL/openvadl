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

import java.io.IOException;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.nodes.IssConstExtractNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.BuiltInCall;

/**
 * The {@link IssNormalizationPass} ensures that all expression results fit within the
 * target register size.
 * For example, if an {@code ADD} operation involves two 50-bit operands,
 * the pass would normally insert truncations to enforce correct bit widths.
 * However, for addition (and other operations),
 * truncating operands beforehand has no impact on the result.
 *
 * <p>This pass removes such unnecessary truncations of built-in operands
 * to minimize emitted TCG operations.</p>
 *
 * <p><b>NOTE:</b> Even without {@link IssNormalizationPass}, this optimization is essential.
 * Consider the RISC-V 64 {@code ADDW} instruction:
 * <pre>{@code
 * instruction ADDW: Wtype =
 *  X(rd) := (X(rs1) as Bits<32> + X(rs2) as Bits<32>) as SInt<64>
 * }</pre>
 * Here, the input operands are explicitly truncated by the user.
 * While required for type checking, this truncation is unnecessary for arithmetic correctness.
 * </p>
 */
public class IssBuiltInArgTruncOptPass extends AbstractIssPass {

  public IssBuiltInArgTruncOptPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS BuiltIn Arg Truncate Optimization");
  }

  @Override
  public boolean skip() {
    return configuration().isSkip(IssConfiguration.IssOptsToSkip.OPT_ARGS);
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    viam.isa().get().ownInstructions()
        .forEach(i -> new IssBuiltInArgTruncOptimizer(i.behavior()).run());
    return null;
  }
}


class IssBuiltInArgTruncOptimizer {

  static final Set<BuiltInTable.BuiltIn> allArgTruncOpt = Set.of(
      // arithmetic
      BuiltInTable.ADD,
      BuiltInTable.SUB,
      BuiltInTable.MUL,

      // logical
      BuiltInTable.NOT,
      BuiltInTable.AND,
      BuiltInTable.XOR,
      BuiltInTable.OR
  );

  static final Set<BuiltInTable.BuiltIn> firstArgTruncOpt = Set.of(
      // shifting
      BuiltInTable.LSL
  );
  Graph graph;

  public IssBuiltInArgTruncOptimizer(Graph graph) {
    this.graph = graph;
  }

  void run() {
    graph.getNodes(BuiltInCall.class)
        .forEach(this::optimize);
  }

  private void optimize(BuiltInCall call) {
    var builtIn = call.builtIn();
    if (allArgTruncOpt.contains(builtIn)) {
      optAllArgs(call);
    } else if (firstArgTruncOpt.contains(builtIn)) {
      optFirstArgs(call);
    }
  }

  private void optAllArgs(BuiltInCall call) {
    var typeWidth = call.type().asDataType().bitWidth();
    for (var arg : call.arguments()) {
      if (arg instanceof IssConstExtractNode extractNode
          // we cannot remove the extract operation if it manipulates the original input value
          && extractNode.preservedWidth() >= typeWidth) {
        extractNode.replaceByGhostCastForUser(call);
      }
    }
  }

  private void optFirstArgs(BuiltInCall call) {
    var firstArg = call.arguments().get(0);
    if (firstArg instanceof IssConstExtractNode extractNode
        && extractNode.isTruncate()) {
      if (extractNode.toWidth() < call.type().asDataType().bitWidth()) {
        // if the truncate also truncates operand bits, we must keep it
        return;
      }
      extractNode.replaceByGhostCastForUser(call);
    }
  }


}