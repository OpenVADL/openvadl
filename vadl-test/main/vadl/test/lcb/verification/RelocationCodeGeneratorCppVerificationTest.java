package vadl.test.lcb.verification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.model.CppUpdateBitRangeNode;
import vadl.gcb.passes.relocation.DetectImmediatePass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForImmediateExtractionPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForPredicatesPass;
import vadl.lcb.codegen.relocation.RelocationCodeGenerator;
import vadl.lcb.codegen.relocation.RelocationOverrideCodeGenerator;
import vadl.lcb.passes.relocation.GenerateElfRelocationPass;
import vadl.lcb.passes.relocation.model.ElfRelocation;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;
import vadl.types.BitsType;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Parameter;

public class RelocationCodeGeneratorCppVerificationTest extends AbstractLcbTest {
  private static final String MOUNT_PATH = "/app/main.cpp";

  private static final Logger logger =
      LoggerFactory.getLogger(RelocationCodeGeneratorCppVerificationTest.class);

  private static final ImageFromDockerfile DOCKER_IMAGE = new ImageFromDockerfile()
      .withDockerfileFromBuilder(builder ->
          builder
              .from("gcc:12.4.0")
              .cmd(String.format("c++ -Wall -Werror %s && /a.out", MOUNT_PATH))
              .build());


  //@TestFactory
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

    var elfRelocations =
        (List<ElfRelocation>) testSetup.passManager().getPassResults()
            .lastResultOf(GenerateElfRelocationPass.class);
    var immediateDetection =
        (DetectImmediatePass.ImmediateDetectionContainer) testSetup.passManager().getPassResults()
            .lastResultOf(DetectImmediatePass.class);
    var cppNormalisedImmediateExtraction = (IdentityHashMap<Function, Function>)
        testSetup.passManager().getPassResults()
            .lastResultOf(CppTypeNormalizationForImmediateExtractionPass.class);

    ArrayList<DynamicTest> tests = new ArrayList<>();
    for (var relocation : elfRelocations) {
      var format = relocation.logicalRelocation().format();
      immediateDetection.getImmediates(format).forEach(immField -> {
        var params = relocation.updateFunction().parameters();
        // The first parameter is hardcoded to be the instruction word.
        // The second parameter is the updated value.
        var arbitraryInstructionWord =
            getArbitrary(params[0]);
        var arbitraryImmediateValue = getArbitrary(immField);

        arbitraryInstructionWord.sampleStream().limit(1).forEach(instructionWordSample -> {
          arbitraryImmediateValue.sampleStream().limit(1).forEach(immediateValue -> {
            var displayName =
                String.format("immField = %s, instr = %s, imm = %s",
                    immField.identifier.lower(),
                    instructionWordSample, immediateValue);
            tests.add(DynamicTest.dynamicTest(displayName,
                () -> testUpdateFunction(displayName, immField, instructionWordSample,
                    immediateValue,
                    relocation,
                    cppNormalisedImmediateExtraction)));
          });
        });
      });
    }

    return tests;
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

  void testUpdateFunction(String testName,
                          Format.Field immField,
                          long instructionWord,
                          long updatedValue,
                          ElfRelocation relocation,
                          IdentityHashMap<Function, Function> cppNormalisedImmediateExtraction) {
    // How do we test the relocation?
    // We have an extraction function for the immediate.
    // And we have an updating function.
    //
    // So first, we generate the cpp code for the extraction function.
    // Then we generate the cpp code for updating function.
    // Finally, we update the instructionWord with the given updatedValue and
    // apply the extraction function.
    // The result of the extraction function must be the updatedValue.
    // TODO handle encoding.

    var normalisedImmediateExtractionFunction =
        cppNormalisedImmediateExtraction.get(immField.extractFunction());

    var extractionFunctionCodeGenerator = new RelocationCodeGenerator();
    var relocationOverrideFunctionCodeGenerator = new RelocationOverrideCodeGenerator();

    var extractionFunctionName = extractionFunctionCodeGenerator.getFunctionName(
        immField.extractFunction().identifier.lower());
    var relocationFunctionName = relocationOverrideFunctionCodeGenerator.getFunctionName(
        relocation.logicalRelocation().updateFunction().identifier.lower());

    String cppCode = String.format("""
            #include <cstdint>
            #include <iostream>
            #include <bitset>
            
            template<std::size_t start, std::size_t end, std::size_t N>
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
            std::bitset<N> set_bit_range(std::bitset<N> dest, const std::bitset<M> source, size_t dest_start, size_t dest_end) {
                for (size_t i = 0; i <= dest_end - dest_start; ++i) {
                    dest.set(dest_start + i, source[i]);
                }
                
                return dest;
            }
            
            template<std::size_t N, std::size_t M, typename... Ranges>
            std::bitset<N> set_multiple_bit_ranges(std::bitset<N> dest, const std::bitset<M> source, Ranges... ranges) {
                return (set_bit_range(dest, source, ranges.first, ranges.second), ...);
            }
            
            // Extraction Function
            %s
                    
            // Relocation Function
            %s
                        
            int main() {
              %s expected = %d;
              auto actual = %s(%s(%s, %s));
              if(actual == expected) {
                std::cout << "ok" << std::endl;
                return 0;
              } else {
                 std::cout << "Actual: " << actual << std::endl;
                return -1;
              }
            }
            """,
        extractionFunctionCodeGenerator.generateFunction(normalisedImmediateExtractionFunction),
        relocationOverrideFunctionCodeGenerator.generateFunction(
            relocation.logicalRelocation().updateFunction()),
        CppTypeMap.getCppTypeNameByVadlType(normalisedImmediateExtractionFunction.returnType()),
        updatedValue,
        extractionFunctionName,
        relocationFunctionName,
        instructionWord,
        updatedValue);

    try {
      logger.info(testName + "\n" + cppCode);
      runContainerWithContent(DOCKER_IMAGE, cppCode, MOUNT_PATH);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
