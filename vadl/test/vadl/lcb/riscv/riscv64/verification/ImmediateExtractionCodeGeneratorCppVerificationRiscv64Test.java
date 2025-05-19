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

package vadl.lcb.riscv.riscv64.verification;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.shaded.com.google.common.collect.Streams;
import vadl.cppCodeGen.common.GcbAccessOrExtractionFunctionCodeGenerator;
import vadl.cppCodeGen.model.GcbImmediateExtractionCppFunction;
import vadl.gcb.passes.typeNormalization.CreateGcbFieldAccessCppFunctionFromExtractionFunctionPass;
import vadl.gcb.passes.typeNormalization.CreateGcbFieldAccessFunctionFromPredicateFunctionPass;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.Pair;
import vadl.utils.VadlFileUtils;
import vadl.viam.Format;

public class ImmediateExtractionCodeGeneratorCppVerificationRiscv64Test extends AbstractLcbTest {
  @TestFactory
  Collection<DynamicTest> instructions() throws IOException, DuplicatedPassKeyException {
    var configuration = getConfiguration(false);
    var temporaryPasses = List.of(
        new TemporaryTestPassInjection(
            CreateGcbFieldAccessFunctionFromPredicateFunctionPass.class,
            new CreateGcbFieldAccessCppFunctionFromExtractionFunctionPass(configuration)
        )
    );
    var testSetup = runLcb(configuration,
        "sys/risc-v/rv64im.vadl", new PassKey(GenerateLinkerComponentsPass.class.getName()),
        temporaryPasses);

    // Move files into Docker Context
    {
      VadlFileUtils.createDirectories(configuration, "encoding", "inputs");
      VadlFileUtils.copyDirectory(
          Path.of(
              "test/resources/images/encodingCodeGeneratorCppVerification/"),
          Path.of(configuration.outputPath() + "/encoding/"));
    }

    var image = new ImageFromDockerfile()
        .withDockerfile(Paths.get(configuration.outputPath() + "/encoding/Dockerfile"));

    return generateInputs(testSetup, image, configuration.outputPath());
  }

  /**
   * The idea of the test is that we have a {@code instruction} and we want to extract
   * the immediate {@code x} from it. This immediate {@code x} has to be equal to the given
   * parameter {@code imm}.
   * However, note that {@code instruction} has not yet the immediate encoded in it. This is also
   * done in the container test with `set_bits` function.
   */
  private Collection<DynamicTest> generateInputs(TestSetup setup,
                                                 ImageFromDockerfile image,
                                                 Path path) throws IOException {
    List<Pair<String, String>> copyMappings = new ArrayList<>();
    setup.specification()
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
                var fileName = fieldAccess.identifier.lower() + ".cpp";
                var filePath = path + "/inputs/" + fileName;
                var code = renderCode(fieldAccess,
                    pair.left(),
                    pair.right(),
                    fieldBitSize);

                copyMappings.add(Pair.of(filePath, "/inputs/" + fileName));
                try {
                  var fs = new FileWriter(filePath);
                  fs.write(code);
                  fs.close();
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
        });

    runContainerAndCopyDirectoryIntoContainerAndCopyOutputBack(image,
        copyMappings,
        path + "/result.csv",
        "/work/output.csv");

    return assertStatusCodes(path + "/result.csv");
  }

  private String renderCode(
      Format.FieldAccess fieldAccess,
      Long imm,
      Long instruction,
      int fieldBitSize) {
    var extractFunction =
        new GcbImmediateExtractionCppFunction(fieldAccess.fieldRef().extractFunction());
    var extractionFunctionCodeGenerator =
        new GcbAccessOrExtractionFunctionCodeGenerator(extractFunction,
            fieldAccess, extractFunction.identifier.lower());
    var extractionFunctionCode = extractionFunctionCodeGenerator.genFunctionDefinition();
    var extractionFunctionName = extractionFunctionCodeGenerator.genFunctionName();

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
        extractionFunctionCode,
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

    return cppCode;
  }

  Arbitrary<Long> uint(int bitWidth) {
    return Arbitraries.longs().greaterOrEqual(0).lessOrEqual((long) Math.pow(2, bitWidth) - 1);
  }
}
