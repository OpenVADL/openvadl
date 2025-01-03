package vadl.lcb.passes.llvmLowering.domain.machineDag;

import vadl.viam.Instruction;

/**
* Name of the instruction which should be emitted.
* This can be {@link Instruction} or generated.
*/
public record OutputInstructionName(String value) {
}
