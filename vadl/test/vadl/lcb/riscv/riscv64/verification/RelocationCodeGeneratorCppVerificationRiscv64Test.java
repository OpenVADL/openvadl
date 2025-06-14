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
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.common.UpdateFieldRelocationFunctionCodeGenerator;
import vadl.cppCodeGen.common.ValueRelocationFunctionCodeGenerator;
import vadl.cppCodeGen.model.GcbImmediateExtractionCppFunction;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.relocation.model.HasRelocationComputationAndUpdate;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.passes.llvmLowering.CreateFunctionsFromImmediatesPass;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.types.BitsType;
import vadl.utils.Pair;
import vadl.utils.VadlFileUtils;
import vadl.viam.Format;
import vadl.viam.Parameter;
import vadl.viam.Specification;

public class RelocationCodeGeneratorCppVerificationRiscv64Test extends AbstractLcbTest {
  @TestFactory
  Collection<DynamicTest> instructions() throws IOException, DuplicatedPassKeyException {
    var configuration = getConfiguration(false);
    var temporaryPasses = List.of(
        new TemporaryTestPassInjection(
            CreateFunctionsFromImmediatesPass.class,
            new CreateFunctionsFromImmediatesPass(configuration)
        )
    );
    var testSetup = runLcb(configuration,
        "sys/risc-v/rv64im.vadl", new PassKey(GenerateLinkerComponentsPass.class.getName()),
        temporaryPasses);

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

    return generateInputs(testSetup, image, configuration.outputPath(), testSetup.specification());
  }

  private Collection<DynamicTest> generateInputs(TestSetup testSetup,
                                                 ImageFromDockerfile image,
                                                 Path path,
                                                 Specification specification) throws IOException {
    var output = (GenerateLinkerComponentsPass.Output) testSetup.passManager().getPassResults()
        .lastResultOf(GenerateLinkerComponentsPass.class);
    var elfRelocations = output.elfRelocations();
    var immediateDetection =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) testSetup.passManager()
            .getPassResults()
            .lastResultOf(IdentifyFieldUsagePass.class);

    var instructions =
        specification.isa().stream().flatMap(x -> x.ownInstructions().stream()).toList();
    List<Pair<String, String>> copyMappings = new ArrayList<>();

    for (var instruction : instructions) {
      var relocation =
          elfRelocations.stream().filter(x -> x.format() == instruction.format()).findFirst();

      relocation.ifPresent(
          hasRelocationComputationAndUpdate -> immediateDetection.getImmediates(instruction)
              .forEach(immField -> {
                var params = hasRelocationComputationAndUpdate.fieldUpdateFunction().parameters();
                // The first parameter is hardcoded to be the instruction word.
                // The second parameter is the updated value.
                var arbitraryInstructionWord =
                    getArbitrary(params[0]);
                var arbitraryImmediateValue = getArbitrary(immField);

                var limit = 3;
                arbitraryInstructionWord.sampleStream().limit(limit)
                    .forEach(
                        instructionWordSample -> arbitraryImmediateValue.sampleStream().limit(limit)
                            .forEach(immediateValue -> {
                              var fileName =
                                  immField.identifier.lower() + "_instr_" + instructionWordSample
                                      + "_imm_"
                                      + immediateValue + ".cpp";
                              var filePath = path + "/inputs/" + fileName;
                              var code = render(immField,
                                  instructionWordSample,
                                  immediateValue,
                                  hasRelocationComputationAndUpdate);
                              copyMappings.add(Pair.of(filePath, "/inputs/" + fileName));
                              try {
                                var fs = new FileWriter(filePath);
                                fs.write(code);
                                fs.close();
                              } catch (IOException e) {
                                throw new RuntimeException(e);
                              }
                            }));
              }));
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

  private Arbitrary<Long> getArbitrary(Parameter parameter) {
    var type = (BitsType) parameter.type();
    return uint(type.bitWidth() - 1);
  }

  private Arbitrary<Long> getArbitrary(Format.Field field) {
    return uint(field.size() - 1);
  }

  Arbitrary<Long> uint(int bitWidth) {
    return Arbitraries.longs().greaterOrEqual(0).lessOrEqual((long) Math.pow(2, bitWidth));
  }

  private String render(
      Format.Field immField,
      long instructionWord,
      long updatedValue,
      HasRelocationComputationAndUpdate relocation) {
    // How do we test the relocation?
    // We have an extraction function for the immediate.
    // And we have an updating function.
    //
    // So first, we generate the cpp code for the extraction function.
    // Then we generate the cpp code for updating function.
    // Finally, we update the instructionWord with the given updatedValue and
    // apply the extraction function.
    // The result of the extraction function must be the updatedValue.

    var normalisedImmediateExtractionFunction =
        new GcbImmediateExtractionCppFunction(immField.extractFunction());

    var extractionFunctionCodeGenerator =
        new ValueRelocationFunctionCodeGenerator(relocation.relocation(),
            normalisedImmediateExtractionFunction);
    var relocationOverrideFunctionCodeGenerator =
        new UpdateFieldRelocationFunctionCodeGenerator(relocation.fieldUpdateFunction());

    var extractionFunctionName = extractionFunctionCodeGenerator.genFunctionName();
    var relocationFunctionName = relocationOverrideFunctionCodeGenerator.genFunctionName();

    var extractionCppFunction =
        extractionFunctionCodeGenerator.genFunctionDefinition();
    var relocationOverrideCppFunction =
        relocationOverrideFunctionCodeGenerator.genFunctionDefinition();

    String cppCode = String.format("""
            #include <cstdint>
            #include <iostream>
            #include <bitset>
            #include <vector>
            #include <tuple>
            
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
            
            // Extraction Function
            %s
            
            // Relocation Function
            %s
            
            int main() {
              %s expected = %s;
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
        extractionCppFunction,
        relocationOverrideCppFunction,
        CppTypeMap.getCppTypeNameByVadlType(
            normalisedImmediateExtractionFunction.returnType().asDataType().fittingCppType()),
        updatedValue,
        extractionFunctionName,
        relocationFunctionName,
        instructionWord,
        updatedValue);

    return cppCode;
  }
}
