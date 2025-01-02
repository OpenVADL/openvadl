package vadl.vdt.model;

import javax.annotation.Nullable;

public interface Node {

  <T> @Nullable T accept(Visitor<T> visitor);

}
