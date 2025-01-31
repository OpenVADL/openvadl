package vadl.iss.codegen;

import static vadl.error.DiagUtils.throwNotAllowed;

import vadl.cppCodeGen.context.CGenContext;
import vadl.iss.passes.opDecomposition.nodes.IssExprNode;
import vadl.javaannotations.Handler;
import vadl.viam.graph.Node;

/**
 * The ISS C mixins for all ISS intermediate nodes added to behaviors.
 * Most of those nodes are replaced before code generation and therefore
 * crash by default if they are getting emitted.
 */
public interface IssCMixins {

  /**
   * Bundles all Invalid ISS node mixins.
   */
  interface Invalid extends IssMul2 {

  }

  /**
   * The invalid ISS MUL2 mixin.
   */
  interface IssMul2 {

    @Handler
    default void impl(CGenContext<Node> ctx, IssExprNode node) {
      throwNotAllowed(node, "IssExprNode");
    }

  }

}
