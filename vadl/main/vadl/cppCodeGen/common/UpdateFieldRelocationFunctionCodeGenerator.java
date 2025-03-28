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

import vadl.cppCodeGen.AbstractRelocationCodeGenerator;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.model.GcbUpdateFieldRelocationCppFunction;
import vadl.javaannotations.DispatchFor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * Produce a pure function that generates relocations.
 */
@DispatchFor(
    value = ExpressionNode.class,
    context = CNodeContext.class,
    include = "vadl.cppCodeGen.model.nodes"
)
public class UpdateFieldRelocationFunctionCodeGenerator extends AbstractRelocationCodeGenerator {
  protected final String functionName;
  protected final CNodeContext context;

  /**
   * Creates a new pure function code generator for the specified function.
   */
  public UpdateFieldRelocationFunctionCodeGenerator(
      GcbUpdateFieldRelocationCppFunction gcbUpdateFieldRelocationCppFunction) {
    super(gcbUpdateFieldRelocationCppFunction);
    this.functionName = function.identifier.lower();
    this.context = new CNodeContext(
        builder::append,
        (ctx, node)
            -> UpdateFieldRelocationFunctionCodeGeneratorDispatcher.dispatch(this, ctx,
            (ExpressionNode) node)
    );
  }

  @Override
  public void handle(CGenContext<Node> ctx, FuncParamNode toHandle) {
    ctx.wr(toHandle.parameter().simpleName());
  }

  @Override
  public String genFunctionSignature() {
    var returnType = function.returnType().asDataType().fittingCppType();

    function.ensure(returnType != null, "No fitting Cpp type found for return type %s", returnType);
    function.ensure(function.behavior().isPureFunction(), "Function is not pure.");

    return CppTypeMap.getCppTypeNameByVadlType(returnType)
        + " %s(%s)".formatted(functionName, genFunctionParameters(function.parameters()));
  }

  @Override
  public CNodeContext context() {
    return context;
  }

  @Override
  public String genFunctionName() {
    return functionName;
  }
}
