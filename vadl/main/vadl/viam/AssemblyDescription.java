package vadl.viam;

import java.util.List;
import vadl.viam.asm.DirectiveMapping;
import vadl.viam.asm.Modifier;
import vadl.viam.asm.rules.GrammarRule;

/**
 * An Assembly Description definition of a VADL specification.
 */
public class AssemblyDescription extends Definition {

  private final List<Modifier> modifiers;
  private final List<DirectiveMapping> directives;
  private final List<GrammarRule> rules;
  private final List<Definition> commonDefinitions;

  public AssemblyDescription(Identifier identifier, List<Modifier> modifiers,
                             List<DirectiveMapping> directives, List<GrammarRule> rules,
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

  public List<Modifier> modifiers() {
    return modifiers;
  }

  public List<DirectiveMapping> directives() {
    return directives;
  }

  public List<GrammarRule> rules() {
    return rules;
  }

  public List<Definition> commonDefinitions() {
    return commonDefinitions;
  }
}
