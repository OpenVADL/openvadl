package vadl.ast;

/**
 * Removes all model definitions in the AST.
 * Model definitions are needed in the AST for ISA inheritance and across module imports, but can be
 * removed after macro expansion. This is especially useful for testing, where two AST trees are
 * often tested for semantic equality and thus stripped of models before comparison.
 */
public class ModelRemover implements DefinitionVisitor<Definition> {

  public void removeModels(Ast ast) {
    ast.definitions.removeIf(this::shouldRemove);
    ast.definitions.replaceAll(definition -> definition.accept(this));
  }

  @Override
  public Definition visit(ConstantDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(FormatDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(InstructionSetDefinition definition) {
    definition.definitions.removeIf(this::shouldRemove);
    return definition;
  }

  @Override
  public Definition visit(CounterDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(MemoryDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(RegisterDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(RegisterFileDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(InstructionDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(PseudoInstructionDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(EncodingDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AssemblyDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(UsingDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(FunctionDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AliasDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(EnumerationDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(ExceptionDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(PlaceholderDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(MacroInstanceDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(MacroMatchDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(DefinitionList definition) {
    return definition;
  }

  @Override
  public Definition visit(ModelDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(RecordTypeDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(ModelTypeDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(ImportDefinition importDefinition) {
    removeModels(importDefinition.moduleAst);
    return importDefinition;
  }

  @Override
  public Definition visit(ProcessDefinition processDefinition) {
    return processDefinition;
  }

  @Override
  public Definition visit(OperationDefinition operationDefinition) {
    return operationDefinition;
  }

  @Override
  public Definition visit(GroupDefinition groupDefinition) {
    return groupDefinition;
  }

  @Override
  public Definition visit(ApplicationBinaryInterfaceDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AbiSequenceDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(SpecialPurposeRegisterDefinition definition) {
    return definition;
  }

  private boolean shouldRemove(Definition definition) {
    return definition instanceof ModelDefinition
        || definition instanceof RecordTypeDefinition
        || definition instanceof ModelTypeDefinition;
  }
}
