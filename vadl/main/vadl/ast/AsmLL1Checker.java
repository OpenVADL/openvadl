package vadl.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;


/**
 * A helper class that evaluates the assembly grammar for LL(1) compliance.
 * LL(1) conflicts are allowed in the grammar, if they are resolved by semantic predicates.
 */
public class AsmLL1Checker {
  // TODO: handle semantic predicates

  /**
   * Verify that the assembly grammar is LL(1) compliant
   * or any conflicts are resolved by semantic predicates.
   *
   * @param ast to verify
   * @throws Diagnostic if there are unresolved LL(1) conflicts
   */
  public void verify(Ast ast) {
    //    var firstSetComputer = new FirstSetComputer();
    //    for (var definition : ast.definitions) {
    //      if (definition instanceof AsmDescriptionDefinition asmDescription) {
    //        for (var rule : asmDescription.rules) {
    //          // TODO: verify LL(1) characteristics
    //        }
    //      }
    //    }
  }

  //  private Boolean CheckOverlap(List<AsmToken> first, List<AsmToken> second) {
  //    for (var outerToken : first) {
  //      for (var innerToken : second) {
  //        if (outerToken.ruleName.equals(innerToken.ruleName)) {
  //          if (outerToken.stringLiteral == null || innerToken.stringLiteral == null) {
  //            return true;
  //          }
  //          if (outerToken.stringLiteral.equals(innerToken.stringLiteral)) {
  //            return true;
  //          }
  //        }
  //      }
  //    }
  //    return false;
  //  }
}

class AsmToken {
  String ruleName;
  @Nullable
  String stringLiteral;

  public AsmToken(String ruleName, @Nullable String stringLiteral) {
    this.ruleName = ruleName;
    this.stringLiteral = stringLiteral;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmToken asmToken = (AsmToken) o;
    return Objects.equals(ruleName, asmToken.ruleName) &&
        Objects.equals(stringLiteral, asmToken.stringLiteral);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ruleName, stringLiteral);
  }
}

class FirstSetComputer implements AsmGrammarEntityVisitor<HashSet<AsmToken>> {

  private final HashMap<String, HashSet<AsmToken>> firstSetCache = new HashMap<>();

  @Override
  public HashSet<AsmToken> visit(AsmGrammarRuleDefinition entity) {
    var ruleName = entity.identifier().name;
    if (firstSetCache.containsKey(ruleName)) {
      return firstSetCache.get(ruleName);
    }

    HashSet<AsmToken> firstSet;
    if (entity.isTerminalRule) {
      firstSet = new HashSet<>(List.of(new AsmToken(ruleName, null)));
    } else {
      firstSet = entity.alternatives.accept(this);
    }

    firstSetCache.put(ruleName, firstSet);
    return firstSet;
  }

  @Override
  public HashSet<AsmToken> visit(AsmGrammarAlternativesDefinition entity) {

    var alternativesTokens = new HashSet<AsmToken>();

    for (var alternative : entity.alternatives) {
      var firstEntityTokens = computeFirstSetOfGroup(alternative);
      alternativesTokens.addAll(firstEntityTokens);
    }

    return alternativesTokens;
  }

  @Override
  public HashSet<AsmToken> visit(AsmGrammarElementDefinition entity) {
    // only ever one of these fields is non-null
    // so the order of the statements has no effect
    if (entity.groupAlternatives != null) {
      return entity.groupAlternatives.accept(this);
    }
    if (entity.optionAlternatives != null) {
      return entity.optionAlternatives.accept(this);
    }
    if (entity.repetitionAlternatives != null) {
      return entity.repetitionAlternatives.accept(this);
    }
    if (entity.asmLiteral != null) {
      return entity.asmLiteral.accept(this);
    }
    if (entity.localVar != null) {
      return entity.localVar.accept(this);
    }
    return new HashSet<>();
  }

  @Override
  public HashSet<AsmToken> visit(AsmGrammarLocalVarDefinition entity) {
    if (entity.asmLiteral.id != null && entity.asmLiteral.id.name.equals("null")) {
      return new HashSet<>();
    }
    return entity.asmLiteral.accept(this);
  }

  @Override
  public HashSet<AsmToken> visit(AsmGrammarLiteralDefinition entity) {

    if (entity.stringLiteral != null) {
      var stringValue = ((StringLiteral) entity.stringLiteral).value;
      var inferredRule = inferTerminalRule(stringValue);
      return new HashSet<>(List.of(new AsmToken(inferredRule.identifier().name, stringValue)));
    }

    Objects.requireNonNull(entity.id);
    var invocationSymbolOrigin = entity.symbolTable().resolveNode(entity.id.name);

    if (invocationSymbolOrigin instanceof AsmGrammarRuleDefinition rule) {
      if (firstSetCache.containsKey(rule.identifier().name)) {
        return firstSetCache.get(rule.identifier().name);
      }
      return rule.accept(this);
    }

    if (invocationSymbolOrigin instanceof AsmGrammarLocalVarDefinition) {
      // on usage of local variable, there is nothing to be parsed
      return new HashSet<>();
    }

    if (invocationSymbolOrigin instanceof FunctionDefinition) {

      HashSet<AsmToken> tokens = new HashSet<>();

      // get the tokens of the first parameter that needs to be parsed
      // it can be the case that no parameter needs to be parsed
      for (int i = 0; i < entity.parameters.size() && tokens.isEmpty(); i++) {
        tokens = entity.parameters.get(i).accept(this);
      }

      return tokens;
    }

    return new HashSet<>();
  }

  public HashSet<AsmToken> computeFirstSetOfGroup(
      @Nullable List<AsmGrammarElementDefinition> elements) {
    if (elements == null) {
      return new HashSet<>();
    }

    AsmGrammarElementDefinition firstEntity = elements.get(0);
    // TODO: transfer to above method

    //    if (firstEntity.semanticPredicate != null) {
    //      return new ArrayList<>();
    //    }

    // get the firstSet of the first entity that needs to be parsed
    var firstEntityTokens = firstEntity.accept(this);
    int i = 1;
    while (firstEntityTokens.isEmpty()) {
      if (i == elements.size()) {
        throw new RuntimeException("No parsable elements in alternative");
      }
      firstEntity = elements.get(i);
      firstEntityTokens = firstEntity.accept(this);
      i++;
    }

    if (firstEntity.optionAlternatives != null || firstEntity.repetitionAlternatives != null) {
      Collection<AsmToken> successorTokens = null;
      while ((successorTokens == null || successorTokens.isEmpty()) && i < elements.size()) {
        successorTokens = elements.get(i).accept(this);
        i++;
      }
      if (successorTokens != null) {
        firstEntityTokens.addAll(successorTokens);
      }
    }
    return firstEntityTokens;
  }

  private AsmGrammarRuleDefinition inferTerminalRule(String value) {
    var inferredRule =
        AsmGrammarDefaultRules.terminalRuleRegexPatterns().entrySet().stream().filter(
            entry -> entry.getValue().matcher(value).matches()
        ).findFirst();

    // FIXME: is this correct with respect to order of terminal rules?

    if (inferredRule.isEmpty()) {
      throw new RuntimeException(
          "Could not infer asm terminal rule for string literal: '%s'".formatted(value));
    }

    return inferredRule.get().getKey();
  }
}

class FollowSetSetComputer implements AsmGrammarEntityVisitor<Void> {

  private final FirstSetComputer firstSetComputer = new FirstSetComputer();
  private final EntityDeletableComputer deletableComputer = new EntityDeletableComputer();

  private final HashMap<String, HashSet<AsmToken>> followSetCache = new HashMap<>();

  // a mapping of "RuleA" -> ["RuleB","RuleC",...]
  // the follow sets of ["RuleB","RuleC",...] need to be merged into the follow set of "RuleA"
  private final HashMap<String, HashSet<String>> followSetsToBeMergedFrom = new HashMap<>();

  @Nullable
  private String currentRuleName;
  @Nullable
  private List<AsmGrammarElementDefinition> successorElements;


  public void computeFollowSets(Ast ast) {
    for (var definition : ast.definitions) {
      if (definition instanceof AsmDescriptionDefinition asmDescription) {
        // compute follow from first e.g. add first(C) to follow(B) for A : B C;
        for (var rule : asmDescription.rules) {
          currentRuleName = rule.identifier().name;
          rule.accept(this);
        }

        // complete follow sets e.g. add follow(A) to follow(B) for A : B;
        for (var rule : asmDescription.rules) {
          currentRuleName = rule.identifier().name;
          complete(rule.identifier().name);
        }
      }
    }
  }

  private void complete(String ruleName) {
    var rulesToGetFollowFrom = followSetsToBeMergedFrom.get(ruleName);
    if (rulesToGetFollowFrom == null) {
      return;
    }
    for (var otherRule : rulesToGetFollowFrom) {
      complete(otherRule);

      if (followSetCache.get(otherRule) != null) {
        followSetCache.computeIfAbsent(ruleName, key -> new HashSet<>())
            .addAll(followSetCache.get(otherRule));
      }

      if (otherRule.equals(currentRuleName)) {
        followSetsToBeMergedFrom.put(currentRuleName, new HashSet<>());
      }
    }
  }

  @Override
  public Void visit(AsmGrammarRuleDefinition entity) {
    entity.alternatives.accept(this);
    return null;
  }

  @Override
  public Void visit(AsmGrammarAlternativesDefinition entity) {
    for (var alternative : entity.alternatives) {
      for (int i = 0; i < alternative.size(); i++) {
        if (i < alternative.size() - 1) {
          successorElements = alternative.subList(i + 1, alternative.size());
        } else {
          successorElements = null;
        }
        alternative.get(i).accept(this);
      }
    }
    return null;
  }

  @Override
  public Void visit(AsmGrammarElementDefinition entity) {
    if (entity.groupAlternatives != null) {
      entity.groupAlternatives.accept(this);
    }
    if (entity.optionAlternatives != null) {
      entity.optionAlternatives.accept(this);
    }
    if (entity.repetitionAlternatives != null) {
      entity.repetitionAlternatives.accept(this);
    }
    if (entity.asmLiteral != null) {
      entity.asmLiteral.accept(this);
    }
    if (entity.localVar != null) {
      entity.localVar.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(AsmGrammarLocalVarDefinition entity) {
    if (entity.asmLiteral.id != null && entity.asmLiteral.id.name.equals("null")) {
      return null;
    }
    entity.asmLiteral.accept(this);
    return null;
  }

  @Override
  public Void visit(AsmGrammarLiteralDefinition entity) {
    if (entity.id == null) {
      return null;
    }

    var invocationSymbolOrigin = entity.symbolTable().resolveNode(entity.id.name);
    if (invocationSymbolOrigin == null) {
      return null;
    }

    if (invocationSymbolOrigin instanceof AsmGrammarRuleDefinition rule) {
      if (rule.isTerminalRule) {
        return null;
      }

      var invokedRule = rule.identifier().name;
      var invokedRuleFollowSet =
          followSetCache.computeIfAbsent(invokedRule, key -> new HashSet<>());
      invokedRuleFollowSet.addAll(firstSetComputer.computeFirstSetOfGroup(successorElements));

      if (successorElements == null ||
          deletableComputer.areAllElementsDeletable(successorElements)) {
        followSetsToBeMergedFrom.computeIfAbsent(invokedRule, key -> new HashSet<>())
            .add(currentRuleName);
      }
    }

    return null;
  }
}

class EntityDeletableComputer implements AsmGrammarEntityVisitor<Boolean> {

  private final HashMap<String, Boolean> isRuleDeletableCache = new HashMap<>();

  Boolean areAllElementsDeletable(List<AsmGrammarElementDefinition> elements) {
    return elements.stream().allMatch(this::visit);
  }

  @Override
  public Boolean visit(AsmGrammarRuleDefinition entity) {
    return isRuleDeletable(entity);
  }

  @Override
  public Boolean visit(AsmGrammarAlternativesDefinition entity) {
    return entity.alternatives.stream().anyMatch(this::areAllElementsDeletable);
  }

  @Override
  public Boolean visit(AsmGrammarElementDefinition entity) {
    if (entity.optionAlternatives != null || entity.repetitionAlternatives != null
        || entity.semanticPredicate != null) {
      return true;
    }
    if (entity.groupAlternatives != null) {
      return entity.groupAlternatives.accept(this);
    }
    if (entity.asmLiteral != null) {
      return entity.asmLiteral.accept(this);
    }
    if (entity.localVar != null) {
      return entity.localVar.accept(this);
    }
    return false;
  }

  @Override
  public Boolean visit(AsmGrammarLocalVarDefinition entity) {
    if (entity.asmLiteral.id != null && entity.asmLiteral.id.name.equals("null")) {
      return true;
    }
    return entity.asmLiteral.accept(this);
  }

  @Override
  public Boolean visit(AsmGrammarLiteralDefinition entity) {
    if (entity.id == null) {
      return false;
    }

    var invocationSymbolOrigin = entity.symbolTable().resolveNode(entity.id.name);
    if (invocationSymbolOrigin == null) {
      return false;
    }

    if (invocationSymbolOrigin instanceof AsmGrammarRuleDefinition rule) {
      return isRuleDeletable(rule);
    }

    return false;
  }

  private boolean isRuleDeletable(AsmGrammarRuleDefinition rule) {
    if (rule.isTerminalRule) {
      return false;
    }

    var ruleName = rule.identifier().name;
    if (isRuleDeletableCache.containsKey(ruleName)) {
      return isRuleDeletableCache.get(ruleName);
    }

    var deletable = rule.alternatives.accept(this);
    isRuleDeletableCache.put(ruleName, deletable);
    return deletable;
  }
}
