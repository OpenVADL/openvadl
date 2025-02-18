package vadl.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
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

// FIXME: @ffreitag part of https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/377
public class FunctionTest extends AbstractTest {


  // FIXME: @ffreitag part of https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/377
  // @Test
  void testValidFunctions() {
    var spec = runAndGetViamSpecification("unit/function/valid_functions.vadl");

    var noArg = TestUtils.findDefinitionByNameIn("FunctionTest::noArg", spec, Function.class);
    var callFunc = TestUtils.findDefinitionByNameIn("FunctionTest::callFunc", spec, Function.class);
    var useConst = TestUtils.findDefinitionByNameIn("FunctionTest::useConst", spec, Function.class);
    var withArgs = TestUtils.findDefinitionByNameIn("FunctionTest::withArgs", spec, Function.class);
    var callFuncOutsideISA =
        TestUtils.findDefinitionByNameIn("FunctionTest::callFuncOutsideISA", spec, Function.class);
    var outSideISA = TestUtils.findDefinitionByNameIn("outSideISA", spec, Function.class);
    var callFuncInsideISA = TestUtils.findDefinitionByNameIn("callFuncInsideISA", spec, Function.class);
    var useConstOfFunc =
        TestUtils.findDefinitionByNameIn("FunctionTest::useConstOfFunc", spec, Function.class);

    {
      Assertions.assertEquals(4, noArg.behavior().getNodes().count());
      // typecast before actual return
      var typeCast = noArg.behavior().getNodes(TypeCastNode.class).findFirst().get();
      var constant = ((ConstantNode) typeCast.value()).constant();
      assertEquals(Constant.Value.of(20, Type.unsignedInt(30)), constant);
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
          ((FuncParamNode) builtInCall.arguments().get(0).inputs().findFirst().get()).parameter());
      Assertions.assertEquals(withArgs.parameters()[1],
          ((FuncParamNode) builtInCall.arguments().get(1).inputs().findFirst().get()).parameter());
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
      var func = ((FuncCallNode) ret.value()).function();
      assertEquals(noArg, func);
    }

  }


  // FIXME: @ffreitag part of https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/377
  // @Test
  void callFunctionInInstruction() {
    var spec = runAndGetViamSpecification("unit/function/valid_functionUsage.vadl");

    var noArg = TestUtils.findDefinitionByNameIn("FunctionCallTest::noArg", spec, Function.class);
    var addition = TestUtils.findDefinitionByNameIn("FunctionCallTest::addition", spec, Function.class);
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
