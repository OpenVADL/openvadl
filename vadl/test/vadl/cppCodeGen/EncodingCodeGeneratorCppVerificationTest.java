// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.cppCodeGen;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.configuration.GcbConfiguration;
import vadl.cppCodeGen.common.GcbAccessOrExtractionFunctionCodeGenerator;
import vadl.cppCodeGen.model.GcbFieldAccessCppFunction;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.gcb.passes.typeNormalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.typeNormalization.CppTypeNormalizationForEncodingsPass;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.types.BitsType;
import vadl.utils.Pair;
import vadl.utils.Quadruple;
import vadl.utils.VadlFileUtils;

public class EncodingCodeGeneratorCppVerificationTest extends AbstractCppCodeGenTest {
  @TestFactory
  Collection<DynamicTest> instructions() throws IOException, DuplicatedPassKeyException {
    var configuration = new GcbConfiguration(getConfiguration(false));
    var setup = runGcbAndCppCodeGen(configuration, "sys/risc-v/rv64im.vadl");

    // Move files into Docker Context
    {
      VadlFileUtils.createDirectories(configuration, "encoding", "inputs");
      VadlFileUtils.copyFile(Path.of(
              "main/resources/templates/common/vadl-builtins.h"
          ),
          Path.of(configuration.outputPath() + "/vadl-builtins.h")
      );
      VadlFileUtils.copyDirectory(
          Path.of(
              "test/resources/images/encodingCodeGeneratorCppVerification/"),
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

    List<Pair<String, String>> copyMappings = new ArrayList<>();
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
          fs.write(testCase.code());
          fs.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }

    // Add vadl-builtin
    copyMappings.add(new Pair<>(path.toString() + "/vadl-builtins.h",
        "/vadl-builtins.h"));

    runContainerAndCopyDirectoryIntoContainerAndCopyOutputBack(image,
        copyMappings,
        path + "/result.csv",
        "/work/output.csv");

    return assertStatusCodes(path + "/result.csv");
  }

  Arbitrary<Integer> uint(int bitWidth) {
    return Arbitraries.integers().greaterOrEqual(0).lessOrEqual((int) Math.pow(2, bitWidth) - 1);
  }

  TestCase render(String testName,
                  int sample,
                  GcbFieldAccessCppFunction accessFunction,
                  GcbFieldAccessCppFunction encodingFunction) {
    var decodeFunctionGenerator =
        new GcbAccessOrExtractionFunctionCodeGenerator(accessFunction, accessFunction.fieldAccess(),
            accessFunction.identifier.lower(),
            accessFunction.fieldAccess().fieldRef().simpleName());
    var encodeFunctionGenerator =
        new GcbAccessOrExtractionFunctionCodeGenerator(encodingFunction,
            encodingFunction.fieldAccess(),
            encodingFunction.identifier.lower(),
            encodingFunction.fieldAccess().fieldRef().simpleName());

    var decodeFunction = decodeFunctionGenerator.genFunctionDefinition();
    var encodeFunction = encodeFunctionGenerator.genFunctionDefinition();
    String expectedReturnType =
        CppTypeMap.getCppTypeNameByVadlType(CppTypeMap.upcast(encodingFunction.returnType()));

    String cppCode = String.format("""
            #include <cstdint>
            #include <iostream>
            #include <bitset>
            #include <vector>
            
            // Imported by manual copy mapping
            #include "/vadl-builtins.h"
            
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
        decodeFunction,
        encodeFunction,
        expectedReturnType,
        sample,
        encodingFunction.identifier.lower(),
        accessFunction.identifier.lower());

    return new TestCase(testName, cppCode);
  }
}
