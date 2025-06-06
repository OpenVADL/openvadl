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

package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.common.GcbAccessOrPredicateFunctionCodeGenerator;
import vadl.cppCodeGen.common.GcbEncodingFunctionCodeGenerator;
import vadl.cppCodeGen.model.GcbCppFunctionBodyLess;
import vadl.cppCodeGen.model.GcbCppFunctionWithBody;
import vadl.lcb.passes.llvmLowering.immediates.GenerateTableGenImmediateRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.Type;
import vadl.utils.SourceLocation;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.PrintableInstruction;
import vadl.viam.Specification;

public class CreateFunctionsFromImmediatesPass extends Pass {
  public CreateFunctionsFromImmediatesPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("CreateFunctionsFromImmediatesPass");
  }

  public record Output(Map<TableGenImmediateRecord, GcbCppFunctionWithBody> encodings,
                       Map<TableGenImmediateRecord, GcbCppFunctionBodyLess> encodingsWrappers,
                       Map<TableGenImmediateRecord, GcbCppFunctionWithBody> decodings,
                       Map<TableGenImmediateRecord, GcbCppFunctionBodyLess> decodingWrappers,
                       Map<TableGenImmediateRecord, GcbCppFunctionWithBody> predicates) {

  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var abi = viam.abi().orElseThrow();
    var tableGenMachineInstructions = ((List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class))
        .stream()
        .collect(Collectors.toMap(x -> (PrintableInstruction) x.instruction(),
            x -> (TableGenInstruction) x));
    var tableGenPseudoInstructions = ((List<TableGenPseudoInstruction>) passResults.lastResultOf(
        GenerateTableGenPseudoInstructionRecordPass.class))
        .stream()
        .collect(Collectors.toMap(x -> (PrintableInstruction) x.pseudoInstruction(),
            x -> (TableGenInstruction) x));

    var tableGenInstructions =
        Stream.concat(tableGenMachineInstructions.entrySet().stream(),
                tableGenPseudoInstructions.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    /*
      What's the difference between `decodings` and `decodingWrappers`?

      The decodings are pure extraction function like what you would expect from your field access
      function.

      ``` (not entirely correct)
        static int64_t RV3264Base_ADDI_decode_wrapper() {
           return VADL_sextract(param, 12);
      ```

      The wrapper is just an abstraction for LLVM. Note that the wrapper method calls the underlying
      decoder function.

      ```
        DecodeStatus RV3264Base_ADDI_decode_wrapper(
          MCInst &Inst,
          uint64_t Imm,
          int64_t Address,
          const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_ADDI_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
      }
      ```
     */

    var decodings = new IdentityHashMap<TableGenImmediateRecord, GcbCppFunctionWithBody>();
    var decodingWrappers = new IdentityHashMap<TableGenImmediateRecord, GcbCppFunctionBodyLess>();

    // Same applies to encoding and encodingWrappers
    var encodings = new IdentityHashMap<TableGenImmediateRecord, GcbCppFunctionWithBody>();
    var encodingWrappers = new IdentityHashMap<TableGenImmediateRecord, GcbCppFunctionBodyLess>();

    var predicates = new IdentityHashMap<TableGenImmediateRecord, GcbCppFunctionWithBody>();
    var immediates = (List<TableGenImmediateRecord>) passResults.lastResultOf(
        GenerateTableGenImmediateRecordPass.class);

    for (var immediate : immediates) {
      // We do not need to encode pseudo instructions.
      if (!(immediate.instructionRef() instanceof Instruction)) {
        continue;
      }

      var tableGenInstruction =
          Objects.requireNonNull(tableGenInstructions.get(immediate.instructionRef()));
      var stackPointer = abi.stackPointer();
      var stackPointerType =
          Objects.requireNonNull(stackPointer.registerFile().resultType().fittingCppType());
      encodingWrappers.put(immediate, encodingWrappers(immediate));
      encodings.put(immediate, encoding(tableGenInstruction, immediate));
      decodingWrappers.put(immediate, decodingWrapper(immediate));
      decodings.put(immediate, decoding(stackPointerType, immediate));
      predicates.put(immediate, predicate(stackPointerType, immediate));
    }

    return new Output(encodings, encodingWrappers, decodings, decodingWrappers, predicates);
  }

  @Nonnull
  private GcbCppFunctionWithBody encoding(TableGenInstruction tableGenInstruction,
                                          TableGenImmediateRecord immediate) {
    var encodingBodyLessFunction = new GcbCppFunctionBodyLess(
        immediate.rawEncoderMethod(),
        new Parameter[] {},
        CppTypeMap.upcast(
            Objects.requireNonNull(immediate.fieldAccessRef().encoding()).targetField().type()),
        immediate.fieldAccessRef().encoding().behavior(),
        immediate.fieldAccessRef());
    return new GcbCppFunctionWithBody(encodingBodyLessFunction,
        new GcbEncodingFunctionCodeGenerator(
            tableGenInstruction,
            encodingBodyLessFunction,
            immediate.fieldAccessRef(),
            immediate.rawEncoderMethod().lower()).genFunctionDefinition());
  }

  @Nonnull
  private GcbCppFunctionBodyLess encodingWrappers(TableGenImmediateRecord immediate) {
    return new GcbCppFunctionBodyLess(
        immediate.encoderMethod(),
        new Parameter[] {},
        CppTypeMap.upcast(
            Objects.requireNonNull(immediate.fieldAccessRef().encoding()).targetField().type()),
        immediate.fieldAccessRef().encoding().behavior(),
        immediate.fieldAccessRef());
  }

  @Nonnull
  private GcbCppFunctionBodyLess decodingWrapper(TableGenImmediateRecord immediate) {
    var bodyLessFunction = new GcbCppFunctionBodyLess(
        immediate.decoderMethod(),
        new Parameter[] {},
        CppTypeMap.upcast(immediate.fieldAccessRef().accessFunction().returnType()),
        immediate.fieldAccessRef().accessFunction().behavior(),
        immediate.fieldAccessRef());
    return bodyLessFunction;
  }

  @Nonnull
  private GcbCppFunctionWithBody decoding(Type stackPointerType,
                                          TableGenImmediateRecord immediate) {
    var bodyLessFunction = new GcbCppFunctionBodyLess(
        immediate.rawDecoderMethod(),
        // We use the size of the stack pointer to decide what the parameter's type is.
        new Parameter[] {
            new Parameter(new Identifier("param", SourceLocation.INVALID_SOURCE_LOCATION),
                stackPointerType)},
        CppTypeMap.upcast(immediate.fieldAccessRef().accessFunction().returnType()),
        immediate.fieldAccessRef().accessFunction().behavior(),
        immediate.fieldAccessRef());
    return new GcbCppFunctionWithBody(bodyLessFunction,
        generateCode(immediate.rawDecoderMethod(),
            immediate.fieldAccessRef(),
            bodyLessFunction));
  }

  @Nonnull
  private GcbCppFunctionWithBody predicate(Type stackPointerType,
                                           TableGenImmediateRecord immediate) {
    var bodyLessFunction = new GcbCppFunctionBodyLess(
        immediate.predicateMethod(),
        new Parameter[] {
            new Parameter(new Identifier("param", SourceLocation.INVALID_SOURCE_LOCATION),
                stackPointerType)},
        Type.bool(),
        immediate.fieldAccessRef().predicate().behavior(),
        immediate.fieldAccessRef());
    return new GcbCppFunctionWithBody(bodyLessFunction,
        generateCode(immediate.predicateMethod(),
            immediate.fieldAccessRef(),
            bodyLessFunction));
  }

  private String generateCode(Identifier identifier,
                              Format.FieldAccess fieldAccess,
                              GcbCppFunctionBodyLess header) {
    return new GcbAccessOrPredicateFunctionCodeGenerator(header,
        fieldAccess,
        identifier.lower()).genFunctionDefinition();
  }
}
