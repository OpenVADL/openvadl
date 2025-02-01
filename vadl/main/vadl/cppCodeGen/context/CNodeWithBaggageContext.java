package vadl.cppCodeGen.context;

import static vadl.viam.ViamError.ensureNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import vadl.viam.graph.Node;

/**
 * A code generation context for {@link Node}s.
 * It extends the {@link CNodeContext} with additional information.
 * This is required when the handle methods do not have enough parameters.
 */
public class CNodeWithBaggageContext extends CNodeContext {

  private final Map<String, Object> baggage = new HashMap<>();

  /**
   * Construct a new code generation context for {@link Node}s.
   */
  public CNodeWithBaggageContext(CNodeContext context) {
    super(context.writer, context.prefix, context.dispatch);
  }

  /**
   * Get a string from the context.
   */
  public String getString(String key) {
    return get(key, String.class);
  }

  /**
   * Get an object from the context and cast it to {@code clazz}.
   */
  public <T> T get(String key, Class<T> clazz) {
    return ensureNonNull(clazz.cast(baggage.get(key)), "must not be null");
  }

  /**
   * Store a value into the context.
   */
  public CNodeWithBaggageContext put(String key, Object value) {
    baggage.put(key, value);
    return this;
  }
}
