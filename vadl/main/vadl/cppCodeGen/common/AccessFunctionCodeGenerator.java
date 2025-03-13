// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.cppCodeGen.common;

import static vadl.error.DiagUtils.throwNotAllowed;

import javax.annotation.Nullable;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.FunctionCodeGenerator;
import vadl.cppCodeGen.context.CGenContext;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * Produce a pure function that allows to access format field references.
 */
public class AccessFunctionCodeGenerator extends FunctionCodeGenerator {

  protected final Format.FieldAccess fieldAccess;
  protected final String functionName;
  protected final String fieldName;

  /**
   * Creates a new pure function code generator for the specified function. The function will be
   * named with the specified name.
   *
   * @param fieldAccess  The field fieldAccess for which the function should be generated
   * @param functionName The name of the access function to generate
   */
  public AccessFunctionCodeGenerator(Format.FieldAccess fieldAccess,
                                     @Nullable String functionName) {
    super(fieldAccess.accessFunction());
    this.fieldAccess = fieldAccess;
    this.functionName = functionName == null ? function.simpleName() : functionName;
    this.fieldName = fieldAccess.fieldRef().simpleName();
  }

  /**
   * Creates a new pure function code generator for the specified function. The function will be
   * named with the specified name. The field to access will be the specified field.
   *
   * @param fieldAccess  The field fieldAccess for which the function should be generated
   * @param functionName The name of the access function to generate
   * @param fieldName    The name of the field to access
   */
  public AccessFunctionCodeGenerator(Format.FieldAccess fieldAccess,
                                     @Nullable String functionName,
                                     @Nullable String fieldName) {
    super(fieldAccess.accessFunction());
    this.fieldAccess = fieldAccess;
    this.functionName = functionName == null ? function.simpleName() : functionName;
    this.fieldName = fieldName == null ? fieldAccess.fieldRef().simpleName() : fieldName;
  }

  /**
   * Creates a new pure function code generator for the specified function. The function will be
   * named with the specified name. The field to access will be the specified field.
   *
   * @param accessOrExtractionFunction The access function to access a field which is not equivalent
   *                                   to {@code FieldAccess#accessFunction} or an extraction
   *                                   function.
   * @param fieldAccess                The field fieldAccess for which the function should be
   *                                   generated.
   * @param functionName               The name of the access function to generate
   * @param fieldName                  The name of the field to access
   */
  public AccessFunctionCodeGenerator(Function accessOrExtractionFunction,
                                     Format.FieldAccess fieldAccess,
                                     @Nullable String functionName,
                                     @Nullable String fieldName) {
    super(accessOrExtractionFunction);
    this.fieldAccess = fieldAccess;
    this.functionName = functionName == null ? function.simpleName() : functionName;
    this.fieldName = fieldName == null ? fieldAccess.fieldRef().simpleName() : fieldName;
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
  protected void handle(CGenContext<Node> ctx, ReadArtificialResNode toHandle) {
    throwNotAllowed(toHandle, "Artificial resource reads");
  }

  @Override
  public void handle(CGenContext<Node> ctx, SliceNode toHandle) {
    throwNotAllowed(toHandle, "Slice node reads");
  }

  @Override
  public void handle(CGenContext<Node> ctx, FuncParamNode toHandle) {
    // Explicit parameters are not allowed. The only parameter is the implicit format field access.
    throwNotAllowed(toHandle, "Function parameters");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, FieldAccessRefNode toHandle) {
    throwNotAllowed(toHandle, "Format field accesses");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, AsmBuiltInCall toHandle) {
    throwNotAllowed(toHandle, "Asm builtin calls");
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
    ctx.wr(fieldName);
  }

  @Override
  public String genFunctionSignature() {
    var returnType = function.returnType().asDataType().fittingCppType();

    function.ensure(returnType != null, "No fitting Cpp type found for return type %s", returnType);
    function.ensure(function.behavior().isPureFunction(), "Function is not pure.");

    var insnType = fieldAccess.fieldRef().format().type();
    var insnCppType = CppTypeMap.getCppTypeNameByVadlType(insnType);

    var cppArgsString = "void* ctx, %s %s".formatted(insnCppType, fieldName);

    return CppTypeMap.getCppTypeNameByVadlType(returnType)
        + " %s(%s)".formatted(functionName, cppArgsString);
  }
}
