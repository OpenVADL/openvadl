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

import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.model.GcbCppFunctionForFieldAccess;
import vadl.cppCodeGen.model.GcbImmediateExtractionCppFunction;
import vadl.types.BitsType;
import vadl.viam.Format;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * Produce a pure function that allows to access format field references.
 * It generates code for accessing fields or extracting fields from instructions.
 */
public class GcbAccessOrExtractionFunctionCodeGenerator extends AccessFunctionCodeGenerator {

  /**
   * Constructor.
   */
  public GcbAccessOrExtractionFunctionCodeGenerator(GcbCppFunctionForFieldAccess accessFunction,
                                                    Format.FieldAccess fieldAccess,
                                                    String functionName) {
    super(accessFunction, fieldAccess, functionName);
  }

  /**
   * Constructor.
   */
  public GcbAccessOrExtractionFunctionCodeGenerator(
      GcbImmediateExtractionCppFunction extractionFunction,
      Format.FieldAccess fieldAccess,
      String functionName) {
    super(extractionFunction, fieldAccess, functionName);
  }

  @Override
  public void handle(CGenContext<Node> ctx, SliceNode toHandle) {
    var parts = toHandle.bitSlice().parts().toList();
    ctx.wr("(");

    int acc = 0;
    for (int i = parts.size() - 1; i >= 0; i--) {
      if (i != parts.size() - 1) {
        ctx.wr(" | ");
      }

      var part = parts.get(i);
      var bitWidth = ((BitsType) toHandle.value().type()).bitWidth();
      if (part.isIndex()) {
        ctx.wr(
            String.format("project_range<%d, %d>(std::bitset<%d>(", part.lsb(), part.msb(),
                bitWidth));
        ctx.gen(toHandle.value()); // same expression
        ctx.wr(String.format(")) << %d", acc));
      } else {
        ctx.wr(
            String.format("project_range<%d, %d>(std::bitset<%d>(", part.lsb(), part.msb(),
                bitWidth));
        ctx.gen(toHandle.value()); // same expression
        ctx.wr(String.format(")) << %d", acc));
      }

      acc += part.msb() - part.lsb() + 1;
    }
    ctx.wr(").to_ulong()");
  }

  @Override
  public void handle(CGenContext<Node> ctx, FuncParamNode toHandle) {
    ctx.wr(toHandle.parameter().simpleName());
  }

  @Override
  public String genFunctionName() {
    return functionName;
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
