package vadl.dump;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import vadl.pass.PassKey;
import vadl.pass.PassResults;
import vadl.viam.Specification;

public interface InfoEnricher {

  void enrich(DumpEntity entity, PassResults passResults);

  static <T extends DumpEntity> InfoEnricher forType(Class<T> type,
                                                     BiConsumer<T, PassResults> enricher) {
    return (e, pr) -> {
      if (type.isInstance(e)) {
        //noinspection unchecked
        enricher.accept((T) e, pr);
      }
    };
  }
}
