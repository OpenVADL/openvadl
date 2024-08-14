package vadl.lcb.passes.isaMatching;

import vadl.viam.Instruction;

/**
 * A collection of labels for a {@link Instruction}.
 * The {@link IsaMatchingPass} tries to assign each {@link Instruction} a
 * {@link InstructionLabel}. This label can be used to quickly search for instructions.
 * How do I read this?
 * You can say the following: An {@link Instruction} with the semantics of summing two unsigned
 * 32bit registers will get the {@link InstructionLabel#ADD_32} assigned.
 */
public enum InstructionLabel {
  ADD_32,
  ADD_64,
  ADDI_32,
  ADDI_64,
  BEQ,
  AND,
  OR,
  SUB, MUL, SUBB, SUBC, SDIV, UDIV, SMOD, UMOD, XOR,
}
