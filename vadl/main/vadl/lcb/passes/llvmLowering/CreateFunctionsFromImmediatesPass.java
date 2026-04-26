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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.common.GcbAccessFunctionCodeGenerator;
import vadl.cppCodeGen.common.GcbEncodingFunctionCodeGenerator;
import vadl.cppCodeGen.common.PredicateFunctionCodeGenerator;
import vadl.cppCodeGen.model.GcbCppAccessFunction;
import vadl.cppCodeGen.model.GcbCppEncodeFunction;
import vadl.cppCodeGen.model.GcbCppEncodingWrapperFunction;
import vadl.cppCodeGen.model.GcbCppFunctionBodyLess;
import vadl.cppCodeGen.model.GcbCppFunctionWithBody;
import vadl.error.Diagnostic;
import vadl.gcb.passes.operands.model.GcbInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.immediates.GenerateTableGenImmediateRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.ReferencesImmediateOperand;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.Type;
import vadl.viam.Format;
import vadl.viam.Format.FieldAccess;
import vadl.viam.Format.FieldEncoding;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.PrintableInstruction;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.passes.functionInliner.Inliner;

/**
 * A pass that creates various functions which are required for the immediates in LLVM.
 */
public class CreateFunctionsFromImmediatesPass extends Pass {
  public CreateFunctionsFromImmediatesPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("CreateFunctionsFromImmediatesPass");
  }

  /**
   * Output of the pass.
   */
  public record Output(Map<Instruction, List<GcbCppEncodeFunction>> encodings,
                       Map<Instruction, GcbCppEncodingWrapperFunction> encodingsWrappers,
                       Map<TableGenImmediateRecord, GcbCppAccessFunction> decodings,
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

      ```
        static int64_t RV3264Base_ADDI_decode_wrapper(int param) {
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

    var decodings = new IdentityHashMap<TableGenImmediateRecord, GcbCppAccessFunction>();
    var decodingWrappers = new IdentityHashMap<TableGenImmediateRecord, GcbCppFunctionBodyLess>();

    // Same applies to encoding and encodingWrappers
    var encodings = new IdentityHashMap<Instruction, List<GcbCppEncodeFunction>>();
    var encodingWrappers = new IdentityHashMap<Instruction, GcbCppEncodingWrapperFunction>();

    var predicateMethods = new HashMap<Identifier, GcbCppFunctionWithBody>();
    var predicates = new IdentityHashMap<TableGenImmediateRecord, GcbCppFunctionWithBody>();
    var immediates = (List<TableGenImmediateRecord>) passResults.lastResultOf(
        GenerateTableGenImmediateRecordPass.class);

    for (var immediate : immediates) {
      // We do not need to encode pseudo instructions.
      if (!(immediate.instructionRef() instanceof Instruction)) {
        continue;
      }

      var stackPointer = abi.stackPointer();
      var stackPointerType =
          Objects.requireNonNull(stackPointer.registerFile().resultType().fittingCppType());

      decodingWrappers.put(immediate, decodingWrapper(immediate));
      decodings.put(immediate, decoding(immediate));

      var predicateMethodId = immediate.predicateMethod();
      var predicateMethod = Optional.ofNullable(predicateMethods.get(predicateMethodId))
          .orElseGet(() -> predicate(stackPointerType, immediate));
      predicateMethods.putIfAbsent(predicateMethodId, predicateMethod);

      predicates.put(immediate, predicateMethod);
    }

    for (var pair : tableGenMachineInstructions.entrySet()) {
      var instruction = (Instruction) pair.getKey();
      var tableGenInstruction = pair.getValue();

      var fieldAccesses = tableGenInstruction.gcbImmediateInputOperands().stream()
          .map(GcbInstructionImmediateOperand::fieldAccess)
          .collect(Collectors.toSet());
      var fieldEncodings = instruction.format().fieldEncodingsOf(fieldAccesses);
      var encodingsFunctions = encoding(instruction, tableGenInstruction, fieldEncodings);
      encodings.put(instruction, encodingsFunctions);

      encodingWrappers.put(instruction,
          encodingWrappers(instruction, tableGenInstructions, encodingsFunctions));
    }

    return new Output(encodings, encodingWrappers, decodings, decodingWrappers, predicates);
  }

  @Nonnull
  private List<GcbCppEncodeFunction> encoding(
      Instruction instruction,
      TableGenInstruction tableGenInstruction,
      List<FieldEncoding> encodings) {
    return encodings.stream().map(encoding -> {
      // Why do we need to handle the operands?
      // The problem is that it might happen that we are given a MCInst
      // and the encoding function cannot handle it because it needs to also used by the parser
      // which does not have a MCInst.
      // Our solution to that is that each operand from the instruction is a parameter. It's also
      // in the same order. Then the encoding wrapper function can extract the operands from MCInst,
      // but the parser can also use this function regularly because they are raw types.

      // We are passing in all the field access functions into the function.
      // The function will only use the parameters (field access functions) that it needs.
      List<FieldAccessParameter> parameters =
          createParametersForEncodingFunctionFromInputOperands(tableGenInstruction);

      var identifier = instruction.identifier.append(encoding.targetField().simpleName());
      var encodingBodyLessFunction = new GcbCppFunctionBodyLess(
          identifier,
          parameters.toArray(Parameter[]::new),
          CppTypeMap.upcast(encoding.targetField().type()),
          encoding.behavior());
      Inliner.inlineFuncs(encoding.behavior(), Inliner.InliningMode.WithoutRelocations);
      return new GcbCppEncodeFunction(encodingBodyLessFunction,
          encoding.targetField(),
          new GcbEncodingFunctionCodeGenerator(
              encodingBodyLessFunction).genFunctionDefinition());
    }).toList();
  }

  /**
   * A {@link FieldEncoding} may have multiple {@link FieldAccess}. However, the VIAM is not
   * forcing a particular order which is not problematic. Therefore, we have to define our own
   * order. Our solution is that a {@link FieldEncoding} uses all the {@link FieldAccess} for
   * an {@link Instruction}. Since all the field access functions' parameters are named and the
   * function refers to the name of the field access function, the encoding function
   * will only use the parameters it requires. We define the order of the parameters to the order
   * in {@link TableGenInstruction#getInOperands()}.
   */
  @Nonnull
  public static List<FieldAccessParameter> createParametersForEncodingFunctionFromInputOperands(
      TableGenInstruction tableGenInstruction) {
    var inputOperands = tableGenInstruction.getInOperands()
        .stream()
        .filter(operand -> operand instanceof ReferencesImmediateOperand)
        .map(operand -> (ReferencesImmediateOperand) operand)
        .toList();

    return createParametersForEncodingFunction(inputOperands);
  }

  @Nonnull
  private static List<FieldAccessParameter> createParametersForEncodingFunction(
      List<ReferencesImmediateOperand> inputOperands) {
    var parameters = new ArrayList<FieldAccessParameter>();

    for (var i = 0; i < inputOperands.size(); i++) {
      var operand = inputOperands.get(i);
      var param = new FieldAccessParameter(
          new Identifier(operand.immediateOperand().fieldAccessRef().identifier.simpleName(),
              operand.immediateOperand().fieldAccessRef().location()),
          operand.immediateOperand().rawType(),
          operand,
          i);
      parameters.add(param);
    }

    return parameters;
  }

  /**
   * Creates parameters from the given {@link FieldAccess} list. Note,
   * that the orders matters!
   */
  @Nonnull
  private static List<Parameter> createParametersForAccessFunction(
      List<Format.Field> fields) {
    var parameters = new ArrayList<Parameter>();

    for (int i = 0; i < fields.size(); i++) {
      var field = fields.get(i);
      var param = new Parameter(
          new Identifier(field.identifier.simpleName(),
              field.location()),
          CppTypeMap.upcast(field.type()),
          i);
      parameters.add(param);
    }

    return parameters;
  }


  @Nonnull
  private GcbCppEncodingWrapperFunction encodingWrappers(
      Instruction instruction,
      Map<PrintableInstruction, TableGenInstruction> tableGenInstructions,
      List<GcbCppEncodeFunction> encodingsFunctions) {
    var encoderMethod = TableGenImmediateRecord.createEncoderMethod(instruction);
    var tableGenInstruction = Objects.requireNonNull(tableGenInstructions.get(instruction));
    var operands = tableGenInstruction.immediateInputOperands();
    var fieldAccesses =
        operands.stream().map(y -> y.immediateOperand().fieldAccessRef()).collect(
            Collectors.toSet());
    var fieldEncodings = instruction.format().fieldEncodingsOf(fieldAccesses);

    return new GcbCppEncodingWrapperFunction(
        encoderMethod,
        new Parameter[] {},
        Type.bits(64),
        instruction,
        fieldAccesses,
        fieldEncodings,
        encodingsFunctions);
  }

  @Nonnull
  private GcbCppFunctionBodyLess decodingWrapper(TableGenImmediateRecord immediate) {
    return new GcbCppFunctionBodyLess(
        immediate.decoderMethod(),
        new Parameter[] {},
        CppTypeMap.upcast(immediate.fieldAccessRef().accessFunction().returnType()),
        immediate.fieldAccessRef().accessFunction().behavior());
  }

  @Nonnull
  private GcbCppAccessFunction decoding(TableGenImmediateRecord immediate) {
    var parameters = createParametersForAccessFunction(immediate.fieldAccessRef().fieldRefs());
    var bodyLessFunction = new GcbCppFunctionBodyLess(
        immediate.rawDecoderMethod(),
        // We use the size of the stack pointer to decide what the parameter's type is.
        parameters.toArray(Parameter[]::new),
        CppTypeMap.upcast(immediate.fieldAccessRef().accessFunction().returnType()),
        immediate.fieldAccessRef().accessFunction().behavior());
    var codeGen = new GcbAccessFunctionCodeGenerator(bodyLessFunction,
        immediate.fieldAccessRef(),
        immediate.rawDecoderMethod().lower());

    return new GcbCppAccessFunction(bodyLessFunction,
        immediate.fieldAccessRef(),
        codeGen.genFunctionDefinition());
  }

  @Nonnull
  private GcbCppFunctionWithBody predicate(Type stackPointerType,
                                           TableGenImmediateRecord immediate) {
    ViamError.ensureNonNull(immediate.fieldAccessRef().predicate(),
        () -> Diagnostic.error("Predicate must not be null",
            immediate.fieldAccessRef().location()));
    var bodyLessFunction = new GcbCppFunctionBodyLess(
        immediate.predicateMethod(),
        new Parameter[] {
            new Parameter(new Identifier(immediate.fieldAccessRef().simpleName(),
                immediate.fieldAccessRef().location()),
                stackPointerType,
                0)},
        Type.bool(),
        immediate.fieldAccessRef().predicate().behavior());
    var codeGen = new PredicateFunctionCodeGenerator(bodyLessFunction,
        immediate.fieldAccessRef(),
        immediate.predicateMethod().lower());

    return new GcbCppFunctionWithBody(bodyLessFunction,
        codeGen.genFunctionDefinition());
  }

  /**
   * Extends {@link Parameter} to additionally store the {@link ReferencesImmediateOperand}.
   */
  public static class FieldAccessParameter extends Parameter {

    private final ReferencesImmediateOperand operand;

    public FieldAccessParameter(Identifier identifier,
                                Type type,
                                ReferencesImmediateOperand operand,
                                int index) {
      super(identifier, type, index);
      this.operand = operand;
    }

    public ReferencesImmediateOperand operand() {
      return operand;
    }
  }
}
