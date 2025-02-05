package vadl.iss.codegen;

import static vadl.error.Diagnostic.ensure;
import static vadl.error.Diagnostic.error;
import static vadl.utils.GraphUtils.getSingleNode;

import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.cppCodeGen.mixins.CInvalidMixins;
import vadl.iss.passes.nodes.IssStaticPcRegNode;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.safeResourceRead.nodes.ExprSaveNode;
import vadl.iss.passes.tcgLowering.nodes.TcgNode;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.viam.Instruction;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * The code generator for the {@code target/gen-arch/translate.c}.
 * It produces translate functions for all instructions
 * in the {@link vadl.viam.InstructionSetArchitecture}.
 */
@DispatchFor(
    value = Node.class,
    context = CNodeContext.class,
    include = {"vadl.viam", "vadl.iss"}
)
public class IssTranslateCodeGenerator implements CDefaultMixins.All,
    CInvalidMixins.SideEffect, CInvalidMixins.ResourceReads, CInvalidMixins.InstrCall,
    IssCMixins.Invalid {

  private Instruction insn;
  private boolean generateInsnCount = false;
  private StringBuilder builder;
  private CNodeContext ctx;

  /**
   * Constructs IssTranslateCodeGenerator.
   *
   * @param generateInsnCount used to determine if the iss generates add instruction for special
   *                          cpu register (QEMU)
   */
  public IssTranslateCodeGenerator(Instruction instr, boolean generateInsnCount) {
    this.insn = instr;
    this.generateInsnCount = generateInsnCount;
    this.builder = new StringBuilder();
    this.ctx = new CNodeContext(
        builder::append,
        (ctx, node)
            -> IssTranslateCodeGeneratorDispatcher.dispatch(this, ctx, node)
    );
  }


  /**
   * The static entry point to get the translation function for a given instruction.
   *
   * @param generateInsnCount used to determine if the iss generates add instruction for special
   *                          cpu register (QEMU)
   */
  public static String fetch(Instruction def, boolean generateInsnCount) {
    var generator = new IssTranslateCodeGenerator(def, generateInsnCount);
    return generator.fetch();
  }

  private String fetch() {

    var name = insn.identifier.simpleName().toLowerCase();
    // static bool trans_<name>(DisasContext *ctx, arg_<name> *a) {\n
    ctx.wr("static bool trans_");
    ctx.wr(name);
    ctx.wr("(DisasContext *ctx, arg_");
    ctx.wr(name);
    ctx.ln(" *a) {");

    ctx.wr("trace_vadl_instr_trans(__func__);");

    if (generateInsnCount) {
      //Add separate add instruction after each that increments special cpu_insn_count flag in
      //QEMU CPU state
      //see resources/templates/iss/target/cpu.h/CPUArchState
      ctx.ln("\ttcg_gen_addi_i64(cpu_insn_count, cpu_insn_count, 1);");
    }

    var start = getSingleNode(insn.behavior(), StartNode.class);
    var current = start.next();

    while (current instanceof DirectionalNode dirNode) {
      ctx.gen(dirNode);
      current = dirNode.next();
    }

    ensure(current instanceof InstrEndNode, () ->
        error("Instruction contains unsupported features (e.g. if-else on constants).",
            insn.identifier.sourceLocation())
    );

    ctx.wr("\n\treturn true; \n}\n");

    return builder.toString();
  }

  @Handler
  void impl(CGenContext<Node> ctx, TcgNode node) {
    var c = node.cCode(ctx::genToString).trim();
    if (!c.endsWith(";")) {
      c += ";";
    }
    ctx.wr("\t")
        .ln(c);
  }

  @Handler
  void impl(CGenContext<Node> ctx, IssStaticPcRegNode node) {
    ctx.wr("(ctx->base.pc_next)");
  }

  @Handler
  void impl(CGenContext<Node> ctx, FieldRefNode node) {
    ctx.wr("a->");
    ctx.wr(node.formatField().simpleName());
  }

  @Handler
  void impl(CGenContext<Node> ctx, FieldAccessRefNode node) {
    ctx.wr("a->");
    ctx.wr(node.fieldAccess().simpleName());
  }

  @Handler
  void handle(CGenContext<Node> ctx, ExprSaveNode toHandle) {
    throw new UnsupportedOperationException("Type ExprSaveNode not yet implemented");
  }

  @Handler
  void handle(CGenContext<Node> ctx, TcgVRefNode toHandle) {
    ctx.wr(toHandle.cCode());
  }

  @Handler
  void handle(CGenContext<Node> ctx, AsmBuiltInCall toHandle) {
    throw new UnsupportedOperationException("Type AsmBuiltInCall not allowed");
  }

}
