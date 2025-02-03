package vadl.viam.asm.elements;

/**
 * Represents a sub-group of elements ({@code ()}) in a grammar rule.
 * <p>
 * It can contain a simple sequence of elements or a choice of one or more alternatives,
 * both represented by the {@code Alternatives} class.
 * </p>
 */
public class AsmGroup implements AsmGrammarElement {
  AsmAlternatives alternatives;

  public AsmGroup(AsmAlternatives alternatives) {
    this.alternatives = alternatives;
  }
}