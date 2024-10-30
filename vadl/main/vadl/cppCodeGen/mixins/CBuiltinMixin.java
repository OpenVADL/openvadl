package vadl.cppCodeGen.mixins;

import java.io.StringWriter;
import vadl.cppCodeGen.CodeGenerator;
import vadl.types.BuiltInTable;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.BuiltInCall;


/**
 * CBuiltinMixin provides default implementations of C generation
 * for built-in operations to be used in conjunction with the {@link CodeGenerator}.
 */
public interface CBuiltinMixin extends CGenMixin {

  /**
   * Adds the C gen TCG node implementations to the given impls.
   */
  default void builtinImpls(CodeGenerator.Impls<Node> impls) {
    impls
        .set(BuiltInCall.class, (BuiltInCall op, StringWriter writer) -> {
          // open scope
          writer.write("(");

          var a = op.arguments().get(0);

          if (op.arguments().size() == 2) {
            var b = op.arguments().get(1);
            if (op.builtIn() == BuiltInTable.LSL) {
              gen(a);
              writer.write(" << ");
              gen(b);
            }
          } else {
            throw new ViamGraphError("built-in to C of %s is not supported", op.builtIn())
                .addContext(op)
                .addContext(op.graph());
          }

          // close scope
          writer.write(")");
        })

    ;
  }


}
