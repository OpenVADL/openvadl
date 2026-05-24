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

package vadl.iss.codegen;

import static vadl.error.DiagUtils.throwNotAllowed;

import java.util.HashSet;
import java.util.Set;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.cppCodeGen.mixins.CInvalidMixins;
import vadl.iss.passes.common.safeResourceRead.nodes.ExprSaveNode;
import vadl.iss.passes.extensions.IssAccessorRegistry;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.utils.GraphUtils;
import vadl.utils.functionInterfaces.TriConsumer;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.OperationExistsNode;
import vadl.viam.graph.dependency.OperationForAllNode;
import vadl.viam.graph.dependency.ParamNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

/**
 * A node dispatcher for procedure c code rendering in the ISS.
 *
 * <p>Procedure/exception/helper generators preload register reads using unified ISS access
 * metadata so emitted C variables follow the same accessor mapping contract as instruction paths.
 * See {@code docs/iss/register-access-domain-map.md}.
 *
 * @see IssResetGen
 */
@DispatchFor(
    value = Node.class,
    context = CNodeContext.class,
    include = "vadl.viam"
)
abstract class IssProcGen implements CDefaultMixins.All,
    CInvalidMixins.ResourceReads, CInvalidMixins.SideEffect, CInvalidMixins.HardwareRelated {

  private final CNodeContext ctx;
  private final StringBuilder builder;
  private final IssAccessorRegistry accessorRegistry;
  private final Set<ReadRegTensorNode> preloadedReadRegs = new HashSet<>();

  public IssProcGen(IssAccessorRegistry accessorRegistry) {
    this(accessorRegistry, IssProcGenDispatcher::dispatch);
  }

  public <T extends IssProcGen> IssProcGen(IssAccessorRegistry accessorRegistry,
                                           TriConsumer<T, CNodeContext, Node> dispatcher) {
    this.builder = new StringBuilder();
    this.accessorRegistry = accessorRegistry;
    //noinspection unchecked
    this.ctx = new CNodeContext(
        builder::append,
        (ctx, node) -> dispatcher.accept((T) this, ctx, node)
    );
  }

  protected CNodeContext ctx() {
    return ctx;
  }

  protected StringBuilder builder() {
    return builder;
  }

  public IssAccessorRegistry accessorRegistry() {
    return accessorRegistry;
  }

  /**
   * Renders register reads at the current context state.
   * This is used to prevent read-write conflict during c-code rendering of unscheduled
   * register reads.
   *
   * @param graph that contains register reads
   */
  void initReadRegs(Graph graph) {
    preloadedReadRegs.clear();
    graph.getNodes(ReadRegTensorNode.class)
        .forEach(this::initSingleReadReg);
  }

  void initSingleReadReg(ReadRegTensorNode read) {
    if (dependsOnForallIndex(read)) {
      return;
    }
    var width = read.type().asDataType().bitWidth();
    read.ensure(width <= 64,
        "Helper read preload expects <=64-bit reads, got %d-bit on %s", width, read);
    var valueType = CppTypeMap.nextFittingUInt(read.type().asDataType());
    var name = readRegVariable(read);
    ctx.wr(valueType + " " + name + " = ");
    RegisterAccessEmitters.emitRead(ctx, read, accessorRegistry);
    ctx.ln(";");
    preloadedReadRegs.add(read);
  }

  /**
   * The name of the register variable corresponding to a certain register read node.
   *
   * @param read node of the register read expression
   * @return the name of the variable
   */
  String readRegVariable(ReadRegTensorNode read) {
    var name = read.regTensor().simpleName().toLowerCase();
    if (!read.regTensor().isSingleRegister()) {
      name += "_" + read.id;
    }
    return name;
  }

  void emitReadReg(CGenContext<Node> ctx, ReadRegTensorNode read) {
    if (preloadedReadRegs.contains(read)) {
      ctx.wr(readRegVariable(read));
      return;
    }
    RegisterAccessEmitters.emitRead(ctx, read, accessorRegistry);
  }

  private String exprSaveVariable(ExprSaveNode save) {
    return "expr_save_" + save.id().numericId();
  }

  private boolean dependsOnForallIndex(Node node) {
    return GraphUtils.isOrHasDependencies(node, ForIdxNode.class::isInstance);
  }

  @Handler
  @Override
  public void handle(CGenContext<Node> ctx, ScheduledNode node) {
    if (node.node() instanceof ExprSaveNode save) {
      if (shouldInlineExprSave(save)) {
        ctx.gen(node.next());
        return;
      }
      var type = CppTypeMap.nextFittingUInt(save.type().asDataType());
      ctx.wr(type + " " + exprSaveVariable(save) + " = ")
          .gen(save.value())
          .ln(";");
      ctx.gen(node.next());
      return;
    }
    ctx.gen(node.node()).ln(";");
    ctx.gen(node.next());
  }

  @Handler
  public void handle(CGenContext<Node> ctx, ExprSaveNode toHandle) {
    if (shouldInlineExprSave(toHandle)) {
      ctx.gen(toHandle.value());
      return;
    }
    ctx.wr(exprSaveVariable(toHandle));
  }

  @Handler
  void handle(CGenContext<Node> ctx, InstrExitNode.PcChange toHandle) {
    handle(ctx, (InstrExitNode) toHandle);
  }

  @Handler
  void handle(CGenContext<Node> ctx, FieldRefNode toHandle) {
    throwNotAllowed(toHandle, "Field references");
  }

  @Handler
  void handle(CGenContext<Node> ctx, FieldAccessRefNode toHandle) {
    throwNotAllowed(toHandle, "Field accesses");
  }

  @Handler
  void handle(CGenContext<Node> ctx, InstrCallNode toHandle) {
    throwNotAllowed(toHandle, "Instruction calls");
  }

  @Handler
  void handle(CGenContext<Node> ctx, AsmBuiltInCall toHandle) {
    throwNotAllowed(toHandle, "Assembler built-in calls");
  }

  @Handler
  void handle(CGenContext<Node> ctx, FoldNode toHandle) {
    throwNotAllowed(toHandle, "forall fold expressions");
  }

  @Handler
  void handle(CGenContext<Node> ctx, OperationForAllNode toHandle) {
    throwNotAllowed(toHandle, "forall then expressions");
  }

  @Handler
  void handle(CGenContext<Node> ctx, OperationExistsNode toHandle) {
    throwNotAllowed(toHandle, "exists then expressions");
  }

  private boolean shouldInlineExprSave(ExprSaveNode save) {
    return isIdentifierLike(save.value(), new HashSet<>());
  }

  private boolean isIdentifierLike(Node node, Set<Node> visited) {
    if (!visited.add(node)) {
      return false;
    }
    if (node instanceof ReadRegTensorNode read && preloadedReadRegs.contains(read)) {
      return true;
    }
    if (node instanceof ParamNode
        || node instanceof FieldRefNode
        || node instanceof FieldAccessRefNode
        || node instanceof ConstantNode) {
      return true;
    }
    if (node instanceof LetNode letNode) {
      return isIdentifierLike(letNode.expression(), visited);
    }
    if (node instanceof ExprSaveNode saveNode) {
      return isIdentifierLike(saveNode.value(), visited);
    }
    return false;
  }

}
