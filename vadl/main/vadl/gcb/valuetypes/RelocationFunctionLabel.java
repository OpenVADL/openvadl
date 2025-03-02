package vadl.gcb.valuetypes;

import vadl.lcb.passes.isaMatching.IsaRelocationMatchingPass;
import vadl.viam.Relocation;

/**
 * A collection of labels for a {@link Relocation}.
 * The {@link IsaRelocationMatchingPass} tries to assign each {@link Relocation} a
 * {@link RelocationFunctionLabel}.
 */
public enum RelocationFunctionLabel {
  LO,
  HI,
  UNKNOWN
}
