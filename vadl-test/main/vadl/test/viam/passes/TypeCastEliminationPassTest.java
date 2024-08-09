package vadl.test.viam.passes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static vadl.test.TestUtils.findDefinitionByNameIn;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.test.AbstractTest;
import vadl.test.TestFrontend;
import vadl.types.BuiltInTable;
import vadl.viam.Function;
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
    new TypeCastEliminationPass().execute(Map.of(), validFrontend.getViam());
  }


  @ParameterizedTest
  @MethodSource("testTrivial_Source")
  void testTrivial_shouldDeleteTypeCastAndMakeArgumentTypeSameAsResultType(String functionName) {
    var behavior = getTestBehavior(functionName);
    assertEquals(3, behavior.getNodes().count());
    var returnNode = getSingleNode(behavior, ReturnNode.class);
    var paramNode = getSingleNode(behavior, FuncParamNode.class);
    assertEquals(returnNode.returnType(), paramNode.type());
    behavior.verify();
  }

  static Stream<Arguments> testTrivial_Source() {
    return Stream.of(
        Arguments.of("Trivial_Bits_Bits"),
        Arguments.of("Trivial_SInt_Bits"),
        Arguments.of("Trivial_UInt_Bits"),
        Arguments.of("Trivial_Bool_Bits"),
        Arguments.of("Trivial_Bits_UInt"),
        Arguments.of("Trivial_SInt_UInt"),
        Arguments.of("Trivial_UInt_UInt"),
        Arguments.of("Trivial_Bool_UInt"),
        Arguments.of("Trivial_Bits_SInt"),
        Arguments.of("Trivial_SInt_SInt"),
        Arguments.of("Trivial_UInt_SInt"),
        Arguments.of("Trivial_Bool_SInt"),
        Arguments.of("Trivial_Sint_Uint_Bits")
    );
  }

  @ParameterizedTest
  @MethodSource("testTruncate_Source")
  void testTruncate_shouldUseTruncateNodeInsteadOfTypeCast(String functionName) {
    var behavior = getTestBehavior(functionName);
    assertEquals(4, behavior.getNodes().count());
    assertEquals(1, behavior.getNodes(TruncateNode.class).count());
  }

  static Stream<Arguments> testTruncate_Source() {
    return Stream.of(
        Arguments.of("Truncate_SInt10_SInt5")
    );
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
    return Stream.of(
        Arguments.of("ZeroExtend_SInt10_UInt20")
    );
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
    return Stream.of(
        Arguments.of("SignExtend_SInt_SInt"),
        Arguments.of("SignExtend_Bits_SInt"),
        Arguments.of("SignExtend_UInt_SInt"),
        Arguments.of("SignExtend_Bool_SInt")
    );
  }


  @ParameterizedTest
  @MethodSource("testBoolCast_Source")
  void testBoolCast_shouldReplaceByNegCallWithSecondArg0Const(String functionName) {
    var behavior = getTestBehavior(functionName);
    assertEquals(4, behavior.getNodes().count());
    var paramNode = getSingleNode(behavior, FuncParamNode.class);
    var compareNode = getSingleNode(behavior, BuiltInCall.class);
    var zeroConstant = getSingleNode(behavior, ConstantNode.class);
    assertEquals(BuiltInTable.NEG, compareNode.builtIn());
    assertEquals(0, zeroConstant.constant().asVal().intValue());
    assertEquals(paramNode, compareNode.arguments().get(0));
    assertEquals(zeroConstant, compareNode.arguments().get(1));
    var returnNode = getSingleNode(behavior, ReturnNode.class);
    assertEquals(compareNode, returnNode.value());
  }

  static Stream<Arguments> testBoolCast_Source() {
    return Stream.of(
        Arguments.of("BoolCast_Bits1"),
        Arguments.of("BoolCast_SInt1"),
        Arguments.of("BoolCast_UInt1"),
        Arguments.of("BoolCast_Bool1"),
        Arguments.of("BoolCast_SInt10")
    );
  }

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


}
