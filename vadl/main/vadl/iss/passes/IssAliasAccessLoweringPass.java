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
import static vadl.iss.passes.TcgPassUtils.instrInfo;
import static vadl.utils.GraphUtils.intU;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.nodes.IssAliasReadRegTensorNode;
import vadl.iss.passes.nodes.IssAliasWriteRegTensorNode;
import vadl.iss.passes.nodes.IssRegBitfieldWriteNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.BigIntUtils;
import vadl.utils.GraphUtils;
import vadl.utils.Pair;
import vadl.utils.ViamUtils;
import vadl.viam.ArtificialResource;
import vadl.viam.Constant;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.DynSliceNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Rewrites alias register accesses into base-register accesses for ISS without cloning
 * artificial read/write behavior graphs.
 */
public class IssAliasAccessLoweringPass extends AbstractIssPass {

  public IssAliasAccessLoweringPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Alias Access Lowering");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam) throws IOException {
    IdentityHashMap<Graph, Boolean> bitfieldWriteEnabled = new IdentityHashMap<>();
    allInstrs(viam).forEach(instr ->
        bitfieldWriteEnabled.put(instr.behavior(), !instrInfo(instr).asHelperCall()));

    ViamUtils.findAllBehaviors(viam).forEach(behavior ->
        new IssAliasAccessLowering(behavior,
            bitfieldWriteEnabled.getOrDefault(behavior, false)).run());
    return null;
  }
}

class IssAliasAccessLowering {

  private final Graph behavior;
  private final boolean enableBitfieldWriteNode;

  IssAliasAccessLowering(Graph behavior, boolean enableBitfieldWriteNode) {
    this.behavior = behavior;
    this.enableBitfieldWriteNode = enableBitfieldWriteNode;
  }

  void run() {
    behavior.getNodes(ReadArtificialResNode.class).toList().forEach(this::lowerRead);
    behavior.getNodes(WriteArtificialResNode.class).toList().forEach(this::lowerWrite);
    if (behavior.getNodes(ReadArtificialResNode.class).findAny().isPresent()) {
      throw new IllegalStateException(
          "ISS alias lowering left artificial reads in behavior graph.");
    }
  }

  private void lowerRead(ReadArtificialResNode read) {
    var semantics = read.resourceDefinition().semantics();
    var aliasIndices = buildAliasIndices(semantics, read.indices());
    var baseDims = semantics.baseTensor().indexDimensions().size();
    var simpleAliasAccessor = semantics.aliasSlice() == null
        && aliasIndices.size() == baseDims;
    var helperSliceAccessor = !enableBitfieldWriteNode
        && semantics.aliasSlice() != null
        && semantics.aliasSlice().isContinuous()
        && aliasIndices.size() == baseDims
        && read.type().asDataType().bitWidth() <= 64;
    var helperExpansionAccessor = !enableBitfieldWriteNode
        && semantics.aliasSlice() == null
        && aliasIndices.size() > baseDims
        && read.type().asDataType().bitWidth() <= 64;

    if (simpleAliasAccessor || helperSliceAccessor || helperExpansionAccessor) {
      var resourceIndices = helperExpansionAccessor
          ? new NodeList<>(aliasIndices.stream().limit(baseDims).toList())
          : aliasIndices;
      var aliasRead = new IssAliasReadRegTensorNode(
          semantics.baseTensor(),
          resourceIndices,
          semantics.baseTensor().resultType(baseDims),
          read.resourceDefinition().simpleName().toLowerCase(),
          aliasIndices.copy()
      );
      aliasRead.setSourceLocationIfNotSet(read.location());
      read.replaceAndDelete(aliasRead);
      return;
    }

    var readValue = lowerBaseAliasRead(read, semantics, aliasIndices);
    var zeroGuard = buildDontMatchGuard(aliasIndices, semantics);
    if (zeroGuard != null) {
      var zero = Constant.Value.of(0, readValue.type().asDataType()).toNode();
      readValue = new SelectNode(readValue.type(), zeroGuard, readValue, zero);
    }

    if (semantics.aliasSlice() != null) {
      readValue = new SliceNode(
          readValue,
          semantics.aliasSlice(),
          Type.bits(semantics.aliasSlice().bitSize()));
    }

    readValue.setSourceLocationIfNotSet(read.location());
    read.replaceAndDelete(readValue);
  }

  private ExpressionNode lowerBaseAliasRead(ReadArtificialResNode read,
                                            ArtificialResource.Semantics semantics,
                                            NodeList<ExpressionNode> aliasIndices) {
    var baseTensor = semantics.baseTensor();
    var baseIndexCount = baseTensor.indexDimensions().size();

    if (aliasIndices.size() == baseIndexCount) {
      return new ReadRegTensorNode(baseTensor, aliasIndices.copy(),
          baseTensor.resultType(baseIndexCount), null);
    }

    if (aliasIndices.size() < baseIndexCount) {
      throw new IllegalStateException(
          "Unsupported alias compression in ISS for " + read.resourceDefinition().simpleName());
    }

    var baseIndices = new NodeList<ExpressionNode>();
    for (int i = 0; i < baseIndexCount; i++) {
      baseIndices.add(aliasIndices.get(i));
    }
    var baseReadType = baseTensor.resultType(baseIndexCount);
    ExpressionNode baseRead = new ReadRegTensorNode(baseTensor, baseIndices, baseReadType, null);

    var remainingIndices = aliasIndices.stream().skip(baseIndexCount).toList();
    var dynamicConsumed = Math.max(0, baseIndexCount - semantics.fixedIndices().size());
    var remainingDimensions = semantics.dynamicDimensions().stream().skip(dynamicConsumed).toList();

    var resultType = read.type().asDataType();
    ensureExpansionAliasSliceFits(read.resourceDefinition(), baseReadType, resultType,
        remainingDimensions);
    var msbLsb = getMsbAndLsbOfIndexAccess(baseReadType, resultType, remainingIndices,
        remainingDimensions);
    return new DynSliceNode(baseRead, msbLsb.left(), msbLsb.right(), resultType);
  }

  private void lowerWrite(WriteArtificialResNode write) {
    var semantics = write.resourceDefinition().semantics();
    var aliasIndices = buildAliasIndices(semantics, write.indices());
    var baseTensor = semantics.baseTensor();
    var baseIndexCount = baseTensor.indexDimensions().size();
    if (aliasIndices.size() < baseIndexCount) {
      throw new IllegalStateException(
          "Unsupported alias compression in ISS for " + write.resourceDefinition().simpleName());
    }

    var baseIndices = new NodeList<ExpressionNode>();
    for (int i = 0; i < baseIndexCount; i++) {
      baseIndices.add(aliasIndices.get(i));
    }

    var userCondition = write.nullableCondition();
    var guard = buildDontMatchGuard(aliasIndices, semantics);
    var condition = userCondition;
    if (guard != null) {
      condition = condition == null ? guard : BuiltInTable.AND.call(condition, guard);
    }

    var helperAliasWriteAccessor = !enableBitfieldWriteNode
        && write.value().type().asDataType().bitWidth() <= 64
        && semantics.aliasSlice() == null
        && aliasIndices.size() >= baseIndexCount;
    var helperSliceWriteAccessor = !enableBitfieldWriteNode
        && write.value().type().asDataType().bitWidth() <= 64
        && semantics.aliasSlice() != null
        && semantics.aliasSlice().isContinuous()
        && aliasIndices.size() == baseIndexCount;
    if (helperAliasWriteAccessor || helperSliceWriteAccessor) {
      var replacement = new IssAliasWriteRegTensorNode(
          baseTensor,
          baseIndices,
          write.value(),
          write.resourceDefinition().simpleName().toLowerCase(),
          aliasIndices.copy(),
          userCondition
      );
      replacement.setSourceLocationIfNotSet(write.location());
      write.replaceAndDelete(replacement);
      return;
    }

    if (enableBitfieldWriteNode
        && semantics.aliasSlice() != null
        && semantics.aliasSlice().isContinuous()
        && semantics.overwriteMode() == ArtificialResource.OverwriteMode.MERGE
        && aliasIndices.size() == baseIndexCount) {
      var replacement = new IssRegBitfieldWriteNode(
          baseTensor,
          baseIndices,
          write.value(),
          intU(semantics.aliasSlice().lsb(), 32).toNode(),
          intU(semantics.aliasSlice().bitSize(), 32).toNode(),
          write.resourceDefinition().simpleName().toLowerCase(),
          condition
      );
      replacement.setSourceLocationIfNotSet(write.location());
      write.replaceAndDelete(replacement);
      return;
    }

    if (enableBitfieldWriteNode
        && aliasIndices.size() > baseIndexCount
        && semantics.overwriteMode() == ArtificialResource.OverwriteMode.MERGE
        && semantics.aliasSlice() == null) {
      var remainingIndices = aliasIndices.stream().skip(baseIndexCount).toList();
      var dynamicConsumed = Math.max(0, baseIndexCount - semantics.fixedIndices().size());
      var remainingDimensions = semantics.dynamicDimensions().stream().skip(dynamicConsumed)
          .toList();
      var sourceType = baseTensor.resultType(baseIndexCount);
      var resultType = write.value().type().asDataType();
      var msbLsb = getMsbAndLsbOfIndexAccess(sourceType, resultType, remainingIndices,
          remainingDimensions);
      var lsb = msbLsb.right();
      if (isTranslationTimeConstant(lsb)) {
        var replacement = new IssRegBitfieldWriteNode(
            baseTensor,
            baseIndices,
            write.value(),
            lsb,
            intU(resultType.bitWidth(), 32).toNode(),
            null,
            condition
        );
        replacement.setSourceLocationIfNotSet(write.location());
        write.replaceAndDelete(replacement);
        return;
      }
    }

    ExpressionNode writeValue = write.value();
    if (semantics.aliasSlice() != null) {
      var sourceType = baseTensor.resultType(baseIndexCount);
      writeValue = switch (semantics.overwriteMode()) {
        case MERGE -> {
          var current = new ReadRegTensorNode(baseTensor, baseIndices.copy(), sourceType, null);
          yield staticSliceWriteValue(writeValue, current, semantics.aliasSlice());
        }
        case ZERO -> new ZeroExtendNode(writeValue, sourceType);
        case SIGN -> new SignExtendNode(writeValue, sourceType);
      };
    }

    if (aliasIndices.size() > baseIndexCount) {
      var remainingIndices = aliasIndices.stream().skip(baseIndexCount).toList();
      var dynamicConsumed = Math.max(0, baseIndexCount - semantics.fixedIndices().size());
      var remainingDimensions = semantics.dynamicDimensions().stream().skip(dynamicConsumed)
          .toList();
      writeValue = lowerExpansionWrite(
          write.resourceDefinition(),
          baseTensor,
          baseIndices,
          writeValue,
          remainingIndices,
          remainingDimensions
      );
    }

    if (semantics.aliasSlice() == null
        && aliasIndices.size() == baseIndexCount
        && semantics.overwriteMode() == ArtificialResource.OverwriteMode.MERGE) {
      var replacement = new IssAliasWriteRegTensorNode(
          baseTensor,
          baseIndices,
          writeValue,
          write.resourceDefinition().simpleName().toLowerCase(),
          baseIndices.copy(),
          null
      );
      replacement.setSourceLocationIfNotSet(write.location());
      write.replaceAndDelete(replacement);
      return;
    }

    var replacement = new WriteRegTensorNode(baseTensor, baseIndices, writeValue, null, condition);
    replacement.setSourceLocationIfNotSet(write.location());
    write.replaceAndDelete(replacement);
  }

  private ExpressionNode lowerExpansionWrite(ArtificialResource alias,
                                             RegisterTensor baseTensor,
                                             NodeList<ExpressionNode> baseIndices,
                                             ExpressionNode writeValue,
                                             List<ExpressionNode> remainingIndices,
                                             List<RegisterTensor.Dimension> remainingDimensions) {
    var sourceType = baseTensor.resultType(baseTensor.indexDimensions().size());
    var resultType = writeValue.type().asDataType();
    ensureExpansionAliasSliceFits(alias, sourceType, resultType, remainingDimensions);
    var msbLsb = getMsbAndLsbOfIndexAccess(sourceType, resultType, remainingIndices,
        remainingDimensions);

    var maskType = sourceType;
    var msb = new ZeroExtendNode(msbLsb.left(), maskType);
    var lsb = new ZeroExtendNode(msbLsb.right(), maskType);
    var one = Constant.Value.one(maskType).toNode();

    ExpressionNode mask = BuiltInTable.SUB.call(msb, lsb);
    mask = BuiltInTable.ADD.call(mask, one);
    mask = BuiltInTable.LSL.call(one, mask);
    mask = BuiltInTable.SUB.call(mask, one);
    mask = BuiltInTable.LSL.call(mask, lsb);

    var fullMask = Constant.Value.fromInteger(
        BigIntUtils.mask(maskType.bitWidth(), 0),
        maskType
    ).toNode();
    var invertedMask = BuiltInTable.XOR.call(mask, fullMask);

    ExpressionNode original = new ReadRegTensorNode(baseTensor, baseIndices.copy(), maskType, null);
    original = BuiltInTable.AND.call(original, invertedMask);

    writeValue = new ZeroExtendNode(writeValue, maskType);
    writeValue = BuiltInTable.LSL.call(writeValue, lsb);

    return BuiltInTable.OR.call(original, writeValue);
  }

  private NodeList<ExpressionNode> buildAliasIndices(ArtificialResource.Semantics semantics,
                                                     NodeList<ExpressionNode> dynamicIndices) {
    var baseDims = semantics.baseTensor().indexDimensions();
    if (semantics.fixedIndices().size() > baseDims.size()) {
      throw new IllegalStateException(
          "Invalid alias semantics: fixed indices exceed base tensor dimensions");
    }
    if (dynamicIndices.size() > semantics.dynamicDimensions().size()) {
      throw new IllegalStateException(
          "Invalid alias access: provided " + dynamicIndices.size()
              + " dynamic indices, expected at most " + semantics.dynamicDimensions().size());
    }

    var indices = new NodeList<ExpressionNode>();
    for (int i = 0; i < semantics.fixedIndices().size(); i++) {
      var dimType = baseDims.get(i).indexType();
      indices.add(semantics.fixedIndices().get(i).castTo(dimType).toNode());
    }
    indices.addAll(dynamicIndices);
    return indices;
  }

  private @Nullable ExpressionNode buildDontMatchGuard(NodeList<ExpressionNode> indices,
                                                       ArtificialResource.Semantics semantics) {
    var zeroConstraint = semantics.zeroConstraint();
    if (zeroConstraint == null) {
      return null;
    }

    var constraints = zeroConstraint.indices();
    if (constraints.size() > indices.size()) {
      throw new IllegalStateException(
          "Invalid zero-constraint alias semantics for " + semantics.baseTensor().simpleName()
              + ": " + constraints.size() + " constraints for " + indices.size() + " indices");
    }

    var checks = new ArrayList<ExpressionNode>();
    for (int i = 0; i < constraints.size(); i++) {
      var idxExpr = indices.get(i);
      var idxConst = constraints.get(i).zeroExtend(idxExpr.type().asDataType()).toNode();
      checks.add(BuiltInTable.NEQ.call(idxExpr, idxConst));
    }

    if (checks.isEmpty()) {
      return Constant.Value.of(false).toNode();
    }
    if (checks.size() == 1) {
      return checks.getFirst();
    }
    return BuiltInTable.OR.call(checks.toArray(ExpressionNode[]::new));
  }

  private Pair<ExpressionNode, ExpressionNode> getMsbAndLsbOfIndexAccess(
      DataType sourceValueType,
      DataType resultType,
      List<ExpressionNode> indices,
      List<RegisterTensor.Dimension> dimensions) {
    var sourceSize = sourceValueType.bitWidth();
    var maxLiteral = Math.max((long) sourceSize, (long) resultType.bitWidth() - 1);
    for (int i = 0; i < indices.size(); i++) {
      maxLiteral = Math.max(maxLiteral, strideForVirtualIndex(resultType, dimensions, i));
    }
    var sliceType = Type.bits(BitsType.indexWidthFor(Math.addExact(maxLiteral, 1)));
    ExpressionNode lsb = Constant.Value.of(0, sliceType).toNode();
    for (int i = 0; i < indices.size(); i++) {
      var indexExpr = new ZeroExtendNode(indices.get(i), sliceType);
      var p = strideForVirtualIndex(resultType, dimensions, i);
      var multiplication = BuiltInTable.MUL.call(
          indexExpr,
          Constant.Value.of(p, sliceType).toNode()
      );
      lsb = BuiltInTable.ADD.call(lsb, multiplication);
    }
    ExpressionNode msb = BuiltInTable.ADD.call(lsb,
        Constant.Value.of(resultType.bitWidth() - 1, sliceType).toNode());

    return Pair.of(msb, lsb);
  }

  private long strideForVirtualIndex(DataType resultType,
                                     List<RegisterTensor.Dimension> dimensions,
                                     int index) {
    long stride = resultType.bitWidth();
    for (var dim : dimensions.stream().skip(index + 1).toList()) {
      stride = Math.multiplyExact(stride, dim.size());
    }
    return stride;
  }

  private boolean isTranslationTimeConstant(ExpressionNode expr) {
    return !GraphUtils.hasDependencies(expr, dep -> dep instanceof ReadResourceNode);
  }

  private void ensureExpansionAliasSliceFits(ArtificialResource alias,
                                             DataType sourceValueType,
                                             DataType resultType,
                                             List<RegisterTensor.Dimension> remainingDimensions) {
    long coveredBits = resultType.bitWidth();
    for (var dim : remainingDimensions) {
      coveredBits = Math.multiplyExact(coveredBits, dim.size());
    }
    if (coveredBits > sourceValueType.bitWidth()) {
      throw new IllegalStateException(
          "Invalid expansion alias mapping for "
              + alias.simpleName()
              + ": virtual alias indexing may address "
              + coveredBits
              + " bits, but source read provides only "
              + sourceValueType.bitWidth()
              + " bits."
      );
    }
  }

  private ExpressionNode staticSliceWriteValue(ExpressionNode value,
                                               ExpressionNode entireRead,
                                               Constant.BitSlice slice) {
    ExpressionNode injected = null;
    int consumed = 0;

    var parts = new ArrayList<>(slice.parts().toList());
    for (int i = parts.size() - 1; i >= 0; i--) {
      var part = parts.get(i);
      var shiftedValue = consumed == 0 ? value :
          BuiltInCall.of(BuiltInTable.LSR, value, intU(consumed, 32).toNode());
      ExpressionNode partValue = new TruncateNode(shiftedValue, Type.bits(part.size()));
      partValue = new ZeroExtendNode(partValue, entireRead.type().asDataType());
      var placed = part.lsb() == 0
          ? partValue
          : BuiltInCall.of(BuiltInTable.LSL, partValue, intU(part.lsb(), 32).toNode());
      injected = injected == null ? placed : BuiltInCall.of(BuiltInTable.OR, injected, placed);
      consumed += part.size();
    }

    var mask = slice.mask().castTo(Type.bits(entireRead.type().asDataType().bitWidth())).not()
        .toNode();
    var cleared = BuiltInCall.of(BuiltInTable.AND, entireRead, mask);
    return BuiltInCall.of(BuiltInTable.OR, cleared, requireNonNull(injected));
  }
}
