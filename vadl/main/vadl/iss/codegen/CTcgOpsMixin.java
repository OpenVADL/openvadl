package vadl.iss.codegen;

import java.io.StringWriter;
import vadl.cppCodeGen.CodeGenerator;
import vadl.cppCodeGen.mixins.CGenMixin;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.iss.passes.tcgLowering.nodes.TcgBinaryImmOpNode;
import vadl.iss.passes.tcgLowering.nodes.TcgBinaryOpNode;
import vadl.iss.passes.tcgLowering.nodes.TcgExtendNode;
import vadl.iss.passes.tcgLowering.nodes.TcgGetVar;
import vadl.iss.passes.tcgLowering.nodes.TcgLoadMemory;
import vadl.iss.passes.tcgLowering.nodes.TcgMoveNode;
import vadl.iss.passes.tcgLowering.nodes.TcgSetRegFile;
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
        .set(TcgGetVar.TcgGetRegFile.class, (TcgGetVar.TcgGetRegFile node, StringWriter writer) -> {
          writer.write("\tTCGv_" + node.res().width() + " " + node.res().varName() + " = ");
          var prefix = node.kind() == TcgGetVar.TcgGetRegFile.Kind.DEST ? "dest" : "get";
          writer.write(prefix + "_" + node.registerFile().simpleName().toLowerCase());
          writer.write("(ctx, ");
          gen(node.index());
          writer.write(");\n");
        })

        .set(TcgMoveNode.class, (TcgMoveNode node, StringWriter writer) -> {
          writer.write("\ttcg_gen_mov_i" + node.width().width);
          writer.write("(" + node.res().varName());
          writer.write(", " + node.arg1().varName() + ");\n");
        })

        .set(TcgGetVar.TcgGetTemp.class, (TcgGetVar.TcgGetTemp node, StringWriter writer) -> {
          writer.write("\tTCGv_" + node.res().width() + " " + node.res().varName() + " = ");
          writer.write("tcg_temp_new_i" + node.res().width().width);
          writer.write("();\n");
        })

        .set(TcgSetRegFile.class, (TcgSetRegFile node, StringWriter writer) -> {
          writer.write("\tgen_set_" + node.registerFile().simpleName().toLowerCase());
          writer.write("(ctx, ");
          gen(node.index());
          writer.write(", " + node.res().varName() + ");\n");
        })

        .set(TcgBinaryOpNode.class, (TcgBinaryOpNode node, StringWriter writer) -> {
          writer.write("\t" + node.tcgFunctionName() + "_i" + node.width().width);
          writer.write("(" + node.res().varName());
          writer.write("," + node.arg1().varName());
          writer.write("," + node.arg2().varName() + ");\n");
        })

        .set(TcgBinaryImmOpNode.class, (TcgBinaryImmOpNode node, StringWriter writer) -> {
          writer.write("\t" + node.tcgFunctionName() + "_i" + node.width().width);
          writer.write("(" + node.res().varName());
          writer.write("," + node.arg1().varName());
          writer.write(",");
          gen(node.arg2());
          writer.write(");\n");
        })

        .set(TcgLoadMemory.class, (TcgLoadMemory node, StringWriter writer) -> {
            writer.write("\t" + "tcg_gen_qemu_ld_i" + node.width().width);
            writer.write("(" + node.res().varName());
            writer.write(", " + node.addr().varName());
            writer.write(", 0");
            writer.write(", " + node.tcgMemOp());
            writer.write(");\n");
        })

        .set(TcgExtendNode.class, (TcgExtendNode node, StringWriter writer) -> {
            writer.write("\t" + node.tcgFunctionName());
            writer.write("(" + node.res().varName());
            writer.write(", " + node.arg().varName());
            writer.write(");\n");
        })

    ;
  }
}
