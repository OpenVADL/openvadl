package vadl.viam.graph.dependency;

import vadl.viam.graph.Node;
import vadl.viam.graph.UniqueNode;

/**
 * Dependency nodes are part of the Data Dependency Graph and
 * are not fixed to some order of emitting. They are not part of the
 * control flow and do serve as dependencies for other nodes of any kind.
 */
public abstract class DependencyNode extends Node implements UniqueNode {


}
