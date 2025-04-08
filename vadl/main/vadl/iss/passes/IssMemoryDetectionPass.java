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

package vadl.iss.passes;

import static vadl.error.Diagnostic.ensure;
import static vadl.error.Diagnostic.error;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.math.BigInteger;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.extensions.MemoryInfo;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.Type;
import vadl.utils.Pair;
import vadl.viam.Constant;
import vadl.viam.MicroProcessor;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.WriteMemNode;

/**
 * Collects information about memory regions in the generated ISS.
 * This includes the reset vector of the program counter, which is either
 * the start of the {@link MicroProcessor#firmware()} definition (ROM) or the
 * {@link MicroProcessor#start()}.
 * It also determines the start and size of the used firmware.
 * If no firmware is specified, the firmware size ({@link MemoryInfo#firmwareSize}) defaults
 * to 0, indicating no firmware.
 *
 * @see MemoryInfo
 * @see vadl.iss.codegen.IssFirmwareCodeGenerator
 */
public class IssMemoryDetectionPass extends AbstractIssPass {

  public IssMemoryDetectionPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Memory Detection Pass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    var processor = viam.mip().get();
    var firmwareInfo = findFirmwareInfo(processor);

    var pcResetAddress = findPcResetAddress(firmwareInfo, processor);

    var memInfo = new MemoryInfo(pcResetAddress, firmwareInfo.left(), firmwareInfo.right());
    processor.attachExtension(memInfo);

    return null;
  }

  /**
   * Find the PC reset address by checking the firmware start and `start` definition expression.
   * If the firmware size is 0, we take the start expression as reset address.
   */
  private Constant.Value findPcResetAddress(Pair<BigInteger, Integer> firmwareInfo,
                                            MicroProcessor processor
  ) {
    if (firmwareInfo.right().equals(0)) {
      // we must use the `start` address definition
      var valNode = getSingleNode(processor.start().behavior(), ReturnNode.class)
          .value();
      if (!(valNode instanceof ConstantNode val)) {
        throw error("Start address not constant", processor.start())
            .help("Simplify the expression to a constant value.")
            .build();
      }
      return val.constant().asVal().toBits();
    }

    // we use the firmware start
    var addressSize = processor.isa().codeMemory().addressType().bitWidth();
    return Constant.Value.fromInteger(firmwareInfo.left(), Type.bits(addressSize));
  }

  /**
   * Find the start and size of the firmware written to memory.
   */
  private Pair<BigInteger, Integer> findFirmwareInfo(MicroProcessor processor) {
    var firmware = processor.firmware();
    if (firmware == null) {
      return Pair.of(BigInteger.ZERO, 0);
    }
    // the firmware definition writes values (instructions) into memory.
    // we have to determine the start and size of this written memory area.
    // as we don't allow any kind of outter world access in this function,
    // all addresses can be considered constantly known.
    var memoryWrites = firmware.behavior().getNodes(WriteMemNode.class).toList();

    ensure(!memoryWrites.isEmpty(), () -> error("Empty firmware definition", firmware)
        .help("You may delete the definition if you don't need it."));

    BigInteger lowestAddress = null;
    BigInteger highestAddress = null;

    for (var write : memoryWrites) {
      ensure(write.memory().wordSize() == 8, () -> error("Invalid memory word size", write.memory())
          .locationDescription(write.memory(), "The ISS currently only supports 8 bit memory words")
      );

      ensure(write.writeBitWidth() <= 64, () -> error("Write operation too big", write)
          .locationDescription(write, "The ISS currently only supports up to 64 bit writes")
      );

      // check written memory size.
      write.ensure(write.address().isConstant(), "Address of write is not constant");
      var addr = ((ConstantNode) write.address()).constant().asVal().unsignedInteger();
      if (lowestAddress == null || lowestAddress.compareTo(addr) > 0) {
        lowestAddress = addr;
      }
      // high bound is addr + written words
      addr = addr.add(BigInteger.valueOf(write.words()));
      if (highestAddress == null || highestAddress.compareTo(addr) < 0) {
        highestAddress = addr;
      }
    }

    ViamError.ensure(lowestAddress != null, "Failing to compute lowest firmware address");
    ViamError.ensure(highestAddress != null, "Failing to compute highest firmware address");

    var size = highestAddress.subtract(lowestAddress).intValue();
    return Pair.of(lowestAddress, size);
  }

}
