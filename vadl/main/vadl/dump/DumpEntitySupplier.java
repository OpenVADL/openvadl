package vadl.dump;

import java.util.List;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * DumpEntitySuppliers produce a list of {@link DumpEntity}s from a given VIAM specification
 * and PassResults.
 * Those entities are rendered as boxes in the HTML dump.
 * The most important supplier is the {@link vadl.dump.supplier.ViamEntitySupplier} that
 * returns a list of {@link vadl.dump.supplier.ViamEntitySupplier.DefinitionEntity}s representing
 * all definitions in the VIAM specification.
 */
public interface DumpEntitySupplier<T extends DumpEntity> {

  /**
   * Returns a list of DumpEntities produced from the given spec and pass results.
   */
  List<T> getEntities(Specification spec, PassResults passResults);

}
