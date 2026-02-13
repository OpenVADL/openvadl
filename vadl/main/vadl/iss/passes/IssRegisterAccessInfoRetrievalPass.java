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
import vadl.iss.passes.nodes.IssRegChunkReadNode;
import vadl.iss.passes.nodes.IssRegChunkWriteNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Retrieves register access information for each register tensor access in the specification.
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
    behavior.getNodes(Set.of(ReadRegTensorNode.class, WriteRegTensorNode.class,
            IssRegChunkReadNode.class, IssRegChunkWriteNode.class))
        .forEach((n) -> {
          if (n instanceof ReadRegTensorNode readRegTensorNode) {
            collectRegisterAccessPattern(readRegTensorNode);
          } else if (n instanceof IssRegChunkReadNode readChunk) {
            collectRegisterAccessPattern(readChunk);
          } else if (n instanceof IssRegChunkWriteNode writeChunk) {
            collectRegisterAccessPattern(writeChunk);
          } else {
            collectRegisterAccessPattern((WriteRegTensorNode) n);
          }
        });
  }

  private void collectRegisterAccessPattern(ReadRegTensorNode node) {
    var info = regInfo(node.regTensor());
    info.accessPatterns.add(RegInfo.AccessPattern.of(node));
  }

  private void collectRegisterAccessPattern(WriteRegTensorNode node) {
    var info = regInfo(node.regTensor());
    info.accessPatterns.add(RegInfo.AccessPattern.of(node));
  }

  private void collectRegisterAccessPattern(IssRegChunkReadNode node) {
    var info = regInfo(node.regTensor());
    info.accessPatterns.add(RegInfo.AccessPattern.of(node));
  }

  private void collectRegisterAccessPattern(IssRegChunkWriteNode node) {
    var info = regInfo(node.regTensor());
    info.accessPatterns.add(RegInfo.AccessPattern.of(node));
  }
}
