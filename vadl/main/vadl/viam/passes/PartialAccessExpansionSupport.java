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
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.ArtificialResource;
import vadl.viam.Constant;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

final class PartialAccessExpansionSupport {

  private PartialAccessExpansionSupport() {
  }

  static ExpressionNode expandReadValue(ArtificialResource resource,
                                        List<ExpressionNode> indices) {
    var fullIndices = fullIndices(resource.dimensions(), indices);
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

  static ExpressionNode expandReadValue(RegisterTensor resource,
                                        List<ExpressionNode> indices,
                                        @Nullable vadl.viam.Counter staticCounterAccess) {
    var fullIndices = fullIndices(resource.indexDimensions(), indices);
    ExpressionNode result = null;
    int currentBitWidth = 0;

    for (var indexList : fullIndices) {
      var read = new ReadRegTensorNode(resource, new NodeList<>(indexList),
          resource.resultType(indexList.size()), staticCounterAccess);
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

  static List<SideEffectNode> expandWriteEffects(WriteArtificialResNode write) {
    var resource = write.resourceDefinition();
    var fullIndices = fullIndices(resource.dimensions(), write.indices());
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

  static List<SideEffectNode> expandWriteEffects(WriteRegTensorNode write) {
    var resource = write.resourceDefinition();
    var fullIndices = fullIndices(resource.indexDimensions(), write.indices());
    var result = new ArrayList<SideEffectNode>(fullIndices.size());
    var elementWidth = resource.resultType(resource.indexDimensions().size()).bitWidth();

    for (int i = 0; i < fullIndices.size(); i++) {
      var lsb = i * elementWidth;
      var msb = lsb + elementWidth - 1;
      var slice = new SliceNode(
          write.value(),
          Constant.BitSlice.of(msb, lsb),
          Type.bits(elementWidth)
      );
      slice.setSourceLocationIfNotSet(write.value().location());

      var elementWrite = new WriteRegTensorNode(
          resource,
          new NodeList<>(fullIndices.get(i)),
          slice,
          write.staticCounterAccess(),
          write.nullableCondition() == null ? null : write.nullableCondition().copy()
      );
      elementWrite.setSourceLocationIfNotSet(write.location());
      result.add(elementWrite);
    }

    return result;
  }

  private static List<List<ExpressionNode>> fullIndices(List<RegisterTensor.Dimension> dimensions,
                                                        List<ExpressionNode> indices) {
    var missingDimensions = dimensions.stream().skip(indices.size()).toList();
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
