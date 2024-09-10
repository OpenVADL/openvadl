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
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForEncodingsPass;
import vadl.lcb.codegen.CodeGenerator;
import vadl.pass.PassOrder;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.types.BitsType;
import vadl.utils.Quadruple;

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
  @Execution(ExecutionMode.CONCURRENT)
  Collection<DynamicTest> instructions() throws IOException, DuplicatedPassKeyException {
    var setup = setupPassManagerAndRunSpec("sys/risc-v/rv64im.vadl",
        PassOrder.gcbAndCppCodeGen(getConfiguration(false)));
    var passManager = setup.passManager();
    var spec = setup.specification();

    var normalizedDecodings =
        (CppTypeNormalizationPass.NormalisedTypeResult) passManager.getPassResults()
            .lastResultOf(CppTypeNormalizationForDecodingsPass.class);
    var normalizedEncodings =
        (CppTypeNormalizationPass.NormalisedTypeResult) passManager.getPassResults()
            .lastResultOf(CppTypeNormalizationForEncodingsPass.class);

    var entries = spec.isa().map(isa -> isa.ownFormats().stream())
        .orElse(Stream.empty())
        .flatMap(format -> Arrays.stream(format.fieldAccesses()))
        .map(
            fieldAccess -> {
              var accessFunction = normalizedDecodings.byFunction(fieldAccess.accessFunction());
              var encodeFunction = normalizedEncodings.byFunction(fieldAccess.encoding());
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
    return Arbitraries.integers().greaterOrEqual(0).lessOrEqual((int) Math.pow(2, bitWidth - 1));
  }

  void testFieldAccess(String testName,
                       int sample,
                       CppFunction acccessFunction,
                       CppFunction encodingFunction) {
    var decodeFunction = new CodeGenerator().generateFunction(acccessFunction);
    var encodeFunction = new CodeGenerator().generateFunction(encodingFunction);
    String expectedReturnType =
        CppTypeMap.getCppTypeNameByVadlType(encodingFunction.returnType());

    String cppCode = String.format("""
            #include <cstdint>
            #include <iostream>
            #include <bitset>
            #include <vector>
            
            template<int start, int end, std::size_t N>
            std::bitset<N> project_range(std::bitset<N> bits)
            {
                std::bitset<N> result;
                size_t result_index = 0; // Index for the new bitset
                        
                // Extract bits from the range [start, end]
                for (size_t i = start; i <= end; ++i) {
                  result[result_index] = bits[i];
                  result_index++;
                }
                        
                return result;
            }
                        
            template<std::size_t N, std::size_t M>
            std::bitset<N> set_bits(std::bitset<N> dest, const std::bitset<M> source, std::vector<int> bits) {
                auto target = 0;
                for (int i = bits.size() - 1; i >= 0 ; --i) {
                    auto j = bits[target];
                    dest.set(j, source[i]);
                    target++;
                }
                
                return dest;
            }
                        
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
        decodeFunction.value(),
        encodeFunction.value(),
        expectedReturnType,
        sample,
        encodingFunction.identifier.lower(),
        acccessFunction.identifier.lower());

    logger.info(testName + "\n" + cppCode);

    try {
      runContainerWithContent(DOCKER_IMAGE, cppCode, MOUNT_PATH);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
