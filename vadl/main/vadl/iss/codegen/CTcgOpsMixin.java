package vadl.iss.codegen;

import java.io.StringWriter;
import vadl.cppCodeGen.CodeGenerator;
import vadl.cppCodeGen.mixins.CGenMixin;
import vadl.iss.passes.nodes.IssStaticPcRegNode;
import vadl.iss.passes.tcgLowering.nodes.TcgNode;
import vadl.viam.graph.Node;

/**
 * A mixin to add C generation support for TCG operations in the ISS.
 */
public interface CTcgOpsMixin extends CGenMixin {

  /**
   * Adds the C gen TCG node implementations to the given impls.
   */
  default void tcgOpImpls(CodeGenerator.Impls<Node> impls) {
    impls

        .set(TcgNode.class, (node, writer) -> {
          var c = node.cCode(this::genToString).trim();
          if (!c.endsWith(";")) {
            c += ";";
          }
          writer.write("\t");
          writer.write(c);
          writer.write("\n");
        })

        .set(IssStaticPcRegNode.class, (IssStaticPcRegNode node, StringWriter writer) -> {
          writer.write("(ctx->base.pc_next)");
        })

    ;
  }
}
