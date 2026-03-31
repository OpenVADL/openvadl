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

import static vadl.iss.passes.TcgPassUtils.instrInfo;
import static vadl.utils.GraphUtils.getSingleNode;

import java.util.stream.IntStream;
import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.iss.passes.extensions.InstrInfo;
import vadl.iss.passes.extensions.IssAccessorRegistry;
import vadl.iss.passes.nodes.IssRegBitfieldWriteNode;
import vadl.iss.passes.nodes.IssStaticPcRegNode;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.viam.Instruction;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.ParamNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.control.InstrExitNode;

/**
 * Generates helper function implementations for instructions in {@code target/gen-arch/helper.c}.
 * These helper functions are called by the translate functions for instructions that are too
 * complex to be directly translated into TCG operations (e.g., instructions using generic vector
 * registers).
 *
 * <p>The generated helper functions have the signature:
 * {@code void helper_<instr_name>_instr(CPU<ARCH>State *env, uint32_t param1, uint32_t param2, ...)
 * }
 * </p>
 *
 * <p>Register accesses are done directly using reads and
 * writes to the CPU state ({@code env->reg}).
 * Instruction format parameters are passed as function arguments.
 * </p>
 */
@DispatchFor(
    value = Node.class,
    context = CNodeContext.class,
    include = {"vadl.viam", "vadl.iss"}
)
public class IssInstrHelperGenerator extends IssProcGen
    implements IssCMixins.CpuSourceWriteRegTensor, IssCMixins.CpuSourceReadWriteMemory,
    IssCMixins.Default, IssCMixins.InvalidTcgC {

  private final IssConfiguration configuration;
  private final InstrInfo instrInfo;
  private final HelperParamPacking packing;

  /**
   * Constructs the helper function generator.
   */
  public IssInstrHelperGenerator(IssConfiguration configuration, InstrInfo instrInfo,
                                 IssAccessorRegistry accessorRegistry) {
    super(accessorRegistry, (issProcGenThis, ctx, node)
        -> IssInstrHelperGeneratorDispatcher.dispatch((IssInstrHelperGenerator) issProcGenThis, ctx,
        node)
    );
    this.configuration = configuration;
    this.instrInfo = instrInfo;
    this.packing = HelperParamPacking.from(instrInfo);
  }

  /**
   * Generates the helper function implementation for the instruction.
   *
   * @return the C code for the helper function
   */
  public String fetch() {
    var targetUpper = configuration.targetName().toUpperCase();
    var params = IntStream.range(0, packing.blockCount())
        .mapToObj(i -> "uint64_t packed" + i)
        .reduce((a, b) -> a + ", " + b)
        .map(s -> ", " + s)
        .orElse("");

    ctx().ln("void HELPER(%s)(CPU%sState *env%s) {",
            instrInfo.helperName(),
            targetUpper,
            params)
        .spacedIn();

    unpackPackedHelperParams();

    // init reads at start of function
    initReadRegs(instrInfo.instr().behavior());

    var start = getSingleNode(instrInfo.instr().behavior(), StartNode.class);
    var current = start.next();
    ctx().gen(current);

    ctx().spaceOut().ln("}");
    return builder().toString();
  }

  /**
   * Returns the helper function name for the given instruction.
   *
   * @param instr the instruction
   * @return the helper function name
   */
  public static String functionName(Instruction instr) {
    return "helper_" + instrInfo(instr).helperName();
  }

  @Override
  public void handle(CGenContext<Node> ctx, FuncCallNode toHandle) {
    // function calls from the helper functions have the env as first argument
    ctx.wr(toHandle.function().simpleName())
        .wr("(env");
    for (var arg : toHandle.arguments()) {
      ctx.wr(", ").gen(arg);
    }
    ctx.wr(")");
  }

  @Override
  void handle(CGenContext<Node> ctx, FieldRefNode toHandle) {
    ctx().wr(paramName(toHandle));
  }

  @Override
  void handle(CGenContext<Node> ctx, FieldAccessRefNode toHandle) {
    ctx().wr(paramName(toHandle));
  }

  @Override
  public void handle(CGenContext<Node> ctx, WriteRegTensorNode toHandle) {
    IssCMixins.CpuSourceWriteRegTensor.super.handle(ctx, toHandle);
    if (toHandle.isPcAccess()) {
      ctx().ln(";").wr("cpu_loop_exit(env_cpu(env))");
    }
  }

  @Override
  public void handle(CGenContext<Node> ctx, WriteMemNode toHandle) {
    IssCMixins.CpuSourceReadWriteMemory.super.handle(ctx, toHandle);
  }

  @Override
  public void handle(CGenContext<Node> ctx, ReadMemNode toHandle) {
    IssCMixins.CpuSourceReadWriteMemory.super.handle(ctx, toHandle);
  }

  @Override
  public void handle(CGenContext<Node> ctx, ReadRegTensorNode node) {
    // use register variables defined at start
    ctx().wr(readRegVariable(node));
  }

  @Handler
  void handle(CGenContext<Node> ctx, IssRegBitfieldWriteNode toHandle) {
    throw new UnsupportedOperationException(
        "Type IssRegBitfieldWriteNode not yet implemented for helper generation");
  }

  @Handler
  void handle(CGenContext<Node> ctx, IssStaticPcRegNode toHandle) {
    throw new UnsupportedOperationException("Type IssStaticPcRegNode not yet implemented");
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

  private String paramName(ParamNode def) {
    return def.definition().simpleName().toLowerCase();
  }

  private void unpackPackedHelperParams() {
    for (var param : packing.params()) {
      var paramType = "uint" + Tcg_32_64.nextFitting(param.param().type()).width + "_t";
      var paramName = paramName(param.param());

      ctx().ln(paramType + " " + paramName + " = 0;");
      for (var slice : packing.slicesForParam(param)) {
        var piece = "packed" + slice.blockIndex();
        if (slice.blockOffset() > 0) {
          piece = "(" + piece + " >> " + slice.blockOffset() + ")";
        }
        if (slice.width() < 64) {
          piece = "(" + piece + " & " + HelperParamPacking.u64MaskLiteral(slice.width()) + ")";
        }

        piece = "((" + paramType + ")" + piece + ")";
        if (slice.paramOffset() > 0) {
          piece = "(" + piece + " << " + slice.paramOffset() + ")";
        }
        ctx().ln(paramName + " |= " + piece + ";");
      }
    }
    if (!packing.params().isEmpty()) {
      ctx().ln();
    }
  }

}
