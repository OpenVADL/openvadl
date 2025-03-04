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

package vadl.lcb.passes.isaMatching;

import java.util.HashMap;
import javax.annotation.Nullable;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmCondCode;
import vadl.viam.Instruction;

/**
 * A collection of labels for a {@link Instruction}.
 * The {@link IsaMachineInstructionMatchingPass} tries to assign each {@link Instruction} a
 * {@link MachineInstructionLabel}. This label can be used to quickly search for instructions.
 * How do I read this?
 * You can say the following: An {@link Instruction} with the semantics of summing two unsigned
 * 32bit registers will get the {@link MachineInstructionLabel#ADD_32} assigned.
 */
public enum MachineInstructionLabel {
  LUI,
  /*
  ARITHMETIC AND LOGIC
   */
  ADD_32,
  ADD_64,
  ADDI_32,
  ADDI_64,
  AND,
  OR,
  ORI,
  SUB,
  MUL,
  SUBB,
  SUBC,
  SDIV,
  UDIV,
  SMOD,
  UMOD,
  XOR,
  XORI,
  MULHU,
  MULHS,
  SLL,
  SLLI,
  SRL,
  ROTL,
  /*
  COMPARISONS
   */
  EQ,
  NEQ,
  LTU,
  LTS,
  LTI, // less than immediate
  LTIU, // less than immediate unsigned
  /*
  MEMORY
   */
  STORE_MEM,
  LOAD_MEM,
  /*
  CONDITIONAL BRANCHES
   */
  BEQ,
  BNEQ,
  BSGEQ,
  BSLEQ,
  BSLTH,
  BSGTH,
  BUGEQ,
  BULEQ,
  BULTH,
  BUGTH,
  /*
  UNCONDITIONAL JUMPS
   */
  JALR,
  JAL,
  /*
  CONDITIONAL MOVE
   */
  CMOVE_32,
  CMOVE_64;

  /**
   * The names of LLVM's cond code given a machine instruction label.
   */
  public static final HashMap<MachineInstructionLabel, LlvmCondCode> branchInstructionMapping =
      new HashMap<>();

  static {
    branchInstructionMapping.put(MachineInstructionLabel.BEQ, LlvmCondCode.SETEQ);
    branchInstructionMapping.put(MachineInstructionLabel.BSGEQ, LlvmCondCode.SETGE);
    branchInstructionMapping.put(MachineInstructionLabel.BSGTH, LlvmCondCode.SETGT);
    branchInstructionMapping.put(MachineInstructionLabel.BSLEQ, LlvmCondCode.SETLE);
    branchInstructionMapping.put(MachineInstructionLabel.BSLTH, LlvmCondCode.SETLT);
    branchInstructionMapping.put(MachineInstructionLabel.BUGEQ, LlvmCondCode.SETUGE);
    branchInstructionMapping.put(MachineInstructionLabel.BUGTH, LlvmCondCode.SETUGT);
    branchInstructionMapping.put(MachineInstructionLabel.BULEQ, LlvmCondCode.SETULE);
    branchInstructionMapping.put(MachineInstructionLabel.BULTH, LlvmCondCode.SETULT);
    branchInstructionMapping.put(MachineInstructionLabel.BNEQ, LlvmCondCode.SETNE);
  }

  /**
   * Return the {@link LlvmCondCode} given a branch {@link MachineInstructionLabel}.
   * This method will return {@code null} when there is no mapping or the
   * {@link MachineInstructionLabel} is not a branch.
   */
  @Nullable
  public static LlvmCondCode getLlvmCondCodeByLabel(MachineInstructionLabel label) {
    return branchInstructionMapping.get(label);
  }
}
