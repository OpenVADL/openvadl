package vadl.dump.entities;

import vadl.dump.DumpEntity;
import vadl.vdt.model.Node;

/**
 * A {@link DumpEntity} that represents the VADL decode tree.
 */
public class VdtEntity extends DumpEntity {

  Node tree;

  /**
   * Creates a new VDT entity.
   *
   * @param tree The VDT tree.
   */
  public VdtEntity(Node tree) {
    this.tree = tree;
  }

  /**
   * Returns the VDT tree.
   */
  public Node tree() {
    return tree;
  }

  @Override
  public String cssId() {
    return "vadl-decode-tree";
  }

  @Override
  public TocKey tocKey() {
    return new TocKey("Decoder", 100);
  }

  @Override
  public String name() {
    return "Decode Tree";
  }
}
