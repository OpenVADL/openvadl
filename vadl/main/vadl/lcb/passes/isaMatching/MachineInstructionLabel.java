package vadl.lcb.passes.isaMatching;

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
  /*
  ARITHMETIC AND LOGIC
   */
  ADD_32,
  ADD_64,
  ADDI_32,
  ADDI_64,
  AND,
  OR,
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
  /*
  COMPARISONS
   */
  LT,
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
  CMOVE_64,
}
