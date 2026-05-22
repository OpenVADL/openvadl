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

import static vadl.iss.passes.TcgPassUtils.regInfo;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.extensions.RegInfo;
import vadl.iss.passes.nodes.IssRegBitfieldWriteNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * Converts chunk-window ISS writes into dedicated TCG bitfield-write nodes for DIRECT_TCG
 * instruction behaviors.
 *
 * <p>The register-access lowering pass emits only unified {@link IssWriteRegNode}. This pass
 * is strategy-aware and performs the TCG-specific rewrite to {@link IssRegBitfieldWriteNode}
 * only for instructions classified as DIRECT_TCG.</p>
 */
public class IssBitfieldWriteLoweringPass extends AbstractIssPass {

  public IssBitfieldWriteLoweringPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Bitfield Write Lowering");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam) throws IOException {
    scalarTcgInstrs(viam)
        .forEach(instr -> instr.behavior()
            .getNodes(IssWriteRegNode.class)
            .toList()
            .forEach(this::maybeLower));
    return null;
  }

  private void maybeLower(IssWriteRegNode write) {
    if (write.windowKind() != IssWriteRegNode.WindowKind.CHUNK) {
      return;
    }
    if (regInfo(write.regTensor()).execClass() != RegInfo.ExecClass.TCG_SCALAR) {
      return;
    }
    if (!isTranslationTimeConstant(write.bitOffset())
        || !isTranslationTimeConstant(write.bitWidth())) {
      return;
    }

    var accessorName = write.accessKind() == IssWriteRegNode.AccessKind.ALIAS
        ? write.accessorName()
        : null;
    var replacement = new IssRegBitfieldWriteNode(
        write.regTensor(),
        write.indices().copy(),
        write.value(),
        write.bitOffset(),
        write.bitWidth(),
        accessorName,
        write.nullableCondition()
    );
    replacement.setSourceLocationIfNotSet(write.location());
    write.replaceAndDelete(replacement);
  }

  private boolean isTranslationTimeConstant(ExpressionNode expr) {
    return expr instanceof ConstantNode
        || expr instanceof FieldRefNode
        || expr instanceof FieldAccessRefNode;
  }
}
