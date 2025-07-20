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

package vadl.gcb.passes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.annotations.CompilerRegisterRenamingAnnotation;
import vadl.gcb.annotations.HalfWidthOfAnnotation;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.ArtificialResource;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.ReadsRegisterTensor;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.matching.TreeMatcher;

/**
 * The used registers in the instruction's behavior may not be how a compiler needs it.
 * For example, AArch64 has one big register file {@code S} and the instruction behavior then
 * slices it for the {@code W} register. We can't do that automatically and require the
 * information from {@link vadl.gcb.annotations.CompilerRegisterRenamingAnnotation} and
 * {@link vadl.gcb.annotations.HalfWidthOfAnnotation} to replace {@code S} by the corresponding
 * registers how the compiler needs them.
 */
public class ApplyCompilerRegisterRenamingPass extends Pass {
  /**
   * Constructor.
   */
  public ApplyCompilerRegisterRenamingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("ApplyCompilerRegisterRenamingPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var candidates =
        viam.isa().orElseThrow().artificialResources().stream()
            .filter(x -> x.hasAnnotation(
                CompilerRegisterRenamingAnnotation.class))
            .toList();

    var worklist = new ArrayList<Node>();
    for (var artificialResource : candidates) {
      var inner = artificialResource.innerResourceRef();

      // If we have this annotation then replace Slice + `src`
      // Otherwise, only `src`
      if (artificialResource.hasAnnotation(HalfWidthOfAnnotation.class)) {
        var annotation =
            Objects.requireNonNull(artificialResource.annotation(HalfWidthOfAnnotation.class));
        for (var instruction : viam.isa().orElseThrow().ownInstructions()) {
          for (var behavior : instruction.behaviors()) {
            var matcher = TreeMatcher.matches(
                behavior.getNodes(SliceNode.class).filter(x -> !x.isDeleted()).map(x -> x),
                node -> {
                  if (node instanceof SliceNode sliceNode
                      && sliceNode.bitSlice().lsb() == 0
                      && sliceNode.bitSlice().msb() == annotation.hi()) {
                    if (sliceNode.value() instanceof ReadRegTensorNode readRegTensorNode) {
                      return readRegTensorNode.regTensor().equals(annotation.resource());
                    } else if (sliceNode.value() instanceof ReadArtificialResNode
                        readArtificialResNode) {
                      return readArtificialResNode.resourceDefinition()
                          .equals(annotation.resource());
                    }
                  }
                  return false;
                });

            worklist.addAll(matcher);
          }
        }
      } else {
        for (var instruction : viam.isa().orElseThrow().ownInstructions()) {
          for (var behavior : instruction.behaviors()) {
            var tensorNodes = behavior.getNodes(ReadResourceNode.class)
                .filter(x -> !x.isDeleted())
                .filter(x -> x instanceof ReadsRegisterTensor readsRegisterTensor
                    && readsRegisterTensor.hasRegisterFile()
                    && readsRegisterTensor.registerTensor().equals(inner)).toList();

            worklist.addAll(tensorNodes);
          }
        }
      }

      replace(worklist, artificialResource);
    }

    return null;
  }

  private void replace(List<Node> matchings, ArtificialResource artificialResource) {
    for (var node : matchings) {
      // Nodes can be contained multiple times, but we only need it once.
      if (node.isDeleted()) {
        continue;
      }

      SliceNode sliceNodeValue = null;
      if (node instanceof SliceNode sliceNode) {
        sliceNodeValue = sliceNode;
        node = sliceNode.value();
      }

      if (node instanceof ReadRegTensorNode regTensorNode) {
        regTensorNode.replaceAndDelete(
            new ReadArtificialResNode(artificialResource, regTensorNode.indices(),
                regTensorNode.type()));
      } else if (node instanceof ReadArtificialResNode artificialResNode) {
        artificialResNode.replaceAndDelete(
            new ReadArtificialResNode(artificialResource, artificialResNode.indices(),
                artificialResNode.type()));
      } else if (node instanceof WriteArtificialResNode artificialResNode) {
        artificialResNode.replaceAndDelete(
            new WriteArtificialResNode(artificialResource, artificialResNode.indices(),
                artificialResNode.value(), artificialResNode.condition()));
      } else if (node instanceof WriteRegTensorNode writeTensorNode) {
        writeTensorNode.replaceAndDelete(
            new WriteArtificialResNode(artificialResource, writeTensorNode.indices(),
                writeTensorNode.value()));
      } else {
        throw Diagnostic.error("Cannot replace node", node.location()).build();
      }

      if (sliceNodeValue != null) {
        sliceNodeValue.replaceAndDelete(sliceNodeValue.value());
      }
    }

  }
}