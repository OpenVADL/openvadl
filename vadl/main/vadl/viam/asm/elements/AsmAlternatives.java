package vadl.viam.asm.elements;

import java.util.List;
import vadl.types.asmTypes.AsmType;

/**
 * Represents a choice of different alternatives in a grammar rule.
 * It contains a list of single alternatives.
 */
public record AsmAlternatives(List<AsmAlternative> alternatives, AsmType asmType)
    implements AsmGrammarElement {
}
