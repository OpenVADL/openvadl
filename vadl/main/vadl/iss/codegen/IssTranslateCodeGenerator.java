package vadl.iss.codegen;

import static vadl.error.Diagnostic.ensure;
import static vadl.error.Diagnostic.error;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.cppCodeGen.CodeGenerator;
import vadl.cppCodeGen.mixins.CBuiltinMixin;
import vadl.cppCodeGen.mixins.CMiscMixin;
import vadl.cppCodeGen.mixins.CTypeCastMixin;
import vadl.viam.Definition;
import vadl.viam.Instruction;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * The code generator for the {@code target/gen-arch/translate.c}.
 * It produces translate functions for all instructions
 * in the {@link vadl.viam.InstructionSetArchitecture}.
 */
public class IssTranslateCodeGenerator extends CodeGenerator
    implements CTypeCastMixin, CTcgOpsMixin, CBuiltinMixin, CMiscMixin {

  public IssTranslateCodeGenerator(StringWriter writer) {
    super(writer);
  }

  /**
   * The static entry point to get the translation function for a given instruction.
   */
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

          var name = insn.identifier.simpleName().toLowerCase();
          // static bool trans_<name>(DisasContext *ctx, arg_<name> *a) {\n
          writer.write("static bool trans_");
          writer.write(name);
          writer.write("(DisasContext *ctx, arg_");
          writer.write(name);
          writer.write(" *a) {\n");

          // format debug string
          var fieldStream = Arrays.stream(insn.format().fields())
              .filter(f -> insn.encoding().fieldEncodingOf(f) == null)
              .filter(f -> !f.simpleName().startsWith("imm"));
          var printingFields = Stream.concat(fieldStream, insn.format()
                  .fieldAccesses().stream())
              .map(Definition::simpleName)
              .toList();

          var fmtString = printingFields.stream().map(f -> f + ": %d ")
              .collect(Collectors.joining(", "));
          var fmtArgs = printingFields.stream().map(f ->
                  "a->" + f)
              .collect(Collectors.joining(", "));

          writer.write("\tqemu_printf(\"[VADL] trans_");
          writer.write(name);
          writer.write(" (" + fmtString + ")");
          writer.write("\\n\", " + fmtArgs + ");\n");

          var current = start.next();

          while (current instanceof DirectionalNode dirNode) {
            gen(dirNode);
            current = dirNode.next();
          }

          ensure(current instanceof InstrEndNode, () ->
              error("Instruction contains unsupported features (e.g. if-else on constants).",
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
    builtinImpls(impls);
    miscImpls(impls);

    impls.set(FieldRefNode.class, (node, writer) -> {
      writer.write("a->");
      writer.write(node.formatField().simpleName());
    });

    impls.set(FieldAccessRefNode.class, (node, writer) -> {
      writer.write("a->");
      writer.write(node.fieldAccess().simpleName());
    });
  }

  @Override
  public StringWriter writer() {
    return writer;
  }
}
