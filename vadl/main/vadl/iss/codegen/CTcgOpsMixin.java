package vadl.iss.codegen;

import java.io.StringWriter;
import vadl.cppCodeGen.CodeGenerator;
import vadl.cppCodeGen.mixins.CGenMixin;
import vadl.iss.passes.tcgLowering.nodes.TcgBinaryOpNode;
import vadl.iss.passes.tcgLowering.nodes.TcgGetVar;
import vadl.iss.passes.tcgLowering.nodes.TcgMoveNode;
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
        .set(TcgBinaryOpNode.class, (TcgBinaryOpNode node, StringWriter writer) -> {
          writer.write("\t" + node.tcgFunctionName() + "_i" + node.width().width);
          writer.write("(" + node.res().varName());
          writer.write("," + node.arg1().varName());
          writer.write("," + node.arg2().varName() + ");\n");
        })

        .set(TcgGetVar.TcgGetRegFile.class, (TcgGetVar.TcgGetRegFile node, StringWriter writer) -> {
          writer.write("\tTCGv_" + node.res().width() + " " + node.res().varName() + " = ");
          writer.write("get_" + node.registerFile().simpleName().toLowerCase());
          writer.write("(ctx, ");
          gen(node.index());
          writer.write(");\n");
        })

        .set(TcgMoveNode.class, (TcgMoveNode node, StringWriter writer) -> {
          writer.write("\ttcg_gen_mov_i" + node.width().width);
          writer.write("(" + node.res().varName());
          writer.write(", " + node.arg1().varName() + ");\n");
        })
    ;
  }
}
