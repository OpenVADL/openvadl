package vadl.cppCodeGen.mixins;

import java.io.StringWriter;
import vadl.cppCodeGen.CodeGenerator;
import vadl.javaannotations.Handler;
import vadl.types.BuiltInTable;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.BuiltInCall;


/**
 * CBuiltinMixin provides default implementations of C generation
 * for built-in operations to be used in conjunction with the {@link CodeGenerator}.
 */
public interface CBuiltinMixin extends CGenMixin {


  @Handler
  default void impl(BuiltInCall op) {

  }

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
            gen(a);
            // TODO: Refactor this unreadable stuff
            if (op.builtIn() == BuiltInTable.LSL) {
              writer.write(" << ");
            } else if (op.builtIn() == BuiltInTable.ADD) {
              writer.write(" + ");
            } else if (op.builtIn() == BuiltInTable.AND) {
              writer.write(" & ");
            } else {
              throw new ViamGraphError("built-in to C of %s is not implemented", op.builtIn())
                  .addContext(op);
            }
            gen(b);
          } else {
            throw new ViamGraphError("built-in to C of %s is not supported", op.builtIn())
                .addContext(op);
          }

          // close scope
          writer.write(")");
        })

    ;
  }


}
