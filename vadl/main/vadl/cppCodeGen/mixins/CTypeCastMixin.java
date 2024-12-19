package vadl.cppCodeGen.mixins;

import static vadl.cppCodeGen.CppTypeMap.getCppTypeNameByVadlType;

import java.io.StringWriter;
import vadl.cppCodeGen.CodeGenerator;
import vadl.javaannotations.Handler;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * A mixin to add C generation support for type case nodes (extend, truncate).
 */
public interface CTypeCastMixin extends CGenMixin {

  /**
   * Adds the C gen cast node implementations to the given impls.
   */
  default void castImpls(CodeGenerator.Impls<Node> impls) {
    impls
        // We can't do sign extension with the c type system.
        // E.g. a 20 bit value in a 32 bit c type cannot be sign extended to 64 bit
        // as the 32 bit value holder might not be sign extended
        // So we use the sextract function to sign extract the value from its holder
        .set(SignExtendNode.class, (SignExtendNode node, StringWriter writer) -> {
          var targetType = node.type().fittingCppType();
          node.ensure(targetType != null, "Nodes type cannot fit in a c/c++ type.");
          var srcType = node.value().type().asDataType();
          writer.write("sextract" + targetType.bitWidth() + "(");
          gen(node.value());
          writer.write(", 0, " + srcType.bitWidth() + ")");
        })

        // We can't do zero extension with the c type system.
        // E.g. a 20 bit value in a 32 bit c type cannot be zero extended to 64 bit
        // as the 32 bit value holder might be sign extended
        // So we use the extract function to extract the value from its holder
        .set(ZeroExtendNode.class, (ZeroExtendNode node, StringWriter writer) -> {
          var type = node.type().fittingCppType();
          node.ensure(type != null, "Nodes type cannot fit in a c/c++ type.");
          writer.write("extract" + type.bitWidth() + "(");
          gen(node.value());
          writer.write(", 0, " + node.type().bitWidth() + ")");
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