package vadl.test.viam.passes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static vadl.test.TestUtils.findDefinitionByNameIn;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassResults;
import vadl.test.AbstractTest;
import vadl.test.TestFrontend;
import vadl.types.BuiltInTable;
import vadl.utils.ViamUtils;
import vadl.viam.Definition;
import vadl.viam.Function;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.passes.typeCastElimination.TypeCastEliminationPass;
import vadl.viam.passes.verification.ViamVerifier;

public class TypeCastEliminationPassTest extends AbstractTest {

  private static TestFrontend validFrontend;

  @BeforeAll
  static void runValidSpecAndEliminationPass() throws IOException {
    validFrontend =
        runViamSpecificationWithNewFrontend(
            "passes/typeCastElimination/valid_type_cast_elimination.vadl");
    // execute type cast elimination
    new TypeCastEliminationPass(
        new GeneralConfiguration(createDirectory().toAbsolutePath().toString(), false)).execute(
        PassResults.empty(), validFrontend.getViam());
    ViamVerifier.verifyAllIn(validFrontend.getViam());
  }


  @ParameterizedTest
  @MethodSource("testTrivial_Source")
  void testTrivial_shouldDeleteTypeCastAndMakeArgumentTypeSameAsResultType(String functionName) {
    var behavior = getTestBehavior(functionName);
    assertEquals(3, behavior.getNodes().count());
    var returnNode = getSingleNode(behavior, ReturnNode.class);
    var paramNode = getSingleNode(behavior, FuncParamNode.class);
    assertEquals(returnNode.returnType(), paramNode.type());
  }

  static Stream<Arguments> testTrivial_Source() {
    return findFuncNameArgumentsByPrefix("Trivial_", validFrontend.getViam());
  }

  @ParameterizedTest
  @MethodSource("testTruncate_Source")
  void testTruncate_shouldUseTruncateNodeInsteadOfTypeCast(String functionName) {
    var behavior = getTestBehavior(functionName);
    assertEquals(4, behavior.getNodes().count());
    assertEquals(1, behavior.getNodes(TruncateNode.class).count());
  }

  static Stream<Arguments> testTruncate_Source() {
    return findFuncNameArgumentsByPrefix("Truncate_", validFrontend.getViam());
  }

  @ParameterizedTest
  @MethodSource("testZeroExtend_Source")
  void testZeroExtend_shouldUseZeroExtendNodeInsteadOfTypeCast(String functionName) {
    var behavior = getTestBehavior(functionName);
    assertEquals(4, behavior.getNodes().count());
    var paramNode = getSingleNode(behavior, FuncParamNode.class);
    var zeroExtendNode = getSingleNode(behavior, ZeroExtendNode.class);
    assertEquals(paramNode, zeroExtendNode.value());
  }

  static Stream<Arguments> testZeroExtend_Source() {
    return findFuncNameArgumentsByPrefix("ZeroExtend_", validFrontend.getViam());
  }


  @ParameterizedTest
  @MethodSource("testSignedExtend_Source")
  void testSignExtend_shouldUseSignExtendNodeInsteadOfTypeCast(String functionName) {
    var behavior = getTestBehavior(functionName);
    assertEquals(4, behavior.getNodes().count());
    var paramNode = getSingleNode(behavior, FuncParamNode.class);
    var signExtendNode = getSingleNode(behavior, SignExtendNode.class);
    assertEquals(paramNode, signExtendNode.value());
  }

  static Stream<Arguments> testSignedExtend_Source() {
    return findFuncNameArgumentsByPrefix("SignExtend_", validFrontend.getViam());
  }

// TODO: @jzottele remove or uncomment when https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/93 is resolved
//  @ParameterizedTest
//  @MethodSource("testBoolCast_Source")
//  void testBoolCast_shouldReplaceByNegCallWithSecondArg0Const(String functionName) {
//    var behavior = getTestBehavior(functionName);
//    assertEquals(4, behavior.getNodes().count());
//    var paramNode = getSingleNode(behavior, FuncParamNode.class);
//    var compareNode = getSingleNode(behavior, BuiltInCall.class);
//    var zeroConstant = getSingleNode(behavior, ConstantNode.class);
//    assertEquals(BuiltInTable.NEG, compareNode.builtIn());
//    assertEquals(0, zeroConstant.constant().asVal().intValue());
//    assertEquals(paramNode, compareNode.arguments().get(0));
//    assertEquals(zeroConstant, compareNode.arguments().get(1));
//    var returnNode = getSingleNode(behavior, ReturnNode.class);
//    assertEquals(compareNode, returnNode.value());
//  }
//
//  static Stream<Arguments> testBoolCast_Source() {
//    return findFuncNameArgumentsByPrefix("BoolCast_", validFrontend.getViam());
//  }

  @Test
  void testNonOptimalExample() {
    var behavior = getTestBehavior("NonOptimalExample");
    assertEquals(5, behavior.getNodes().count());
    var truncateNode = getSingleNode(behavior, TruncateNode.class);
    var signExtendNode = getSingleNode(behavior, SignExtendNode.class);
    var paramNode = getSingleNode(behavior, FuncParamNode.class);
    assertEquals(paramNode, signExtendNode.value());
    assertEquals(signExtendNode, truncateNode.value());
  }

  private Graph getTestBehavior(String testFunction) {
    var spec = validFrontend.getViam();
    var simpleTypCast = findDefinitionByNameIn(testFunction, spec, Function.class);
    ViamVerifier.verifyAllIn(simpleTypCast);
    var behavior = simpleTypCast.behavior();
    assertEquals(0, behavior.getNodes(TypeCastNode.class).count());
    assertEquals(1, behavior.getNodes(StartNode.class).count());
    assertEquals(1, behavior.getNodes(ReturnNode.class).count());
    return behavior;
  }

  private static Stream<Arguments> findFuncNameArgumentsByPrefix(String prefix,
                                                                 Specification spec) {
    return
        ViamUtils.findDefinitionByFilter(spec,
                (def) -> def.identifier.name().startsWith(prefix)
                    && def instanceof Function)
            .stream()
            .map(Definition::name)
            .map(Arguments::of);
  }

}
