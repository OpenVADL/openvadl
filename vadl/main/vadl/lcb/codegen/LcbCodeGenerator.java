package vadl.lcb.codegen;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.stream.Collectors;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.GenericCppCodeGeneratorVisitor;
import vadl.cppCodeGen.model.CppClassImplName;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.viam.Function;
import vadl.viam.ViamError;
import vadl.viam.graph.control.ReturnNode;

/**
 * Parent code generator to encapsulate generic functionality.
 */
public class LcbCodeGenerator {
  /**
   * Returns the function header of a {@link Function}.
   * For example: int testFunction(int param1, int param2)
   */
  public String generateFunctionHeader(CppFunction function) {
    var name = function.functionName().lower();
    var parameters = Arrays.stream(function.parameters()).map(param -> {
      var cppTypeName = CppTypeMap.getCppTypeNameByVadlType(param.type());
      return cppTypeName + " " + param.name();
    }).collect(Collectors.joining(","));
    var returnType = function.returnType();
    var cppTypeReturnType = CppTypeMap.getCppTypeNameByVadlType(returnType);

    return cppTypeReturnType + " " + name + "(" + parameters + ")";
  }


  /**
   * Returns the function header of a {@link Function}.
   * For example: int testFunction(int param1, int param2)
   */
  public String generateFunctionHeader(CppClassImplName classImplName, CppFunction function,
                                       boolean isConst) {
    var name = function.functionName().lower();
    var parameters = Arrays.stream(function.parameters()).map(param -> {
      var cppTypeName = CppTypeMap.getCppTypeNameByVadlType(param.type());
      return cppTypeName + " " + param.name();
    }).collect(Collectors.joining(","));
    var returnType = function.returnType();
    var cppTypeReturnType = CppTypeMap.getCppTypeNameByVadlType(returnType);

    return cppTypeReturnType + " " + classImplName.lower() + "::" + name + "(" + parameters + ")"
        + (isConst ? " const" : "");
  }

  protected String generateFunctionBody(CppFunction function) {
    var writer = new StringWriter();
    var returnNode = function.behavior().getNodes(ReturnNode.class).findFirst();

    if (returnNode.isEmpty()) {
      throw new ViamError("For the function is a return node required.");
    }

    new GenericCppCodeGeneratorVisitor(writer).visit(returnNode.get());
    return writer.toString();
  }

  /**
   * Generate a cpp function from the given {@link Function}.
   */
  public CppFunctionCode generateFunction(CppFunction function) {
    return new CppFunctionCode(generateFunctionHeader(function) + " {\n"
        + generateFunctionBody(function) + ";\n}");
  }

  /**
   * Generate a cpp function from the given {@link Function}. It will prepend
   * {@link CppClassImplName} to the function name.
   */
  public CppFunctionCode generateFunction(CppClassImplName classImplName, CppFunction function,
                                          boolean isConst) {
    return new CppFunctionCode(generateFunctionHeader(classImplName, function, isConst)
        + " {\n"
        + generateFunctionBody(function) + ";\n}");
  }
}
