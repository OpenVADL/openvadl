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

package vadl.viam.passes;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.utils.ViamUtils;
import vadl.viam.ArtificialResource;
import vadl.viam.Constant;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;

/**
 * Expands partial alias accesses into the concrete concatenated/sliced access shape.
 *
 * <p>The frontend now preserves partial alias accesses such as {@code V(vd)} as one
 * {@link ReadArtificialResNode} / {@link WriteArtificialResNode}. This pass restores the old VIAM
 * shape by enumerating the missing alias dimensions and lowering them to the exact read/write
 * sequence that downstream passes already understand.</p>
 */
public class ArtificialResPartialAccessExpansionPass extends Pass {

  public ArtificialResPartialAccessExpansionPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("Artificial Resource Partial Access Expansion");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    ViamUtils.findAllBehaviors(viam).forEach(this::expandInBehavior);
    return null;
  }

  private void expandInBehavior(Graph behavior) {
    behavior.getNodes(ReadArtificialResNode.class)
        .filter(this::isPartialAccess)
        .toList()
        .forEach(this::expandRead);
    behavior.getNodes(WriteArtificialResNode.class)
        .filter(this::isPartialAccess)
        .toList()
        .forEach(this::expandWrite);
  }

  private boolean isPartialAccess(ReadArtificialResNode read) {
    return read.indices().size() < read.resourceDefinition().dimensions().size();
  }

  private boolean isPartialAccess(WriteArtificialResNode write) {
    return write.indices().size() < write.resourceDefinition().dimensions().size();
  }

  private void expandRead(ReadArtificialResNode read) {
    var replacement = expandReadValue(read.resourceDefinition(), read.indices());
    replacement.setSourceLocationIfNotSet(read.location());
    read.replaceAndDelete(replacement);
  }

  private void expandWrite(WriteArtificialResNode write) {
    var ends = write.usages()
        .filter(AbstractEndNode.class::isInstance)
        .map(AbstractEndNode.class::cast)
        .toList();
    var replacementByEnd = ends.stream()
        .map(end -> expandWriteEffects(write))
        .toList();

    for (int i = 0; i < ends.size(); i++) {
      var end = ends.get(i);
      var replacements = replacementByEnd.get(i);
      replacements.forEach(effect -> write.ensureGraph().addWithInputs(effect));
      end.replaceSideEffect(write, replacements);
    }

    if (write.usageCount() == 0) {
      write.safeDelete();
    }
  }

  private ExpressionNode expandReadValue(ArtificialResource resource,
                                         List<ExpressionNode> indices) {
    var fullIndices = fullIndices(resource, indices);
    ExpressionNode result = null;
    int currentBitWidth = 0;

    for (var indexList : fullIndices) {
      var read = new ReadArtificialResNode(resource, new NodeList<>(indexList),
          resource.resultType(indexList.size()));
      currentBitWidth += read.type().bitWidth();
      if (result == null) {
        result = read;
      } else {
        result = new BuiltInCall(BuiltInTable.CONCATENATE_BITS, new NodeList<>(read, result),
            Type.bits(currentBitWidth));
      }
    }

    return requireNonNull(result);
  }

  private List<SideEffectNode> expandWriteEffects(WriteArtificialResNode write) {
    var resource = write.resourceDefinition();
    var fullIndices = fullIndices(resource, write.indices());
    var result = new ArrayList<SideEffectNode>(fullIndices.size());
    var elementWidth = resource.resultType(resource.dimensions().size()).bitWidth();

    for (int i = 0; i < fullIndices.size(); i++) {
      var lsb = i * elementWidth;
      var msb = lsb + elementWidth - 1;
      var slice = new SliceNode(
          write.value(),
          Constant.BitSlice.of(msb, lsb),
          Type.bits(elementWidth)
      );
      slice.setSourceLocationIfNotSet(write.value().location());

      var elementWrite = new WriteArtificialResNode(
          resource,
          new NodeList<>(fullIndices.get(i)),
          slice,
          write.nullableCondition() == null ? null : write.nullableCondition().copy()
      );
      elementWrite.setSourceLocationIfNotSet(write.location());
      result.add(elementWrite);
    }

    return result;
  }

  private List<List<ExpressionNode>> fullIndices(ArtificialResource resource,
                                                 List<ExpressionNode> indices) {
    var missingDimensions = resource.dimensions().stream().skip(indices.size()).toList();
    var missingTypes = missingDimensions.stream().map(RegisterTensor.Dimension::indexType)
        .toList();
    var missingPermutations = permutationOfTensorIndices(
        missingDimensions.stream().map(RegisterTensor.Dimension::size).toList());

    var missingIndices = missingPermutations.stream()
        .map(entry -> Streams.zip(entry.stream(), missingTypes.stream(),
            (value, type) -> (ExpressionNode) Constant.Value.of(value, type).toNode()).toList())
        .toList();

    return missingIndices.stream().map(item ->
        Streams.concat(indices.stream(), item.stream()).toList()
    ).toList();
  }

  private static List<List<Long>> permutationOfTensorIndices(List<Integer> dimensionSizes) {
    if (dimensionSizes.isEmpty()) {
      return List.of(List.of());
    }

    var currentSize = dimensionSizes.getFirst();
    var suffixPermutations = permutationOfTensorIndices(dimensionSizes.subList(1,
        dimensionSizes.size()));
    var result = new ArrayList<List<Long>>(currentSize * suffixPermutations.size());
    for (long i = 0; i < currentSize; i++) {
      for (var suffix : suffixPermutations) {
        var entry = new ArrayList<Long>(1 + suffix.size());
        entry.add(i);
        entry.addAll(suffix);
        result.add(entry);
      }
    }
    return result;
  }
}
