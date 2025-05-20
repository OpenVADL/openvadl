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

import static java.util.function.Function.identity;
import static vadl.error.DiagUtils.throwNotAllowed;

import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.FunctionCodeGenerator;
import vadl.cppCodeGen.context.CGenContext;
import vadl.viam.Definition;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;

/**
 * Produce a pure function that allows to access format field references.
 */
public class AccessFunctionCodeGenerator extends FunctionCodeGenerator {

  protected final Format.FieldAccess fieldAccess;
  protected final String functionName;
  protected final Map<Format.Field, String> fieldNames;

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
    this.fieldNames = fieldAccess.fieldRefs().stream()
        .collect(Collectors.toMap(identity(), Definition::simpleName));
  }

  /**
   * Creates a new pure function code generator for the specified function. The function will be
   * named with the specified name. The references to the fields to access will be generated based
   * on the supplied fieldNames.
   *
   * @param fieldAccess  The field fieldAccess for which the function should be generated
   * @param functionName The name of the access function to generate
   * @param fieldNames   The names of the fields to access. If left empty, the default names will be
   *                     used.
   */
  public AccessFunctionCodeGenerator(Format.FieldAccess fieldAccess,
                                     @Nullable String functionName,
                                     @Nullable Map<Format.Field, String> fieldNames) {
    super(fieldAccess.accessFunction());
    this.fieldAccess = fieldAccess;
    this.functionName = functionName == null ? function.simpleName() : functionName;
    this.fieldNames = fieldNames == null || fieldNames.isEmpty() ? fieldAccess.fieldRefs().stream()
        .collect(Collectors.toMap(identity(), Definition::simpleName)) : fieldNames;
  }

  /**
   * Creates a new pure function code generator for the specified function. The function will be
   * named with the specified name.
   *
   * @param accessOrExtractionFunction The access function to access a field which is not equivalent
   *                                   to {@code FieldAccess#accessFunction} or an extraction
   *                                   function.
   * @param fieldAccess                The field fieldAccess for which the function should be
   *                                   generated.
   * @param functionName               The name of the access function to generate
   */
  public AccessFunctionCodeGenerator(Function accessOrExtractionFunction,
                                     Format.FieldAccess fieldAccess,
                                     @Nullable String functionName) {
    super(accessOrExtractionFunction);
    this.fieldAccess = fieldAccess;
    this.functionName = functionName == null ? function.simpleName() : functionName;
    this.fieldNames = fieldAccess.fieldRefs().stream()
        .collect(Collectors.toMap(identity(), Definition::simpleName));
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadRegTensorNode toHandle) {
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
    toHandle.ensure(fieldNames.containsKey(toHandle.formatField()),
        "Expected a translation for the field reference to exist.");

    // Render with the configured translation
    ctx.wr(fieldNames.get(toHandle.formatField()));
  }

  @Override
  public String genFunctionSignature() {
    var returnType = function.returnType().asDataType().fittingCppType();

    function.ensure(returnType != null, "No fitting Cpp type found for return type %s", returnType);
    function.ensure(function.behavior().isPureFunction(), "Function is not pure.");

    var insnType = fieldAccess.fieldRefs().getFirst().format().type();
    var insnCppType = CppTypeMap.getCppTypeNameByVadlType(insnType);

    var cppArgsString = "void* ctx, %s %s".formatted(insnCppType, fieldAccess.simpleName());

    return CppTypeMap.getCppTypeNameByVadlType(returnType)
        + " %s(%s)".formatted(functionName, cppArgsString);
  }
}
