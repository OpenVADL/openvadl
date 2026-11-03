// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

/**
 * Represents the configuration for QEMU user-mode emulation.
 * <p>
 * This class defines architecture-specific parameters required for Linux-user emulation,
 * including system call register mappings, signal trampoline instructions, stack
 * alignment requirements, and exception IDs.
 * </p>
 */
public class UserModeEmulation extends Definition {

  /**
   * Represents a custom architecture-specific system call.
   *
   * @param number the system call ID
   * @param name   the system call name
   */
  public record CustomSyscall(int number, String name) {}

  private final InstructionSetArchitecture isa;
  private final Abi abi;

  private final List<RegisterRef> args;
  private final Instruction syscallInstr;
  private final RegisterRef syscallNumber;
  private final RegisterRef syscallReturn;
  private final List<CustomSyscall> customSyscalls;

  /**
   * Constructs a UserModeEmulation configuration.
   */
  public UserModeEmulation(
      Identifier identifier, InstructionSetArchitecture isa, Abi abi,
      List<RegisterRef> args,
      Instruction syscallInstr, RegisterRef syscallNumber, RegisterRef syscallReturn,
      List<CustomSyscall> customSyscalls
  ) {

    super(identifier);
    this.isa = isa;
    this.abi = abi;
    this.args = args;
    this.syscallInstr = syscallInstr;
    this.syscallNumber = syscallNumber;
    this.syscallReturn = syscallReturn;
    this.customSyscalls = customSyscalls;
  }

  public Instruction syscallInstr() {
    return syscallInstr;
  }

  public RegisterRef getSyscallNumber() {
    return syscallNumber;
  }

  public RegisterRef getSyscallReturn() {
    return syscallReturn;
  }

  public List<RegisterRef> args() {
    return args;
  }

  public InstructionSetArchitecture isa() {
    return isa;
  }

  public Abi abi() {
    return abi;
  }

  public List<CustomSyscall> customSyscalls() {
    return customSyscalls;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return simpleName() + " [args=" + args + "]";
  }

}
