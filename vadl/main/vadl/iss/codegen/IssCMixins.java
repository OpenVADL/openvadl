package vadl.iss.codegen;

import static vadl.error.DiagUtils.throwNotAllowed;

import vadl.cppCodeGen.context.CGenContext;
import vadl.iss.passes.nodes.IssExtractNode;
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
  interface Invalid extends IssExpr {

  }

  /**
   * Bundles all valid ISS node mixins.
   */
  interface Default extends IssExtract {
  }

  /**
   * The invalid ISS Expr Node mixin.
   */
  interface IssExpr {

    @Handler
    default void impl(CGenContext<Node> ctx,
                      vadl.iss.passes.opDecomposition.nodes.IssExprNode node) {
      throwNotAllowed(node, "IssExprNode");
    }

  }

  /**
   * The ISS extract node rendering.
   */
  interface IssExtract {


    /**
     * Implements the C code representation of the {@link IssExtractNode}.
     */
    @Handler
    default void impl(CGenContext<Node> ctx,
                      IssExtractNode node) {
      var sign = node.isSigned() ? "s" : "u";
      ctx.wr("VADL_" + sign + "extract(")
          .gen(node.value())
          .wr("," + node.fromWidth())
          .wr(")");
    }

  }

}
