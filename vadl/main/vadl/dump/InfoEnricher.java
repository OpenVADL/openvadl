package vadl.dump;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import vadl.pass.PassKey;
import vadl.viam.Specification;

public interface InfoEnricher {

  void enrich(DumpEntity entity, Map<PassKey, Object> passResults);

  static <T extends DumpEntity> InfoEnricher forType(Class<T> type,
                                                     BiConsumer<T, Map<PassKey, Object>> enricher) {
    return (e, pr) -> {
      if (type.isInstance(e)) {
        //noinspection unchecked
        enricher.accept((T) e, pr);
      }
    };
  }
}
