package vadl.gcb.passes;

import java.util.IdentityHashMap;
import java.util.Optional;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Format;
import vadl.viam.Instruction;

/**
 * An extension for the {@link Instruction}. It will be used to indicate what value
 * ranges the instruction's immediates have.
 */
public class ValueRangeCtx extends DefinitionExtension<Instruction> {
  private final IdentityHashMap<Format.Field, ValueRange> ranges;

  public ValueRangeCtx() {
    this.ranges = new IdentityHashMap<>();
  }

  /**
   * Add a new range.
   */
  public void add(Format.Field field, ValueRange range) {
    ranges.put(field, range);
  }

  /**
   * Get the first range.
   */
  public Optional<ValueRange> getFirst() {
    return this.ranges.values().stream().findFirst();
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Definition.class;
  }
}
