package vadl.test.cppCodeGen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import vadl.configuration.GcbConfiguration;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.gcb.passes.typeNormalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.typeNormalization.CppTypeNormalizationForEncodingsPass;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.codegen.LcbGenericCodeGenerator;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.AbstractTest;
import vadl.types.BitsType;
import vadl.utils.Pair;
import vadl.utils.Quadruple;
import vadl.utils.VadlFileUtils;

public class EncodingCodeGeneratorCppVerificationTest extends AbstractCppCodeGenTest {
  private static final Logger logger =
      LoggerFactory.getLogger(EncodingCodeGeneratorCppVerificationTest.class);

  private static Stream<String> inputFilesFromCFile(String inputDirectory) {
    return Arrays.stream(
            Objects.requireNonNull(new File(inputDirectory)
                .listFiles()))
        .filter(File::isFile)
        .map(File::getName);
  }

  @TestFactory
  Collection<DynamicTest> instructions() throws IOException, DuplicatedPassKeyException {
    var configuration = new GcbConfiguration(getConfiguration(false));

    var setup = runGcbAndCppCodeGen(configuration, "sys/risc-v/rv64im.vadl");

    // Move files into Docker Context
    {
      Files.createDirectory(Path.of(configuration.outputPath() + "/encoding"));
      Files.createDirectory(Path.of(configuration.outputPath() + "/inputs"));

      VadlFileUtils.copyDirectory(
          Path.of(
              "../../open-vadl/vadl-test/main/resources/images/encodingCodeGeneratorCppVerification/"),
          Path.of(configuration.outputPath() + "/encoding/"));
    }

    var image = new ImageFromDockerfile()
        .withDockerfile(Paths.get(configuration.outputPath() + "/encoding/Dockerfile"));

    // Generate files and output them into the temporary directory.
    return generateInputs(setup, image, configuration.outputPath());
  }

  private Collection<DynamicTest> generateInputs(TestSetup setup,
                                                 ImageFromDockerfile image,
                                                 Path path) throws IOException {
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
        .flatMap(format -> format.fieldAccesses().stream())
        .map(
            fieldAccess -> {
              var accessFunction = normalizedDecodings.byFunction(fieldAccess.accessFunction());
              var encodeFunction = normalizedEncodings.byFunction(fieldAccess.encoding());
              var inputType =
                  Arrays.stream(fieldAccess.accessFunction().parameters()).findFirst().get().type();
              return new Quadruple<>(fieldAccess.identifier.lower(), (BitsType) inputType,
                  accessFunction,
                  encodeFunction);
            })
        .toList();

    ArrayList<TestCase> testCases = new ArrayList<>();
    List<Pair<String, String>> copyMappings = new ArrayList<>();
    ArrayList<DynamicTest> tests = new ArrayList<>();
    for (var entry : entries) {
      var arbitrary = uint(entry.second().bitWidth());
      arbitrary.sampleStream().limit(15).forEach(sample -> {
        var fileName = entry.first() + "_sample_" + sample + ".cpp";
        var filePath = path + "/inputs/" + fileName;
        var testCase = render(fileName,
            sample,
            entry.third(),
            entry.fourth());
        copyMappings.add(Pair.of(filePath, "/inputs/" + fileName));
        try {
          var fs = new FileWriter(filePath);
          fs.write(testCase.code);
          fs.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        testCases.add(testCase);
      });
    }

    runContainerAndCopyDirectoryIntoContainerAndCopyOutputBack(image,
        copyMappings,
        path + "/result.csv",
        "/work/output.csv");

    try (Stream<String> stream = Files.lines(Paths.get(path + "/result.csv"))) {
      stream.forEach(x -> {
        var split = x.split(",");
        var name = split[0];
        var statusCode = split[1];

        tests.add(DynamicTest.dynamicTest(name,
            () -> Assertions.assertEquals("0", statusCode)));
      });
    }

    return tests;
  }

  Arbitrary<Integer> uint(int bitWidth) {
    return Arbitraries.integers().greaterOrEqual(0).lessOrEqual((int) Math.pow(2, bitWidth) - 1);
  }

  record TestCase(String testName, String code) {

  }

  TestCase render(String testName,
                  int sample,
                  CppFunction accessFunction,
                  CppFunction encodingFunction) {
    var decodeFunction = new LcbGenericCodeGenerator().generateFunction(accessFunction);
    var encodeFunction = new LcbGenericCodeGenerator().generateFunction(encodingFunction);
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
        accessFunction.identifier.lower());

    return new TestCase(testName, cppCode);
  }
}
