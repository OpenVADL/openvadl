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

import static java.util.Objects.requireNonNull;
import static vadl.iss.passes.TcgPassUtils.regInfo;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.extensions.RegInfo;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Collects backend register access patterns from unified ISS register access nodes.
 *
 * <p>This pass is the bridge between ISS graph-level register access metadata and emitted C
 * accessor functions. It interprets:
 * <ul>
 *   <li>resource indices from unified read/write nodes,</li>
 *   <li>window metadata ({@code bitOffset}/{@code bitWidth}),</li>
 *   <li>alias-vs-base access kind for base-resource pattern ownership.</li>
 * </ul>
 *
 * <p>See {@code docs/iss/register-access-domain-map.md}.
 */
public class IssRegisterAccessInfoRetrievalPass extends AbstractIssPass {
  public IssRegisterAccessInfoRetrievalPass(IssConfiguration config) {
    super(config);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Register Access Info Retrieval");
  }

  @CheckForNull
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    ViamUtils.findAllBehaviors(viam).forEach(this::collectRegisterAccessPatterns);

    // add custom one for program counter
    var pc = requireNonNull(viam.isa().get().pc());
    var info = regInfo(pc.registerTensor());
    info.accessPatterns.add(new RegInfo.AccessPattern(
        info, RegInfo.AccessType.READ, List.of(), pc.resultType().bitWidth(),
        pc.resultType().bitWidth(), 0, pc
    ));
    info.accessPatterns.add(new RegInfo.AccessPattern(
        info, RegInfo.AccessType.WRITE, List.of(), pc.resultType().bitWidth(),
        pc.resultType().bitWidth(), 0, pc
    ));

    return null;
  }

  private void collectRegisterAccessPatterns(Graph behavior) {
    behavior.getNodes(Set.of(ReadRegTensorNode.class, WriteRegTensorNode.class))
        .forEach((n) -> {
          if (n instanceof ReadRegTensorNode readRegTensorNode) {
            collectRegisterAccessPattern(readRegTensorNode);
          } else {
            collectRegisterAccessPattern((WriteRegTensorNode) n);
          }
        });
  }

  private void collectRegisterAccessPattern(ReadRegTensorNode node) {
    var info = regInfo(node.regTensor());
    if (node instanceof IssReadRegNode readNode
        && readNode.accessKind() == IssReadRegNode.AccessKind.ALIAS) {
      var baseIndexCount = readNode.regTensor().indexDimensions().size();
      var baseIndices = readNode.indices().stream().limit(baseIndexCount).toList();
      var baseWidth = readNode.regTensor().resultType(baseIndexCount).bitWidth();
      var readOffset = constIntOr(readNode.bitOffset(), 0);
      var readWidth = constIntOr(readNode.bitWidth(), baseWidth);
      info.accessPatterns.add(RegInfo.AccessPattern.of(
          readNode,
          RegInfo.AccessType.READ,
          readNode.regTensor(),
          baseIndices,
          readWidth,
          baseWidth,
          readOffset
      ));
      return;
    }
    info.accessPatterns.add(RegInfo.AccessPattern.of(node));
  }

  private void collectRegisterAccessPattern(WriteRegTensorNode node) {
    var info = regInfo(node.regTensor());
    if (node instanceof IssWriteRegNode writeNode
        && writeNode.accessKind() == IssWriteRegNode.AccessKind.ALIAS) {
      var baseIndexCount = writeNode.regTensor().indexDimensions().size();
      var baseIndices = writeNode.indices().stream().limit(baseIndexCount).toList();
      var baseWidth = writeNode.regTensor().resultType(baseIndexCount).bitWidth();
      var writeOffset = constIntOr(writeNode.bitOffset(), 0);
      var writeWidth = constIntOr(writeNode.bitWidth(), baseWidth);
      info.accessPatterns.add(RegInfo.AccessPattern.of(
          writeNode,
          RegInfo.AccessType.WRITE,
          writeNode.regTensor(),
          baseIndices,
          writeWidth,
          baseWidth,
          writeOffset
      ));
      return;
    }
    info.accessPatterns.add(RegInfo.AccessPattern.of(node));
  }

  private int constIntOr(ExpressionNode expr, int fallback) {
    if (expr instanceof ConstantNode constantNode) {
      return constantNode.constant().asVal().intValue();
    }
    return fallback;
  }
}
