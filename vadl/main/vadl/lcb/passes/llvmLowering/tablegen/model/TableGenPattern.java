package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.viam.graph.Graph;

/**
 * TableGen pattern has a tree for LLVM Dag nodes to select a pattern in the instruction
 * selection. This is represented by {@code selector}.
 * And a tree for the emitted machine instruction. This is represented by {@code machine}.
 */
public record TableGenPattern(Graph selector, Graph machine) {
}
