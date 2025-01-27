package vadl.iss.codegen;

import static vadl.error.DiagUtils.throwNotAllowed;

import vadl.cppCodeGen.context.CGenContext;
import vadl.iss.passes.opDecomposition.nodes.IssExprNode;
import vadl.javaannotations.Handler;
import vadl.viam.graph.Node;

public interface IssCMixins {

  interface Invalid extends IssMul2 {

  }

  interface IssMul2 {

    @Handler
    default void impl(CGenContext<Node> ctx, IssExprNode node) {
      throwNotAllowed(node, "IssExprNode");
    }

  }

}
