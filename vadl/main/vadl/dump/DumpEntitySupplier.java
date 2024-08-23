package vadl.dump;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import vadl.pass.PassKey;
import vadl.pass.PassResults;
import vadl.viam.Specification;

public interface DumpEntitySupplier<T extends DumpEntity> {

  List<T> getEntities(Specification spec, PassResults passResults);

}
