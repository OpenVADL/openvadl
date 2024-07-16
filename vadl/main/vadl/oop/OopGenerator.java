package vadl.oop;

import java.util.Arrays;
import java.util.stream.Collectors;
import vadl.types.Type;
import vadl.viam.Encoding;
import vadl.viam.Function;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;

/**
 * Generates Cpp Code given a behavior {@link Graph}.
 * This class will be used to generate the {@link Encoding}.
 */
public class OopGenerator {
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
    var returnNode = function.behavior().getNodes(ReturnNode.class).findFirst().get();
    return returnNode.generateOopExpression();
  }

  /**
   * Generate a cpp function from the given {@link Function}.
   */
  public String generateFunction(Function function) {
    return generateFunctionHeader(function) + " {\n"
        + generateFunctionBody(function) + ";\n}";
  }
}
