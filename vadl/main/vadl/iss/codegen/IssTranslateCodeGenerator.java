package vadl.iss.codegen;

import static vadl.error.Diagnostic.ensure;
import static vadl.error.Diagnostic.error;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.cppCodeGen.CodeGenerator;
import vadl.cppCodeGen.GenericCppCodeGeneratorVisitor;
import vadl.cppCodeGen.mixins.CTypeCastMixin;
import vadl.viam.Definition;
import vadl.viam.Instruction;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.WriteRegNode;

public class IssTranslateCodeGenerator extends CodeGenerator
    implements CTypeCastMixin, CTcgOpsMixin {

  private static final Logger log = LoggerFactory.getLogger(IssTranslateCodeGenerator.class);

  public IssTranslateCodeGenerator(StringWriter writer) {
    super(writer);
  }

  public static String fetch(Instruction def) {
    var generator = new IssTranslateCodeGenerator(new StringWriter());
    generator.gen(def);
    return generator.writer.toString();
  }


  @Override
  public void defImpls(Impls<Definition> impls) {
    impls
        .set(Instruction.class, (insn, writer) -> {
          var start = getSingleNode(insn.behavior(), StartNode.class);

          writer.write("static bool trans_addi(DisasContext *ctx, arg_add *a) {\n");

          var current = start.next();
          log.info("Generating " + current);

          while (current instanceof DirectionalNode dirNode) {
            gen(dirNode);
            current = dirNode.next();
          }

          ensure(current instanceof InstrEndNode, () ->
              error("Instruction contains unsupported features.",
                  insn.identifier.sourceLocation())
          );

          writer.write("\n\treturn true; \n}\n");
        })

    ;
  }

  @Override
  public void nodeImpls(Impls<Node> impls) {
    castImpls(impls);
    tcgOpImpls(impls);

    impls.set(FieldRefNode.class, (node, writer) -> {
      writer.write("a->imm");
    });
  }

  @Override
  public StringWriter writer() {
    return writer;
  }
}
