package vadl.viam;

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


  /**
   * DefinitionVisitor.Recursive is an abstract class that implements the DefinitionVisitor
   * interface.
   * It provides default implementations for the visit methods for all types of definitions in a
   * VADL specification, allowing for recursive traversal of the definition hierarchy.
   */
  abstract class Recursive implements DefinitionVisitor {

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
      instructionSetArchitecture
          .formats()
          .forEach(e -> e.accept(this));
      instructionSetArchitecture
          .instructions()
          .forEach(e -> e.accept(this));
      instructionSetArchitecture
          .pseudoInstructions()
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
      encoding
          .fieldEncodings()
          .forEach(e -> e.accept(this));
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
      format.fields()
          .forEach(e -> e.accept(this));
      format.fieldAccesses()
          .forEach(e -> e.accept(this));
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
      formatFieldAccess.decoding().accept(this);
      formatFieldAccess.encoding().accept(this);
      formatFieldAccess.predicate().accept(this);
      afterTraversal(formatFieldAccess);
    }

    @Override
    public void visit(Function function) {
      beforeTraversal(function);
      function.parameters()
          .forEach(e -> e.accept(this));
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
      pseudoInstruction.parameters()
          .forEach(e -> e.accept(this));
      pseudoInstruction.assembly()
          .accept(this);
      afterTraversal(pseudoInstruction);
    }
  }

}
