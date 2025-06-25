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
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.model.GcbCppFunctionBodyLess;
import vadl.error.Diagnostic;
import vadl.utils.SourceLocation;
import vadl.viam.Format;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;

/**
 * Produce a pure function that implements a predicate function.
 */
public class PredicateFunctionCodeGenerator extends AccessFunctionCodeGenerator {

  /**
   * Creates a new pure function code generator for the specified function. The function will be
   * named with the specified name.
   *
   * @param fieldAccess  The field fieldAccess for which the function should be generated
   * @param functionName The name of the access function to generate
   */
  public PredicateFunctionCodeGenerator(GcbCppFunctionBodyLess functionHeader,
                                        Format.FieldAccess fieldAccess,
                                        @Nullable String functionName) {
    super(functionHeader, fieldAccess, functionName);
  }

  @Override
  protected void handle(CGenContext<Node> ctx, FieldRefNode toHandle) {
    throwNotAllowed(toHandle, "Field ref node");
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
    throwNotAllowed(toHandle, "Func parameter");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, FieldAccessRefNode toHandle) {
    if (!toHandle.fieldAccess().equals(fieldAccess)) {
      // Check because when we inline then we might lose source code location.
      var location =
          toHandle.location().equals(SourceLocation.INVALID_SOURCE_LOCATION) ? fieldAccess :
              toHandle.location();
      throw Diagnostic.error(
              "Predicate uses field access function does not belong to this function", location)
          .build();
    }

    ctx.wr(toHandle.fieldAccess().simpleName());
  }

  @Override
  protected void handle(CGenContext<Node> ctx, AsmBuiltInCall toHandle) {
    throwNotAllowed(toHandle, "Asm builtin calls");
  }

  @Override
  public String genFunctionSignature() {
    var returnType = function.returnType().asDataType().fittingCppType();

    function.ensure(returnType != null, "No fitting Cpp type found for return type %s", returnType);
    function.ensure(function.behavior().isPureFunction(), "Function is not pure.");

    return CppTypeMap.getCppTypeNameByVadlType(returnType)
        + " %s(%s)".formatted(functionName, genFunctionParameters(function.parameters()));
  }
}
