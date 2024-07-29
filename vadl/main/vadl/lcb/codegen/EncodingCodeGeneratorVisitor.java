package vadl.lcb.codegen;

import static vadl.oop.CppTypeMap.getCppTypeNameByVadlType;

import java.io.StringWriter;
import vadl.oop.GenericCppCodeGeneratorVisitor;
import vadl.oop.OopGraphNodeVisitor;
import vadl.oop.passes.type_normalization.UpcastedTypeCastNode;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.viam.ViamError;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.EndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;

/**
 * The {@link GraphNodeVisitor} for the {@link EncodingCodeGenerator}.
 * The tasks of this class is to generate the Cpp code for LLVM which
 * is called by tablegen to encode an immediate.
 */
public class EncodingCodeGeneratorVisitor extends GenericCppCodeGeneratorVisitor implements OopGraphNodeVisitor {
  public EncodingCodeGeneratorVisitor(StringWriter writer) {
    super(writer);
  }

  @Override
  public void visit(UpcastedTypeCastNode upcastedTypeCastNode) {
    var castType = upcastedTypeCastNode.castType();
    var originalType = upcastedTypeCastNode.originalType();
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
