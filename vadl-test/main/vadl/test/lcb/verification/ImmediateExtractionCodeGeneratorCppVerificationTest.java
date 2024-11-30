package vadl.test.lcb.verification;

import java.io.IOException;
import java.util.ArrayList;
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
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.gcb.passes.typeNormalization.CppTypeNormalizationForImmediateExtractionPass;
import vadl.gcb.passes.typeNormalization.CppTypeNormalizationForPredicatesPass;
import vadl.lcb.codegen.LcbGenericCodeGenerator;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;
import vadl.utils.Pair;
import vadl.viam.Format;

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
        .flatMap(format -> format.fieldAccesses().stream())
        .forEach(fieldAccess -> {
          var fieldBitSize = fieldAccess.fieldRef().size();
          // A random immediate which should be extracted from the `arbitraryInstruction` later.
          var arbitraryImmediate =
              uint(fieldBitSize);
          // A random instruction word.
          var arbitraryInstruction =
              uint(fieldAccess.fieldRef().format().type().bitWidth());
          var limit = 10;

          Streams.zip(arbitraryImmediate.sampleStream().limit(limit),
                  arbitraryInstruction.sampleStream().limit(limit),
                  Pair::of)
              .forEach(pair -> {
                var displayName =
                    String.format("fieldAccess = %s (%s, %s)",
                        fieldAccess.identifier.lower(),
                        pair.left(),
                        pair.right());
                tests.add(DynamicTest.dynamicTest(displayName,
                    () -> testExtractFunction(displayName, fieldAccess, pair.left(), pair.right(),
                        fieldBitSize,
                        cppNormalisedImmediateExtraction)));
              });

        });

    return tests;
  }

  /**
   * The idea of the test is that we have a {@code instruction} and we want to extract
   * the immediate {@code x} from it. This immediate {@code x} has to be equal to the given
   * parameter {@code imm}.
   * However, note that {@code instruction} has not yet the immediate encoded in it. This is also
   * done in the container test with `set_bits` function.
   */
  private void testExtractFunction(String testName,
                                   Format.FieldAccess fieldAccess,
                                   Long imm,
                                   Long instruction,
                                   int fieldBitSize,
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
              ulong expected = %d;
              std::vector<int> args = { %s };
              // We have a random set of bits and set the immediate to the position of the
              // instruction (based on `args`).
              ulong instruction =  set_bits(std::bitset<%d>(%d), std::bitset<%d>(%d), args)
                .to_ulong();
              // Now, we extract the immediate again and this *must* be the same.
              auto actual = %s(instruction);
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
        imm,
        fieldAccess.fieldRef().bitSlice().stream()
            .mapToObj(String::valueOf)
            .collect(
                Collectors.joining(", ")),
        fieldAccess.fieldRef().format().type().bitWidth(),
        instruction,
        fieldBitSize,
        imm,
        extractionFunctionName
    );

    try {
      logger.info(testName + "\n" + cppCode);
      runContainerAndCopyInputIntoContainer(DOCKER_IMAGE, cppCode, MOUNT_PATH);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  Arbitrary<Long> uint(int bitWidth) {
    return Arbitraries.longs().greaterOrEqual(0).lessOrEqual((long) Math.pow(2, bitWidth) - 1);
  }
}
