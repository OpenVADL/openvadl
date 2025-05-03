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
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.context.CGenContext;
import vadl.iss.passes.extensions.MemoryRegionInfo;
import vadl.iss.template.hw.EmitIssHwMachineCPass;
import vadl.viam.Processor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.WriteMemNode;

/**
 * Generates the memory region initialization functions in the generated machine
 * ({@code gen-machine.c}).
 * It is responsible for initializing the specified memory regions with the provided
 * initialization behavior.
 *
 * <p>It does this by creating an init vector array representing the memory.
 * Every memory write operation writes to this memory.
 * In the {@link vadl.iss.passes.IssMemoryDetectionPass} it is also checked that
 * each memory address is constant, so that we know that start and size of the memory.</p>
 *
 * @see MemoryRegionInfo
 * @see vadl.iss.passes.IssMemoryDetectionPass
 * @see EmitIssHwMachineCPass
 */
public class IssMemoryRegionInitCodeGen extends IssProcGen {

  private final MemoryRegionInfo memInfo;
  private final IssConfiguration config;

  /**
   * Constructs the memory region init code generator.
   *
   * @param memInfo of the generating memory region
   */
  public IssMemoryRegionInitCodeGen(MemoryRegionInfo memInfo, IssConfiguration config) {
    this.memInfo = memInfo;
    this.config = config;
  }

  /**
   * Produces the {@code init_<memory_region_name>()} function setup the ROM, which correspond
   * to the {@link Processor#firmware()} definition in the specification.
   *
   * @return the full function code, including signature.
   */
  public String fetch() {
    var machineName = config.machineName();
    ctx().wr("static void init_%s() {\n", memInfo.name().toLowerCase())
        .spacedIn()
        .ln("uint8_t init_vec[%d] = {0};", memInfo.sizeInTable());

    var start = getSingleNode(memInfo.memReg().behavior(), StartNode.class);
    var current = start.next();

    ctx().gen(current);

    ctx().ln("rom_add_blob_fixed_as(\"%s.init\", init_vec, sizeof(init_vec),",
            memInfo.name().toLowerCase())
        .ln("%s_memmap[%s].base, &address_space_memory);", machineName.toLowerCase(),
            memInfo.enumName())
        .spaceOut().ln("}");
    return builder().toString();
  }

  @Override
  public void handle(CGenContext<Node> ctx, WriteMemNode node) {
    // we normalize the node to an offset.
    var addr = ((ConstantNode) node.address()).constant().asVal().unsignedInteger();
    var offset = addr.subtract(memInfo.memReg().expectBase());
    var writeWidth = node.writeBitWidth();
    var writeType = CppTypeMap.cppUintType(writeWidth);

    // we first produce the correct value with the right endianness.
    // then we write the value to the init_vec using memcpy.
    // we do this out of correctness, as the init_vec is a byte vector, but the values could
    // be arbitrarily big (8, 16, 32 or 64 bit).
    //    uint32_t val0x0 = cpu_to_le32(   ((uint32_t) 0x00000297 )   );
    //    memcpy(&init_vec[0x0], &val0x0, sizeof(val0x0));

    // TODO: Use specified endianness of memory. Currently we always use little endianness.
    var endiannessConvertion = writeWidth == 8 ? "" : "cpu_to_le" + writeWidth;
    var val = "val0x" + offset.toString(16);
    // uint32_t val0x0 = cpu_to_le32(((uint32_t) 0x00000297));
    ctx.wr("%s %s = ", writeType, val)
        .wr(endiannessConvertion + "(")
        .gen(node.value())
        .ln(");");

    // memcpy(&init_vec[0x0], &val0x0, sizeof(val0x0));
    ctx.wr("memcpy(&init_vec[0x%s], ", offset.toString(16))
        .wr("&" + val)
        .wr(", sizeof(" + val + "))");
  }

}
