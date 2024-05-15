package vadl.viam;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.types.BitsType;
import vadl.types.Type;

/**
 * The encoding for a specific instruction.
 *
 * <p>It holds instruction encoded fields available to the instruction.
 * Each field has a bit range and type that specifies the location.
 * Optionally a field also has a constant value if it is known in advance.</p>
 */
public class Encoding extends Definition {

  private final Format format;

  public Encoding(Identifier identifier, Format format) {
    super(identifier);
    this.format = format;
  }


  public Type type() {
    return format.type();
  }

}
