package vadl.lcb.passes.isaMatching;

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
