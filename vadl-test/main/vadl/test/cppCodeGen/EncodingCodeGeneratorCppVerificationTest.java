package vadl.test.cppCodeGen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.passes.fieldNodeReplacement.FieldNodeReplacementPass;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.gcb.passes.encoding_generation.strategies.EncodingGenerationStrategy;
import vadl.lcb.codegen.DecodingCodeGenerator;
import vadl.lcb.codegen.EncodingCodeGenerator;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.AbstractTest;
import vadl.utils.Triple;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.passes.translation_validation.TranslationValidation;

public class EncodingCodeGeneratorCppVerificationTest extends AbstractCppCodeGenTest {
  private static final String MOUNT_PATH = "/app/main.cpp";

  private static final Logger logger =
      LoggerFactory.getLogger(EncodingCodeGeneratorCppVerificationTest.class);

  private static final ImageFromDockerfile DOCKER_IMAGE = new ImageFromDockerfile()
      .withDockerfileFromBuilder(builder ->
          builder
              .from("gcc:12.4.0")
              .cmd(String.format("c++ -Wall -Werror %s && /a.out", MOUNT_PATH))
              .build());


  @TestFactory
  Collection<DynamicTest> instructions() throws IOException, DuplicatedPassKeyException {
    var setup = setupPassManagerAndRunSpec("examples/rv3264im.vadl");
    var spec = setup.right();

    var entries = spec.isas().flatMap(isa -> isa.formats().stream())
        .flatMap(format -> Arrays.stream(format.fieldAccesses()))
        .map(
            fieldAccess -> new Triple<>(fieldAccess.identifier.name(), fieldAccess.accessFunction(),
                fieldAccess.encoding()))
        .toList();

    ArrayList<DynamicTest> tests = new ArrayList<>();
    for (var entry : entries) {
      tests.add(DynamicTest.dynamicTest(entry.left(), () -> {
        testFieldAccess(entry.middle(), entry.right());
      }));
    }

    return tests;
  }

  void testFieldAccess(Function acccessFunction,
                       Function encodingFunction) {
    var decodeCodeGenerator = new DecodingCodeGenerator();
    var encodeCodeGenerator = new EncodingCodeGenerator();

    var decodeFunction = decodeCodeGenerator.generateFunction(acccessFunction);
    var encodeFunction = encodeCodeGenerator.generateFunction(encodingFunction);
    String expectedReturnType =
        CppTypeMap.getCppTypeNameByVadlType(encodingFunction.returnType());

    String cppCode = String.format("""
            #include <cstdint>
            #include <iostream>
                    
            %s 
                    
            %s
                    
            int main() {
              %s expected = %d;
              auto actual = %s(%s(expected));
              if(actual == expected) {
                std::cout << "ok" << std::endl;
                return 0; 
              } else {
                 std::cout << "Actual: " << actual << std::endl; 
                return -1; 
              }
            }
            """,
        decodeFunction,
        encodeFunction,
        expectedReturnType,
        100L,
        EncodingCodeGenerator.generateFunctionName(encodingFunction.identifier.lower()),
        DecodingCodeGenerator.generateFunctionName(acccessFunction.identifier.lower()));

    logger.info(cppCode);

    try {
      runContainerWithContent(DOCKER_IMAGE, cppCode, MOUNT_PATH);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
