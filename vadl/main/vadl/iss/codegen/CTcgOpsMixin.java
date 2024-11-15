package vadl.iss.codegen;

import java.io.StringWriter;
import vadl.cppCodeGen.CodeGenerator;
import vadl.cppCodeGen.mixins.CGenMixin;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.iss.passes.tcgLowering.nodes.TcgBinaryImmOpNode;
import vadl.iss.passes.tcgLowering.nodes.TcgBinaryOpNode;
import vadl.iss.passes.tcgLowering.nodes.TcgBr;
import vadl.iss.passes.tcgLowering.nodes.TcgBrCondImm;
import vadl.iss.passes.tcgLowering.nodes.TcgConstantNode;
import vadl.iss.passes.tcgLowering.nodes.TcgExtendNode;
import vadl.iss.passes.tcgLowering.nodes.TcgGenLabel;
import vadl.iss.passes.tcgLowering.nodes.TcgGetVar;
import vadl.iss.passes.tcgLowering.nodes.TcgGottoTbAbs;
import vadl.iss.passes.tcgLowering.nodes.TcgLoadMemory;
import vadl.iss.passes.tcgLowering.nodes.TcgMoveNode;
import vadl.iss.passes.tcgLowering.nodes.TcgSetCond;
import vadl.iss.passes.tcgLowering.nodes.TcgSetIsJmp;
import vadl.iss.passes.tcgLowering.nodes.TcgSetLabel;
import vadl.iss.passes.tcgLowering.nodes.TcgSetReg;
import vadl.iss.passes.tcgLowering.nodes.TcgSetRegFile;
import vadl.iss.passes.tcgLowering.nodes.TcgStoreMemory;
import vadl.iss.passes.tcgLowering.nodes.TcgTruncateNode;
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
          writer.write(", " + node.arg().varName() + ");\n");
        })

        .set(TcgGetVar.TcgGetTemp.class, (TcgGetVar.TcgGetTemp node, StringWriter writer) -> {
          writer.write("\tTCGv_" + node.res().width() + " " + node.res().varName() + " = ");
          writer.write("tcg_temp_new_i" + node.res().width().width);
          writer.write("();\n");
        })

        .set(TcgGetVar.TcgGetReg.class, (TcgGetVar.TcgGetReg node, StringWriter writer) -> {
          writer.write("\tTCGv_" + node.res().width() + " " + node.res().varName() + " = ");
          writer.write("cpu_" + node.register().simpleName().toLowerCase() + ";\n");
        })

        .set(ReadRegNode.class, (ReadRegNode node, StringWriter writer) -> {
          // this can only happen if the register is the PC
          // TODO: Make a custom node (TcgReadPC)
          writer.write("(ctx->base.pc_next)");
        })

        .set(TcgConstantNode.class, (TcgConstantNode node, StringWriter writer) -> {
          writer.write("\tTCGv_" + node.res().width() + " " + node.res().varName() + " = ");
          writer.write(node.tcgFunctionName());
          writer.write("(");
          gen(node.arg());
          writer.write(");\n");
        })

        .set(TcgSetRegFile.class, (TcgSetRegFile node, StringWriter writer) -> {
          writer.write("\tgen_set_" + node.registerFile().simpleName().toLowerCase());
          writer.write("(ctx, ");
          gen(node.index());
          writer.write(", " + node.res().varName() + ");\n");
        })

        .set(TcgSetReg.class, (TcgSetReg node, StringWriter writer) -> {
          writer.write("\ttcg_gen_mov_i" + node.width().width);
          writer.write("(" + "cpu_" + node.register().simpleName().toLowerCase());
          writer.write(", " + node.res().varName() + ");\n");
        })

        .set(TcgGottoTbAbs.class, (TcgGottoTbAbs node, StringWriter writer) -> {
          writer.write("\tgen_goto_tb_abs(ctx, ");
          gen(node.targetPc());
          writer.write(");\n");
        })

        .set(TcgSetIsJmp.class, (TcgSetIsJmp node, StringWriter writer) -> {
          writer.write("\tctx->base.is_jmp = ");
          writer.write(node.type().cCode() + ";\n");
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

        .set(TcgStoreMemory.class, (TcgStoreMemory node, StringWriter writer) -> {
          writer.write("\t" + "tcg_gen_qemu_st_i" + node.width().width);
          writer.write("(" + node.val().varName());
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

        .set(TcgTruncateNode.class, (TcgTruncateNode node, StringWriter writer) -> {
          writer.write("\t" + node.tcgFunctionName());
          writer.write("(" + node.res().varName());
          writer.write(", " + node.arg().varName());
          writer.write(", " + node.bitWidth());
          writer.write(");\n");
        })

        .set(TcgSetCond.class, (TcgSetCond node, StringWriter writer) -> {
          writer.write("\t" + node.tcgFunctionName() + "_i" + node.width().width);
          writer.write("(" + node.condition().cCode());
          writer.write(", " + node.res().varName());
          writer.write(", " + node.arg1().varName());
          writer.write(", " + node.arg2().varName());
          writer.write(");\n");
        })

        //// JUMP/LABELS EMITS ////

        .set(TcgGenLabel.class, (TcgGenLabel node, StringWriter writer) -> {
          writer.write("\tTCGLabel *" + node.label().varName() + " = gen_new_label();\n");
        })

        .set(TcgSetLabel.class, (TcgSetLabel node, StringWriter writer) -> {
          writer.write("\tgen_set_label(" + node.label().varName() + ");\n");
        })

        .set(TcgBr.class, (TcgBr node, StringWriter writer) -> {
          writer.write("\ttcg_gen_br(" + node.label().varName() + ");\n");
        })

        .set(TcgBrCondImm.class, (TcgBrCondImm node, StringWriter writer) -> {
          writer.write("\ttcg_gen_brcondi_i" + node.width().width);
          writer.write("(" + node.condition().cCode());
          writer.write(", " + node.cmpArg1().varName() + ", ");
          gen(node.cmpArg2());
          writer.write(", " + node.label().varName() + ");\n");
        })

    ;
  }
}
