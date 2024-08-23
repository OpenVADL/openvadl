package vadl.lcb.passes.llvmLowering;

import vadl.viam.Instruction;
import vadl.viam.Memory;

/**
 * Marker interface that indicates that an {@link Instruction} may store from {@link Memory}.
 */
public interface LlvmMayStoreMemory {
}
