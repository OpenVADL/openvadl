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

import static java.util.Objects.requireNonNull;
import static vadl.iss.passes.TcgPassUtils.regInfo;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.extensions.IssAccessorRegistry;
import vadl.iss.passes.extensions.IssAliasAccessorDescriptors;
import vadl.iss.passes.extensions.RegInfo;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.ArtificialResource;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Collects backend base accessor descriptors from unified ISS register access nodes.
 *
 * <p>This pass is the bridge between ISS graph-level register access metadata and emitted raw C
 * storage accessors. It interprets:
 * <ul>
 *   <li>resource indices from unified read/write nodes,</li>
 *   <li>window metadata ({@code bitOffset}/{@code bitWidth}),</li>
 *   <li>alias-vs-base access kind for base-resource accessor ownership.</li>
 * </ul>
 *
 * <p>It records each descriptor centrally on {@link RegInfo} so later codegen reuses the collected
 * descriptor instead of reconstructing it ad hoc.
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
    var registry = new IssAccessorRegistry();
    viam.isa().ifPresent(isa -> {
      isa.artificialResources()
          .forEach(alias -> collectAllAliasAccessorDescriptors(alias, registry));
      ViamUtils.findAllBehaviors(isa)
          .filter(behavior -> !(behavior.parentDefinition() instanceof ArtificialResource))
          .forEach(behavior -> collectAccessorDescriptors(behavior, registry));
    });

    // add custom one for program counter
    var pc = requireNonNull(viam.isa().get().pc());
    var info = regInfo(pc.registerTensor());
    registry.addBaseAccessor(new RegInfo.BaseAccessorDescriptor(
        info, RegInfo.AccessType.READ, List.of(), pc.resultType().bitWidth(),
        pc.resultType().bitWidth(), 0, pc
    ));
    registry.addBaseAccessor(new RegInfo.BaseAccessorDescriptor(
        info, RegInfo.AccessType.WRITE, List.of(), pc.resultType().bitWidth(),
        pc.resultType().bitWidth(), 0, pc
    ));

    return registry;
  }

  private void collectAccessorDescriptors(Graph behavior,
                                         IssAccessorRegistry registry) {
    behavior.getNodes(Set.of(ReadRegTensorNode.class, WriteRegTensorNode.class))
        .forEach((n) -> {
          if (n instanceof ReadRegTensorNode readRegTensorNode) {
            collectAccessorDescriptor(behavior, readRegTensorNode, registry);
            collectAliasAccessorDescriptor(readRegTensorNode, registry);
          } else {
            var writeRegTensorNode = (WriteRegTensorNode) n;
            collectAccessorDescriptor(behavior, writeRegTensorNode, registry);
            collectAliasAccessorDescriptor(writeRegTensorNode, registry);
          }
        });
  }

  private void collectAccessorDescriptor(Graph behavior, ReadRegTensorNode node,
                                         IssAccessorRegistry registry) {
    var descriptor = RegInfo.BaseAccessorDescriptor.ofOrigin(node);
    if (!shouldCollectDescriptor(behavior, node, descriptor)) {
      return;
    }
    registry.recordBaseAccessor(node, descriptor);
  }

  private void collectAccessorDescriptor(Graph behavior, WriteRegTensorNode node,
                                         IssAccessorRegistry registry) {
    var descriptor = RegInfo.BaseAccessorDescriptor.ofOrigin(node);
    if (!shouldCollectDescriptor(behavior, node, descriptor)) {
      return;
    }
    registry.recordBaseAccessor(node, descriptor);
  }

  private void collectAliasAccessorDescriptor(ReadRegTensorNode node,
                                              IssAccessorRegistry registry) {
    if (!(node instanceof IssReadRegNode readNode)
        || readNode.accessKind() != IssReadRegNode.AccessKind.ALIAS
        || readNode.windowKind() != IssReadRegNode.WindowKind.FULL) {
      return;
    }
    collectAliasAccessorDescriptor(readNode.aliasResource(), RegInfo.AccessType.READ, registry);
  }

  private void collectAliasAccessorDescriptor(WriteRegTensorNode node,
                                              IssAccessorRegistry registry) {
    if (!(node instanceof IssWriteRegNode writeNode)
        || writeNode.accessKind() != IssWriteRegNode.AccessKind.ALIAS
        || writeNode.windowKind() != IssWriteRegNode.WindowKind.FULL) {
      return;
    }
    collectAliasAccessorDescriptor(writeNode.aliasResource(), RegInfo.AccessType.WRITE, registry);
  }

  private void collectAliasAccessorDescriptor(@CheckForNull ArtificialResource alias,
                                              RegInfo.AccessType type,
                                              IssAccessorRegistry registry) {
    if (alias == null) {
      return;
    }
    for (var backend : RegInfo.BackendKind.values()) {
      var descriptor = IssAliasAccessorDescriptors.descriptor(alias, type, backend);
      if (descriptor != null) {
        registry.addAliasAccessor(descriptor);
      }
    }
  }

  private void collectAllAliasAccessorDescriptors(ArtificialResource alias,
                                                  IssAccessorRegistry registry) {
    for (var type : RegInfo.AccessType.values()) {
      collectAliasAccessorDescriptor(alias, type, registry);
    }
  }

  private boolean shouldCollectDescriptor(Graph behavior, ReadRegTensorNode node,
                                          RegInfo.BaseAccessorDescriptor descriptor) {
    if (descriptor.elementWidth() <= configuration().targetSize().width) {
      return true;
    }
    if (regInfo(node.regTensor()).execClass() == RegInfo.ExecClass.HELPER_ONLY) {
      // Helper-only wide accesses are emitted via helper/cpu paths and do not require
      // scalar base-accessor signatures.
      return false;
    }
    var owner = behavior.parentDefinition();
    node.ensure(false,
        "Unsupported base accessor descriptor above target width in ISS retrieval: %s, "
            + "behavior=%s, owner=%s",
        descriptor,
        behavior,
        owner == null ? "null" : owner.getClass().getSimpleName() + ":" + owner);
    return false;
  }

  private boolean shouldCollectDescriptor(Graph behavior, WriteRegTensorNode node,
                                          RegInfo.BaseAccessorDescriptor descriptor) {
    if (descriptor.elementWidth() <= configuration().targetSize().width) {
      return true;
    }
    if (regInfo(node.regTensor()).execClass() == RegInfo.ExecClass.HELPER_ONLY) {
      // Helper-only wide accesses are emitted via helper/cpu paths and do not require
      // scalar base-accessor signatures.
      return false;
    }
    var owner = behavior.parentDefinition();
    node.ensure(false,
        "Unsupported base accessor descriptor above target width in ISS retrieval: %s, "
            + "behavior=%s, owner=%s",
        descriptor,
        behavior,
        owner == null ? "null" : owner.getClass().getSimpleName() + ":" + owner);
    return false;
  }
}
