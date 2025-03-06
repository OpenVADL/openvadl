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


}
