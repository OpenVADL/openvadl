package vadl.viam;

import vadl.viam.asm.AsmDirectiveMapping;
import vadl.viam.asm.AsmModifier;
import vadl.viam.asm.rules.AsmBuiltinRule;
import vadl.viam.asm.rules.AsmNonTerminalRule;
import vadl.viam.asm.rules.AsmTerminalRule;

/**
 * DefinitionVisitor is an interface that defines the visit methods for all types of
 * definitions in a VADL specification.
 */
public interface DefinitionVisitor {

  void visit(Specification specification);

  void visit(InstructionSetArchitecture instructionSetArchitecture);

  void visit(Instruction instruction);

  void visit(Assembly assembly);

  void visit(Encoding encoding);

  void visit(Encoding.Field encodingField);

  void visit(Format format);

  void visit(Format.Field formatField);

  void visit(Format.FieldAccess formatFieldAccess);

  void visit(Function function);

  void visit(Relocation relocation);

  void visit(Parameter parameter);

  void visit(PseudoInstruction pseudoInstruction);

  void visit(Register register);

  void visit(RegisterFile registerFile);

  void visit(Memory memory);

  void visit(Counter counter);

  void visit(Abi abi);

  void visit(MicroProcessor microProcessor);

  void visit(MicroArchitecture microArchitecture);

  void visit(Logic logic);

  void visit(Signal signal);

  void visit(Stage stage);

  void visit(StageOutput stageOutput);

  void visit(AssemblyDescription assemblyDescription);

  void visit(AsmDirectiveMapping directive);

  void visit(AsmModifier modifier);

  void visit(AsmBuiltinRule builtinRule);

  void visit(AsmTerminalRule terminalRule);

  void visit(AsmNonTerminalRule nonTerminalRule);

  /**
   * DefinitionVisitor.Recursive is an abstract class that implements the DefinitionVisitor
   * interface.
   * It provides default implementations for the visit methods for all types of definitions in a
   * VADL specification, allowing for recursive traversal of the definition hierarchy.
   */
  class Recursive implements DefinitionVisitor {

    public void beforeTraversal(Definition definition) {
    }

    public void afterTraversal(Definition definition) {
    }

    @Override
    public void visit(Specification specification) {
      beforeTraversal(specification);
      specification
          .definitions()
          .forEach(e -> e.accept(this));
      afterTraversal(specification);
    }

    @Override
    public void visit(InstructionSetArchitecture instructionSetArchitecture) {
      beforeTraversal(instructionSetArchitecture);
      // do not visit PC as it is included as register in registers()
      instructionSetArchitecture
          .ownFormats()
          .forEach(e -> e.accept(this));
      instructionSetArchitecture
          .ownFunctions()
          .forEach(e -> e.accept(this));
      instructionSetArchitecture
          .ownRelocations()
          .forEach(e -> e.accept(this));
      instructionSetArchitecture
          .ownRegisters()
          .forEach(e -> e.accept(this));
      instructionSetArchitecture
          .ownRegisterFiles()
          .forEach(e -> e.accept(this));
      instructionSetArchitecture
          .ownMemories()
          .forEach(e -> e.accept(this));
      instructionSetArchitecture
          .ownInstructions()
          .forEach(e -> e.accept(this));
      instructionSetArchitecture
          .ownPseudoInstructions()
          .forEach(e -> e.accept(this));
      var pc = instructionSetArchitecture.pc();
      if (pc != null) {
        pc.accept(this);
      }
      afterTraversal(instructionSetArchitecture);
    }

    @Override
    public void visit(Instruction instruction) {
      beforeTraversal(instruction);
      instruction
          .assembly()
          .accept(this);
      instruction
          .encoding()
          .accept(this);
      afterTraversal(instruction);
    }

    @Override
    public void visit(Assembly assembly) {
      beforeTraversal(assembly);
      assembly
          .function()
          .accept(this);
      afterTraversal(assembly);
    }

    @Override
    public void visit(Encoding encoding) {
      beforeTraversal(encoding);
      for (Encoding.Field field : encoding.fieldEncodings()) {
        field.accept(this);
      }
      afterTraversal(encoding);
    }

    @Override
    public void visit(Encoding.Field encodingField) {
      beforeTraversal(encodingField);
      afterTraversal(encodingField);
    }

    @Override
    public void visit(Format format) {
      beforeTraversal(format);
      for (var field : format.fields()) {
        field.accept(this);
      }
      for (var fieldAccess : format.fieldAccesses()) {
        fieldAccess.accept(this);
      }
      afterTraversal(format);
    }

    @Override
    public void visit(Format.Field formatField) {
      beforeTraversal(formatField);
      afterTraversal(formatField);
    }

    @Override
    public void visit(Format.FieldAccess formatFieldAccess) {
      beforeTraversal(formatFieldAccess);
      formatFieldAccess.accessFunction().accept(this);
      if (formatFieldAccess.encoding() != null) {
        formatFieldAccess.encoding().accept(this);
      }
      formatFieldAccess.predicate().accept(this);
      afterTraversal(formatFieldAccess);
    }

    @Override
    public void visit(Function function) {
      beforeTraversal(function);
      for (var param : function.parameters()) {
        param.accept(this);
      }
      afterTraversal(function);
    }

    @Override
    public void visit(Parameter parameter) {
      beforeTraversal(parameter);
      afterTraversal(parameter);
    }

    @Override
    public void visit(PseudoInstruction pseudoInstruction) {
      beforeTraversal(pseudoInstruction);
      for (var param : pseudoInstruction.parameters()) {
        param.accept(this);
      }
      pseudoInstruction.assembly()
          .accept(this);
      afterTraversal(pseudoInstruction);
    }

    @Override
    public void visit(Register register) {
      beforeTraversal(register);
      // Do not travers sub registers, as they are included in upper call to registers
      afterTraversal(register);
    }

    @Override
    public void visit(RegisterFile registerFile) {
      beforeTraversal(registerFile);
      afterTraversal(registerFile);
    }

    @Override
    public void visit(Memory memory) {
      beforeTraversal(memory);
      afterTraversal(memory);
    }

    @Override
    public void visit(Relocation relocation) {
      beforeTraversal(relocation);
      for (var param : relocation.parameters()) {
        param.accept(this);
      }
      afterTraversal(relocation);
    }

    @Override
    public void visit(Counter counter) {
      beforeTraversal(counter);
      // no visit of register/register file as they are not
      // owned by the counter
      afterTraversal(counter);
    }

    @Override
    public void visit(Abi abi) {
      beforeTraversal(abi);
      afterTraversal(abi);
    }

    @Override
    public void visit(MicroProcessor microProcessor) {
      beforeTraversal(microProcessor);
      var start = microProcessor.startNullable();
      if (start != null) {
        start.accept(this);
      }
      var stop = microProcessor.stop();
      if (stop != null) {
        stop.accept(this);
      }
      microProcessor.isa().accept(this);
      afterTraversal(microProcessor);
    }

    @Override
    public void visit(MicroArchitecture microArchitecture) {
      beforeTraversal(microArchitecture);
      microArchitecture.stages().forEach(stage -> stage.accept(this));
      microArchitecture.logic().forEach(logic -> logic.accept(this));
      microArchitecture.signals().forEach(signal -> signal.accept(this));
      microArchitecture.ownRegisters().forEach(register -> register.accept(this));
      microArchitecture.ownRegisterFiles().forEach(registerFile -> registerFile.accept(this));
      microArchitecture.ownMemories().forEach(memory -> memory.accept(this));
      microArchitecture.ownFunctions().forEach(function -> function.accept(this));
      afterTraversal(microArchitecture);
    }

    @Override
    public void visit(Logic logic) {
      beforeTraversal(logic);
      afterTraversal(logic);
    }

    @Override
    public void visit(Signal signal) {
      beforeTraversal(signal);
      afterTraversal(signal);
    }

    @Override
    public void visit(Stage stage) {
      beforeTraversal(stage);
      stage.outputs().forEach(output -> output.accept(this));
      afterTraversal(stage);
    }

    @Override
    public void visit(StageOutput stageOutput) {
      beforeTraversal(stageOutput);
      afterTraversal(stageOutput);
    }

    @Override
    public void visit(AssemblyDescription assemblyDescription) {
      beforeTraversal(assemblyDescription);
      for (var directive : assemblyDescription.directives()) {
        directive.accept(this);
      }
      for (var modifier : assemblyDescription.modifiers()) {
        modifier.accept(this);
      }
      for (var rule : assemblyDescription.rules()) {
        rule.accept(this);
      }
      for (var definition : assemblyDescription.commonDefinitions()) {
        definition.accept(this);
      }
      afterTraversal(assemblyDescription);
    }

    @Override
    public void visit(AsmDirectiveMapping directive) {
      beforeTraversal(directive);
      afterTraversal(directive);
    }

    @Override
    public void visit(AsmModifier modifier) {
      beforeTraversal(modifier);
      afterTraversal(modifier);
    }

    @Override
    public void visit(AsmBuiltinRule builtinRule) {
      beforeTraversal(builtinRule);
      afterTraversal(builtinRule);
    }

    @Override
    public void visit(AsmTerminalRule terminalRule) {
      beforeTraversal(terminalRule);
      afterTraversal(terminalRule);
    }

    @Override
    public void visit(AsmNonTerminalRule nonTerminalRule) {
      beforeTraversal(nonTerminalRule);
      afterTraversal(nonTerminalRule);
    }
  }

  /**
   * An empty visitor that allows to implement only a required subset of definition
   * visit methods.
   *
   * <p>An example usecase is {@link vadl.dump.entitySuppliers.ViamEntitySupplier}</p>
   */
  class Empty implements DefinitionVisitor {

    @Override
    public void visit(Specification specification) {

    }

    @Override
    public void visit(InstructionSetArchitecture instructionSetArchitecture) {

    }

    @Override
    public void visit(Instruction instruction) {

    }

    @Override
    public void visit(Assembly assembly) {

    }

    @Override
    public void visit(Encoding encoding) {

    }

    @Override
    public void visit(Encoding.Field encodingField) {

    }

    @Override
    public void visit(Format format) {

    }

    @Override
    public void visit(Format.Field formatField) {

    }

    @Override
    public void visit(Format.FieldAccess formatFieldAccess) {

    }

    @Override
    public void visit(Function function) {

    }

    @Override
    public void visit(Parameter parameter) {

    }

    @Override
    public void visit(PseudoInstruction pseudoInstruction) {

    }

    @Override
    public void visit(Register register) {

    }

    @Override
    public void visit(RegisterFile registerFile) {

    }

    @Override
    public void visit(Memory memory) {

    }

    @Override
    public void visit(Relocation relocation) {

    }


    @Override
    public void visit(Counter counter) {

    }


    @Override
    public void visit(Abi abi) {

    }

    @Override
    public void visit(MicroProcessor microProcessor) {

    }

    @Override
    public void visit(MicroArchitecture microArchitecture) {

    }

    @Override
    public void visit(Logic logic) {

    }

    @Override
    public void visit(Signal signal) {

    }

    @Override
    public void visit(Stage stage) {

    }

    @Override
    public void visit(StageOutput stageOutput) {

    }

    @Override
    public void visit(AssemblyDescription assemblyDescription) {

    }

    @Override
    public void visit(AsmDirectiveMapping directive) {

    }

    @Override
    public void visit(AsmModifier modifier) {

    }

    @Override
    public void visit(AsmBuiltinRule builtinRule) {

    }

    @Override
    public void visit(AsmTerminalRule terminalRule) {

    }

    @Override
    public void visit(AsmNonTerminalRule nonTerminalRule) {

    }
  }

}
