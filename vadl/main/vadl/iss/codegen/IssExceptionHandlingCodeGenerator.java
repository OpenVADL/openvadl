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

import static vadl.utils.GraphUtils.getSingleNode;

import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.context.CGenContext;
import vadl.iss.passes.extensions.ExceptionInfo;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
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
public class IssExceptionHandlingCodeGenerator extends IssProcGen
    implements IssCMixins.CpuSourceWriteRegTensor {

  private final ExceptionInfo.Entry excInfo;
  private final IssConfiguration config;

  /**
   * Constructs the exception handling code generator.
   */
  public IssExceptionHandlingCodeGenerator(ExceptionInfo.Entry excInfo, IssConfiguration config) {
    this.excInfo = excInfo;
    this.config = config;
  }

  /**
   * Generates the {@code do_exception} function for the given exception.
   */
  public String fetch() {
    var targetUpper = config.targetName().toUpperCase();
    ctx().ln("static void %s(CPU%sState *env) {", excInfo.handlingFuncName(), targetUpper)
        .spacedIn();
    // init reads at start of function
    initReadRegs(excInfo.def.behavior());

    var start = getSingleNode(excInfo.def.behavior(), StartNode.class);
    var current = start.next();
    ctx().gen(current);

    ctx().spaceOut().ln("}");
    return builder().toString();
  }

  @Override
  public void handle(CGenContext<Node> ctx, FuncParamNode toHandle) {
    var param = excInfo.params.get(toHandle.parameter());
    toHandle.ensure(param != null, "Exception parameter unknown.");
    ctx.wr("env->" + param.nameInCpuState());
  }

  @Override
  public void handle(CGenContext<Node> ctx, WriteRegTensorNode toHandle) {
    IssCMixins.CpuSourceWriteRegTensor.super.handle(ctx, toHandle);
  }

  @Override
  public void handle(CGenContext<Node> ctx, ReadRegTensorNode node) {
    // use register variables defined at start
    ctx().wr(readRegVariable(node));
  }

  /**
   * Directly call the cause and wrap it in a statement line.
   * Then call the next control node.
   */
  @Override
  public void handle(CGenContext<Node> ctx, InstrExitNode.PcChange node) {
    ctx.gen(node.cause())
        .ln(";")
        .gen(node.next());
  }

}
