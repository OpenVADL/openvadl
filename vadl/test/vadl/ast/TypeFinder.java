package vadl.ast;

import javax.annotation.Nullable;
import vadl.types.Type;
import vadl.types.asmTypes.AsmType;

class TypeFinder implements DefinitionVisitor<Void> {

  private String target = "";

  private static class FoundTypeSignal extends RuntimeException {
    @Nullable
    Type type;

    public FoundTypeSignal(@Nullable Type type) {
      this.type = type;
    }
  }

  private static class FoundAsmTypeSignal extends RuntimeException {
    @Nullable
    AsmType type;

    public FoundAsmTypeSignal(@Nullable AsmType type) {
      this.type = type;
    }
  }

  @Nullable
  Type getConstantType(Ast ast, String name) {
    target = name;
    try {
      for (var definition : ast.definitions) {
        definition.accept(this);
      }
    } catch (FoundTypeSignal e) {
      return e.type;
    }
    throw new RuntimeException("No constant with the name %s found.".formatted(name));
  }

  AsmType getAsmRuleType(Ast ast, String name) {
    target = name;
    try {
      for (var definition : ast.definitions) {
        definition.accept(this);
      }
    } catch (FoundAsmTypeSignal e) {
      return e.type;
    }
    throw new RuntimeException("No asm rule with the name %s found.".formatted(name));
  }

  @Override
  public Void visit(AbiSequenceDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AliasDefinition definition) {
    return null;
  }

  @Override
  public Void visit(ApplicationBinaryInterfaceDefinition definition) {
    for (var def : definition.definitions) {
      def.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(AsmDescriptionDefinition definition) {
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
    return null;
  }

  @Override
  public Void visit(AsmGrammarRuleDefinition definition) {
    if (definition.identifier().name.equals(target)) {
      throw new FoundAsmTypeSignal(definition.asmType);
    }
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
    return null;
  }

  @Override
  public Void visit(ConstantDefinition definition) {
    if (definition.identifier().name.equals(target)) {
      throw new FoundTypeSignal(definition.type);
    }
    return null;
  }

  @Override
  public Void visit(CounterDefinition definition) {
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
    return null;
  }

  @Override
  public Void visit(EnumerationDefinition definition) {
    return null;
  }

  @Override
  public Void visit(ExceptionDefinition definition) {
    return null;
  }

  @Override
  public Void visit(FormatDefinition definition) {
    return null;
  }

  @Override
  public Void visit(FunctionDefinition definition) {
    return null;
  }

  @Override
  public Void visit(GroupDefinition definition) {
    return null;
  }

  @Override
  public Void visit(ImportDefinition definition) {
    return null;
  }

  @Override
  public Void visit(InstructionDefinition definition) {
    return null;
  }

  @Override
  public Void visit(InstructionSetDefinition definition) {
    for (var def : definition.definitions) {
      def.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(LogicDefinition definition) {
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
    return null;
  }

  @Override
  public Void visit(MicroArchitectureDefinition definition) {
    for (var def : definition.definitions) {
      def.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(MicroProcessorDefinition definition) {
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
    return null;
  }

  @Override
  public Void visit(PatchDefinition definition) {
    return null;
  }

  @Override
  public Void visit(PipelineDefinition definition) {
    return null;
  }

  @Override
  public Void visit(PlaceholderDefinition definition) {
    return null;
  }

  @Override
  public Void visit(PortBehaviorDefinition definition) {
    return null;
  }

  @Override
  public Void visit(ProcessDefinition definition) {
    return null;
  }

  @Override
  public Void visit(PseudoInstructionDefinition definition) {
    return null;
  }

  @Override
  public Void visit(RecordTypeDefinition definition) {
    return null;
  }

  @Override
  public Void visit(RegisterDefinition definition) {
    return null;
  }

  @Override
  public Void visit(RegisterFileDefinition definition) {
    return null;
  }

  @Override
  public Void visit(RelocationDefinition definition) {
    return null;
  }

  @Override
  public Void visit(SignalDefinition definition) {
    return null;
  }

  @Override
  public Void visit(SourceDefinition definition) {
    return null;
  }

  @Override
  public Void visit(SpecialPurposeRegisterDefinition definition) {
    return null;
  }

  @Override
  public Void visit(StageDefinition definition) {
    return null;
  }

  @Override
  public Void visit(UsingDefinition definition) {
    return null;
  }
}
