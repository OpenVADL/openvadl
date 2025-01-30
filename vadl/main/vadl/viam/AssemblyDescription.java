package vadl.viam;

import java.util.List;
import vadl.viam.asm.AsmDirectiveMapping;
import vadl.viam.asm.AsmModifier;
import vadl.viam.asm.rules.AsmGrammarRule;

/**
 * An Assembly Description definition of a VADL specification.
 */
public class AssemblyDescription extends Definition {

  private final List<AsmModifier> modifiers;
  private final List<AsmDirectiveMapping> directives;
  private final List<AsmGrammarRule> rules;
  private final List<Definition> commonDefinitions;

  /**
   * Creates a new Assembly Description definition.
   *
   * @param identifier        the identifier of the definition
   * @param modifiers         the modifiers of the definition
   * @param directives        the directives of the definition
   * @param rules             the rules of the definition
   * @param commonDefinitions the common definitions of the definition
   */
  public AssemblyDescription(Identifier identifier, List<AsmModifier> modifiers,
                             List<AsmDirectiveMapping> directives, List<AsmGrammarRule> rules,
                             List<Definition> commonDefinitions) {
    super(identifier);
    this.modifiers = modifiers;
    this.directives = directives;
    this.rules = rules;
    this.commonDefinitions = commonDefinitions;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  public List<AsmModifier> modifiers() {
    return modifiers;
  }

  public List<AsmDirectiveMapping> directives() {
    return directives;
  }

  public List<AsmGrammarRule> rules() {
    return rules;
  }

  public List<Definition> commonDefinitions() {
    return commonDefinitions;
  }
}
