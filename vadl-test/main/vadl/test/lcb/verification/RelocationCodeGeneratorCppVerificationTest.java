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
            
            // drop bits outside the range (R, L) == [R, L]
            template<std::size_t R, std::size_t L, std::size_t N>
            std::bitset<N> project_range(std::bitset<N> b)
            {
                static_assert(R <= L && L <= N, "invalid bitrange");
                b >>= R;            // drop R rightmost bits
                b <<= (N - L + R + 1);  // drop L-1 leftmost bits
                b >>= (N - L);      // shift back into place
                return b;
            }
                    
            // Extraction Function
            %s
                    
            // Relocation Function
            %s
                        
            int main() {
              auto expected = %d;
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
