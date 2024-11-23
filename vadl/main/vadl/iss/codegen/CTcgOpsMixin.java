package vadl.iss.codegen;

import java.io.StringWriter;
import vadl.cppCodeGen.CodeGenerator;
import vadl.cppCodeGen.mixins.CGenMixin;
import vadl.iss.passes.tcgLowering.nodes.TcgNode;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadRegNode;

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

        .set(ReadRegNode.class, (ReadRegNode node, StringWriter writer) -> {
          // this can only happen if the register is the PC
          // TODO: Make a custom node (TcgReadPC)
          writer.write("(ctx->base.pc_next)");
        })


    ;
  }
}
