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

package vadl.iss.passes.extensions;

import java.math.BigInteger;
import java.util.Map;
import vadl.template.Renderable;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Processor;

/**
 * A {@link Processor} extension containing information about the memory layout and regions
 * in the generated QEMU frontend.
 * This includes the PC reset vector address, which defines the initial(reset) value of
 * the program counter during simulation.
 *
 * <p>It also contains the {@link Processor#firmware()} (ROM) start and size.
 * If the specification does not specify firmware, the firmwareSize is 0, indicating
 * no firmware.
 *
 * <p>This information is collected and added to the {@link Processor} by the
 * {@link vadl.iss.passes.IssMemoryDetectionPass}.</p>
 *
 * @see vadl.iss.passes.IssMemoryDetectionPass
 * @see vadl.iss.codegen.IssFirmwareCodeGenerator
 * @see Processor
 */
public class MemoryInfo extends DefinitionExtension<Processor>
    implements Renderable {

  public final BigInteger firmwareStart;
  // if the firmwareSize == 0, then no firmware is emitted
  public final int firmwareSize;

  /**
   * Construct the memory info.
   *
   * @param firmwareStart The start (base) address of the firmware (ROM) memory region.
   * @param firmwareSize  The size of the ROM memory region.
   *                      This is {@code 0} if no firmware was specified.
   */
  public MemoryInfo(
      BigInteger firmwareStart,
      int firmwareSize) {
    this.firmwareStart = firmwareStart;
    this.firmwareSize = firmwareSize;
  }


  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Processor.class;
  }

  @Override
  public Map<String, Object> renderObj() {
    return Map.of(
        "rom_start", "0x" + firmwareStart.toString(16),
        "rom_size", firmwareSize
    );
  }
}
