package vadl.iss.passes.opDecomposition.nodes;

/**
 * This defines the kind of multiplication for all ISS-related multiplication nodes.
 * Instead of creating three node classes for some kind of multiplication, we just create
 * one with a {@code kind} field, that defines the type of multiplication.
 */
public enum IssMulKind {
  SIGNED_SIGNED,
  UNSIGNED_UNSIGNED,
  SIGNED_UNSIGNED
}
