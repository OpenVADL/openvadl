package vadl.test.lcb.verification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.shaded.com.google.common.collect.Streams;
import vadl.cppCodeGen.CppTypeMap;
import vadl.gcb.passes.relocation.DetectImmediatePass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForEncodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForImmediateExtractionPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForPredicatesPass;
import vadl.lcb.codegen.encoding.DecodingCodeGenerator;
import vadl.lcb.codegen.encoding.EncodingCodeGenerator;
import vadl.lcb.codegen.relocation.RelocationCodeGenerator;
import vadl.lcb.codegen.relocation.RelocationOverrideCodeGenerator;
import vadl.lcb.passes.relocation.GenerateElfRelocationPass;
import vadl.lcb.passes.relocation.model.ElfRelocation;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;
import vadl.types.BitsType;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Function;
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
    //@Execution(ExecutionMode.CONCURRENT)
  Collection<DynamicTest> instructions() throws IOException, DuplicatedPassKeyException {
    var configuration = getConfiguration(false);
    var temporaryPasses = List.of(
        new TemporaryTestPassInjection(
            CppTypeNormalizationForPredicatesPass.class,
            new CppTypeNormalizationForImmediateExtractionPass(configuration)
        )
    );
    var testSetup = runLcb(configuration,
        "sys/risc-v/rv64im.vadl", new PassKey(GenerateElfRelocationPass.class.getName()),
        temporaryPasses);

    var immediateDetection =
        (DetectImmediatePass.ImmediateDetectionContainer) testSetup.passManager().getPassResults()
            .lastResultOf(DetectImmediatePass.class);
    var normalizedDecodings =
        (IdentityHashMap<Function, Function>) testSetup.passManager().getPassResults()
            .lastResultOf(CppTypeNormalizationForDecodingsPass.class);
    var normalizedEncodings =
        (IdentityHashMap<Function, Function>) testSetup.passManager().getPassResults()
            .lastResultOf(CppTypeNormalizationForEncodingsPass.class);
    var cppNormalisedImmediateExtraction = (IdentityHashMap<Function, Function>)
        testSetup.passManager().getPassResults()
            .lastResultOf(CppTypeNormalizationForImmediateExtractionPass.class);

    ArrayList<DynamicTest> tests = new ArrayList<>();
    testSetup.specification()
        .isas()
        .flatMap(isa -> isa.ownFormats().stream())
        .flatMap(format -> Arrays.stream(format.fieldAccesses()))
        .forEach(fieldAccess -> {
          var params = fieldAccess.fieldRef().extractFunction().parameters();
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
                    String.format("fieldAccess = %s",
                        fieldAccess.identifier.lower());
                tests.add(DynamicTest.dynamicTest(displayName,
                    () -> testExtractFunction(displayName, fieldAccess, pair.left(), pair.right(),
                        bitWidth,
                        normalizedDecodings,
                        normalizedEncodings,
                        cppNormalisedImmediateExtraction)));
              });

        });

    return tests;
  }

  private void testExtractFunction(String testName, Format.FieldAccess fieldAccess,
                                   Long imm,
                                   Long tail,
                                   int bitWidth,
                                   IdentityHashMap<Function, Function> normalisedDecodings,
                                   IdentityHashMap<Function, Function> normalisedEncodings,
                                   IdentityHashMap<Function, Function> cppNormalisedImmediateExtraction) {
    var decodeCodeGenerator = new DecodingCodeGenerator();
    var encodeCodeGenerator = new EncodingCodeGenerator();
    var extractionFunctionCodeGenerator = new RelocationCodeGenerator();

    var extractFunction =
        cppNormalisedImmediateExtraction.get(fieldAccess.fieldRef().extractFunction());

    var extractionFunctionCode = extractionFunctionCodeGenerator.generateFunction(extractFunction);

    var extractionFunctionName = extractionFunctionCodeGenerator.getFunctionName(
        fieldAccess.fieldRef().extractFunction().identifier.lower());

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
            std::bitset<N> set_bit_range(std::bitset<N> dest, const std::bitset<M> source, int dest_start, int dest_end, int offset = 0) {
                auto j = dest_end;
                std::cout << "Dest start " << dest_start << std::endl;
                for (int i = M - offset - 1; j >= dest_start ; --i) {
                    dest.set(j, source[i]);
                    std::cout << "Set " << j << " from " << i << std::endl;
                    j--;
                }
                std::cout << std::endl;
                
                return dest;
            }
                        
            template<std::size_t N, std::size_t M>
            std::bitset<N> set_multiple_bit_ranges(std::bitset<N> dest, const std::bitset<M> source, std::vector<std::tuple<int, int>> ranges) {
                auto acc = 0;
                for(const auto &i : ranges) {
                  dest |= set_bit_range(dest, source, std::get<0>(i), std::get<1>(i), acc) << acc;
                  std::cout << "acc " << acc << std::endl;
                  acc += std::get<1>(i) - std::get<0>(i) + 1;
                  //std::cout << "dest " << dest.to_string() << std::endl;
                }
                 
                return dest;
            }
                        
            // Extraction Function
            %s
                    
            int main() {
              %s expected = %d;
              std::vector<std::tuple<int, int>> args = { %s };
              auto actual = %s(set_multiple_bit_ranges(std::bitset<%d>(%d), std::bitset<%d>(%d), args).to_ulong());
              if(actual == expected) {
                std::cout << "ok" << std::endl;
                return 0;
              } else {
                 std::cout << "Actual: " << actual << std::endl;
                return -1;
              }
            }
            """,
        extractionFunctionCode,
        CppTypeMap.getCppTypeNameByVadlType(extractFunction.returnType()),
        imm,
        fieldAccess.fieldRef().bitSlice().parts()
            .map(slice -> String.format("std::make_pair(%d, %d)", slice.lsb(), slice.msb()))
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

  private Arbitrary<Long> getArbitrary(Format.Field field) {
    return uint(field.size());
  }

  Arbitrary<Long> uint(int bitWidth) {
    return Arbitraries.longs().greaterOrEqual(0).lessOrEqual((long) Math.pow(2, bitWidth));
  }
}
