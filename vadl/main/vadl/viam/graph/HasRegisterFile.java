package vadl.viam.graph;

import vadl.viam.RegisterFile;

/**
 * Interface to indicate that the implementing class has {@link RegisterFile}.
 */
public interface HasRegisterFile {
  /**
   * Get register file.
   */
  RegisterFile registerFile();
}
