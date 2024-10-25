package vadl.cppCodeGen.mixins;

import static vadl.cppCodeGen.CppTypeMap.getCppTypeNameByVadlType;

import java.io.StringWriter;
import vadl.cppCodeGen.CodeGenerator;
import vadl.types.DataType;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;

/**
 * A mixin to add C generation support for type case nodes (extend, truncate).
 */
public interface CTypeCastMixin extends CGenMixin {

  /**
   * Adds the C gen cast node implementations to the given impls.
   */
  default void castImpls(CodeGenerator.Impls<Node> impls) {
    impls
        // Produces a simple (target_val) type cast
        .set(SignExtendNode.class, (SignExtendNode node, StringWriter writer) -> {
          var type = node.type().fittingCppType();
          node.ensure(type != null, "Nodes type cannot fit in a c/c++ type.");
          writer.write("(("
              + getCppTypeNameByVadlType(type)
              + ") (");
          gen(node.value());
          writer.write("))");
        })


        // Produces a simple (target_val) type cast
        .set(TruncateNode.class, (TruncateNode node, StringWriter writer) -> {
          var type = node.type().fittingCppType();
          node.ensure(type != null, "Nodes type cannot fit in a c/c++ type.");
          writer.write("(("
              + getCppTypeNameByVadlType(type)
              + ") (");
          gen(node.value());
          writer.write("))");
        })
    ;

  }

}
