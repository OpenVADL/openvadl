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
import java.util.Map;
import java.util.Optional;
import vadl.utils.Pair;

/**
 * VADL ABI representation.
 */
public class Abi extends Definition {

  /**
   * Register Spilling Alignments.
   */
  public enum Alignment {
    NO_ALIGNMENT(-1),
    HALF_WORD(4),
    WORD(8),
    DOUBLE_WORD(16);

    private final int byteAlignment;

    Alignment(int byteAlignment) {
      this.byteAlignment = byteAlignment;
    }

    public String inBytes() {
      return byteAlignment + "";
    }

    public int byteAlignment() {
      return byteAlignment;
    }

    public int bitAlignment() {
      return byteAlignment() * 8;
    }
  }

  /**
   * Constructor.
   *
   * @param registerFile is the "parent" of the register.
   * @param addr         represents the index in a register file.
   *                     E.g., RISC-V's X11 would have {@code addr = 11}.
   * @param alignment    for the spilling of the register.
   */
  public record RegisterRef(RegisterFile registerFile,
                            int addr,
                            Alignment alignment) {
    public String render() {
      return registerFile.identifier.simpleName() + addr;
    }
  }

  /**
   * Value type for alias.
   */
  public record RegisterAlias(String value) {
  }


  private final RegisterRef returnAddress;
  private final RegisterRef stackPointer;
  private final RegisterRef globalPointer;
  private final RegisterRef framePointer;
  private final Optional<RegisterRef> threadPointer;


  private final Map<Pair<RegisterFile, Integer>, List<RegisterAlias>> aliases;
  private final List<RegisterRef> callerSaved;
  private final List<RegisterRef> calleeSaved;
  private final List<RegisterRef> argumentRegisters;
  private final List<RegisterRef> returnRegisters;
  private final PseudoInstruction returnSequence;
  private final PseudoInstruction callSequence;
  private final PseudoInstruction addressSequence;

  private final Alignment stackAlignment;

  /**
   * This property is stricter than `stackAlignment` because it
   * enforces the alignment at *all* times. This is e.g. also
   * for RISC-V required.
   */
  private final Alignment transientStackAlignment;

  private final Map<RegisterFile, Abi.Alignment> registerFileAlignment;

  /**
   * Constructor.
   */
  public Abi(Identifier identifier,
             RegisterRef returnAddress,
             RegisterRef stackPointer,
             RegisterRef framePointer,
             RegisterRef globalPointer,
             Optional<RegisterRef> threadPointer,
             Map<Pair<RegisterFile, Integer>, List<RegisterAlias>> aliases,
             List<RegisterRef> callerSaved,
             List<RegisterRef> calleeSaved,
             List<RegisterRef> argumentRegisters,
             List<RegisterRef> returnRegisters,
             PseudoInstruction returnSequence,
             PseudoInstruction callSequence,
             PseudoInstruction addressSequence,
             Alignment stackAlignment,
             Alignment transientStackAlignment,
             Map<RegisterFile, Abi.Alignment> registerFileAlignment
  ) {
    super(identifier);
    this.returnAddress = returnAddress;
    this.stackPointer = stackPointer;
    this.framePointer = framePointer;
    this.globalPointer = globalPointer;
    this.threadPointer = threadPointer;
    this.aliases = aliases;
    this.callerSaved = callerSaved;
    this.calleeSaved = calleeSaved;
    this.argumentRegisters = argumentRegisters;
    this.returnRegisters = returnRegisters;
    this.returnSequence = returnSequence;
    this.callSequence = callSequence;
    this.addressSequence = addressSequence;
    this.stackAlignment = stackAlignment;
    this.transientStackAlignment = transientStackAlignment;
    this.registerFileAlignment = registerFileAlignment;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }


  public RegisterRef returnAddress() {
    return returnAddress;
  }

  public RegisterRef stackPointer() {
    return stackPointer;
  }

  public RegisterRef framePointer() {
    return framePointer;
  }

  public RegisterRef globalPointer() {
    return globalPointer;
  }

  public Optional<RegisterRef> threadPointer() {
    return threadPointer;
  }

  public Map<Pair<RegisterFile, Integer>, List<RegisterAlias>> aliases() {
    return aliases;
  }

  public List<RegisterRef> callerSaved() {
    return callerSaved;
  }

  public List<RegisterRef> calleeSaved() {
    return calleeSaved;
  }

  public List<RegisterRef> argumentRegisters() {
    return argumentRegisters;
  }

  public List<RegisterRef> returnRegisters() {
    return returnRegisters;
  }

  public boolean hasFramePointer() {
    return true;
  }

  public PseudoInstruction returnSequence() {
    return returnSequence;
  }

  public PseudoInstruction callSequence() {
    return callSequence;
  }

  public PseudoInstruction addressSequence() {
    return addressSequence;
  }

  public Alignment stackAlignment() {
    return stackAlignment;
  }

  public Alignment transientStackAlignment() {
    return transientStackAlignment;
  }

  public Map<RegisterFile, Alignment> registerFileAlignment() {
    return registerFileAlignment;
  }
}
