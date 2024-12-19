package vadl.cppCodeGen.mixins;

import static vadl.cppCodeGen.CppTypeMap.getCppTypeNameByVadlType;

import java.io.StringWriter;
import vadl.cppCodeGen.CodeGenerator;
import vadl.javaannotations.Handler;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * CMiscMixin is an interface that extends CGenMixin and provides additional
 * methods to add code generation implementations for miscellaneous nodes.
 * These nodes are not further categorized and are unique in their own right.
 *
 * <p>Such nodes are
 * <ul>
 *   <li>{@link ConstantNode}</li>
 * </ul></p>
 */
public interface CMiscMixin extends CGenMixin {

  /**
   * Adds the C implementations of misc nodes, that are note further categorized.
   */
  default void miscImpls(CodeGenerator.Impls<Node> impls) {
    impls

        .set(ConstantNode.class, (ConstantNode op, StringWriter writer) -> {
          var fittingCppType = op.type().asDataType().fittingCppType();
          op.ensure(fittingCppType != null, "No fitting cpp type");
          var cppType = getCppTypeNameByVadlType(fittingCppType);
          writer.write("((" + cppType + ") " + op.constant().asVal().decimal() + " )");
        })

    ;
  }

  @Handler
  default void impl(ConstantNode node) {

  }

}
