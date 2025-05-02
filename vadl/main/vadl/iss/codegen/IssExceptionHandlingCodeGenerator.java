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
import vadl.viam.Processor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

/**
 * Generates the exception handling functions in {@code /target/gen-arch/do_exception.c.inc}
 * which is included to the {@code /target/gen-arch/cpu.c}.
 * Those handling functions are called by the {@code do_interrupt()} function that is invoked
 * by the main execution loop if the {@code cs->exception_index != 0}.
 *
 * <p>Register access are directly done using read and writes to the CPU state ({@code env->reg}).
 * PC modification does not need special handling, as it is not a direct jump to anywhere.
 * Exception arguments are store in the CPU state, so they are also accessed using
 * {@code env->arg_xy}.
 * </p>
 */
@DispatchFor(
    value = Node.class,
    context = CNodeContext.class,
    include = "vadl.viam"
)
public class IssExceptionHandlingCodeGenerator implements CDefaultMixins.All,
    CInvalidMixins.ResourceReads, CInvalidMixins.HardwareRelated, CInvalidMixins.AsmRelated {

  private final ExceptionInfo.Entry excInfo;
  private final IssConfiguration config;

  private final CNodeContext ctx;
  private final StringBuilder builder = new StringBuilder();


  /**
   * Constructs the exception handling code generator.
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
   * to the {@link Processor#firmware()} definition in the specification.
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

  @Handler
  void handle(CGenContext<Node> ctx, WriteRegTensorNode toHandle) {
    toHandle.ensure(toHandle.regTensor().isSingleRegister(),
        "Only registers supported at the moment.");
    ctx.wr("env->" + toHandle.regTensor().simpleName().toLowerCase() + " = ")
        .gen(toHandle.value());
  }

  @Override
  public void handle(CGenContext<Node> ctx, ReadRegTensorNode node) {
    node.ensure(node.regTensor().isSingleRegister(),
        "Only registers supported at the moment.");
    ctx.wr("env->" + node.regTensor().simpleName().toLowerCase());
  }

  /**
   * Directly call the cause and wrap it in a statement line.
   * Then call the next control node.
   */
  @Handler
  public void handle(CGenContext<Node> ctx, InstrExitNode.PcChange node) {
    ctx.gen(node.cause())
        .ln(";")
        .gen(node.next());
  }

  ///  INVALID NODES  ///

  @Handler
  public void handle(CGenContext<Node> ctx, InstrExitNode.Raise node) {
    throwNotAllowed(node, "Raising exceptions");
  }

  @Handler
  public void handle(CGenContext<Node> ctx, WriteMemNode node) {
    throw new UnsupportedOperationException("Type WriteMemNode not yet implemented");
  }

  @Handler
  void handle(CGenContext<Node> ctx, WriteArtificialResNode toHandle) {
    throw new UnsupportedOperationException("Type WriteArtificialResNode not yet implemented");
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
  public void handle(CGenContext<Node> ctx, ProcCallNode node) {
    throwNotAllowed(node, "Procedure calls");
  }


}
