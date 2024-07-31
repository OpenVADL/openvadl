package vadl.lcb.codegen;

import static vadl.oop.CppTypeMap.getCppTypeNameByVadlType;

import java.io.StringWriter;
import vadl.oop.GenericCppCodeGeneratorVisitor;
import vadl.oop.OopGraphNodeVisitor;
import vadl.oop.passes.type_normalization.UpcastedTypeCastNode;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.viam.graph.GraphNodeVisitor;

/**
 * The {@link GraphNodeVisitor} for the {@link EncodingCodeGenerator}.
 * The tasks of this class is to generate the Cpp code for LLVM which
 * is called by tablegen to encode an immediate.
 */
public class EncodingCodeGeneratorVisitor extends GenericCppCodeGeneratorVisitor
    implements OopGraphNodeVisitor {

  public EncodingCodeGeneratorVisitor(StringWriter writer) {
    super(writer);
  }

  @Override
  public void visit(UpcastedTypeCastNode upcastedTypeCastNode) {
    var castType = upcastedTypeCastNode.castType();
    var originalType = upcastedTypeCastNode.originalType();

    if (castType == BoolType.bool()) {
      // Integer downcasts truncated the higher bits
      // but, boolean downcasts (v != 0)
      writer.write("((" + getCppTypeNameByVadlType(castType) + ") ");
      visit(upcastedTypeCastNode.value());
      writer.write(" & 0x1)");
    } else {
      writer.write("((" + getCppTypeNameByVadlType(castType) + ") ");
      visit(upcastedTypeCastNode.value());

      upcastedTypeCastNode.ensure(originalType instanceof BitsType
          || originalType instanceof BoolType, "Type must be bits or bool");

      if (originalType instanceof BitsType cast) {
        writer.write(
            ") & " + generateBitmask(cast.bitWidth()));
      } else if (upcastedTypeCastNode.originalType() instanceof BoolType) {
        writer.write(") & 1");
      }
    }
  }
}
