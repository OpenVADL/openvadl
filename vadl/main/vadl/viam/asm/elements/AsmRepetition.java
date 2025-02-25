package vadl.viam.asm.elements;

import java.util.Set;
import javax.annotation.Nullable;
import vadl.viam.Function;
import vadl.viam.asm.AsmToken;

/**
 * Represents a repetition block ({@code {}}) in a grammar rule.
 */
public record AsmRepetition(@Nullable Function semanticPredicate,
                            Set<AsmToken> firstTokens,
                            AsmAlternatives alternatives) implements AsmGrammarElement {
}
