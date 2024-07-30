package vadl.oop.passes.type_normalization;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.AbstractTest;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Parameter;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.TypeCastNode;

class CppTypeNormalizationPassTest extends AbstractTest {

  private static Stream<Arguments> generateTypesWhichRequireUpcast() {
    // (type, upcasted type)
    return Stream.of(
        Arguments.of(DataType.unsignedInt(2), DataType.unsignedInt(8)),
        Arguments.of(DataType.unsignedInt(9), DataType.unsignedInt(16)),
        Arguments.of(DataType.unsignedInt(17), DataType.unsignedInt(32)),
        Arguments.of(DataType.unsignedInt(33), DataType.unsignedInt(64)),
        Arguments.of(DataType.unsignedInt(65), DataType.unsignedInt(128)),
        Arguments.of(DataType.signedInt(2), DataType.signedInt(8)),
        Arguments.of(DataType.signedInt(9), DataType.signedInt(16)),
        Arguments.of(DataType.signedInt(17), DataType.signedInt(32)),
        Arguments.of(DataType.signedInt(33), DataType.signedInt(64)),
        Arguments.of(DataType.signedInt(65), DataType.signedInt(128)),
        Arguments.of(DataType.bits(2), DataType.bits(8)),
        Arguments.of(DataType.bits(9), DataType.bits(16)),
        Arguments.of(DataType.bits(17), DataType.bits(32)),
        Arguments.of(DataType.bits(33), DataType.bits(64)),
        Arguments.of(DataType.bits(65), DataType.bits(128))
    );
  }

  private static Stream<Arguments> generateTypesWhichDontRequireUpcast() {
    return Stream.of(
        Arguments.of(DataType.unsignedInt(1)),
        Arguments.of(DataType.unsignedInt(8)),
        Arguments.of(DataType.unsignedInt(16)),
        Arguments.of(DataType.unsignedInt(32)),
        Arguments.of(DataType.unsignedInt(64)),
        Arguments.of(DataType.signedInt(1)),
        Arguments.of(DataType.signedInt(8)),
        Arguments.of(DataType.signedInt(16)),
        Arguments.of(DataType.signedInt(32)),
        Arguments.of(DataType.signedInt(64)),
        Arguments.of(DataType.bits(1)),
        Arguments.of(DataType.bits(8)),
        Arguments.of(DataType.bits(16)),
        Arguments.of(DataType.bits(32)),
        Arguments.of(DataType.bits(64))
    );
  }

  @ParameterizedTest
  @MethodSource("generateTypesWhichRequireUpcast")
  void makeTypesCppConform_shouldUpcastParameters(Type before, Type after) {
    // Given
    var function = createFunction("functionValueName", new Parameter(
        createIdentifier("parameterValue"), before
    ), DataType.bool());

    // When
    var updatedFunction = CppTypeNormalizationPass.makeTypesCppConform(function);

    // Then
    assertThat(updatedFunction).isNotNull();
    assertThat(updatedFunction.returnType()).isEqualTo(DataType.bool());
    assertThat(updatedFunction.parameters().length).isEqualTo(1);
    assertThat(Arrays.stream(updatedFunction.parameters()).toList().get(0).identifier).isEqualTo(
        createIdentifier("parameterValue"));
    assertThat(Arrays.stream(updatedFunction.parameters()).toList().get(0).type()).isEqualTo(
        after);
  }

  @ParameterizedTest
  @MethodSource("generateTypesWhichDontRequireUpcast")
  void makeTypesCppConform_shouldNotUpcastParameters_whenTypeOk(Type type) {
    // Given
    var function = createFunction("functionValueName", new Parameter(
        createIdentifier("parameterValue"), type
    ), DataType.bool());

    // When
    var updatedFunction = CppTypeNormalizationPass.makeTypesCppConform(function);

    // Then
    assertThat(updatedFunction).isNotNull();
    assertThat(updatedFunction.returnType()).isEqualTo(DataType.bool());
    assertThat(updatedFunction.parameters().length).isEqualTo(1);
    assertThat(Arrays.stream(updatedFunction.parameters()).toList().get(0).identifier).isEqualTo(
        createIdentifier("parameterValue"));
    assertThat(Arrays.stream(updatedFunction.parameters()).toList().get(0).type()).isEqualTo(
        type);
  }

  @ParameterizedTest
  @MethodSource("generateTypesWhichRequireUpcast")
  void makeTypesCppConform_shouldUpcastReturnType(DataType before, DataType after) {
    // Given
    var function = createFunction("functionValueName", new Parameter(
        createIdentifier("parameterValue"), DataType.bool()
    ), before);

    // When
    var updatedFunction = CppTypeNormalizationPass.makeTypesCppConform(function);

    // Then
    assertThat(updatedFunction).isNotNull();
    assertThat(updatedFunction.parameters().length).isEqualTo(1);
    assertThat(updatedFunction.returnType()).isEqualTo(after);
  }

  @ParameterizedTest
  @MethodSource("generateTypesWhichDontRequireUpcast")
  void makeTypesCppConform_shouldNotUpcastReturnType_whenTypeOk(DataType type) {
    // Given
    var function = createFunction("functionValueName", new Parameter(
        createIdentifier("parameterValue"), DataType.bool()
    ), type);

    // When
    var updatedFunction = CppTypeNormalizationPass.makeTypesCppConform(function);

    // Then
    assertThat(updatedFunction).isNotNull();
    assertThat(updatedFunction.parameters().length).isEqualTo(1);
    assertThat(updatedFunction.returnType()).isEqualTo(type);
  }

  @ParameterizedTest
  @MethodSource("generateTypesWhichRequireUpcast")
  void makeTypesCppConform_shouldUpdateTypeCast(DataType before, DataType after) {
    // Given
    var function = createFunction("functionValueName", new Parameter(
        createIdentifier("parameterValue"), DataType.bool()
    ), DataType.bool());
    function.behavior().addWithInputs(
        new TypeCastNode(new ConstantNode(Constant.Value.of(0, Type.signedInt(8))),
            before));

    // When
    var updatedFunction = CppTypeNormalizationPass.makeTypesCppConform(function);

    // Then
    var node = updatedFunction.behavior().getNodes(UpcastedTypeCastNode.class).toList().get(0);
    assertThat(node.castType()).isEqualTo(after);
    assertThat(node.originalType()).isEqualTo(before);
  }

  @ParameterizedTest
  @MethodSource("generateTypesWhichDontRequireUpcast")
  void makeTypesCppConform_shouldNotUpdateTypeCast_whenTypeOk(DataType type) {
    // Given
    var function = createFunction("functionValueName", new Parameter(
        createIdentifier("parameterValue"), DataType.bool()
    ), DataType.bool());
    function.behavior().addWithInputs(
        new TypeCastNode(new ConstantNode(Constant.Value.of(0, Type.signedInt(8))),
            type));

    // When
    var updatedFunction = CppTypeNormalizationPass.makeTypesCppConform(function);

    // Then
    assertThat(updatedFunction.behavior().getNodes(TypeCastNode.class).toList().get(0)
        .castType()).isEqualTo(type);
  }

  @ParameterizedTest
  @MethodSource("generateTypesWhichRequireUpcast")
  void makeTypesCppConform_shouldUpdateConstants(DataType before, DataType after) {
    // Given
    var function = createFunction("functionValueName", new Parameter(
        createIdentifier("parameterValue"), DataType.bool()
    ), DataType.bool());
    function.behavior().addWithInputs(
        new ConstantNode(Constant.Value.of(0, before)));

    // When
    var updatedFunction = CppTypeNormalizationPass.makeTypesCppConform(function);

    // Then
    var node = updatedFunction.behavior().getNodes(ConstantNode.class).toList().get(0);
    var constant = (Constant.Value) node.constant();
    assertThat(constant.integer()).isEqualTo(0);
    assertThat(constant.type()).isEqualTo(after);
  }


  @ParameterizedTest
  @MethodSource("generateTypesWhichDontRequireUpcast")
  void makeTypesCppConform_shouldNotUpdateConstants_whenTypeOk(DataType type) {
    // Given
    var function = createFunction("functionValueName", new Parameter(
        createIdentifier("parameterValue"), DataType.bool()
    ), DataType.bool());
    function.behavior().addWithInputs(
        new ConstantNode(Constant.Value.of(0, type)));

    // When
    var updatedFunction = CppTypeNormalizationPass.makeTypesCppConform(function);

    // Then
    var node = updatedFunction.behavior().getNodes(ConstantNode.class).toList().get(0);
    var constant = (Constant.Value) node.constant();
    assertThat(constant.integer()).isEqualTo(0);
    assertThat(constant.type()).isEqualTo(type);
  }

}