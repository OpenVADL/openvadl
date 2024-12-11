package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.assembly.AssemblyInstructionPrinterCodeGenerator;
import vadl.lcb.passes.EncodeAssemblyImmediateAnnotation;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionImmediateLabelOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Identifier;
import vadl.viam.Specification;

/**
 * This file contains the implementation for emitting asm instructions.
 */
public class EmitInstPrinterCppFilePass extends LcbTemplateRenderingPass {

  public EmitInstPrinterCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetInstPrinter.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "InstPrinter.cpp";
  }

  record PrintableInstruction(String name, CppFunctionCode code) {

  }

  record InstructionWithImmediate(Identifier identifier,
                                  String rawEncoderMethod) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var machineRecords = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);
    var supported = machineRecords.stream().map(TableGenMachineInstruction::instruction)
        .collect(Collectors.toSet());
    var tableGenLookup = machineRecords.stream().collect(Collectors.toMap(
        TableGenMachineInstruction::instruction,
        x -> x));
    var printableInstructions = specification
        .isa().map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
        .filter(supported::contains)
        .map(instruction -> {
          var codeGen = new AssemblyInstructionPrinterCodeGenerator();
          var tableGenRecord =
              ensureNonNull(tableGenLookup.get(instruction), "tablegen record must exist");
          var result = codeGen.generateFunctionBody(instruction, tableGenRecord);
          return new PrintableInstruction(instruction.identifier.simpleName(), result);
        })
        .toList();

    var machineInstructionsWithImmediate = machineRecords
        .stream()
        .filter(
            x -> x.instruction().assembly().hasAnnotation(EncodeAssemblyImmediateAnnotation.class))
        .filter(x -> x.getInOperands().stream()
            .anyMatch(y -> y instanceof TableGenInstructionImmediateOperand))
        .map(x -> {
          var immOperand = x.getInOperands().stream()
              .filter(y -> y instanceof TableGenInstructionImmediateOperand)
              .map(y -> (TableGenInstructionImmediateOperand) y)
              .toList();

          ensure(immOperand.size() == 1, () -> Diagnostic.error(
              "Currently only machine instructions with one immediate are supported",
              x.instruction()
                  .sourceLocation()));

          return new InstructionWithImmediate(x.instruction().identifier,
              immOperand.get(0).immediateOperand().rawEncoderMethod());
        })
        .toList();

    var machineInstructionsWithLabel = machineRecords
        .stream()
        .filter(x -> x.getInOperands().stream()
            .anyMatch(y -> y instanceof TableGenInstructionImmediateLabelOperand))
        .filter(
            x -> x.instruction().assembly().hasAnnotation(EncodeAssemblyImmediateAnnotation.class))
        .map(x -> {
          var immOperand = x.getInOperands().stream()
              .filter(y -> y instanceof TableGenInstructionImmediateLabelOperand)
              .map(y -> (TableGenInstructionImmediateLabelOperand) y)
              .toList();

          ensure(immOperand.size() == 1, () -> Diagnostic.error(
              "Currently only machine instructions with one immediate are supported",
              x.instruction()
                  .sourceLocation()));

          return new InstructionWithImmediate(x.instruction().identifier,
              immOperand.get(0).immediateOperand().rawEncoderMethod());
        })
        .toList();
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "instructions", printableInstructions,
        "instructionWithEncodedImmediate", Stream.concat(machineInstructionsWithImmediate.stream(),
            machineInstructionsWithLabel.stream()).toList());
  }
}
