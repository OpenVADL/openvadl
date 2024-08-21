package vadl.dump;

import java.util.Map;
import vadl.pass.PassKey;

public interface InfoEnricher {

  void enrich(DumpEntity entity, Map<PassKey, Object> passResults);
}
