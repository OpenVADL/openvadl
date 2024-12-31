package vadl.iss.passes.decode.dto;

import vadl.viam.Definition;

/**
 * Interface for objects that can be mapped back to a source definition.
 */
public interface SourceMapping {
  /**
   * Get the source definition of the object.
   *
   * @return The source definition
   */
  Definition getSource();
}
