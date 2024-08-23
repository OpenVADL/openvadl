package vadl.test.cppCodeGen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.cppCodeGen.CppTypeMap;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForEncodingsPass;
import vadl.lcb.codegen.DecodingCodeGenerator;
import vadl.lcb.codegen.EncodingCodeGenerator;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.Triple;
import vadl.viam.Function;

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
    var passManager = setup.left();
    var spec = setup.right();

    var normalizedDecodings =
        (IdentityHashMap<Function, Function>) passManager.getPassResults().get(new PassKey(
            CppTypeNormalizationForDecodingsPass.class.getName()));
    var normalizedEncodings =
        (IdentityHashMap<Function, Function>) passManager.getPassResults().get(new PassKey(
            CppTypeNormalizationForEncodingsPass.class.getName()));

    var entries = spec.isas().flatMap(isa -> isa.formats().stream())
        .flatMap(format -> Arrays.stream(format.fieldAccesses()))
        .map(
            fieldAccess -> {
              var accessFunction = normalizedDecodings.get(fieldAccess.accessFunction());
              var encodeFunction = normalizedEncodings.get(fieldAccess.encoding());
              return new Triple<>(fieldAccess.identifier.name(), accessFunction, encodeFunction);
            })
        .toList();

    ArrayList<DynamicTest> tests = new ArrayList<>();
    for (var entry : entries) {
      tests.add(DynamicTest.dynamicTest(entry.left(), () -> {
        testFieldAccess(entry.left(), entry.middle(), entry.right());
      }));
    }

    return tests;
  }

  void testFieldAccess(String testName,
                       Function acccessFunction,
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
        31L,
        EncodingCodeGenerator.generateFunctionName(encodingFunction.identifier.lower()),
        DecodingCodeGenerator.generateFunctionName(acccessFunction.identifier.lower()));

    logger.info(testName + "\n" + cppCode);

    try {
      runContainerWithContent(DOCKER_IMAGE, cppCode, MOUNT_PATH);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
