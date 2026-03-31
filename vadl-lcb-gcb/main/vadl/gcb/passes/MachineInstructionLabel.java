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

package vadl.gcb.passes;

import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
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
  SUB_RR_WITH_STATUS_REGISTER_64, // subtraction (register register) and sets status flags
  SUB_RR_WITH_STATUS_REGISTER_32, // subtraction (register register) and sets status flags
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
  STORE_MEM_WITH_IMMEDIATE,
  LOAD_MEM_WITH_IMMEDIATE,
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
  BEQ_BY_STATUS_REGISTER,
  BNEQ_BY_STATUS_REGISTER,
  BSGEQ_BY_STATUS_REGISTER,
  BSLEQ_BY_STATUS_REGISTER,
  BSLTH_BY_STATUS_REGISTER,
  BSGTH_BY_STATUS_REGISTER,
  /*
  UNCONDITIONAL JUMPS
   */
  JALR, //  unconditional indirect jump without immediate. With linking register.
  JAL, // the difference between JAL and J is that JAL also writes a linking register.
  J, // unconditional jump with no linking register.
  JR, //  unconditional indirect jump without immediate. No linking register.
  /*
  CONDITIONAL MOVE
   */
  CSEL_EQ_I32,
  CSEL_EQ_I64,
  CSEL_NEQ_I32,
  CSEL_NEQ_I64
}
