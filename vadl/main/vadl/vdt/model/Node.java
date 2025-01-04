package vadl.vdt.model;

import javax.annotation.Nullable;

/**
 * Represents a single node in the decode tree.
 */
public interface Node {

  <T> @Nullable T accept(Visitor<T> visitor);

}
