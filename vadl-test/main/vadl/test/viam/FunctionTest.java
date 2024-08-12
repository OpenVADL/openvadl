package vadl.test.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.test.TestUtils.findDefinitionByNameIn;

import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.test.AbstractTest;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ParamNode;
import vadl.viam.graph.dependency.TypeCastNode;

public class FunctionTest extends AbstractTest {


  @Test
  void testValidFunctions() {
    var spec = runAndGetViamSpecification("function/valid_functions.vadl");

    var noArg = findDefinitionByNameIn("FunctionTest::noArg", spec, Function.class);
    var callFunc = findDefinitionByNameIn("FunctionTest::callFunc", spec, Function.class);
    var useConst = findDefinitionByNameIn("FunctionTest::useConst", spec, Function.class);
    var withArgs = findDefinitionByNameIn("FunctionTest::withArgs", spec, Function.class);
    var callFuncOutsideISA =
        findDefinitionByNameIn("FunctionTest::callFuncOutsideISA", spec, Function.class);
    var outSideISA = findDefinitionByNameIn("outSideISA", spec, Function.class);
    var callFuncInsideISA = findDefinitionByNameIn("callFuncInsideISA", spec, Function.class);
    var useConstOfFunc =
        findDefinitionByNameIn("FunctionTest::useConstOfFunc", spec, Function.class);

    {
      assertEquals(4, noArg.behavior().getNodes().count());
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
      assertEquals(BuiltInTable.ADD, builtInCall.builtIn());
      assertEquals(withArgs.parameters()[0],
          ((FuncParamNode) builtInCall.arguments().get(0).inputs().findFirst().get()).parameter());
      assertEquals(withArgs.parameters()[1],
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


  @Test
  void callFunctionInInstruction() {
    var spec = runAndGetViamSpecification("function/valid_functionUsage.vadl");

    var noArg = findDefinitionByNameIn("FunctionCallTest::noArg", spec, Function.class);
    var addition = findDefinitionByNameIn("FunctionCallTest::addition", spec, Function.class);
    var funcCallTest =
        findDefinitionByNameIn("FunctionCallTest::FuncCallTest", spec, Instruction.class);

    var additionCall =
        funcCallTest.behavior().getNodes(FuncCallNode.class).filter(e -> e.arguments().size() == 2)
            .findFirst().get();

    assertEquals(addition, additionCall.function());

    var args = additionCall.arguments();
    assertEquals(noArg, ((FuncCallNode) args.get(1)).function());
  }

}
