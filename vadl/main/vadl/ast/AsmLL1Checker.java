package vadl.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.viam.asm.AsmToken;


/**
 * A class that evaluates the assembly grammar for LL(1) compliance.
 * If there are LL(1) conflicts in the grammar, they need to be resolved by semantic predicates.
 */
public class AsmLL1Checker {
  FirstSetComputer firstSetComputer = new FirstSetComputer();
  FollowSetSetComputer followSetSetComputer = new FollowSetSetComputer(firstSetComputer);
  EntityDeletableComputer deletableComputer = new EntityDeletableComputer();

  @Nullable
  AsmGrammarRuleDefinition currentRule;

  /**
   * Verify that the assembly grammar is LL(1) compliant
   * or any conflicts are resolved by semantic predicates.
   *
   * @param rules grammar rules to verify
   * @throws Diagnostic if there are unresolved LL(1) conflicts or misplaced semantic predicates
   */
  public void verify(List<AsmGrammarRuleDefinition> rules) {
    followSetSetComputer.computeFollowSets(rules);

    for (var rule : rules) {
      if (!rule.isTerminalRule && !rule.isBuiltinRule) {
        currentRule = rule;
        verifyAlternatives(rule.alternatives, false);
      }
    }
  }

  private void verifyAlternatives(AsmGrammarAlternativesDefinition entity,
                                  boolean isInOptionOrRepetition) {
    HashSet<AsmToken> previousAlternativesTokens = new HashSet<>();
    Set<AsmToken> allAlternativesTokens = new HashSet<>();
    entity.alternativesFirstTokens = new ArrayList<>(entity.alternatives.size());

    for (var alternative : entity.alternatives) {
      allAlternativesTokens.addAll(expectedTokensForConflict(alternative));
    }

    for (var alternative : entity.alternatives) {

      var expected = expectedTokens(alternative);
      entity.alternativesFirstTokens.add(expected);

      var firstElement = alternative.get(0);
      if (!(entity.alternatives.size() == 1 && isInOptionOrRepetition)
          && firstElement.semanticPredicate != null) {

        if (!getOverlappingTokens(previousAlternativesTokens, expected).isEmpty()) {
          throw Diagnostic.error("Misplaced semantic predicate.", firstElement)
              .note("This semantic predicate will never be evaluated."
                  + "Place it at previous conflicting alternative.").build();
        }
        if (getOverlappingTokens(allAlternativesTokens, expected).isEmpty()) {
          throw Diagnostic.error("Misplaced semantic predicate.", firstElement)
              .note("There is no LL(1) conflict here.").build();
        }
      }

      var expectedForConflict = expectedTokensForConflict(alternative);
      checkAlternativeLL1Conflict(previousAlternativesTokens, expectedForConflict);

      previousAlternativesTokens.addAll(expectedForConflict);
      verifyElementsOfAlternative(alternative);
    }
  }

  private void checkAlternativeLL1Conflict(Set<AsmToken> previousAlternativesTokens,
                                           Set<AsmToken> expectedForConflict) {
    var overlappingTokens = getOverlappingTokens(previousAlternativesTokens, expectedForConflict);
    if (!overlappingTokens.isEmpty()) {
      Objects.requireNonNull(currentRule);
      throw Diagnostic.error("LL(1) conflict in %s".formatted(currentRule.identifier().name),
              currentRule)
          .note("%s %s the start of several alternatives.", String.join(", ",
                  overlappingTokens.stream().map(AsmToken::toString).toList()),
              overlappingTokens.size() > 1 ? "are" : "is")
          .note("Start of each alternative must be distinct.")
          .build();
    }
  }

  private void verifyElementsOfAlternative(List<AsmGrammarElementDefinition> alternative) {
    List<AsmGrammarElementDefinition> successorElements;

    for (int i = 0; i < alternative.size(); i++) {
      var element = alternative.get(i);

      if (element.semanticPredicate != null && i > 0) {
        throw Diagnostic.error("Misplaced semantic predicate.", element)
            .note("Semantic predicates must be at the beginning of an alternative or a block.")
            .build();
      }

      successorElements = i < alternative.size() - 1
          ? alternative.subList(i + 1, alternative.size()) : new ArrayList<>();

      if (element.optionAlternatives != null) {
        verifyOptionOrRepetitionElement(element.optionAlternatives, successorElements);
        verifyAlternatives(element.optionAlternatives, true);
      }

      if (element.repetitionAlternatives != null) {
        verifyOptionOrRepetitionElement(element.repetitionAlternatives, successorElements);
        verifyAlternatives(element.repetitionAlternatives, true);
      }

      if (element.groupAlternatives != null) {
        verifyAlternatives(element.groupAlternatives, false);
      }
    }
  }

  private void verifyOptionOrRepetitionElement(AsmGrammarAlternativesDefinition alternatives,
                                               List<AsmGrammarElementDefinition> successors) {
    var expectedTokensAfter = expectedTokens(successors);

    if (alternatives.alternatives.size() == 1) {
      var firstElement = alternatives.alternatives.get(0).get(0);
      if (firstElement.semanticPredicate != null) {
        var expected = firstSetComputer.visit(alternatives);
        if (getOverlappingTokens(expected, expectedTokensAfter).isEmpty()) {
          throw Diagnostic.error("Misplaced semantic predicate.", firstElement)
              .note("There is no LL(1) conflict here.").build();
        }
      }
    }

    checkDeletableWithinOptionOrRepetition(alternatives);

    var expectedTokens = expectedTokensForConflict(alternatives);

    var overlappingTokens = getOverlappingTokens(expectedTokens, expectedTokensAfter);
    if (!overlappingTokens.isEmpty()) {
      Objects.requireNonNull(currentRule);
      throw Diagnostic.error("LL(1) conflict in %s".formatted(currentRule.identifier().name),
              currentRule)
          .locationDescription(alternatives, "%s %s start and successor of this block.",
              String.join(", ", overlappingTokens.stream().map(AsmToken::toString).toList()),
              overlappingTokens.size() > 1 ? "are" : "is")
          .note("Start and successor of [...] or {...} must be distinct.")
          .build();
    }
  }

  private Set<AsmToken> expectedTokensForConflict(List<AsmGrammarElementDefinition> elements) {
    if (!elements.isEmpty() && elements.get(0).semanticPredicate != null) {
      return new HashSet<>();
    }
    return expectedTokens(elements);
  }

  /**
   * <p>
   * For a semantic predicate to apply to an option/repetition, there can only be one alternative.
   * Compare rule A with rule B:
   * </p>
   * <ul>
   *  <li>A : [?(true) (?(true) "A" | "A")] "A";
   *  here the first semantic predicate applies to option.</li>
   * <li>B : [?(true) "B" | "B"] "B";
   * here the semantic predicate applies to the alternative within the option.</li>
   * </ul>
   * <p>
   * In rule A we see that within the option there is only one alternative (with 2 elements:
   * a semantic predicate and a nested group).
   * </p>
   *
   * @param alternatives the alternatives of an option/repetition block
   * @return the expected tokens for the block
   */
  private Set<AsmToken> expectedTokensForConflict(AsmGrammarAlternativesDefinition alternatives) {
    if (alternatives.alternatives.size() == 1) {
      if (alternatives.alternatives.get(0).get(0).semanticPredicate != null) {
        return new HashSet<>();
      }
    }
    return expectedTokens(alternatives);
  }

  private Set<AsmToken> expectedTokens(List<AsmGrammarElementDefinition> elements) {
    var expectedTokens = firstSetComputer.computeFirstSetOfGroup(elements);
    if (deletableComputer.areAllElementsDeletable(elements)) {
      addFollowSetOfCurrentRule(expectedTokens);
    }
    return expectedTokens;
  }

  private Set<AsmToken> expectedTokens(AsmGrammarAlternativesDefinition alternatives) {
    var expectedTokens = firstSetComputer.visit(alternatives);
    if (deletableComputer.visit(alternatives)) {
      addFollowSetOfCurrentRule(expectedTokens);
    }
    return expectedTokens;
  }

  private void addFollowSetOfCurrentRule(Set<AsmToken> tokens) {
    Objects.requireNonNull(currentRule);
    var followSet = followSetSetComputer.getFollowSets().get(currentRule.identifier().name);
    if (followSet != null) {
      tokens.addAll(followSet);
    }
  }

  private List<AsmToken> getOverlappingTokens(Set<AsmToken> outerSet, Set<AsmToken> innerSet) {
    return outerSet.stream().filter(innerSet::contains).toList();
  }

  private void checkDeletableWithinOptionOrRepetition(
      AsmGrammarAlternativesDefinition alternatives) {
    Objects.requireNonNull(currentRule);
    if (deletableComputer.visit(alternatives)) {
      throw Diagnostic.error("LL(1) conflict in %s".formatted(currentRule.identifier().name),
              currentRule)
          .locationDescription(alternatives, "Deletable block.")
          .note("Contents of [...] or {...} must not be deletable.")
          .build();
    }
  }
}

class FirstSetComputer implements AsmGrammarEntityVisitor<Set<AsmToken>> {

  private final HashMap<String, Set<AsmToken>> firstSetCache = new HashMap<>();

  @Override
  public Set<AsmToken> visit(AsmGrammarRuleDefinition entity) {
    var ruleName = entity.identifier().name;
    if (firstSetCache.containsKey(ruleName)) {
      return firstSetCache.get(ruleName);
    }

    Set<AsmToken> firstSet;
    if (entity.isTerminalRule || entity.isBuiltinRule) {
      firstSet = new HashSet<>(List.of(new AsmToken(ruleName, null)));
    } else {
      firstSet = entity.alternatives.accept(this);
    }

    firstSetCache.put(ruleName, firstSet);
    return firstSet;
  }

  @Override
  public Set<AsmToken> visit(AsmGrammarAlternativesDefinition entity) {

    var alternativesTokens = new HashSet<AsmToken>();

    for (var alternative : entity.alternatives) {
      var firstEntityTokens = computeFirstSetOfGroup(alternative);
      alternativesTokens.addAll(firstEntityTokens);
    }

    return alternativesTokens;
  }

  @Override
  public Set<AsmToken> visit(AsmGrammarElementDefinition entity) {
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
  public Set<AsmToken> visit(AsmGrammarLocalVarDefinition entity) {
    if (entity.asmLiteral.id != null && entity.asmLiteral.id.name.equals("null")) {
      return new HashSet<>();
    }
    return entity.asmLiteral.accept(this);
  }

  @Override
  public Set<AsmToken> visit(AsmGrammarLiteralDefinition entity) {

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

      Set<AsmToken> tokens = new HashSet<>();

      // get the tokens of the first parameter that needs to be parsed
      // it can be the case that no parameter needs to be parsed
      for (int i = 0; i < entity.parameters.size() && tokens.isEmpty(); i++) {
        tokens = entity.parameters.get(i).accept(this);
      }

      return tokens;
    }

    return new HashSet<>();
  }

  public Set<AsmToken> computeFirstSetOfGroup(
      @Nullable List<AsmGrammarElementDefinition> elements) {
    if (elements == null || elements.isEmpty()) {
      return new HashSet<>();
    }

    AsmGrammarElementDefinition firstEntity = elements.get(0);

    // get the firstSet of the first entity that needs to be parsed
    var firstEntityTokens = firstEntity.accept(this);
    int i = 1;
    while (firstEntityTokens.isEmpty()) {
      if (i == elements.size()) {
        // no parsable element in elements
        return new HashSet<>();
      }
      firstEntity = elements.get(i);
      firstEntityTokens = firstEntity.accept(this);
      i++;
    }

    if (firstEntity.optionAlternatives != null || firstEntity.repetitionAlternatives != null) {
      Set<AsmToken> successorTokens = null;
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

    if (inferredRule.isEmpty()) {
      throw new RuntimeException(
          "Could not infer asm terminal rule for string literal: '%s'".formatted(value));
    }

    return inferredRule.get().getKey();
  }
}

class FollowSetSetComputer implements AsmGrammarEntityVisitor<Void> {

  private final FirstSetComputer firstSetComputer;
  private final EntityDeletableComputer deletableComputer = new EntityDeletableComputer();

  private final HashMap<String, Set<AsmToken>> followSetCache = new HashMap<>();

  // a mapping of "RuleA" -> ["RuleB","RuleC",...]
  // the follow sets of ["RuleB","RuleC",...] need to be merged into the follow set of "RuleA"
  private final HashMap<String, HashSet<String>> followSetsToBeMergedFrom = new HashMap<>();

  @Nullable
  private String currentRuleName;
  @Nullable
  private List<AsmGrammarElementDefinition> successors;

  public FollowSetSetComputer(FirstSetComputer firstSetComputer) {
    this.firstSetComputer = firstSetComputer;
  }

  public Map<String, Set<AsmToken>> getFollowSets() {
    return followSetCache;
  }

  public void computeFollowSets(List<AsmGrammarRuleDefinition> rules) {
    // compute follow from first e.g. add first(C) to follow(B) for A : B C;
    for (var rule : rules) {
      currentRuleName = rule.identifier().name;
      rule.accept(this);
    }

    // complete follow sets e.g. add follow(A) to follow(B) for A : B;
    for (var rule : rules) {
      if (!rule.isBuiltinRule) {
        currentRuleName = rule.identifier().name;
        complete(rule.identifier().name);
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
          successors = alternative.subList(i + 1, alternative.size());
        } else {
          successors = null;
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

      invokedRuleFollowSet.addAll(firstSetComputer.computeFirstSetOfGroup(successors));

      if (successors == null || deletableComputer.areAllElementsDeletable(successors)) {
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
    if (rule.isTerminalRule || rule.isBuiltinRule) {
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
