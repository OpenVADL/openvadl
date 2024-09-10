package vadl.test.cppCodeGen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.cppCodeGen.CppTypeMap;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForEncodingsPass;
import vadl.lcb.codegen.encoding.DecodingCodeGenerator;
import vadl.lcb.codegen.encoding.EncodingCodeGenerator;
import vadl.pass.PassOrder;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.types.BitsType;
import vadl.utils.Quadruple;
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


  //@TestFactory
  //@Execution(ExecutionMode.CONCURRENT)
  Collection<DynamicTest> instructions() throws IOException, DuplicatedPassKeyException {
    var setup = setupPassManagerAndRunSpec("sys/risc-v/rv64im.vadl",
        PassOrder.gcbAndCppCodeGen(getConfiguration(false)));
    var passManager = setup.passManager();
    var spec = setup.specification();

    var normalizedDecodings =
        (IdentityHashMap<Function, Function>) passManager.getPassResults()
            .lastResultOf(CppTypeNormalizationForDecodingsPass.class);
    var normalizedEncodings =
        (IdentityHashMap<Function, Function>) passManager.getPassResults()
            .lastResultOf(CppTypeNormalizationForEncodingsPass.class);

    var entries = spec.isa().map(isa -> isa.ownFormats().stream())
        .orElse(Stream.empty())
        .flatMap(format -> Arrays.stream(format.fieldAccesses()))
        .map(
            fieldAccess -> {
              var accessFunction = normalizedDecodings.get(fieldAccess.accessFunction());
              var encodeFunction = normalizedEncodings.get(fieldAccess.encoding());
              var inputType =
                  Arrays.stream(fieldAccess.accessFunction().parameters()).findFirst().get().type();
              return new Quadruple<>(fieldAccess.identifier.name(), (BitsType) inputType,
                  accessFunction,
                  encodeFunction);
            })
        .toList();

    ArrayList<DynamicTest> tests = new ArrayList<>();
    for (var entry : entries) {
      var arbitrary = uint(entry.second().bitWidth());
      arbitrary.sampleStream().limit(15).forEach(sample -> {
        var displayName = entry.first() + " sample=" + sample;
        tests.add(DynamicTest.dynamicTest(displayName,
            () -> testFieldAccess(entry.first(), sample, entry.third(), entry.fourth())));
      });
    }

    return tests;
  }


  Arbitrary<Integer> uint(int bitWidth) {
    return Arbitraries.integers().greaterOrEqual(0).lessOrEqual((int) Math.pow(2, bitWidth));
  }

  void testFieldAccess(String testName,
                       int sample,
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
        sample,
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
