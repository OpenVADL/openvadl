// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.common.opDecomposition;

import static vadl.utils.StreamUtils.only;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.ArrayList;
import java.util.HashSet;
import javax.annotation.Nullable;
import vadl.iss.passes.common.opDecomposition.decomposer.ArithmeticDecomposer;
import vadl.iss.passes.common.opDecomposition.decomposer.CountDecomposer;
import vadl.iss.passes.common.opDecomposition.decomposer.FoldDecomposer;
import vadl.iss.passes.common.opDecomposition.decomposer.LogicDecomposer;
import vadl.iss.passes.common.opDecomposition.decomposer.ShiftDecomposer;
import vadl.iss.passes.common.opDecomposition.decomposer.TensorDecomposer;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.utils.VadlBuiltInEmptyNoStatusDispatcher;
import vadl.viam.ArtificialResource;
import vadl.viam.Constant;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DynSliceNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LabelNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.OperationExistsNode;
import vadl.viam.graph.dependency.OperationForAllNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.StructGetFieldNode;
import vadl.viam.graph.dependency.TensorNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * The decomposer is designed as a request system, where one node requests
 * a slice from a dependency node.
 * The slice's width must be smaller/equal than the target size.
 * The request is dispatched to the correct implementation, which than takes care of
 * returning an expression that represents the result of the requested slice.
 * It must only use operations that are smaller equal the target size.
 *
 * <p>For register accesses this pass emits unified {@link IssReadRegNode}/{@link IssWriteRegNode}
 * nodes with chunk window metadata instead of backend-specific chunk node types.
 * This keeps one register-access representation across decomposition, accessor-descriptor retrieval
 * and code generation.
 *
 * <p>See {@code docs/iss/register-access-domain-map.md}.
 */
@DispatchFor(
    value = ExpressionNode.class,
    include = "vadl.viam",
    context = Decomposer.Request.class
)
@SuppressWarnings("OverloadMethodsDeclarationOrder")
class Decomposer
    implements VadlBuiltInEmptyNoStatusDispatcher<Decomposer.Request>, ShiftDecomposer,
    LogicDecomposer, CountDecomposer,
    ArithmeticDecomposer, TensorDecomposer, FoldDecomposer {

  record Slice(int hi, int lo) {
    int width() {
      return hi - lo + 1;
    }

    Slice shift(int n) {
      return new Slice(hi + n, lo + n);
    }
  }

  static class Request {
    Slice slice;
    /**
     * The result of the decomposition request.
     * The passed result will be used to replace the original expression.
     */
    @Nullable
    ExpressionNode result;

    public Request(Slice slice) {
      this.slice = slice;
    }
  }

  @LazyInit
  BuiltInCall currCall;
  int targetSize;

  public Decomposer(int targetSize) {
    this.targetSize = targetSize;
  }

  /**
   * Decompose the expression by requesting its substitute and replace it by the substitute.
   * The return type of the requested node must be <= the target size.
   *
   * @param expr the expression to decompose (will be replaced by the decomposed version)
   */
  void decompose(ExpressionNode expr) {
    var exprW = expr.type().asDataType().bitWidth();
    // we must start a decomposition with a node that has a valid return type size
    expr.ensure(exprW <= targetSize, "Can only decompose expr that fits in target size.");
    // start the request with the expressions' normal size
    var repl = internalRequest(expr, new Slice(exprW - 1, 0));
    if (repl == expr) {
      // if the replacement is the same as the original, we can skip the replace
      return;
    }
    ensureSubgraphWithinTarget(repl);
    expr.replaceAndDelete(repl);
  }

  /**
   * Ensures that the decomposition result subgraph does not contain non-read expressions that
   * exceed the target width. This catches illegal decomposition outputs early.
   */
  private void ensureSubgraphWithinTarget(ExpressionNode root) {
    var nodes = new ArrayList<ExpressionNode>();
    root.collectInputsWithChildren(nodes, ExpressionNode.class);

    var visited = new HashSet<ExpressionNode>();
    visited.add(root);
    visited.addAll(nodes);

    for (var node : visited) {
      if (node.isDeleted()) {
        continue;
      }
      if (!(node.type() instanceof DataType dt)) {
        continue;
      }
      node.ensure(dt.bitWidth() <= targetSize,
          "Decomposer produced node above target width: %s (%d > %d)",
          node, dt.bitWidth(), targetSize);
    }
  }

  void decompose(SideEffectNode sideEffect) {
    switch (sideEffect) {
      case WriteMemNode w -> handle(w);
      case WriteRegTensorNode w -> handle(w);
      default -> sideEffect.fail("Unexpected side effect to handle: %s", sideEffect.getClass());
    }
  }

  @Override
  public int targetSize() {
    return targetSize;
  }

  @Override
  public ExpressionNode request(ExpressionNode node, int msb, int lsb) {
    return request(node, new Slice(msb, lsb));
  }

  /**
   * Helper to extract a slice from a chunk and accumulate it into the result.
   * Handles source location copying and concatenation logic.
   */
  @SuppressWarnings("LocalVariableName")
  private ExpressionNode accumulateChunk(
      @Nullable
      ExpressionNode result,
      ExpressionNode chunkRead,
      Slice requestedSlice,
      int chunkLsb,
      int chunkWidth,
      ExpressionNode sourceNode
  ) {
    chunkRead.setSourceLocation(sourceNode.location());

    // Extract the requested slice from this chunk
    int sliceInChunk_lo = Math.max(0, requestedSlice.lo() - chunkLsb);
    int sliceInChunk_hi = Math.min(chunkWidth - 1, requestedSlice.hi() - chunkLsb);

    var chunkPart = new SliceNode(chunkRead,
        Constant.BitSlice.of(sliceInChunk_hi, sliceInChunk_lo),
        Type.bits(sliceInChunk_hi - sliceInChunk_lo + 1));

    // Concatenate chunks (or use single chunk if that's all we need)
    return result == null ? chunkPart : GraphUtils.concat(chunkPart, result);
  }

  private ExpressionNode request(ExpressionNode node, Slice slice) {
    var nodeWidth = node.type().asDataType().bitWidth();
    if (nodeWidth <= targetSize) {
      // For already-mappable nodes, avoid recursive decomposition completely and
      // just materialize the requested slice directly.
      if (slice.lo() == 0 && nodeWidth - 1 == slice.hi()) {
        return node;
      }
      return GraphUtils.slice(node, slice.hi(), slice.lo());
    }

    return internalRequest(node, slice);
  }

  private ExpressionNode internalRequest(ExpressionNode node, Slice slice) {
    var req = new Request(slice);
    DecomposerDispatcher.dispatch(this, req, node);
    if (req.result == null) {
      node.fail("No decomposition result was produced for %s", node.getClass().getSimpleName());
    }
    return req.result;
  }

  void handle(WriteMemNode write) {
    int writeWidth = write.writeBitWidth();
    if (writeWidth <= targetSize) {
      return; // fits in target size, no decomposition needed
    }

    var memory = write.memory();
    var wordSize = memory.wordSize();
    var address = write.address();
    var value = write.value();
    var condition = write.nullableCondition();
    var graph = write.ensureGraph();

    var ends = write.usages().gather(only(AbstractEndNode.class)).toList();

    // Calculate how many target-sized chunks we need
    int chunksNeeded = (writeWidth + targetSize - 1) / targetSize;

    // Split the write into multiple smaller writes
    for (int i = 0; i < chunksNeeded; i++) {
      int lsb = i * targetSize;
      int msb = Math.min(lsb + targetSize - 1, writeWidth - 1);
      int chunkWidth = msb - lsb + 1;

      // Extract slice of the value
      var chunkValue = request(value, msb, lsb);

      // Calculate address offset (in words)
      int byteOffset = lsb / 8;
      int wordOffset = byteOffset / (wordSize / 8);

      // Create address with offset
      var offsetConst = Constant.Value.of(
          wordOffset,
          address.type().asDataType()
      ).toNode();
      var adjustedAddress = wordOffset == 0 ? address :
          graph.addWithInputs(BuiltInTable.ADD.call(address, offsetConst));

      // Calculate words for this chunk
      int chunkWords = (chunkWidth + wordSize - 1) / wordSize;

      // Create new WriteMemNode for this chunk
      var chunkWrite = graph.addWithInputs(
          new WriteMemNode(memory, chunkWords, adjustedAddress, chunkValue, condition));

      // Preserve source location
      chunkWrite.setSourceLocation(write.location());

      for (var end : ends) {
        end.addSideEffect(chunkWrite);
      }
    }

    // Delete the original large write
    removeFromAllEndSideEffects(write);
    write.safeDelete();
  }

  void handle(WriteRegTensorNode write) {
    int writeWidth = write.writeBitWidth();
    if (writeWidth <= targetSize) {
      return; // fits in target size, no decomposition needed
    }

    var regTensor = write.regTensor();
    var value = write.value();
    var condition = write.nullableCondition();
    var graph = write.ensureGraph();

    var ends = write.usages().gather(only(AbstractEndNode.class)).toList();
    for (int chunkOffset = 0; chunkOffset < writeWidth; chunkOffset += targetSize) {
      int chunkWidth = Math.min(targetSize, writeWidth - chunkOffset);
      int chunkMsb = chunkOffset + chunkWidth - 1;
      var chunkValue = request(value, chunkMsb, chunkOffset);
      var chunkWrite = graph.addWithInputs(new IssWriteRegNode(
          regTensor,
          write.indices().copy(),
          chunkValue,
          write.staticCounterAccess(),
          condition,
          writeAccessKind(write),
          writeGuardKind(write),
          writeAccessorName(write),
          writeAliasResource(write),
          writeAccessorIndices(write),
          IssWriteRegNode.WindowKind.CHUNK,
          Constant.Value.of(chunkOffset, Type.bits(32)).toNode(),
          Constant.Value.of(chunkWidth, Type.bits(32)).toNode()
      ));
      chunkWrite.setSourceLocation(write.location());

      for (var end : ends) {
        end.addSideEffect(chunkWrite);
      }
    }

    // Delete the original large write
    removeFromAllEndSideEffects(write);
    write.safeDelete();
  }

  private void removeFromAllEndSideEffects(SideEffectNode sideEffect) {
    var removed = true;
    while (removed) {
      removed = false;
      for (var usage : sideEffect.usages().toList()) {
        if (usage instanceof AbstractEndNode end && end.sideEffects().contains(sideEffect)) {
          end.removeSideEffect(sideEffect);
          removed = true;
        }
      }
    }
  }

  @Override
  public void handleConcat(Request req) {
    var lsb = req.slice.lo();
    var msb = req.slice.hi();
    var loVal = currCall.arg(1);
    var hiVal = currCall.arg(0);
    var loValWidth = loVal.type().asDataType().bitWidth();
    if (lsb >= loValWidth) {               // slice lies completely in HI part
      req.result = request(hiVal, req.slice.shift(-loValWidth));
    } else if (msb < loValWidth) {         // slice lies completely in LO part
      req.result = request(loVal, req.slice);
    } else {                               // slice spans the boundary
      var loSlice = new Slice(loValWidth - 1, lsb);           // 63…lsb
      var hiSlice = new Slice(msb - loValWidth, 0);           // (msb-64)…0
      var newLo = request(loVal, loSlice);
      var newHi = request(hiVal, hiSlice);
      req.result = GraphUtils.concat(newHi, newLo);
    }
  }


  @Handler
  void handle(Request rq, TruncateNode t) {
    // width of the truncated value (= result width)
    int outW = t.type().asDataType().bitWidth();

    // slice must lie completely inside the truncated range
    if (rq.slice.hi() >= outW) {
      throw new IllegalArgumentException(
          "requested bits " + rq.slice + " exceed " + outW + "-bit truncate");
    }

    /* forward the same slice to the input of the truncate */
    rq.result = request(t.value(), rq.slice);
  }

  @Handler
  void handle(Request rq, SignExtendNode se) {
    int fromW = se.fromBitWidth();       // width before extension
    int lsb = rq.slice.lo();
    int msb = rq.slice.hi();
    var src = se.value();                // original value

    if (msb < fromW) {
      // slice lies completely inside the original value
      rq.result = request(src, rq.slice);

    } else if (lsb >= fromW) {
      // slice lies completely in the replicated sign-bit area
      var signBit = request(src, new Slice(fromW - 1, fromW - 1));
      rq.result = GraphUtils.signExtend(signBit, Type.bits(rq.slice.width()));
    } else {
      // slice crosses the boundary
      var lowSlice = new Slice(fromW - 1, lsb);                // inside src
      var highSize = msb - fromW + 1;                          // #sign bits
      var lowPart = request(src, lowSlice);
      var signBit = request(src, new Slice(fromW - 1, fromW - 1));
      var highPart = GraphUtils.signExtend(signBit, Type.bits(highSize));
      rq.result = GraphUtils.concat(highPart, lowPart);     // (hi, lo)
    }
  }

  @Override
  public void handleLSR(Request rq) {
    rq.result = lsrDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  @Override
  public void handleLSL(Request rq) {
    rq.result = lslDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  @Override
  public void handleNOT(Request rq) {
    rq.result = notDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  @Override
  public void handleOR(Request rq) {
    rq.result = orDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  @Override
  public void handleAND(Request rq) {
    rq.result = andDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  @Override
  public void handleXOR(Request rq) {
    rq.result = xorDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  @Override
  public void handleADD(Request rq) {
    rq.result = addDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  @Override
  public void handleSUB(Request rq) {
    rq.result = subDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  @Override
  public void handleUMULL(Request rq) {
    rq.result = mullDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  @Override
  public void handleSMULL(Request rq) {
    rq.result = mullDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  @Override
  public void handleSUMULL(Request rq) {
    rq.result = mullDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  @Override
  public void handleUDIV(Request rq) {
    rq.result = udivDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  @Override
  public void handleCTZ(Request rq) {
    rq.result = ctzDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  @Override
  public void handleCLZ(Request rq) {
    rq.result = clzDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  @Override
  public void handleCOB(Request rq) {
    rq.result = cobDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  @Override
  public void handleUMOD(Request rq) {
    rq.result = umodDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }

  private void handleCompare(Request rq) {
    // Compare built-ins produce a single result bit. When a wider slice is requested,
    // bit 0 carries the compare result and higher bits are zero.
    if (rq.slice.lo() > 0) {
      rq.result = Constant.Value.zero(Type.bits(rq.slice.width())).toNode();
      return;
    }

    var lhs = currCall.arg(0);
    var rhs = currCall.arg(1);
    var width = lhs.type().asDataType().bitWidth();

    ExpressionNode cmpBit = null;
    for (int chunkLo = 0; chunkLo < width; chunkLo += targetSize) {
      int chunkHi = Math.min(chunkLo + targetSize - 1, width - 1);
      var lhsChunk = request(lhs, chunkHi, chunkLo);
      var rhsChunk = request(rhs, chunkHi, chunkLo);
      var chunkCmp = currCall.builtIn() == BuiltInTable.EQU
          ? GraphUtils.equ(lhsChunk, rhsChunk)
          : GraphUtils.neq(lhsChunk, rhsChunk);
      if (cmpBit == null) {
        cmpBit = chunkCmp;
      } else {
        cmpBit = currCall.builtIn() == BuiltInTable.EQU
            ? GraphUtils.and(cmpBit, chunkCmp)
            : GraphUtils.or(cmpBit, chunkCmp);
      }
    }

    // Defensive fallback for zero-width compare inputs (should not happen for valid data types).
    if (cmpBit == null) {
      cmpBit = Constant.Value.one(Type.bool()).toNode();
    }

    cmpBit = GraphUtils.truncate(cmpBit, Type.bits(1));
    if (rq.slice.width() == 1) {
      rq.result = cmpBit;
      return;
    }

    rq.result = GraphUtils.zeroExtend(cmpBit, Type.bits(rq.slice.width()));
  }

  @Override
  public void handleEQU(Request rq) {
    handleCompare(rq);
  }

  @Override
  public void handleNEQ(Request rq) {
    handleCompare(rq);
  }


  @Handler
  void handle(Request rq, MiaBuiltInCall toHandle) {
    throw new UnsupportedOperationException("Type MiaBuiltInCall not yet implemented");
  }

  @Handler
  void handle(Request rq, ReadRegTensorNode toHandle) {
    if (toHandle instanceof IssReadRegNode readRegNode) {
      handle(rq, readRegNode);
      return;
    }
    toHandle.ensure(false,
        "Unexpected ReadRegTensorNode in ISS op decomposition; expected IssReadRegNode.");
  }

  void handle(Request rq, IssReadRegNode toHandle) {
    var regTensor = toHandle.regTensor();
    var readWidth = toHandle.readBitWidth();

    // Ensure the requested slice is within bounds
    toHandle.ensure(rq.slice.hi() < readWidth,
        "Requested slice [%d:%d] exceeds register width of %d bits",
        rq.slice.hi(), rq.slice.lo(), readWidth);

    // If read fits in target size, just slice it
    if (readWidth <= targetSize) {
      rq.result = new SliceNode(toHandle,
          Constant.BitSlice.of(rq.slice.hi(), rq.slice.lo()),
          Type.bits(rq.slice.width()));
      return;
    }

    // Need to decompose: read one or more target-sized windows.
    // We emit exact requested windows directly in IssReadRegNode chunk metadata,
    // so no follow-up SliceNode is necessary.
    int firstChunkIdx = rq.slice.lo() / targetSize;
    int lastChunkIdx = rq.slice.hi() / targetSize;
    ExpressionNode result = null;

    for (int chunkIdx = firstChunkIdx; chunkIdx <= lastChunkIdx; chunkIdx++) {
      int chunkOffset = chunkIdx * targetSize;
      int chunkWidth = Math.min(targetSize, readWidth - chunkOffset);
      int requestedLsbInChunk = Math.max(rq.slice.lo(), chunkOffset);
      int requestedMsbInChunk = Math.min(rq.slice.hi(), chunkOffset + chunkWidth - 1);
      int requestedWidth = requestedMsbInChunk - requestedLsbInChunk + 1;
      var chunkRead = new IssReadRegNode(
          regTensor,
          toHandle.indices().copy(),
          Type.bits(requestedWidth).asDataType(),
          toHandle.staticCounterAccess(),
          readAccessKind(toHandle),
          readShape(toHandle),
          readAccessorName(toHandle),
          readAliasResource(toHandle),
          readAccessorIndices(toHandle),
          IssReadRegNode.WindowKind.CHUNK,
          Constant.Value.of(requestedLsbInChunk, Type.bits(32)).toNode(),
          Constant.Value.of(requestedWidth, Type.bits(32)).toNode()
      );
      chunkRead.setSourceLocation(toHandle.location());
      result = result == null ? chunkRead : GraphUtils.concat(chunkRead, result);
    }

    rq.result = result;
  }

  @Handler
  void handle(Request rq, FieldRefNode toHandle) {
    throw new UnsupportedOperationException("Type FieldRefNode not yet implemented");
  }

  @Handler
  void handle(Request rq, FuncCallNode toHandle) {
    throw new UnsupportedOperationException("Type FuncCallNode not yet implemented");
  }

  @Handler
  void handle(Request rq, ReadStageOutputNode toHandle) {
    throw new UnsupportedOperationException("Type ReadStageOutputNode not yet implemented");
  }

  @Handler
  void handle(Request rq, LabelNode toHandle) {
    throw new UnsupportedOperationException("Type LabelNode not yet implemented");
  }

  @Handler
  void handle(Request rq, BuiltInCall toHandle) {
    var previousCall = currCall;
    currCall = toHandle;
    dispatch(rq, toHandle.builtIn());
    currCall = previousCall;
  }

  @Handler
  void handle(Request rq, ConstantNode toHandle) {
    var val = toHandle.constant().asVal();
    var sliced = val.slice(Constant.BitSlice.of(rq.slice.hi(), rq.slice.lo()));
    rq.result = sliced.toNode();
  }

  @Handler
  void handle(Request rq, FieldAccessRefNode toHandle) {
    throw new UnsupportedOperationException("Type FieldAccessRefNode not yet implemented");
  }

  @Handler
  void handle(Request rq, ReadMemNode toHandle) {
    int readWidth = toHandle.readBitWidth();
    var memory = toHandle.memory();
    var wordSize = memory.wordSize();
    var address = toHandle.address();

    // If the entire read fits in target size, just slice it
    if (readWidth <= targetSize) {
      rq.result = new SliceNode(toHandle,
          Constant.BitSlice.of(rq.slice.hi(), rq.slice.lo()),
          Type.bits(rq.slice.width()));
      return;
    }

    // Determine which chunks contain our requested slice
    int chunkSize = targetSize;
    int chunkStartIdx = rq.slice.lo() / chunkSize;
    int chunkEndIdx = rq.slice.hi() / chunkSize;

    var graph = toHandle.ensureGraph();
    ExpressionNode result = null;

    // Process all chunks that contain parts of the requested slice
    for (int chunkIdx = chunkStartIdx; chunkIdx <= chunkEndIdx; chunkIdx++) {
      int chunkLsb = chunkIdx * chunkSize;
      int chunkMsb = Math.min(chunkLsb + chunkSize - 1, readWidth - 1);
      int chunkWidth = chunkMsb - chunkLsb + 1;

      // Calculate address offset (in words)
      int byteOffset = chunkLsb / 8;
      int wordOffset = byteOffset / (wordSize / 8);
      int chunkWords = (chunkWidth + wordSize - 1) / wordSize;

      // Create address with offset
      var offsetConst = Constant.Value.of(wordOffset, address.type().asDataType()).toNode();
      var adjustedAddress = wordOffset == 0 ? address :
          graph.addWithInputs(BuiltInTable.ADD.call(address, offsetConst));

      // Create read for this chunk
      var chunkRead = new ReadMemNode(memory, chunkWords, adjustedAddress,
          Type.bits(chunkWidth).asDataType());

      result = accumulateChunk(result, chunkRead, rq.slice, chunkLsb, chunkWidth, toHandle);
    }

    rq.result = result;
  }

  @Handler
  void handle(Request rq, ReadArtificialResNode toHandle) {
    throw new UnsupportedOperationException("Type ReadArtificialResNode not yet implemented");
  }

  @Handler
  void handle(Request rq, StructGetFieldNode toHandle) {
    throw new UnsupportedOperationException("Type StructGetFieldNode not yet implemented");
  }

  @Handler
  void handle(Request rq, AsmBuiltInCall toHandle) {
    throw new UnsupportedOperationException("Type AsmBuiltInCall not yet implemented");
  }

  @Handler
  void handle(Request rq, FuncParamNode toHandle) {
    toHandle.fail("FuncParamNode not yet supported in decomposing ISS ops");
  }

  @Handler
  void handle(Request rq, ZeroExtendNode toHandle) {
    var value = toHandle.value();
    var valueWidth = value.type().asDataType().bitWidth();
    if (rq.slice.hi() < valueWidth) {
      // if the request is only interested in the original value, we just propagate
      // the request to the original value.
      rq.result = request(value, rq.slice);
    } else if (rq.slice.lo() >= valueWidth) {
      // if the request is only interested in the zero-extended part, the result is 0
      rq.result = Constant.Value.zero(Type.bits(rq.slice.width())).toNode();
    } else {
      // else we request the value part and zero extend it to the requested width.
      var valuePart = request(value, valueWidth - 1, rq.slice.lo());
      rq.result = GraphUtils.zeroExtend(valuePart, Type.bits(rq.slice.width()));
    }
  }

  @Handler
  void handle(Request rq, LetNode toHandle) {
    throw new UnsupportedOperationException("Type LetNode not yet implemented");
  }

  @Handler
  void handle(Request rq, SliceNode toHandle) {
    var bitSlice = toHandle.bitSlice();
    toHandle.ensure(bitSlice.isContinuous(),
        "Decomposing slices with multiple parts is not yet implemented");

    var slice = bitSlice.parts().findFirst().get();
    // the request slice defines indices on the slice node's result value type.
    // we can normalize this to the slice node's value type by shifting the indices
    // by the slice node's lsb value, such that the requested window stays the same
    // while removing the intermediate slice node.
    var newReqSlice = rq.slice.shift(slice.lsb());

    // we request the adjusted request slice from the slice node's value.
    rq.result = request(toHandle.value(), newReqSlice);
  }

  @Handler
  void handle(Request rq, DynSliceNode toHandle) {
    // A common oversized pattern is a target-sized dynamic slice on top of a wide register read.
    // Represent it directly as a chunk-window IssReadRegNode so helper/TCG paths avoid full-wide
    // preloads.
    if (toHandle.value() instanceof IssReadRegNode readRegNode) {
      var requestedWidth = rq.slice.width();
      var lsbType = toHandle.lsb().type().asDataType();
      var lsbOffset = rq.slice.lo() == 0
          ? toHandle.lsb()
          : toHandle.ensureGraph().addWithInputs(BuiltInTable.ADD.call(
          toHandle.lsb(),
          Constant.Value.of(rq.slice.lo(), lsbType).toNode()));

      var chunkRead = new IssReadRegNode(
          readRegNode.regTensor(),
          readRegNode.indices().copy(),
          Type.bits(requestedWidth).asDataType(),
          readRegNode.staticCounterAccess(),
          readRegNode.accessKind(),
          readRegNode.readShape(),
          readRegNode.accessorName(),
          readRegNode.aliasResource(),
          new NodeList<>(readRegNode.accessorIndices()),
          IssReadRegNode.WindowKind.CHUNK,
          lsbOffset,
          Constant.Value.of(requestedWidth, Type.bits(32)).toNode());
      chunkRead.setSourceLocation(toHandle.location());
      rq.result = chunkRead;
      return;
    }

    toHandle.ensure(false,
        "DynSlice decomposition above target width requires IssReadReg source, got %s",
        toHandle.value().getClass().getSimpleName());
  }

  @Handler
  void handle(Request rq, SelectNode toHandle) {
    var condition = request(toHandle.condition(), 0, 0);
    var trueCase = request(toHandle.trueCase(), rq.slice.hi(), rq.slice.lo());
    var falseCase = request(toHandle.falseCase(), rq.slice.hi(), rq.slice.lo());
    rq.result = GraphUtils.select(condition, trueCase, falseCase);
  }

  @Handler
  void handle(Request rq, FoldNode toHandle) {
    rq.result = decomposeFoldSlice(toHandle, rq.slice.hi(), rq.slice.lo());
  }

  @Handler
  void handle(Request rq, ForIdxNode toHandle) {
    rq.result = new SliceNode(toHandle,
        Constant.BitSlice.of(rq.slice.hi(), rq.slice.lo()),
        Type.bits(rq.slice.width()));
  }

  @Handler
  void handle(Request rq, TensorNode toHandle) {
    rq.result = decomposeTensorSlice(toHandle, rq.slice.hi(), rq.slice.lo());
  }


  @Handler
  void handle(Request rq, ReadSignalNode toHandle) {
    throw new UnsupportedOperationException("Type ReadSignalNode not yet implemented");
  }

  @Handler
  void handle(Request rq, OperationForAllNode toHandle) {
    final var indices = toHandle.indices().stream()
        .map(i -> request(i, rq.slice))
        .map(OperationForAllNode.Index.class::cast)
        .toList();
    final var body = request(toHandle.body(), rq.slice);
    rq.result = new OperationForAllNode(toHandle.type(), indices, body);
  }

  @Handler
  void handle(Request rq, OperationForAllNode.Index toHandle) {
    // Nothing to decompose
    rq.result = toHandle;
  }

  @Handler
  void handle(Request rq, OperationExistsNode toHandle) {
    final var indices = toHandle.indices().stream()
        .map(i -> request(i, rq.slice))
        .map(OperationForAllNode.Index.class::cast)
        .toList();
    var body = toHandle.body();
    if (body != null) {
      body = request(body, rq.slice);
    }
    rq.result = new OperationExistsNode(toHandle.type(), indices, body);
  }


  private IssReadRegNode.AccessKind readAccessKind(ReadRegTensorNode read) {
    if (read instanceof IssReadRegNode issRead) {
      return issRead.accessKind();
    }
    return IssReadRegNode.AccessKind.BASE;
  }

  private IssReadRegNode.ReadShape readShape(ReadRegTensorNode read) {
    if (read instanceof IssReadRegNode issRead) {
      return issRead.readShape();
    }
    return IssReadRegNode.ReadShape.FULL;
  }

  private @Nullable String readAccessorName(ReadRegTensorNode read) {
    if (read instanceof IssReadRegNode issRead) {
      return issRead.accessorName();
    }
    return null;
  }

  private @Nullable ArtificialResource readAliasResource(ReadRegTensorNode read) {
    if (read instanceof IssReadRegNode issRead) {
      return issRead.aliasResource();
    }
    return null;
  }

  private NodeList<ExpressionNode> readAccessorIndices(ReadRegTensorNode read) {
    if (read instanceof IssReadRegNode issRead) {
      return new NodeList<>(issRead.accessorIndices());
    }
    return read.indices().copy();
  }

  private IssWriteRegNode.AccessKind writeAccessKind(WriteRegTensorNode write) {
    if (write instanceof IssWriteRegNode issWrite) {
      return issWrite.accessKind();
    }
    return IssWriteRegNode.AccessKind.BASE;
  }

  private IssWriteRegNode.WriteGuardKind writeGuardKind(WriteRegTensorNode write) {
    if (write instanceof IssWriteRegNode issWrite) {
      return issWrite.writeGuardKind();
    }
    return write.nullableCondition() == null
        ? IssWriteRegNode.WriteGuardKind.NONE
        : IssWriteRegNode.WriteGuardKind.CONDITIONAL;
  }

  private @Nullable String writeAccessorName(WriteRegTensorNode write) {
    if (write instanceof IssWriteRegNode issWrite) {
      return issWrite.accessorName();
    }
    return null;
  }

  private @Nullable ArtificialResource writeAliasResource(WriteRegTensorNode write) {
    if (write instanceof IssWriteRegNode issWrite) {
      return issWrite.aliasResource();
    }
    return null;
  }

  private NodeList<ExpressionNode> writeAccessorIndices(WriteRegTensorNode write) {
    if (write instanceof IssWriteRegNode issWrite) {
      return new NodeList<>(issWrite.accessorIndices());
    }
    return write.indices().copy();
  }

}
