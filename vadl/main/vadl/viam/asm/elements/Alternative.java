package vadl.viam.asm.elements;

import java.util.List;
import javax.annotation.Nullable;
import vadl.viam.graph.Graph;

/**
 * Represents a single alternative in a grammar rule.
 * Contains a list of grammar elements and an optional semantic predicate.
 */
public class Alternative implements GrammarElement {
  // FIXME: is semantic predicate really a graph?
  // actually needed is a lowering of an Expr to CPP code
  @Nullable
  Graph semanticPredicate;
  List<GrammarElement> elements;

  public Alternative(@Nullable Graph semanticPredicate, List<GrammarElement> elements) {
    this.semanticPredicate = semanticPredicate;
    this.elements = elements;
  }
}
