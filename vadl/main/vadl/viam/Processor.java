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

package vadl.viam;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Represents VADLs Micro Processor definition.
 * It is used by the ISS and LCB and defines a combination of ISA and ABI together with
 * additional information like the emulation start address, emulation stop condition,
 * memory regions, and startup functionality.
 */
public class Processor extends Definition {

  private final String targetName;

  private final InstructionSetArchitecture isa;

  @Nullable
  private final Abi abi;

  @Nullable
  private final Function stop;

  private final Procedure reset;

  private final List<MemoryRegion> memoryRegions;

  /**
   * Constructs the microprocessor.
   */
  public Processor(Identifier identifier, InstructionSetArchitecture isa, @Nullable Abi abi,
                   @Nullable Function stop,
                   Procedure reset, List<MemoryRegion> memoryRegions,
                   @Nullable String targetName) {
    super(identifier);
    this.isa = isa;
    this.abi = abi;
    this.stop = stop;
    this.reset = reset;
    this.memoryRegions = memoryRegions;
    if (targetName != null) {
      this.targetName = targetName;
    } else {
      this.targetName = isa.simpleName();
    }
  }


  /**
   * Returns the abi and throws if the abi isn't set.
   *
   * @return the abi.
   */
  public Abi abi() {
    if (abi == null) {
      throw new IllegalStateException("abi must be set");
    }
    return abi;
  }

  public @Nullable Abi abiNullable() {
    return abi;
  }

  public InstructionSetArchitecture isa() {
    return isa;
  }

  public Procedure reset() {
    return reset;
  }

  @Nullable
  public Function stop() {
    return stop;
  }

  public List<MemoryRegion> memoryRegions() {
    return memoryRegions;
  }

  public Stream<Instruction> instructions() {
    return isa.ownInstructions().stream();
  }

  public String targetName() {
    return targetName;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
