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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import vadl.lcb.templateUtils.RegisterUtils;
import vadl.types.BitsType;
import vadl.types.Type;
import vadl.utils.Pair;
import vadl.utils.SourceLocation;
import vadl.viam.graph.Graph;

/**
 * Represents the configuration for QEMU user-mode emulation.
 * <p>
 * This class defines architecture-specific parameters required for Linux-user emulation,
 * including system call register mappings, signal trampoline instructions, stack
 * alignment requirements, and exception IDs.
 * </p>
 */
public class UserModeEmulation extends Definition {

  private final Identifier excCauseVar;
  private final RegisterTensor mainRegisterFile;
  private final RegisterUtils.Register sysReg;
  private final RegisterUtils.Register retReg;
  private final RegisterUtils.Register spReg;
  private final RegisterUtils.Register raReg;
  private final RegisterUtils.Register tpReg;
  private final List<RegisterTensor> signalStateTensors;
  private final List<RegisterUtils.Register> args;
  private final Map<String, Integer> excIds;
  private final Instruction syscallInstr;
  private final ExceptionDef syscallException;
  private final ExceptionDef breakpointExcName;
  private final ExceptionDef illegalInstrExcName;
  private final Identifier ptRegPc;
  private final Identifier ptRegSp;
  private final boolean hasIcacheFlush;
  private final int insnWidthBytes;
  private final int stackAlignMask;
  private final int sigtrampLoadSyscallInstr;
  private final int sigtrampTrapInstr;

  /**
   * Constructs a UserModeEmulation configuration.
   */
  public UserModeEmulation(
      Identifier identifier, ExceptionDef syscallException,
      RegisterTensor mainRegisterFile,
      RegisterUtils.Register sysReg, RegisterUtils.Register retReg,
      RegisterUtils.Register spReg, RegisterUtils.Register raReg, RegisterUtils.Register tpReg,
      List<RegisterTensor> signalStateTensors,
      List<RegisterUtils.Register> args, Map<String, Integer> excIds,
      Instruction syscallInstr, ExceptionDef breakpointExcName,
      ExceptionDef illegalInstrExcName,
      Identifier ptRegPc, Identifier ptRegSp, Identifier excCauseVar, boolean hasIcacheFlush,
      int insnWidthBytes, int stackAlignMask, int sigtrampLoadSyscallInstr,
      int sigtrampTrapInstr) {

    super(identifier);
    this.syscallException = syscallException;
    this.mainRegisterFile = mainRegisterFile;
    this.signalStateTensors = signalStateTensors;
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
    this.syscallInstr = syscallInstr;
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
  public static UserModeEmulation createDummySolution() {
    Identifier identifier = new Identifier(new String[]{"ume"},
        SourceLocation.INVALID_SOURCE_LOCATION);

    RegisterTensor.Dimension regDim = new RegisterTensor.Dimension(
        0,
        Type.bits(5),
        32
    );

    RegisterTensor.Dimension dummyDim = new RegisterTensor.Dimension(
        1,
        Type.bits(1),
        1
    );

    List<RegisterTensor.Dimension> dimensions = List.of(regDim, dummyDim);
    RegisterTensor mainFile = new RegisterTensor(
        new Identifier(new String[]{"x"},
            SourceLocation.INVALID_SOURCE_LOCATION),
        dimensions
    );

    var dummyMap = new HashMap<Pair<RegisterResource, Integer>, List<Abi.RegisterAlias>>();

    RegisterUtils.RegisterClass gprClass = RegisterUtils.getRegisterClass(mainFile, dummyMap);

    RegisterUtils.Register sp = gprClass.registers().get(2);
    RegisterUtils.Register ra = gprClass.registers().get(1);
    RegisterUtils.Register tp = gprClass.registers().get(4);
    RegisterUtils.Register sys = gprClass.registers().get(17);
    RegisterUtils.Register ret = gprClass.registers().get(10);

    List<RegisterUtils.Register> args = IntStream.range(10, 16)
        .mapToObj(i -> gprClass.registers().get(i))
        .toList();

    Parameter[] emptyParams = new Parameter[0];
    Graph emptyGraph = new Graph("empty_graph");

    ExceptionDef mockSyscallExc = new ExceptionDef(
        new Identifier(new String[]{"EXC"},
            SourceLocation.INVALID_SOURCE_LOCATION),
        emptyParams,
        emptyGraph,
        ExceptionDef.Kind.DECLARED
    );

    ExceptionDef mockBreakpointExc = new ExceptionDef(
        new Identifier(new String[]{"BREAKPOINT"},
            SourceLocation.INVALID_SOURCE_LOCATION),
        emptyParams,
        emptyGraph,
        ExceptionDef.Kind.DECLARED
    );

    ExceptionDef mockIllegalExc = new ExceptionDef(
        new Identifier(new String[]{"ILLEGAL_INSTR"},
            SourceLocation.INVALID_SOURCE_LOCATION),
        emptyParams,
        emptyGraph,
        ExceptionDef.Kind.DECLARED
    );

    BitsType mockType = BitsType.bits(32);

    Function mockFunc = new Function(
        new Identifier(new String[]{"dummy_function"},
            SourceLocation.INVALID_SOURCE_LOCATION),
        new Parameter[0],
        Type.string(),
        emptyGraph
    );

    Assembly emptyAssembly = new Assembly(new Identifier(new String[]{"dummy_assembly"},
        SourceLocation.INVALID_SOURCE_LOCATION), mockFunc);

    Format dummyFormat = new Format(new Identifier(new String[]{"dummy_format"},
        SourceLocation.INVALID_SOURCE_LOCATION), mockType);

    Encoding emptyEncoding = new Encoding(
        new Identifier(new String[]{"dummy_encoding"},
            SourceLocation.INVALID_SOURCE_LOCATION),
        dummyFormat, new Encoding.Field[0]);

    Instruction mockSyscallInsn = new Instruction(
        new Identifier(new String[]{"ECALL"},
            SourceLocation.INVALID_SOURCE_LOCATION),
        emptyGraph, emptyAssembly, emptyEncoding
    );


    Identifier riscvCauseVar = new Identifier(new String[]{"cause"},
        SourceLocation.INVALID_SOURCE_LOCATION);

    Map<String, Integer> excIds = Map.of(
        "ILLEGAL_INSTR", 2,
        "BREAKPOINT", 3,
        "ECALL", 11
    );

    Identifier ptRegsPcField = new Identifier(new String[]{"sepc"},
        SourceLocation.INVALID_SOURCE_LOCATION);
    Identifier ptRegsSpField = new Identifier(new String[]{"sp"},
        SourceLocation.INVALID_SOURCE_LOCATION);

    List<RegisterTensor> signalStateTensors = List.of(mainFile);

    return new UserModeEmulation(
        identifier,
        mockSyscallExc,
        mainFile,
        sys, ret, sp, ra, tp,
        signalStateTensors, args,
        excIds,
        mockSyscallInsn,
        mockBreakpointExc,
        mockIllegalExc,
        ptRegsPcField, ptRegsSpField, riscvCauseVar,
        true, 4, 0xf,
        0x08b00893, 0x00000073
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

  public Identifier getPtRegPc() {
    return ptRegPc;
  }

  public Identifier getPtRegSp() {
    return ptRegSp;
  }

  public Identifier getExcCauseVar() {
    return excCauseVar;
  }

  public boolean hasIcacheFlush() {
    return hasIcacheFlush;
  }

  public Instruction getSyscallInstr() {
    return syscallInstr;
  }

  public ExceptionDef getBreakpointExcName() {
    return breakpointExcName;
  }

  public ExceptionDef getIllegalInstrExcName() {
    return illegalInstrExcName;
  }

  public RegisterUtils.Register getSysReg() {
    return sysReg;
  }

  public RegisterUtils.Register getRetReg() {
    return retReg;
  }

  public RegisterUtils.Register getSpReg() {
    return spReg;
  }

  public RegisterUtils.Register getRaReg() {
    return raReg;
  }

  public RegisterUtils.Register getTpReg() {
    return tpReg;
  }

  public List<RegisterUtils.Register> getArgs() {
    return args;
  }

  public Map<String, Integer> getExcIds() {
    return excIds;
  }

  public ExceptionDef getSyscallException() {
    return syscallException;
  }

  public RegisterTensor getMainRegisterFile() {
    return mainRegisterFile;
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

  public List<RegisterTensor> getSignalStateTensors() {
    return signalStateTensors;
  }
}
