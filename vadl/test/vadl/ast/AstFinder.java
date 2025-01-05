package vadl.ast;

import javax.annotation.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.Type;
import vadl.types.asmTypes.AsmType;

class AstFinder implements DefinitionVisitor<Void> {

  private String target = "";
  private Class<Definition> targetType;

  private static class FoundSignal extends RuntimeException {
    Definition definition;

    public FoundSignal(Definition definition) {
      this.definition = definition;
    }
  }

  /**
   * Find a definition of a specified type.
   *
   * @param ast  to search.
   * @param name to search for.
   * @param type which the target must have
   * @return the definition if found.
   * @throws RuntimeException if no matching element is found.
   */
  <T extends Definition> T findDefinition(Ast ast, String name, Class<T> type) {
    target = name;
    targetType = (Class<Definition>) type;
    try {
      for (var definition : ast.definitions) {
        definition.accept(this);
      }
    } catch (FoundSignal e) {
      return type.cast(e.definition);
    }
    throw new RuntimeException(
        "No %s with the name %s found.".formatted(type.getSimpleName(), name));
  }

  ConstantValue getConstantValue(Ast ast, String name) {
    var evaluator = new ConstantEvaluator();
    var constDef = findDefinition(ast, name, ConstantDefinition.class);
    return evaluator.eval(constDef.value);
  }

  @Nullable
  Type getConstantType(Ast ast, String name) {
    var constDef = findDefinition(ast, name, ConstantDefinition.class);
    return constDef.value.type;
  }

  @Nullable
  ConcreteRelationType getFunctionType(Ast ast, String name) {
    var constDef = findDefinition(ast, name, FunctionDefinition.class);
    return constDef.type;
  }

  AsmType getAsmRuleType(Ast ast, String name) {
    var asmRule = findDefinition(ast, name, AsmGrammarRuleDefinition.class);
    return asmRule.asmType;
  }

  /**
   * Throw FoundSignal if the provided definition is the one we are searching for.
   *
   * @param definition to check against.
   * @throws FoundSignal if a match is found.
   */
  private <T extends Definition & IdentifiableNode> void visitDefinition(T definition) {
    if ((targetType.equals(definition.getClass())) && definition.identifier().name.equals(target)) {
      throw new FoundSignal(definition);
    }
  }

  @Override
  public Void visit(AbiSequenceDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AliasDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(ApplicationBinaryInterfaceDefinition definition) {
    visitDefinition(definition);
    for (var def : definition.definitions) {
      def.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(AsmDescriptionDefinition definition) {
    visitDefinition(definition);
    for (var rule : definition.rules) {
      rule.accept(this);
    }
    for (var commonDef : definition.commonDefinitions) {
      commonDef.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(AsmDirectiveDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AsmGrammarAlternativesDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AsmGrammarElementDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AsmGrammarLiteralDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AsmGrammarLocalVarDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(AsmGrammarRuleDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(AsmGrammarTypeDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AsmModifierDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AssemblyDefinition definition) {
    for (var def : definition.instructionNodes) {
      def.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(CacheDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(ConstantDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(CounterDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(CpuFunctionDefinition definition) {
    return null;
  }

  @Override
  public Void visit(CpuProcessDefinition definition) {
    return null;
  }

  @Override
  public Void visit(DefinitionList definition) {
    for (var def : definition.items) {
      def.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(EncodingDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(EnumerationDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(ExceptionDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(FormatDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(FunctionDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(GroupDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(ImportDefinition definition) {
    return null;
  }

  @Override
  public Void visit(InstructionDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(InstructionSetDefinition definition) {
    visitDefinition(definition);
    for (var def : definition.definitions) {
      def.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(LogicDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(MacroInstanceDefinition definition) {
    return null;
  }

  @Override
  public Void visit(MacroInstructionDefinition definition) {
    return null;
  }

  @Override
  public Void visit(MacroMatchDefinition definition) {
    return null;
  }

  @Override
  public Void visit(MemoryDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(MicroArchitectureDefinition definition) {
    visitDefinition(definition);
    for (var def : definition.definitions) {
      def.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(MicroProcessorDefinition definition) {
    visitDefinition(definition);
    for (var def : definition.definitions) {
      def.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(ModelDefinition definition) {
    return null;
  }

  @Override
  public Void visit(ModelTypeDefinition definition) {
    return null;
  }

  @Override
  public Void visit(OperationDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(PatchDefinition definition) {
    return null;
  }

  @Override
  public Void visit(PipelineDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(PlaceholderDefinition definition) {
    return null;
  }

  @Override
  public Void visit(PortBehaviorDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(ProcessDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(PseudoInstructionDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(RecordTypeDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(RegisterDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(RegisterFileDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(RelocationDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(SignalDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(SourceDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(SpecialPurposeRegisterDefinition definition) {
    return null;
  }

  @Override
  public Void visit(StageDefinition definition) {
    visitDefinition(definition);
    return null;
  }

  @Override
  public Void visit(UsingDefinition definition) {
    visitDefinition(definition);
    return null;
  }
}
