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

package vadl.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.TestUtils;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.TypeCastNode;

public class FunctionTest extends AbstractTest {

  @Test
  public void testValidFunctions() {
    var spec = runAndGetViamSpecification("unit/function/valid_functions.vadl");

    var noArg = TestUtils.findDefinitionByNameIn("FunctionTest::noArg", spec, Function.class);
    var callFunc = TestUtils.findDefinitionByNameIn("FunctionTest::callFunc", spec, Function.class);
    var useConst = TestUtils.findDefinitionByNameIn("FunctionTest::useConst", spec, Function.class);
    var withArgs = TestUtils.findDefinitionByNameIn("FunctionTest::withArgs", spec, Function.class);
    var callFuncOutsideISA =
        TestUtils.findDefinitionByNameIn("FunctionTest::callFuncOutsideISA", spec, Function.class);
    var outSideISA = TestUtils.findDefinitionByNameIn("outSideISA", spec, Function.class);
    var callFuncInsideISA =
        TestUtils.findDefinitionByNameIn("callFuncInsideISA", spec, Function.class);
    var useConstOfFunc =
        TestUtils.findDefinitionByNameIn("FunctionTest::useConstOfFunc", spec, Function.class);

    {
      Assertions.assertEquals(3, noArg.behavior().getNodes().count());
      // No typecast
      Assertions.assertEquals(0, noArg.behavior().getNodes(TypeCastNode.class).count());
      var constant = (noArg.behavior().getNodes(ConstantNode.class).findFirst().get()).constant();
      assertEquals(Constant.Value.of(20, Type.bits(30)), constant);
    }

    {
      var ret = useConst.behavior().getNodes(ReturnNode.class).findFirst().get();
      var constant = ((ConstantNode) ret.value()).constant();
      assertEquals(Constant.Value.of(30, Type.bits(30)), constant);
    }

    {
      var ret = callFunc.behavior().getNodes(ReturnNode.class).findFirst().get();
      var func = ((FuncCallNode) ret.value()).function();
      assertEquals(noArg, func);
    }

    {
      var builtInCall = withArgs.behavior().getNodes(BuiltInCall.class).findFirst().get();
      Assertions.assertEquals(BuiltInTable.ADD, builtInCall.builtIn());
      Assertions.assertEquals(withArgs.parameters()[0],
          ((FuncParamNode) builtInCall.arguments().get(0)).parameter());
      Assertions.assertEquals(withArgs.parameters()[1],
          ((FuncParamNode) builtInCall.arguments().get(1)).parameter());
    }


    {
      var ret = callFuncOutsideISA.behavior().getNodes(ReturnNode.class).findFirst().get();
      var func = ((FuncCallNode) ret.value()).function();
      assertEquals(outSideISA, func);
    }

    {
      var ret = callFuncInsideISA.behavior().getNodes(ReturnNode.class).findFirst().get();
      var func = ((FuncCallNode) ret.value()).function();
      assertEquals(noArg, func);
    }

    {
      var ret = useConstOfFunc.behavior().getNodes(ReturnNode.class).findFirst().get();
      var constant = ((ConstantNode) ret.value()).constant();
      assertEquals(Constant.Value.of(20, Type.bits(30)), constant);
    }

  }


  @Test
  void callFunctionInInstruction() {
    var spec = runAndGetViamSpecification("unit/function/valid_functionUsage.vadl");

    var noArg = TestUtils.findDefinitionByNameIn("FunctionCallTest::noArg", spec, Function.class);
    var addition =
        TestUtils.findDefinitionByNameIn("FunctionCallTest::addition", spec, Function.class);
    var funcCallTest =
        TestUtils.findDefinitionByNameIn("FunctionCallTest::FuncCallTest", spec, Instruction.class);

    var additionCall =
        funcCallTest.behavior().getNodes(FuncCallNode.class).filter(e -> e.arguments().size() == 2)
            .findFirst().get();

    Assertions.assertEquals(addition, additionCall.function());

    var args = additionCall.arguments();
    assertEquals(noArg, ((FuncCallNode) args.get(1)).function());
  }

}
