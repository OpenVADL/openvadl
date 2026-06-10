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
 * Instructions can only be classified with a single label.
 */
public enum MachineInstructionLabel {
  /*
   Instructions, which write the left-shifted-value (shifted by a constant) of a field-access into 
   a register-file.
   */
  LUI,

  /*
  ARITHMETIC AND LOGIC
   */
  /*
   Instructions, which add two registers (using only the builtins ADD/ADDS) and write the result
   into a register-file of the given size. (ADD_32 restrict the result to 32-bit, ADD_64 to 64-bit).
   */
  ADD_32,
  ADD_64,
  /*
   Instructions, which add a register and an immediate (using only the builtins ADD/ADDS) and 
   write the result into a register-file of the given size. 
   (ADDI_32 restrict the result to 32-bit, ADDI_64 to 64-bit)

   No more than one register-read is allowed.
   */
  ADDI_32,
  ADDI_64,
  /*
   Instructions, which 'and'  two registers, or a register and an immediate 
   (using only the vadl-builtins AND/ANDS), and do not access the Program-Counter in any way.
  */
  AND,
  /*
   Instructions, which 'or' / 'xor' two registers (using only the vadl-builtins OR/ORS; XOR/XORS)
   and do not access the Program-Counter in any way.
  */
  OR,
  XOR,
  /*
   Instructions, which 'or' / 'xor'  a register and an immediate 
   (using only the vadl-builtins OR/ORS ; XOR/XORS) and do not access the Program-Counter 
   in any way.
  */
  ORI,
  XORI,
  /*
   Instructions, which subtract  on two registers, or a register and an immediate, 
   (use only the vadl-builtins SUB for 'SUB' ; SUBB/SUBSB for 'SUBB' ; SUBC/SUBSC for 'SUBC')
   and do not access the Program-Counter in any way.
  */
  SUB,
  SUBB,
  SUBC,
  /*
   Instructions, which subtract two registers (using only the vadl-builtin 'SUBSC')
   and set the Carry-, Negative-, Overflow- and Zero-Status-Registers in the process. Furthermore,
   the instructions do not access the Program-Counter in any way.
  */
  SUB_RR_WITH_STATUS_REGISTER_64,
  SUB_RR_WITH_STATUS_REGISTER_32,
  /*
   Instructions, which multiply two registers (using the vadl-builtin MUL/MULS/SMULL/SMULLS).
   */
  MUL,
  /*
   Instructions, which divide two registers (using the vadl-builtin SDIV/SDIVS for 'SDIV' ; 
   UDIV/UDIVS for 'UDIV'), write the result in a register and do not access the Program-Counter
   in any way.
   */
  SDIV, // signed division
  UDIV, // unsigned division
  /*
   Instructions, which compute the modulo of two registers (using the vadl-builtin
   SMOD/SMODS for 'SMOD' ; UMOD/UMODS for 'UMOD'), write the result into a register
   and do not access the Program-Counter in any way.
   */
  SMOD, // signed modulo
  UMOD, // unsigned modulo
  /*
   Instructions which multiply two registers (using the vadl-builtin UMULL/UMULLS for 'MULHU' ;
   SMULL/SMULLS for 'MULHS') and write the upper half of the result into a register-file.
   */
  MULHU,
  MULHS,
  /*
   Instructions, which left-shift a register with another register
   (using the vadl-builtin LSL/LSLS), write the result into a register and do not access the
   Program-Counter in any way.
   */
  SLL,
  /*
   Instructions, which left-shift a register with an immediate (using the vadl-builtin LSL/LSLS), 
   write the result into a register and do not access the Program-Counter in any way. 
   Furthermore, the result may not be sign-extended.
   */
  SLLI,
  /*
   Instructions, which right-shift a register with another register
   (using the vadl-builtin LSR/LSRS) and do not access the Program-Counter in any way.
   */
  SRL,
  ROTL, // currently unused

  /*
  COMPARISONS
   */
  EQ,   // currently unused
  NEQ,  // currently unused
  /*
   Instructions, which compare two registers using less-than 
   (using the vadl-builtin ULTH for 'LTU' / SLTH for 'LTS') and do not access the Program-Counter 
   in any way.
   */
  LTU,  // unsigned less-than
  LTS,  // signed less-than
  /*
   Instructions, which compare register and an immediate using less-than 
   (using the vadl-builtin ULTH for 'LTU' / SLTH for 'LTS') and do not access the Program-Counter 
   in any way.
   */
  LTI,  // less than immediate
  LTIU, // less than immediate unsigned

  /* 
  MEMORY 
  */
  /*
   Instructions, which write the content of a register-file, indexed by an 
   immediate, to a single memory location, and where no other memory- or register-writes happen.
   */
  STORE_MEM_WITH_IMMEDIATE,
  /*
   Instructions, which read a single value from memory once and write it
   back into an indexed register-file (no single-register writes allowed). The memory-address must 
   be computed by addition, using at least one immediate.
   */
  LOAD_MEM_WITH_IMMEDIATE,

  /*
   CONDITIONAL BRANCHES using vadl builtins as condition.

   Instructions, which write to the Program-Counter-Register 
   based on a given condition, using an if. The conditions are determined by the vadl-builtin
   being used.

   There is no restriction on what is being written into the PC.
   */
  BEQ,    // equal condition
  BNEQ,   // not-equal condition
  BSGEQ,  // signed-greater-equal condition
  BSLEQ,  // signed-less-equal conditoon
  BSLTH,  // signed-less-than condition
  BSGTH,  // signed-greater-than condition
  BUGEQ,  // unsigned-greater-equal condition
  BULEQ,  // unsigned-less-equal condition
  BULTH,  // unsigned-less-than condition
  BUGTH,  // unsigned-greater-than condition

  /*
   CONDITIONAL BRANCHES using status-register-comparisons as condition.

   Instructions, which write to the Program-Counter-Register
   based on a given condition, using an if. The conditons are deduced from the 
   status-register-comparisons being done.

   There is no restriction on what is being written into the PC.
   */
  BEQ_BY_STATUS_REGISTER,    // equal condition
  BNEQ_BY_STATUS_REGISTER,   // not-equal condition
  BSGEQ_BY_STATUS_REGISTER,  // signed-greater-equal condition
  BSLEQ_BY_STATUS_REGISTER,  // signed-less-equal conditoon
  BSLTH_BY_STATUS_REGISTER,  // signed-less-than condition
  BSGTH_BY_STATUS_REGISTER,  // signed-greater-than condition
  B_CS,                      // carry-clear condition
  B_CC,                      // carry-set condition

  /*
   UNCONDITIONAL JUMPS
   */
  /*
   Instructions, which write to the Program-Counter, to a register-file and perform an operation
   (ADD / ADDS / SUB) on a register and a second operand.
   */
  JALR,
  /*
   Instructions, which write a register-file to the Program-Counter and do not use any immediates.
   */
  BLR,
  /*
   Instructions, which modify the Program-Counter with an operation (ADD/ADDS/SUB) involving the PC
   and write to a register(-file).
   */
  JAL,
  /*
   Instructions, which modify the Program-Counter with an addition involving an immediate, and do 
   not write to any register(-file).
   */
  J,
  /*
   Instructions, which write a register(-file) to the Program-Counter and do not use any immediates.
   */
  JR,

  /*
   CONDITIONAL SELECT

   Instructions, which conditionally write the contents of a register
   into a target register, using a select.

   The conditions are determined based on status-registers (abbreviated SR in the following
   comments) (Zero-, Carry-, Overflow-, Negative-SR) used in the select-condition.

   The 32-bit / 64-bit variants are determined based on whether the register-read is truncated to
   32-bits.
   */
  CSEL_EQ_I32,    // equals condition: Zero-SR == 1
  CSEL_EQ_I64,

  CSEL_NEQ_I32,   // not equals condition: Zero-SR == 0
  CSEL_NEQ_I64,

  CSEL_SLTH_I32,  // signed-less-than condition: Zero-SR != Overflow-SR
  CSEL_SLTH_I64,

  CSEL_SGTH_I32,  // signed-greater-than condition: Negative-SR == Overflow-SR && Zero-SR == 0
  CSEL_SGTH_I64,

  CSEL_SLEQ_I32,  // signed-less-or-equal conditon: Negative-SR != Overflow-SR || Zero-SR == 1
  CSEL_SLEQ_I64,

  CSEL_SGEQ_I32,  // signed-greater-or-equal condition: Negative-SR == Overflow-SR
  CSEL_SGEQ_I64,

  CSEL_CC_I32,  // carry-clear condition: Carry-SR == 0
  CSEL_CC_I64,

  CSEL_CS_I32,  // carry-set condition: Carry-SR == 1
  CSEL_CS_I64,
}
