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

package vadl.cppCodeGen;

import static vadl.utils.GraphUtils.getSingleNode;

import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.cppCodeGen.mixins.CInvalidMixins;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;

/**
 * Abstract base class responsible for generating C code from a given function's expression nodes.
 * Uses dispatching mechanisms to handle various node types
 * and produce a finalized C++ function.
 */
@DispatchFor(
    value = ExpressionNode.class,
    context = CNodeContext.class,
    include = "vadl.viam"
)
public abstract class FunctionCodeGenerator extends AbstractFunctionCodeGenerator
    implements CDefaultMixins.AllExpressions, CDefaultMixins.Utils, CInvalidMixins.ReadStageOutput,
    CInvalidMixins.ReadSignal {

  protected final CNodeContext context;

  /**
   * Creates a new code generator for the specified function.
   *
   * @param function the function for which code should be generated
   */
  public FunctionCodeGenerator(Function function) {
    super(function);
    this.context = new CNodeContext(
        builder::append,
        (ctx, node)
            -> FunctionCodeGeneratorDispatcher.dispatch(this, ctx, (ExpressionNode) node)
    );
  }

  @Handler
  protected abstract void handle(CGenContext<Node> ctx, ReadRegTensorNode toHandle);

  @Handler
  protected abstract void handle(CGenContext<Node> ctx, ReadMemNode toHandle);

  @Handler
  protected abstract void handle(CGenContext<Node> ctx, ReadArtificialResNode toHandle);

  @Handler
  protected abstract void handle(CGenContext<Node> ctx, FieldAccessRefNode toHandle);

  @Handler
  protected abstract void handle(CGenContext<Node> ctx, FieldRefNode toHandle);

  @Handler
  protected abstract void handle(CGenContext<Node> ctx, AsmBuiltInCall toHandle);

  public String genReturnExpression() {
    var returnNode = getSingleNode(function.behavior(), ReturnNode.class);
    return context.genToString(returnNode.value());
  }

  @Override
  public CNodeContext context() {
    return context;
  }
}


