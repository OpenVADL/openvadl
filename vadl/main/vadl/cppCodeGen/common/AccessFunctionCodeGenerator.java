package vadl.cppCodeGen.common;

import static vadl.error.DiagUtils.throwNotAllowed;

import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.FunctionCodeGenerator;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.viam.Format;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;

/**
 * Produce a pure function that allows to access format field references.
 */
public class AccessFunctionCodeGenerator extends FunctionCodeGenerator {

  protected final Format.FieldAccess fieldAccess;
  protected final String functionName;

  /**
   * Creates a new pure function code generator for the specified function.
   *
   * @param fieldAccess The field fieldAccess for which the function should be generated
   */
  public AccessFunctionCodeGenerator(Format.FieldAccess fieldAccess) {
    super(fieldAccess.accessFunction());
    this.fieldAccess = fieldAccess;
    this.functionName = function.simpleName();
  }

  /**
   * Creates a new pure function code generator for the specified function. The function will be
   * named with the specified name.
   *
   * @param fieldAccess  The field fieldAccess for which the function should be generated
   * @param functionName The name of the access function to generate
   */
  public AccessFunctionCodeGenerator(Format.FieldAccess fieldAccess, String functionName) {
    super(fieldAccess.accessFunction());
    this.fieldAccess = fieldAccess;
    this.functionName = functionName;
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadRegNode toHandle) {
    throwNotAllowed(toHandle, "Register reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadRegFileNode toHandle) {
    throwNotAllowed(toHandle, "Register reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadMemNode toHandle) {
    throwNotAllowed(toHandle, "Memory reads");
  }

  @Override
  public void handle(CNodeContext ctx, FuncParamNode toHandle) {
    // Explicit parameters are not allowed. The only parameter is the implicit format field access.
    throwNotAllowed(toHandle, "Function parameters");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, FieldAccessRefNode toHandle) {
    throwNotAllowed(toHandle, "Format field accesses");
  }

  /**
   * Access functions allow to access a single persistent format field.
   *
   * @param ctx      The generation context
   * @param toHandle The field reference node to handle
   */
  @Override
  protected void handle(CGenContext<Node> ctx, FieldRefNode toHandle) {
    toHandle.ensure(toHandle.formatField().equals(fieldAccess.fieldRef()),
        "Field reference does not match the field access.");

    // Reference the function parameter
    ctx.wr("field");
  }

  @Override
  public String genFunctionSignature() {
    var returnType = function.returnType().asDataType().fittingCppType();

    function.ensure(returnType != null, "No fitting Cpp type found for return type %s", returnType);
    function.ensure(function.behavior().isPureFunction(), "Function is not pure.");

    var insnType = fieldAccess.fieldRef().format().type();
    var insnCppType = CppTypeMap.getCppTypeNameByVadlType(insnType);

    var cppArgsString = "void* ctx, %s field".formatted(insnCppType);

    return CppTypeMap.getCppTypeNameByVadlType(returnType)
        + " %s(%s)".formatted(functionName, cppArgsString);
  }
}
