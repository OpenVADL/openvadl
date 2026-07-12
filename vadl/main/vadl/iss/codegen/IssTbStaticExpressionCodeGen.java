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

package vadl.iss.codegen;

import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.cppCodeGen.mixins.CInvalidMixins;
import vadl.iss.passes.nodes.IssGvecOpNode;
import vadl.iss.passes.nodes.IssRegBitfieldWriteNode;
import vadl.iss.passes.nodes.IssStaticPcRegNode;
import vadl.iss.passes.nodes.IssStaticReadRegNode;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.GroupRef;
import vadl.viam.graph.dependency.OperationExistsNode;
import vadl.viam.graph.dependency.OperationForAllNode;
import vadl.viam.graph.dependency.TensorNode;

/**
 * A code generator class responsible for producing C expressions which may only
 * depend on resources which can be statically read from the {@code DisasContext}
 * (for example the program counter or registers annotated with {@code [execution state]}).
 *
 * <p>This class implements interfaces for writing expression and ISS nodes (except TCG nodes,
 * side effects, non-static resource reads, instruction calls and anything hardware-related).
 */
@DispatchFor(
    value = Node.class,
    context = CNodeContext.class,
    include = {"vadl.viam", "vadl.iss"}
)
public class IssTbStaticExpressionCodeGen implements
    CDefaultMixins.AllExpressions,
    IssCMixins.Default, IssCMixins.StaticReadRegTensor, IssCMixins.InvalidTcgC,
    CInvalidMixins.SideEffect, CInvalidMixins.ResourceReads,
    CInvalidMixins.InstrCall, CInvalidMixins.HardwareRelated {

  protected final ExpressionNode expr;
  protected final StringBuilder builder;
  protected final CNodeContext context;

  /**
   * Creates a new expression code generator for the given expression.
   * The only resource accesses in the function may be {@link IssStaticReadRegNode}s and
   * {@link IssStaticPcRegNode}s.
   *
   * @param expr the expression for which code should be generated
   */
  public IssTbStaticExpressionCodeGen(ExpressionNode expr) {
    this.expr = expr;
    this.builder = new StringBuilder();
    this.context = new CNodeContext(
        builder::append,
        (ctx, node)
            -> IssTbStaticExpressionCodeGenDispatcher.dispatch(this, ctx, node)
    );
  }

  public ExpressionNode expr() {
    return expr;
  }

  public StringBuilder builder() {
    return builder;
  }

  public CNodeContext context() {
    return context;
  }

  /**
   * Generates a C expression for return value of the given {@link Function}.
   */
  public String fetch() {
    var ctx = context();
    ctx.gen(expr());
    return builder().toString();
  }

  @Handler
  void handle(CGenContext<Node> ctx, AsmBuiltInCall toHandle) {
    throw new UnsupportedOperationException("Type AsmBuiltInCall not allowed");
  }

  @Handler
  void handle(CGenContext<Node> ctx, FoldNode toHandle) {
    throw new UnsupportedOperationException("Type FoldNode not allowed");
  }

  @Handler
  void handle(CGenContext<Node> ctx, OperationForAllNode toHandle) {
    throw new UnsupportedOperationException("Type ForAll not allowed");
  }

  @Handler
  void handle(CGenContext<Node> ctx, OperationExistsNode toHandle) {
    throw new UnsupportedOperationException("Type Exists not allowed");
  }

  @Handler
  void handle(CGenContext<Node> ctx, IssRegBitfieldWriteNode toHandle) {
    throw new UnsupportedOperationException("Type IssRegBitfieldWriteNode not allowed");
  }

  @Handler
  void handle(CGenContext<Node> ctx, IssGvecOpNode toHandle) {
    throw new UnsupportedOperationException("Type IssGvecOpNode not allowed");
  }

  @Handler
  void handle(CGenContext<Node> ctx, FieldRefNode toHandle) {
    throw new UnsupportedOperationException("Type FieldRefNode not allowed");
  }

  @Handler
  void handle(CGenContext<Node> ctx, FieldAccessRefNode toHandle) {
    throw new UnsupportedOperationException("Type FieldAccessRefNode not allowed");
  }

  @Handler
  void handle(CGenContext<Node> ctx, ControlNode toHandle) {
    throw new UnsupportedOperationException("Type ControlNode not allowed");
  }

  @Handler
  void handle(CGenContext<Node> ctx, TensorNode toHandle) {
    throw new UnsupportedOperationException("Type TensorNode not allowed");
  }

  @Handler
  void handle(CGenContext<Node> ctx, GroupRef toHandle) {
    throw new UnsupportedOperationException("Type GroupRef not yet implemented");
  }
}
