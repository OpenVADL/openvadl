package vadl.lcb.codegen;

import static vadl.cppCodeGen.CppTypeMap.getCppTypeNameByVadlType;

import java.io.StringWriter;
import vadl.cppCodeGen.CppCodeGenGraphNodeVisitor;
import vadl.cppCodeGen.GenericCppCodeGeneratorVisitor;
import vadl.cppCodeGen.passes.type_normalization.UpcastedTypeCastNode;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * The {@link GraphNodeVisitor} for the {@link EncodingCodeGenerator}.
 * The tasks of this class is to generate the Cpp code for LLVM which
 * is called by tablegen to encode an immediate.
 */
public class EncoderDecoderCodeGeneratorVisitor extends GenericCppCodeGeneratorVisitor
    implements CppCodeGenGraphNodeVisitor {
  public EncoderDecoderCodeGeneratorVisitor(StringWriter writer) {
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

  @Override
  public void visit(SideEffectNode sideEffectNode) {
    sideEffectNode.accept(this);
  }

  @Override
  public void visit(ZeroExtendNode node) {
    writer.write("((" + getCppTypeNameByVadlType(node.type()) + ") ");
    visit(node.value());
    writer.write(")");
  }

  @Override
  public void visit(SignExtendNode node) {
    writer.write("((" + getCppTypeNameByVadlType(node.type()) + ") ");
    visit(node.value());
    writer.write(")");
  }

  @Override
  public void visit(TruncateNode node) {
    if (node.type() instanceof BoolType) {
      writer.write("((" + getCppTypeNameByVadlType(node.type()) + ") ");
      visit(node.value());
      writer.write(" & 0x1)");
    } else {
      writer.write("((" + getCppTypeNameByVadlType(node.type()) + ") ");
      visit(node.value());
      writer.write(")");
    }
  }
}
