package vadl.viam.passes;

import vadl.viam.graph.Node;

/**
 * A helper construct to define a tuple.
 */
public record Pair(Node oldNode, Node newNode) {
}
