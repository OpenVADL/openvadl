package vadl.iss.codegen;

import static vadl.cppCodeGen.CppTypeMap.getCppTypeNameByVadlType;

import java.io.StringWriter;
import java.util.function.BiConsumer;
import vadl.cppCodeGen.CodeGenerator;
import vadl.cppCodeGen.mixins.CGenMixin;
import vadl.types.BuiltInTable;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;

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

        // TODO: Move in separate class
        .set(ConstantNode.class, (ConstantNode op, StringWriter writer) -> {
          var fittingCppType = op.type().asDataType().fittingCppType();
          op.ensure(fittingCppType != null, "No fitting cpp type");
          var cppType = getCppTypeNameByVadlType(fittingCppType);
          writer.write("((" + cppType + ") " + op.constant().asVal().decimal() + " )");
        })

    ;
  }


}
