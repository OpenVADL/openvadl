package vadl.viam;

import vadl.viam.passes.dummyAbi.DummyAbi;

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

  void visit(Parameter parameter);

  void visit(PseudoInstruction pseudoInstruction);

  void visit(Register register);

  void visit(RegisterFile registerFile);

  void visit(Memory memory);

  void visit(Relocation relocation);

  void visit(Counter counter);

  void visit(DummyAbi dummyAbi);

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
    public void visit(DummyAbi dummyAbi) {
      beforeTraversal(dummyAbi);
      afterTraversal(dummyAbi);
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
    public void visit(DummyAbi dummyAbi) {

    }
  }

}
