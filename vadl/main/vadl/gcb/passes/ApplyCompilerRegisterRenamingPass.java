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
import vadl.types.BitsType;
import vadl.utils.Pair;
import vadl.viam.ArtificialResource;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.ReadsRegisterTensor;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
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
        viam.isa().map(InstructionSetArchitecture::artificialResources).orElse(List.of())
            .stream()
            .filter(x -> x.hasAnnotation(CompilerRegisterRenamingAnnotation.class))
            .toList();

    for (var artificialResource : candidates) {
      var inner = artificialResource.innerResourceRef();

      // We have to replace the reads ...

      // If we have this annotation then replace Slice + `src`
      // Otherwise, only `src`
      if (artificialResource.hasAnnotation(HalfWidthOfAnnotation.class)) {
        var annotation =
            Objects.requireNonNull(artificialResource.annotation(HalfWidthOfAnnotation.class));
        for (var instruction : viam.isa().orElseThrow().ownInstructions()) {
          for (var behavior : instruction.behaviors()) {
            {
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

              var type = BitsType.bits(annotation.hi() - annotation.lo() + 1);
              for (var sliceNodeRaw : matcher) {
                var sliceNode = (SliceNode) sliceNodeRaw;
                var value = sliceNode.value();
                replaceBy(value, artificialResource, type);
                sliceNode.replaceAndDelete(sliceNode.value());
              }
            }

            {
              var matcher = TreeMatcher.matches(
                  behavior.getNodes(TruncateNode.class).filter(x -> !x.isDeleted()).map(x -> x),
                  node -> {
                    if (node instanceof TruncateNode truncateNode
                        && truncateNode.type().bitWidth() == annotation.hi() + 1) {
                      if (truncateNode.value() instanceof ReadRegTensorNode readRegTensorNode) {
                        return readRegTensorNode.regTensor().equals(annotation.resource());
                      } else if (truncateNode.value() instanceof ReadArtificialResNode
                          readArtificialResNode) {
                        return readArtificialResNode.resourceDefinition()
                            .equals(annotation.resource());
                      }
                    }
                    return false;
                  });

              var type = BitsType.bits(annotation.hi() - annotation.lo() + 1);
              for (var truncateNodeRaw : matcher) {
                var truncateNode = (TruncateNode) truncateNodeRaw;
                var value = truncateNode.value();
                replaceBy(value, artificialResource, type);
                truncateNode.replaceAndDelete(truncateNode.value());
              }
            }
          }
        }
      } else {
        for (var instruction : viam.isa().orElseThrow().ownInstructions()) {
          for (var behavior : instruction.behaviors()) {
            List<Pair<Node, BitsType>> tensorNodes = behavior.getNodes(ReadResourceNode.class)
                .filter(x -> !x.isDeleted())
                .filter(x -> x instanceof ReadsRegisterTensor readsRegisterTensor
                    && readsRegisterTensor.hasRegisterFile()
                    && readsRegisterTensor.registerTensor().equals(inner))
                .map(x -> Pair.of((Node) x, x.type().toBitsType()))
                .toList();

            for (var entry : tensorNodes) {
              var node = entry.left();
              var type = entry.right();
              replaceBy(node, artificialResource, type);
            }
          }
        }
      }

      // but we also have to replace the writes.
      if (artificialResource.hasAnnotation(HalfWidthOfAnnotation.class)) {
        var annotation =
            Objects.requireNonNull(artificialResource.annotation(HalfWidthOfAnnotation.class));
        for (var instruction : viam.isa().orElseThrow().ownInstructions()) {
          for (var behavior : instruction.behaviors()) {
            var matcher = TreeMatcher.matches(
                behavior.getNodes(WriteResourceNode.class).filter(x -> !x.isDeleted()).map(x -> x),
                node -> {
                  var hi = (annotation.hi() + 1) * 2;
                  if (node instanceof WriteResourceNode writeResourceNode
                      && writeResourceNode.value() instanceof ZeroExtendNode zeroExtendNode
                      && zeroExtendNode.type().asDataType().bitWidth() == hi) {
                    if (writeResourceNode instanceof WriteRegTensorNode readRegTensorNode) {
                      return readRegTensorNode.regTensor().equals(annotation.resource());
                    } else if (writeResourceNode instanceof WriteArtificialResNode writeArtificialResNode) {
                      return writeArtificialResNode.resourceDefinition()
                          .equals(annotation.resource());
                    }
                  }

                  if (node instanceof WriteResourceNode writeResourceNode
                      && writeResourceNode.value() instanceof SignExtendNode signExtendNode
                      && signExtendNode.type().asDataType().bitWidth() == hi) {
                    if (writeResourceNode instanceof WriteRegTensorNode readRegTensorNode) {
                      return readRegTensorNode.regTensor().equals(annotation.resource());
                    } else if (writeResourceNode instanceof WriteArtificialResNode writeArtificialResNode) {
                      return writeArtificialResNode.resourceDefinition()
                          .equals(annotation.resource());
                    }

                    if (writeResourceNode.value() instanceof ReadRegTensorNode readRegTensorNode) {
                      return readRegTensorNode.regTensor().equals(annotation.resource());
                    } else if (writeResourceNode.value() instanceof ReadArtificialResNode
                        readArtificialResNode) {
                      return readArtificialResNode.resourceDefinition()
                          .equals(annotation.resource());
                    }
                  }


                  return false;
                });

            var type = BitsType.bits(annotation.hi() - annotation.lo() + 1);
            for (var writeRaw : matcher) {
              var writeNode = (WriteResourceNode) writeRaw;
              var value = writeNode.value();
              replaceBy(writeNode, artificialResource, type);

              if (value instanceof ZeroExtendNode zeroExtendNode) {
                zeroExtendNode.replaceAndDelete(zeroExtendNode.value());
              } else if (value instanceof SignExtendNode signExtendNode) {
                signExtendNode.replaceAndDelete(signExtendNode.value());
              }
            }
          }
        }
      }
    }

    return null;
  }

  private void replaceBy(Node node, ArtificialResource artificialResource, BitsType type) {
    switch (node) {
      case ReadRegTensorNode regTensorNode -> {
        var resNode = new ReadArtificialResNode(artificialResource, regTensorNode.indices(),
            type);
        regTensorNode.replaceAndDelete(resNode);
      }
      case ReadArtificialResNode artificialResNode -> {
        var resNode = new ReadArtificialResNode(artificialResource, artificialResNode.indices(),
            type);
        artificialResNode.replaceAndDelete(resNode);
      }
      case WriteArtificialResNode artificialResNode -> {
        var resNode = new WriteArtificialResNode(artificialResource, artificialResNode.indices(),
            artificialResNode.value(), artificialResNode.nullableCondition());
        artificialResNode.replaceAndDelete(resNode);
      }
      case WriteRegTensorNode writeTensorNode -> {
        var resNode = new WriteArtificialResNode(artificialResource, writeTensorNode.indices(),
            writeTensorNode.value());
        writeTensorNode.replaceAndDelete(resNode);
      }
      default -> throw Diagnostic.error("Cannot replace node", node.location()).build();
    }
  }
}