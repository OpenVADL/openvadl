package vadl.dump;

import java.util.List;
import java.util.Map;
import vadl.pass.PassKey;
import vadl.viam.Specification;

public interface DumpEntitySupplier<T extends DumpEntity> {

  List<T> getEntities(Specification spec, Map<PassKey, Object> passResults);

}
