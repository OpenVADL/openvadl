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
import java.util.Map;
import vadl.utils.SourceLocation;

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
   * Returns a map representation of this UserModeEmulation for template rendering.
   */
  public Map<String, Object> asMap() {
    return Map.ofEntries(
      Map.entry("sysReg", sysReg),
      Map.entry("retReg", retReg),
      Map.entry("spReg", spReg),
      Map.entry("raReg", raReg),
      Map.entry("tpReg", tpReg),
      Map.entry("args", args),
      Map.entry("excIds", excIds),
      Map.entry("SYSCALL_NAME", syscallExcName),
      Map.entry("BREAKPOINT_NAME", breakpointExcName),
      Map.entry("ILLEGAL_INSTR_NAME", illegalInstrExcName),
      Map.entry("ptRegPc", ptRegPc),
      Map.entry("ptRegSp", ptRegSp),
      Map.entry("excCauseVar", excCauseVar),
      Map.entry("hasIcacheFlush", hasIcacheFlush),
      Map.entry("insn_width_bytes", insnWidthBytes),
      Map.entry("stack_align_mask", stackAlignMask),
      Map.entry("sigtrampLoadSyscallInstr", sigtrampLoadSyscallInstr),
      Map.entry("sigtrampTrapInstr", sigtrampTrapInstr)
      );
  }

  private final int sysReg;
  private final int retReg;
  private final int spReg;
  private final int raReg;
  private final int tpReg;
  private final List<Integer> args;
  private final Map<String, Integer> excIds;
  private final String syscallExcName;
  private final String breakpointExcName;
  private final String illegalInstrExcName;
  private final String ptRegPc;
  private final String ptRegSp;
  private final String excCauseVar;
  private final boolean hasIcacheFlush;
  private final int insnWidthBytes;
  private final int stackAlignMask;
  private final int sigtrampLoadSyscallInstr;
  private final int sigtrampTrapInstr;

  /**
   * Constructs a UserModeEmulation configuration.
   */
  public UserModeEmulation(
      Identifier identifier,
      int sysReg, int retReg, int spReg, int raReg, int tpReg,
      List<Integer> args, Map<String, Integer> excIds,
      String syscallExcName, String breakpointExcName, String illegalInstrExcName,
      String ptRegPc, String ptRegSp, String excCauseVar, boolean hasIcacheFlush,
      int insnWidthBytes, int stackAlignMask, int sigtrampLoadSyscallInstr,
      int sigtrampTrapInstr) {

    super(identifier);
    if (args == null || args.isEmpty()) {
      throw new IllegalArgumentException("args must not be null/empty");
    }

    if (excIds == null || excIds.isEmpty()) {
      throw new IllegalArgumentException("excIds must not be null/empty");
    }

    this.sysReg = sysReg;
    this.retReg = retReg;
    this.spReg = spReg;
    this.raReg = raReg;
    this.tpReg = tpReg;
    this.args = args;
    this.excIds = excIds;
    this.syscallExcName = syscallExcName;
    this.breakpointExcName = breakpointExcName;
    this.illegalInstrExcName = illegalInstrExcName;
    this.ptRegPc = ptRegPc;
    this.ptRegSp = ptRegSp;
    this.excCauseVar = excCauseVar;
    this.hasIcacheFlush = hasIcacheFlush;
    this.insnWidthBytes = insnWidthBytes;
    this.stackAlignMask = stackAlignMask;
    this.sigtrampLoadSyscallInstr = sigtrampLoadSyscallInstr;
    this.sigtrampTrapInstr = sigtrampTrapInstr;
  }

  /**
   * Creates a default {@link UserModeEmulation} configuration,
   * pre-configured for the RISC-V architecture.
   * * @return a standard RISC-V user-mode emulation setup.
   */
  public static UserModeEmulation createDefault() {
    Identifier identifier = new Identifier(new String[]{"ume"},
        SourceLocation.INVALID_SOURCE_LOCATION);
    Map<String, Integer> excIds = Map.of("ILLEGAL_INSTR",
        2, "BREAKPOINT", 3, "ECALL", 11);

    return new UserModeEmulation(
        identifier,
        17, 10, 2, 1, 4,
        List.of(10, 11, 12, 13, 14, 15),
        excIds,
        "ECALL", "BREAKPOINT", "ILLEGAL_INSTR",
        "sepc", "sp", "arg_exc_cause", true,
        4, 0xf, 0x08b00893, 0x00000073
    );
  }

  public int getSigtrampLoadSyscallInstr() {
    return sigtrampLoadSyscallInstr;
  }

  public int getSigtrampTrapInstr() {
    return sigtrampTrapInstr;
  }

  public int getStackAlignMask() {
    return stackAlignMask;
  }

  public int getInsnWidthBytes() {
    return insnWidthBytes;
  }

  public String getPtRegPc() {
    return ptRegPc;
  }

  public String getPtRegSp() {
    return ptRegSp;
  }

  public String getExcCauseVar() {
    return excCauseVar;
  }

  public boolean hasIcacheFlush() {
    return hasIcacheFlush;
  }

  public String getSyscallExcName() {
    return syscallExcName;
  }

  public String getBreakpointExcName() {
    return breakpointExcName;
  }

  public String getIllegalInstrExcName() {
    return illegalInstrExcName;
  }

  public int getSysReg() {
    return sysReg;
  }

  public int getRetReg() {
    return retReg;
  }

  public int getSpReg() {
    return spReg;
  }

  public int getRaReg() {
    return raReg;
  }

  public int getTpReg() {
    return tpReg;
  }

  public List<Integer> getArgs() {
    return args;
  }

  public Map<String, Integer> getExcIds() {
    return excIds;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return simpleName() + " [sysReg=" + sysReg + ", retReg=" + retReg + ", spReg=" + spReg
        + ", raReg=" + raReg + ", tpReg=" + tpReg + ", args=" + args + ", excIds=" + excIds + "]";
  }
}
