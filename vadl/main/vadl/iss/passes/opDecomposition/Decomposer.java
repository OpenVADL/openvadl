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

package vadl.iss.passes.opDecomposition;

import com.google.errorprone.annotations.concurrent.LazyInit;
import javax.annotation.Nullable;
import vadl.iss.passes.opDecomposition.decomposer.ShiftDecomposer;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.utils.VadlBuiltInEmptyNoStatusDispatcher;
import vadl.viam.Constant;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
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
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TensorNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TupleGetFieldNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * The decomposer is designed as a request system, where one node requests
 * a slice from a dependency node.
 * The slice's width must be smaller/equal than the target size.
 * The request is dispatched to the correct implementation, which than takes care of
 * returning an expression that represents the result of the requested slice.
 * It must only use operations that are smaller equal the target size.
 */
@DispatchFor(
    value = ExpressionNode.class,
    include = "vadl.viam",
    context = Decomposer.Request.class
)
@SuppressWarnings("OverloadMethodsDeclarationOrder")
class Decomposer
    implements VadlBuiltInEmptyNoStatusDispatcher<Decomposer.Request>, ShiftDecomposer {

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
    expr.replaceAndDelete(repl);
  }

  @Override
  public int targetSize() {
    return targetSize;
  }

  @Override
  public ExpressionNode request(ExpressionNode node, int msb, int lsb) {
    return request(node, new Slice(msb, lsb));
  }

  private ExpressionNode request(ExpressionNode node, Slice slice) {
    if (slice.lo() == 0 && node.type().asDataType().bitWidth() <= targetSize) {
      // if the slice is just a truncate and the node does fit in the target size, we
      // can just truncate and return it.
      if (node.type().asDataType().bitWidth() - 1 == slice.hi()) {
        return node;
      } else {
        return GraphUtils.truncate(node, Type.bits(slice.width()));
      }
    }

    return internalRequest(node, slice);
  }

  private ExpressionNode internalRequest(ExpressionNode node, Slice slice) {
    var req = new Request(slice);
    DecomposerDispatcher.dispatch(this, req, node);
    if (req.result == null) {
      throw new IllegalStateException("Not yet implemented: " + node);
    }
    return req.result;
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


  @Handler
  void handle(Request rq, MiaBuiltInCall toHandle) {
    throw new UnsupportedOperationException("Type MiaBuiltInCall not yet implemented");
  }

  @Handler
  void handle(Request rq, ReadRegTensorNode toHandle) {
    throw new UnsupportedOperationException("Type ReadRegTensorNode not yet implemented");
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
    throw new UnsupportedOperationException("Type ReadMemNode not yet implemented");
  }

  @Handler
  void handle(Request rq, ReadArtificialResNode toHandle) {
    throw new UnsupportedOperationException("Type ReadArtificialResNode not yet implemented");
  }

  @Handler
  void handle(Request rq, TupleGetFieldNode toHandle) {
    throw new UnsupportedOperationException("Type TupleGetFieldNode not yet implemented");
  }

  @Handler
  void handle(Request rq, AsmBuiltInCall toHandle) {
    throw new UnsupportedOperationException("Type AsmBuiltInCall not yet implemented");
  }

  @Handler
  void handle(Request rq, FuncParamNode toHandle) {
    throw new UnsupportedOperationException("Type FuncParamNode not yet implemented");
  }

  @Handler
  void handle(Request rq, ZeroExtendNode toHandle) {
    throw new UnsupportedOperationException("Type ZeroExtendNode not yet implemented");
  }

  @Handler
  void handle(Request rq, LetNode toHandle) {
    throw new UnsupportedOperationException("Type LetNode not yet implemented");
  }

  @Handler
  void handle(Request rq, SliceNode toHandle) {
    throw new UnsupportedOperationException("Type SliceNode not yet implemented");
  }

  @Handler
  void handle(Request rq, SelectNode toHandle) {
    throw new UnsupportedOperationException("Type SelectNode not yet implemented");
  }

  @Handler
  void handle(Request rq, FoldNode toHandle) {
    throw new UnsupportedOperationException("Type FoldNode not yet implemented");
  }

  @Handler
  void handle(Request rq, ForIdxNode toHandle) {
    throw new UnsupportedOperationException("Type ForIdxNode not yet implemented");
  }

  @Handler
  void handle(Request rq, TensorNode toHandle) {
    throw new UnsupportedOperationException("Type TensorNode not yet implemented");
  }

}