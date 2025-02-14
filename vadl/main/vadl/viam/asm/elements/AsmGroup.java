package vadl.viam.asm.elements;

import vadl.types.asmTypes.AsmType;

/**
 * Represents a sub-group of elements ({@code ()}) in a grammar rule.
 * <p>
 * It can contain a simple sequence of elements or a choice of one or more alternatives,
 * both represented by the {@code Alternatives} class.
 * </p>
 */
public record AsmGroup(AsmAlternatives alternatives,
                       AsmType asmType) implements AsmGrammarElement {
}