package vadl.viam.asm.elements;

import java.util.Set;
import javax.annotation.Nullable;
import vadl.viam.Function;
import vadl.viam.asm.AsmToken;

/**
 * Represents an optional block ({@code []}) in a grammar rule.
 */
public record AsmOption(@Nullable Function semanticPredicate,
                        Set<AsmToken> firstTokens,
                        AsmAlternatives alternatives) implements AsmGrammarElement {
}
