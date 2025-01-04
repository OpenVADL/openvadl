package vadl.dump.entitySuppliers;

import java.util.List;
import vadl.dump.DumpEntitySupplier;
import vadl.dump.entities.VdtEntity;
import vadl.pass.PassResults;
import vadl.vdt.model.Node;
import vadl.vdt.passes.VdtLoweringPass;
import vadl.viam.Specification;

/**
 * A {@link DumpEntitySupplier} that produces a {@link VdtEntity} for the VDT tree.
 */
public class VdtEntitySupplier implements DumpEntitySupplier<VdtEntity> {

  @Override
  public List<VdtEntity> getEntities(Specification spec, PassResults passResults) {

    if (!passResults.hasRunPassOnce(VdtLoweringPass.class)) {
      // Nothing to do here
      return List.of();
    }

    var vdt = passResults.lastResultOf(VdtLoweringPass.class, Node.class);
    return List.of(new VdtEntity(vdt));
  }
}
