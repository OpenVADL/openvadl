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

import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.cppCodeGen.mixins.CInvalidMixins;
import vadl.iss.passes.extensions.MemoryInfo;
import vadl.iss.template.hw.EmitIssHwMachineCPass;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.viam.MicroProcessor;
import vadl.viam.Procedure;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.WriteMemNode;

/**
 * Generates the {@code setup_rom_reset_vec} function in the generated machine ({@code gen-machine.c}).
 * It is responsible for writing ROM memory as defined in the {@link MicroProcessor#firmware()}
 * definition.
 *
 * <p>It does this, by creating a reset vector array representing the ROM.
 * Every memory write operation writes to this ROM.
 * In the {@link vadl.iss.passes.IssMemoryDetectionPass} it is also checked that
 * each memory address is constant, so that we know that start and size of the ROM.</p>
 *
 * @see MemoryInfo
 * @see vadl.iss.passes.IssMemoryDetectionPass
 * @see EmitIssHwMachineCPass
 */
@DispatchFor(
    value = Node.class,
    context = CNodeContext.class,
    include = "vadl.viam"
)
public class IssFirmwareCodeGenerator implements CDefaultMixins.All,
    CInvalidMixins.ResourceReads, CInvalidMixins.SideEffect, CInvalidMixins.HardwareRelated {

  private final Procedure firmware;
  private final MemoryInfo memoryInfo;
  private final CNodeContext ctx;
  private final StringBuilder builder = new StringBuilder();

  /**
   * Constructs the firmware setup code generator.
   *
   * @param firmware   The firmware that should be generated.
   * @param memoryInfo Memory info that contains the start and size of the ROM.
   */
  public IssFirmwareCodeGenerator(Procedure firmware, MemoryInfo memoryInfo) {
    this.firmware = firmware;
    this.memoryInfo = memoryInfo;
    this.ctx = new CNodeContext(
        builder::append,
        (ctx, node)
            -> IssFirmwareCodeGeneratorDispatcher.dispatch(this, ctx, node)
    );
  }

  /**
   * Produces the {@code setup_rom_reset_vec()} function setup the ROM, which correspond
   * to the {@link MicroProcessor#firmware()} definition in the specification.
   *
   * @return the full function code, including signature.
   */
  public String fetch(String machineName) {
    ctx.wr("static void setup_rom_reset_vec() {\n")
        .spacedIn()
        .ln("uint8_t reset_vec[%d] = {0};", memoryInfo.firmwareSize);

    var start = getSingleNode(firmware.behavior(), StartNode.class);
    var current = start.next();

    ctx.gen(current);

    ctx.ln("rom_add_blob_fixed_as(\"mrom.reset\", reset_vec, sizeof(reset_vec),")
        .ln("%s_memmap[%s_MROM].base, &address_space_memory);", machineName.toLowerCase(),
            machineName.toUpperCase())
        .spaceOut().ln("}");
    return builder.toString();
  }

  @Override
  public void impl(CGenContext<Node> ctx, WriteMemNode node) {
    // we normalize the node to an offset.
    var addr = ((ConstantNode) node.address()).constant().asVal().unsignedInteger();
    var offset = addr.subtract(memoryInfo.firmwareStart);
    var writeWidth = node.writeBitWidth();
    var writeType = CppTypeMap.cppUintType(writeWidth);

    // we first produce the correct value with the right endianness.
    // then we write the value to the reset_vec using memcpy.
    // we do this out of correctness, as the reset_vec is a byte vector, but the values could
    // be arbitrarily big (8, 16, 32 or 64 bit).
    //    uint32_t val0x0 = cpu_to_le32(   ((uint32_t) 0x00000297 )   );
    //    memcpy(&reset_vec[0x0], &val0x0, sizeof(val0x0));

    // TODO: Use specified endianness of memory. Currently we always use little endianness.
    var endiannessConvertion = writeWidth == 8 ? "" : "cpu_to_le" + writeWidth;
    var val = "val0x" + offset.toString(16);
    // uint32_t val0x0 = cpu_to_le32(((uint32_t) 0x00000297));
    ctx.wr("%s %s = ", writeType, val)
        .wr(endiannessConvertion + "(")
        .gen(node.value())
        .ln(");");

    // memcpy(&reset_vec[0x0], &val0x0, sizeof(val0x0));
    ctx.wr("memcpy(&reset_vec[0x%s], ", offset.toString(16))
        .wr("&" + val)
        .wr(", sizeof(" + val + "))");
  }

  ///  INVALID NODES  ///

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
