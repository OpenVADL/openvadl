package vadl.test.lcb.verification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
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
import org.testcontainers.shaded.com.google.common.collect.Streams;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForImmediateExtractionPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForPredicatesPass;
import vadl.lcb.codegen.LcbGenericCodeGenerator;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;
import vadl.types.BitsType;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Parameter;

public class ImmediateExtractionCodeGeneratorCppVerificationTest extends AbstractLcbTest {
  private static final String MOUNT_PATH = "/app/main.cpp";

  private static final Logger logger =
      LoggerFactory.getLogger(ImmediateExtractionCodeGeneratorCppVerificationTest.class);

  private static final ImageFromDockerfile DOCKER_IMAGE = new ImageFromDockerfile()
      .withDockerfileFromBuilder(builder ->
          builder
              .from("gcc:12.4.0")
              .cmd(String.format("c++ -Wall -Werror %s && /a.out", MOUNT_PATH))
              .build());


  @TestFactory
  @Execution(ExecutionMode.CONCURRENT)
  Collection<DynamicTest> instructions() throws IOException, DuplicatedPassKeyException {
    var configuration = getConfiguration(false);
    var temporaryPasses = List.of(
        new TemporaryTestPassInjection(
            CppTypeNormalizationForPredicatesPass.class,
            new CppTypeNormalizationForImmediateExtractionPass(configuration)
        )
    );
    var testSetup = runLcb(configuration,
        "sys/risc-v/rv64im.vadl", new PassKey(GenerateLinkerComponentsPass.class.getName()),
        temporaryPasses);

    var cppNormalisedImmediateExtraction = (CppTypeNormalizationPass.NormalisedTypeResult)
        testSetup.passManager().getPassResults()
            .lastResultOf(CppTypeNormalizationForImmediateExtractionPass.class);

    ArrayList<DynamicTest> tests = new ArrayList<>();
    testSetup.specification()
        .isa()
        .map(isa -> isa.ownFormats().stream())
        .orElse(Stream.empty())
        .flatMap(format -> Arrays.stream(format.fieldAccesses()))
        .forEach(fieldAccess -> {
          var bitWidth = fieldAccess.fieldRef().format().type().bitWidth();
          var arbitraryImmediate =
              getArbitrary(fieldAccess.accessFunction().parameters()[0]);
          var arbitraryTail =
              uint(bitWidth);
          var limit = 1;

          Streams.zip(arbitraryImmediate.sampleStream().limit(limit),
                  arbitraryTail.sampleStream().limit(limit),
                  Pair::of)
              .forEach(pair -> {
                var displayName =
                    String.format("fieldAccess = %s (%s, %s",
                        fieldAccess.identifier.lower(),
                        pair.left(),
                        pair.right());
                tests.add(DynamicTest.dynamicTest(displayName,
                    () -> testExtractFunction(displayName, fieldAccess, pair.left(), pair.right(),
                        bitWidth,
                        cppNormalisedImmediateExtraction)));
              });

        });

    return tests;
  }

  private void testExtractFunction(String testName, Format.FieldAccess fieldAccess,
                                   Long imm,
                                   Long tail,
                                   int bitWidth,
                                   CppTypeNormalizationPass.NormalisedTypeResult cppNormalisedImmediateExtraction) {
    var extractionFunctionCodeGenerator = new LcbGenericCodeGenerator();

    var extractFunction =
        cppNormalisedImmediateExtraction.byFunction(fieldAccess.fieldRef().extractFunction());

    var extractionFunctionCode = extractionFunctionCodeGenerator.generateFunction(extractFunction);

    var extractionFunctionName =
        fieldAccess.fieldRef().extractFunction().identifier.lower();

    String cppCode = String.format("""
            #include <cstdint>
            #include <iostream>
            #include <bitset>
            #include <vector>
            #include <tuple>
                        
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
                        
            // Extraction Function
            %s
                        
            int main() {
              %s expected = %d;
              std::vector<int> args = { %s };
              auto actual = %s(set_bits(std::bitset<%d>(%d), std::bitset<%d>(%d), args).to_ulong());
              if(actual == expected) {
                std::cout << "ok" << std::endl;
                return 0;
              } else {
                 std::cout << "Actual: " << actual << std::endl;
                return -1;
              }
            }
            """,
        extractionFunctionCode.value(),
        CppTypeMap.getCppTypeNameByVadlType(extractFunction.returnType()),
        imm,
        fieldAccess.fieldRef().bitSlice().stream()
            .mapToObj(String::valueOf)
            .collect(
                Collectors.joining(", ")),
        extractionFunctionName,
        bitWidth,
        tail,
        fieldAccess.fieldRef().type().bitWidth(),
        imm
    );

    try {
      logger.info(testName + "\n" + cppCode);
      runContainerWithContent(DOCKER_IMAGE, cppCode, MOUNT_PATH);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Arbitrary<Long> getArbitrary(Parameter parameter) {
    var type = (BitsType) parameter.type();
    return uint(type.bitWidth());
  }

  Arbitrary<Long> uint(int bitWidth) {
    return Arbitraries.longs().greaterOrEqual(0).lessOrEqual((long) Math.pow(2, bitWidth));
  }
}
