package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.viam.graph.Graph;

/**
 * This has the same properties like {@link TableGenPattern} but an explicit class
 * makes it easier to filter for it.
 */
public class TableGenSelectionPattern extends TableGenPattern {
  public TableGenSelectionPattern(Graph selector) {
    super(selector);
  }
}
