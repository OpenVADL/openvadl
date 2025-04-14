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

package vadl.iss.codegen;

import static vadl.error.DiagUtils.throwNotAllowed;
import static vadl.utils.GraphUtils.getSingleNode;

import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.cppCodeGen.mixins.CInvalidMixins;
import vadl.iss.passes.extensions.ExceptionInfo;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.viam.MicroProcessor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

@DispatchFor(
    value = Node.class,
    context = CNodeContext.class,
    include = "vadl.viam"
)
public class IssExceptionHandlingCodeGenerator implements CDefaultMixins.All,
    CInvalidMixins.ResourceReads, CInvalidMixins.SideEffect, CInvalidMixins.HardwareRelated {

  private final ExceptionInfo.Entry excInfo;
  private final IssConfiguration config;

  private final CNodeContext ctx;
  private final StringBuilder builder = new StringBuilder();


  /**
   * Constructs the firmware setup code generator.
   */
  public IssExceptionHandlingCodeGenerator(ExceptionInfo.Entry excInfo, IssConfiguration config) {
    this.excInfo = excInfo;
    this.config = config;
    this.ctx = new CNodeContext(
        builder::append,
        (ctx, node)
            -> IssExceptionHandlingCodeGeneratorDispatcher.dispatch(this, ctx, node)
    );
  }

  /**
   * Produces the {@code setup_rom_reset_vec()} function setup the ROM, which correspond
   * to the {@link MicroProcessor#firmware()} definition in the specification.
   *
   * @return the full function code, including signature.
   */
  public String fetch() {
    var targetUpper = config.targetName().toUpperCase();
    ctx.ln("static void %s(CPU%sState *env) {", excInfo.handlingFuncName(), targetUpper)
        .spacedIn();

    var start = getSingleNode(excInfo.def.behavior(), StartNode.class);
    var current = start.next();
    ctx.gen(current);

    ctx.spaceOut().ln("}");
    return builder.toString();
  }

  @Override
  public void handle(CGenContext<Node> ctx, FuncParamNode toHandle) {
    var param = excInfo.params.get(toHandle.parameter());
    toHandle.ensure(param != null, "Exception parameter unknown.");
    ctx.wr("env->" + param.nameInCpuState());
  }

  @Override
  public void impl(CGenContext<Node> ctx, WriteRegNode node) {
    ctx.wr("env->" + node.register().simpleName().toLowerCase() + " = ")
        .gen(node.value());
  }

  @Override
  public void impl(CGenContext<Node> ctx, ReadRegNode node) {
    ctx.wr("env->" + node.register().simpleName().toLowerCase());
  }

  @Handler
  public void impl(CGenContext<Node> ctx, InstrExitNode.PcChange node) {
    ctx.gen(node.cause())
        .ln(";")
        .gen(node.next());
  }

  ///  INVALID NODES  ///

  @Handler
  public void impl(CGenContext<Node> ctx, InstrExitNode.Raise node) {
    throwNotAllowed(node, "Raising exceptions");
  }

  @Override
  public void impl(CGenContext<Node> ctx, WriteMemNode node) {
    throwNotAllowed(node, "Memory writes");
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

}
