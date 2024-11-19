package vadl.iss.codegen;

import java.io.StringWriter;
import vadl.cppCodeGen.CodeGenerator;
import vadl.cppCodeGen.mixins.CGenMixin;
import vadl.iss.passes.tcgLowering.nodes.TcgBinaryImmOpNode;
import vadl.iss.passes.tcgLowering.nodes.TcgBinaryOpNode;
import vadl.iss.passes.tcgLowering.nodes.TcgBr;
import vadl.iss.passes.tcgLowering.nodes.TcgBrCondImm;
import vadl.iss.passes.tcgLowering.nodes.TcgConstantNode;
import vadl.iss.passes.tcgLowering.nodes.TcgExtendNode;
import vadl.iss.passes.tcgLowering.nodes.TcgLabelLabel;
import vadl.iss.passes.tcgLowering.nodes.TcgGetVar;
import vadl.iss.passes.tcgLowering.nodes.TcgGottoTbAbs;
import vadl.iss.passes.tcgLowering.nodes.TcgLoadMemory;
import vadl.iss.passes.tcgLowering.nodes.TcgMoveNode;
import vadl.iss.passes.tcgLowering.nodes.TcgNode;
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
