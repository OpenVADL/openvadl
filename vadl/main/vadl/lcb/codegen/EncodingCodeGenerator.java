package vadl.lcb.codegen;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.stream.Collectors;
import vadl.oop.CppTypeMap;
import vadl.viam.Encoding;
import vadl.viam.Function;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;

/**
 * Generates Cpp Code given a behavior {@link Graph}.
 * This class will be used to generate the {@link Encoding}.
 */
public class EncodingCodeGenerator {
  /**
   * Returns the function header of a {@link Function}.
   * For example: int testFunction(int param1, int param2)
   */
  private String generateFunctionHeader(Function function) {
    var name = function.name();
    var parameters = Arrays.stream(function.parameters()).map(param -> {
      var cppTypeName = CppTypeMap.getCppTypeNameByVadlType(param.type());
      return cppTypeName + " " + param.name();
    }).collect(Collectors.joining(","));
    var returnType = function.returnType();
    var cppTypeReturnType = CppTypeMap.getCppTypeNameByVadlType(returnType);

    return cppTypeReturnType + " " + name + "(" + parameters + ")";
  }

  private String generateFunctionBody(Function function) {
    var writer = new StringWriter();
    var returnNode = function.behavior().getNodes(ReturnNode.class).findFirst();

    if (returnNode.isEmpty()) {
      throw new ViamError("For the encoding function is a return node required.");
    }

    new EncodingCodeGeneratorVisitor(writer).visit(returnNode.get());
    return writer.toString();
  }

  /**
   * Generate a cpp function from the given {@link Function}.
   */
  public String generateFunction(Function function) {
    return generateFunctionHeader(function) + " {\n"
        + generateFunctionBody(function) + ";\n}";
  }
}
