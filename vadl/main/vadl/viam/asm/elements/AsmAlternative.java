package vadl.viam.asm.elements;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.viam.Function;
import vadl.viam.asm.AsmGrammarVisitor;
import vadl.viam.asm.AsmToken;

/**
 * Represents a single alternative in a grammar rule.
 * Contains a list of grammar elements and an optional semantic predicate.
 */
public record AsmAlternative(@Nullable Function semanticPredicate,
                             Set<AsmToken> firstTokens,
                             List<AsmGrammarElement> elements) implements AsmGrammarElement {

  @Override
  public void accept(AsmGrammarVisitor visitor) {
    visitor.visit(this);
  }
}
