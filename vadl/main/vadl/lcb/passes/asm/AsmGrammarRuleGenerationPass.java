// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.lcb.passes.asm;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.types.asmTypes.ConstantAsmType;
import vadl.types.asmTypes.InstructionAsmType;
import vadl.types.asmTypes.OperandAsmType;
import vadl.utils.Pair;
import vadl.utils.SourceLocation;
import vadl.viam.AssemblyDescription;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.PrintableInstruction;
import vadl.viam.Specification;
import vadl.viam.annotations.AsmGenerateRulesAnno;
import vadl.viam.asm.AsmToken;
import vadl.viam.asm.elements.AsmAlternative;
import vadl.viam.asm.elements.AsmAlternatives;
import vadl.viam.asm.elements.AsmAssignToAttribute;
import vadl.viam.asm.elements.AsmAssignToLocalVar;
import vadl.viam.asm.elements.AsmGrammarElement;
import vadl.viam.asm.elements.AsmGroup;
import vadl.viam.asm.elements.AsmLocalVarDefinition;
import vadl.viam.asm.elements.AsmLocalVarUse;
import vadl.viam.asm.elements.AsmRuleInvocation;
import vadl.viam.asm.elements.HasAssignTo;
import vadl.viam.asm.rules.AsmGrammarRule;
import vadl.viam.asm.rules.AsmNonTerminalRule;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.passes.NormalizeFieldsToFieldAccessFunctionsPass;

/**
 * A pass that generates assembly grammar rules per instruction,
 * based on their assembly printing function.
 * To illustrate with an example, consider this assembly printing function for an ADD instruction:
 * <pre>
 * {@code assembly ADD = ("ADD", " ", register(rd), ",", register(rs1), ",", register(rs2))}
 * </pre>
 * This pass uses this printing function to generate an assembly grammar rule like:
 * <pre>{@code
 *  AddInstruction @instruction:
 *    mnemonic = "ADD" @operand
 *    rd = Register@operand ","
 *    rs1 = Register@operand ","
 *    rs2 = Register@operand
 *  ;
 * }</pre>
 * The overall purpose is to relieve the user from writing grammar rules for instructions,
 * by inferring them from printing functions when possible.
 */
public class AsmGrammarRuleGenerationPass extends Pass {

  @Nullable
  private AssemblyDescription assemblyDescription;

  private final String mergingConstantLocalVar = "constant_var";
  private final String mergedRuleSuffix = "_MERGED_RULE";

  public AsmGrammarRuleGenerationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("AsmGrammarRuleGenerationPass");
  }

  private int namingSequence = 0;

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    // TODO: Later when rule generation is the default,
    //  create an assembly description if it does not exist
    if (viam.assemblyDescription().isEmpty()) {
      return null;
    }
    this.assemblyDescription = viam.assemblyDescription().get();

    var shouldGenerateRulesAnno = assemblyDescription.annotation(AsmGenerateRulesAnno.class);
    if (shouldGenerateRulesAnno == null || !shouldGenerateRulesAnno.shouldGenerateRules()) {
      return null;
    }

    var generatedInstructionRules = viam.isa().get().ownInstructions().stream()
        .map(this::mapToGeneratedRuleContextPair);

    var generatedPseudoRules = viam.isa().get().ownPseudoInstructions().stream()
        .map(this::mapToGeneratedRuleContextPair);

    var generatedPairs = Stream.concat(generatedInstructionRules, generatedPseudoRules).toList();

    var instructionRule = getNonTerminalRule("Instruction");

    var conflictingRules = computeConflictingRules(instructionRule, generatedPairs);

    var nonConflictingRules = generatedPairs.stream()
        .filter(p -> !conflictingRules.contains(p))
        .toList();

    var mergedRules = mergeOverlappingRules(nonConflictingRules);

    // Add invocations of the generated rules to the alternatives of the Instruction rule
    instructionRule.getAlternatives().alternatives().addAll(
        mergedRules.stream()
            .map(r -> (AsmNonTerminalRule) r)
            .map(rule -> ruleInvocationInAlternative(rule,
                rule.getAlternatives().alternatives().getFirst()
                    .firstTokens()))
            .toList()
    );

    // Add the generated rules to the assembly description
    var allRules = new ArrayList<>(assemblyDescription.rules());
    allRules.addAll(mergedRules);
    assemblyDescription.setRules(allRules);

    // Add the generated functions to the common definitions
    var allCommonDefinitions = new ArrayList<>(assemblyDescription.commonDefinitions());
    allCommonDefinitions.addAll(
        nonConflictingRules.stream().flatMap(p -> p.right().generatedFunctions.stream()).toList());
    assemblyDescription.setCommonDefinitions(allCommonDefinitions);

    return null;
  }

  private Pair<PrintableInstruction, AsmRuleContext> mapToGeneratedRuleContextPair(
      PrintableInstruction instruction) {
    var returnNodes =
        instruction.assembly().function().behavior().getNodes(ReturnNode.class).toList();
    var returnNode = returnNodes.getFirst();

    var ruleGenerator = new AsmGrammarRuleGenerator(instruction,
        getNonTerminalRule("Register"),
        getNonTerminalRule("ImmediateOperand")
    );
    var ctx = new AsmRuleContext();
    AsmGrammarRuleGeneratorDispatcher.dispatch(ruleGenerator, ctx, returnNode);

    return new Pair<>(instruction, ctx);
  }

  /**
   * Compute LL(1) conflicts between user written grammar rules and rules generated in this pass.
   * A conflict arises when the {@link AsmAlternative#firstTokens()} sets
   * of two rules overlap, i.e. an {@link AsmToken} is contained in the firstToken sets of both
   * rules. We cannot add conflicting rules to the parser, since then the parser would not able
   * to decide which rule to apply based on the parsed first token.
   * Therefore, we filter out any generated rules that conflict with a user written rule.
   *
   * <p>An example for a conflict would be the following scenario. First, we have an ADD
   * instruction with this assembly printing function:
   * <pre>
   * {@code assembly ADD = ("ADD", " ", register(rd), ",", register(rs1), ",", register(rs2))}
   * </pre>
   * Second, the user has written the following grammar rule for the instruction:
   * <pre>{@code
   *  AddInstruction @instruction:
   *    mnemonic = "ADD" @operand
   *    rd = Register@operand ","
   *    rs1 = Register@operand ","
   *    rs2 = Register@operand
   *  ;
   * }</pre></p>
   * This pass infers a rule for the assembly printing function with the first token set {"ADD"}.
   * But the user written rule {@code AddInstruction} also has the first token set {"ADD"}.
   * Since the first token sets overlap, we have a conflict and discard the generated rule.
   */
  private List<
      Pair<PrintableInstruction, AsmRuleContext>> computeConflictingRules(
      AsmNonTerminalRule instructionRule,
      List<Pair<PrintableInstruction, AsmRuleContext>> generated) {
    var conflicting =
        new ArrayList<Pair<PrintableInstruction, AsmRuleContext>>();

    for (var alternative : instructionRule.getAlternatives().alternatives()) {
      for (var generatedPair : generated) {
        var generatedRule = (AsmNonTerminalRule) generatedPair.right().builtRule;
        for (var generatedAlternative : generatedRule.getAlternatives()
            .alternatives()) {

          var intersection = alternative.firstTokens().stream().filter(
              token -> generatedAlternative.firstTokens().contains(token)
          ).toList();

          if (!intersection.isEmpty()) {
            var conflictingRule = ((AsmRuleInvocation) alternative.elements().getFirst()).rule();
            conflicting.add(generatedPair);

            reportWarningForConflictingRule(generatedPair.left(), intersection, conflictingRule);
          }
        }
      }
    }
    return conflicting;
  }

  private void reportWarningForConflictingRule(PrintableInstruction instruction,
                                               List<AsmToken> intersection,
                                               AsmGrammarRule conflictingRule) {
    DeferredDiagnosticStore.add(
        Diagnostic.warning("Cannot generate assembly grammar rule for instruction: "
                + instruction.identifier().simpleName(), instruction.assembly())
            .note("The overlapping first tokens are [%s]",
                String.join(", ", intersection.stream().map(AsmToken::toString).toList()))
            .locationDescription(conflictingRule,
                "Trying to generate an grammar rule leads to an LL(1) conflict"
                    + " with the user defined grammar rule %s.",
                conflictingRule.simpleName())
            .build()
    );
  }

  private AsmAlternative ruleInvocationInAlternative(AsmGrammarRule rule,
                                                     Set<AsmToken> firstTokens) {
    var invocation = new AsmRuleInvocation(null, rule, List.of(), rule.getAsmType());
    return new AsmAlternative(null, firstTokens, rule.getAsmType(), false, List.of(invocation));
  }

  private AsmNonTerminalRule getNonTerminalRule(String name) {
    var nonTerminal =
        requireNonNull(assemblyDescription).rules().stream()
            .filter(rule -> rule.simpleName().equals(name))
            .findFirst()
            .orElseThrow();
    return (AsmNonTerminalRule) nonTerminal;
  }

  /**
   * Merge generated rules starting with the same {@link AsmToken} into a single rule,
   * which contains semantic predicates to guide the parser which instruction to parse.
   * <p>As example consider an ISA which allows the following three versions of an ADDI instruction:
   * <pre>{@code
   *  addi rd, imm      // ADDI_S (rd is src & dest)
   *  addi rd, rs1, imm // ADDI   (11 bit immediate)
   *  addi rd, rs1, imm // ADDI_L (32 bit immediate)
   * }</pre></p>
   * As all three start with the token "addi" they need to be merged into a single rule to
   * avoid LL(1) conflicts.
   */
  private List<AsmGrammarRule> mergeOverlappingRules(
      List<Pair<PrintableInstruction, AsmRuleContext>> generated) {

    var groupedByFirstToken = groupByToken(generated);
    var groupedByFirstTokenThenBySyntax = groupByTokenThenBySyntax(groupedByFirstToken);

    return mergeRulesPerToken(groupedByFirstTokenThenBySyntax);
  }

  /**
   * To know which rules to merge, we first need to analyze which generated rules start with
   * the same {@link AsmToken}. This method groups generated rules on their first token
   * and returns a mapping from {@link AsmToken} to the list of rules starting with this token.
   * Illustrating on the example above this method conceptually creates the mapping
   * {@code "addi" -> [ADDI_S,ADDI,ADDI_L]}.
   */
  private Map<AsmToken,
      List<Pair<PrintableInstruction, AsmRuleContext>>> groupByToken(
      List<Pair<PrintableInstruction, AsmRuleContext>> generated) {
    var groupedByFirstToken =
        new HashMap<AsmToken,
            List<Pair<PrintableInstruction, AsmRuleContext>>>();

    generated.forEach(pair -> {
      var inst = pair.left();
      var builtCtx = pair.right();
      if (builtCtx.firstTokens.size() != 1) {
        throw Diagnostic.error("Instruction %s has multiple first tokens: [%s]".formatted(
                inst.identifier().simpleName(),
                builtCtx.firstTokens.stream().map(AsmToken::toString)
                    .collect(Collectors.joining(","))),
            inst.assembly()).build();
      }
      var token = pair.right().firstTokens.iterator().next();
      groupedByFirstToken.putIfAbsent(token, new ArrayList<>());
      groupedByFirstToken.get(token).add(pair);
    });
    return groupedByFirstToken;
  }

  /**
   * Within a token group we analyze the syntactic structure of rules and
   * introduce a second level grouping on {@link AsmSyntax}. The difference in syntactic
   * structure then serves as basis for the introduced semantic predicates.
   * Illustrating on the example above this method conceptually creates the mapping
   * {@code "addi" -> [[ADDI_S],[ADDI,ADDI_L]]} (Note: ADDI and ADDI_L are in the same subgroup,
   * since they are syntactically equivalent).
   */
  private Map<AsmToken,
      Map<AsmSyntax, List<Pair<PrintableInstruction, AsmRuleContext>>>> groupByTokenThenBySyntax(
      Map<AsmToken, List<Pair<PrintableInstruction, AsmRuleContext>>> groupedByFirstToken
  ) {
    var groupedByFirstTokenThenBySyntax = new HashMap<AsmToken,
        Map<AsmSyntax, List<Pair<PrintableInstruction, AsmRuleContext>>>>();

    groupedByFirstToken.forEach((token, pairs) -> {

      var pairsGroupedBySyntax =
          new HashMap<AsmSyntax,
              List<Pair<PrintableInstruction, AsmRuleContext>>>();

      pairs.forEach(pair -> {
        var syntax = syntaxOf(pair.right());
        pairsGroupedBySyntax.putIfAbsent(syntax, new ArrayList<>());
        pairsGroupedBySyntax.get(syntax).add(pair);
      });

      groupedByFirstTokenThenBySyntax.put(token, pairsGroupedBySyntax);
    });
    return groupedByFirstTokenThenBySyntax;
  }

  private AsmSyntax syntaxOf(AsmRuleContext ctx) {
    List<Set<AsmToken>> tokenSets = ctx.elements.stream()
        .map(Pair::right)
        .filter(set -> !set.isEmpty())
        .toList();
    return new AsmSyntax(tokenSets);
  }

  /**
   * Based on the grouping of generated rules, a single "_MERGED_RULE" per distinct
   * first {@link AsmToken} is created. Within this merged rule semantic predicates are used
   * to differentiate between the multiple syntax kinds per {@link AsmToken}.
   */
  private List<AsmGrammarRule> mergeRulesPerToken(
      Map<AsmToken,
          Map<AsmSyntax,
              List<Pair<PrintableInstruction, AsmRuleContext>>>>
          groupedByFirstTokenThenBySyntax) {
    var rulesAfterMerging = new ArrayList<AsmGrammarRule>();

    groupedByFirstTokenThenBySyntax.forEach((token, syntaxKinds) -> {

      if (syntaxKinds.size() == 1) {
        var generatedPairs = syntaxKinds.values().iterator().next();
        if (generatedPairs.size() == 1) {
          rulesAfterMerging.addAll(
              generatedPairs.stream().map(pair -> pair.right().builtRule).toList());
        } else {
          var mergedAlternative = semanticRuleMerge(generatedPairs, null);
          var resultRule = new AsmNonTerminalRule(
              Identifier.noLocation(token.getStringLiteral() + mergedRuleSuffix),
              new AsmAlternatives(List.of(mergedAlternative), mergedAlternative.asmType()),
              mergedAlternative.asmType(), SourceLocation.INVALID_SOURCE_LOCATION);
          rulesAfterMerging.add(resultRule);
        }
        return;
      }

      var alternatives = buildAlternativePerSyntaxKind(syntaxKinds);
      rulesAfterMerging.add(buildRuleWithMergedAlternatives(token, alternatives));
    });

    return rulesAfterMerging;
  }

  private List<AsmAlternative> buildAlternativePerSyntaxKind(
      Map<AsmSyntax,
          List<Pair<PrintableInstruction, AsmRuleContext>>> syntaxKinds) {
    var sortedByTokenSetSizeDesc = syntaxKinds.entrySet().stream().sorted(
        Comparator.comparingInt((Map.Entry<AsmSyntax, ?> e) -> e.getKey().numberOfTokenSets())
            .reversed()).toList();

    var alternatives = new ArrayList<AsmAlternative>();

    for (int i = 0; i < sortedByTokenSetSizeDesc.size() - 1; i++) {
      var currentKind = sortedByTokenSetSizeDesc.get(i);
      var nextKind = sortedByTokenSetSizeDesc.get(i + 1);

      var nonOverlapIndex = currentKind.getKey().firstNonOverlappingIndex(nextKind.getKey());
      var nonOverlapTokenSet = currentKind.getKey().getTokenSets().get(nonOverlapIndex);
      var semPredFunction = buildSemPredFunction(nonOverlapIndex, nonOverlapTokenSet);

      AsmAlternative mergedAlternative;
      var currentKindRules = currentKind.getValue();

      if (currentKindRules.size() == 1) {
        mergedAlternative = ((AsmNonTerminalRule) currentKind.getValue().getFirst()
            .right().builtRule).getAlternatives().alternatives().getFirst();
        alternatives.add(
            wrapAlternativeInGroupWithInstructionAsmType(mergedAlternative, semPredFunction));
      } else {
        mergedAlternative = semanticRuleMerge(currentKindRules, semPredFunction);
        alternatives.add(mergedAlternative);
      }
    }

    // Add last alternative without semantic predicate
    var lastKind = sortedByTokenSetSizeDesc.getLast();
    var lastBuiltAlternative =
        ((AsmNonTerminalRule) lastKind.getValue().getFirst().right().builtRule)
            .getAlternatives().alternatives().getFirst();
    alternatives.add(
        wrapAlternativeInGroupWithInstructionAsmType(lastBuiltAlternative, null));

    return alternatives;
  }

  private AsmAlternative wrapAlternativeInGroupWithInstructionAsmType(
      AsmAlternative alternative, @Nullable Function semPredFunction) {
    var group = new AsmGroup(
        new AsmAssignToAttribute("inst", false),
        new AsmAlternatives(List.of(alternative), alternative.asmType()),
        false,
        InstructionAsmType.instance()
    );
    return new AsmAlternative(semPredFunction, alternative.firstTokens(),
        InstructionAsmType.instance(), false, List.of(group));
  }

  private AsmGrammarRule buildRuleWithMergedAlternatives(AsmToken firstToken,
                                                         List<AsmAlternative> alternatives) {
    return new AsmNonTerminalRule(
        Identifier.noLocation(firstToken.getStringLiteral() + mergedRuleSuffix),
        new AsmAlternatives(alternatives, InstructionAsmType.instance()),
        InstructionAsmType.instance(),
        SourceLocation.INVALID_SOURCE_LOCATION
    );
  }

  private Function buildSemPredFunction(int lookupIndex,
                                        Set<AsmToken> nonOverlapTokens) {
    var laIndexNode = new ConstantNode(
        Constant.Value.fromInteger(BigInteger.valueOf(lookupIndex),
            Type.unsignedInt(64)));

    // Use the LA_ID_IN builtin for string literal tokens
    var laIdInArgsList = nonOverlapTokens.stream().map(AsmToken::getStringLiteral)
        .filter(Objects::nonNull).map(arg -> new ConstantNode(new Constant.Str(arg)))
        .toList();

    // Use the LA_KIND_IN builtin for tokens that refer to a terminal rule
    var laKindInArgsList = nonOverlapTokens.stream().filter(t -> t.getStringLiteral() == null)
        .map(AsmToken::getRuleName).map(arg -> new ConstantNode(new Constant.Str(arg)))
        .toList();

    var graph = new Graph("generated_sem_pred_behavior" + namingSequence++);

    // Build the expression based on which token types are present
    var returnExpression = buildSemPredExpression(laIndexNode, laIdInArgsList, laKindInArgsList);
    graph.addWithInputs(new ReturnNode(returnExpression));

    return new Function(Identifier.noLocation("sem_pred_" + namingSequence++),
        new Parameter[0], Type.bool(), graph);
  }

  // If there is only one type of arguments, use the corresponding builtin call as expression
  // If there are both types of arguments use both builtins in an OR expression
  private ExpressionNode buildSemPredExpression(ConstantNode laIndexNode,
                                                List<ConstantNode> laIdInArgsList,
                                                List<ConstantNode> laKindInArgsList) {
    if (laKindInArgsList.isEmpty()) {
      return buildStringLiteralBuiltinCall(laIndexNode, laIdInArgsList);
    } else if (laIdInArgsList.isEmpty()) {
      return buildKindBuiltinCall(laIndexNode, laKindInArgsList);
    } else {
      var stringCall = buildStringLiteralBuiltinCall(laIndexNode, laIdInArgsList);
      var kindCall = buildKindBuiltinCall(laIndexNode, laKindInArgsList);
      return new BuiltInCall(BuiltInTable.OR,
          new NodeList<>(stringCall, kindCall), Type.bool());
    }
  }

  private AsmBuiltInCall buildStringLiteralBuiltinCall(ConstantNode laIndexNode,
                                                       List<ConstantNode> laIdInArgsList) {
    var laIdInArgsNodes = Stream.concat(Stream.of(laIndexNode), laIdInArgsList.stream()).toList();
    return new AsmBuiltInCall(BuiltInTable.LA_ID_IN, new NodeList<>(laIdInArgsNodes),
        Type.bool());
  }

  private AsmBuiltInCall buildKindBuiltinCall(ConstantNode laIndexNode,
                                              List<ConstantNode> laKindInArgsList) {
    var laKindInArgsNodes =
        Stream.concat(Stream.of(laIndexNode), laKindInArgsList.stream()).toList();
    return new AsmBuiltInCall(BuiltInTable.LA_KIND_IN, new NodeList<>(laKindInArgsNodes),
        Type.bool());
  }

  /**
   * If there are multiple instructions of a single syntax kind, they are merged into a single
   * rule by differentiating according to the first immediate operand.
   * <p>As example say there are three versions of an ADDI instruction ADDI_S, ADDI and ADDI_L,
   * but all with the same syntax: {@code addi rd, rs1, imm}. Upon parsing the assembler needs to
   * decide which of version of ADDI should be emitted.
   * </p>
   * <p>
   * To do this we create a merged rule where each version of the instruction is an alternative
   * guarded by a semantic predicate calling the predicate of the respective immediate operand.
   * The alternatives are ordered by instruction bit size ascending, such that the assembler
   * chooses the smallest instruction for which the predicate holds. In the case that the
   * immediate operand is a label, we fall back to the longest version of the instruction.
   * </p>
   * Conceptually the resulting merged rule then looks like this:
   * <pre>{@code
   * ADDI_MERGED_RULE @instruction :
   *   // parse up until immediate operand in question
   *   "ADDI"
   *   // parse other operands up until immediate into local vars
   *   inst = (
   *     // local var usages up to immediate
   *     mnemonic = addi_l_mnemonic<> @operand
   *     offset = Label @operand
   *     // parse operands after immediate
   *   ) @instrution
   *   |(
   *   var constant_var = Integer
   *   inst = ( ?(addi_s_imm_predicate(constant_var))
   *     // local var usages up to immediate
   *     mnemonic = addi_s_mnemonic<> @operand
   *     imm = constant_var @operand
   *     // parse operands after immediate
   *   ) @instruction
   *   | inst = ( ?(addi_imm_predicate(constant_var))
   *     // local var usages up to immediate
   *     mnemonic = addi_mnemonic<> @operand
   *     imm = constant_var @operand
   *     // parse operands after immediate
   *   ) @instruction
   *   | inst = (
   *     // local var usages up to immediate
   *     mnemonic = addi_l_mnemonic<> @operand
   *     imm = constant_var @operand // if val does not fit, error gets thrown in parser
   *     // parse operands after immediate
   *   ) @instruction
   *   )
   * ;
   * }</pre>
   */
  private AsmAlternative semanticRuleMerge(
      List<Pair<PrintableInstruction, AsmRuleContext>> pairOfKind,
      @Nullable Function syntaxKindPredicate) {
    // Sort by total bitwidth ascending
    pairOfKind.sort(Comparator.comparingInt(p -> p.left().bitWidth()));

    var resultGrammarElements = new ArrayList<AsmGrammarElement>();
    var localVarUsages = new ArrayList<AsmLocalVarUse>();

    // If immediate is a label, use the longest instruction
    var longestInstruction = pairOfKind.getLast();

    var ruleElements = longestInstruction.right().getElements();
    var immediatePositionInRule = ruleElements.indexOf(ruleElements.stream().filter(
            e -> e instanceof AsmRuleInvocation r
                && r.rule().simpleName().equals("ImmediateOperand"))
        .findFirst().orElseThrow());

    var mnemonicElementOfLongest =
        buildElementsUpToImmediate(immediatePositionInRule, ruleElements,
            resultGrammarElements, localVarUsages);

    var labelAlternative =
        buildLabelDefaultAlternative(ruleElements, immediatePositionInRule,
            mnemonicElementOfLongest,
            localVarUsages);

    var constantAlternatives =
        buildConstantAlternatives(pairOfKind, localVarUsages, immediatePositionInRule,
            labelAlternative);

    resultGrammarElements.add(constantAlternatives);
    var firstTokens = pairOfKind.getFirst().right().firstTokens;

    return AsmGrammarRuleGenerator.createInstructionAlternative(syntaxKindPredicate,
        resultGrammarElements,
        firstTokens);
  }


  /**
   * Parse all elements up to immediate into local vars to later use in the concrete alternatives.
   */
  private AsmGrammarElement buildElementsUpToImmediate(
      int immediatePositionInRule, List<AsmGrammarElement> ruleElements,
      List<AsmGrammarElement> resultGrammarElements,
      List<AsmLocalVarUse> localVarUsages) {
    AsmGrammarElement mnemonicElementOfLongest = null;

    for (int i = 0; i < immediatePositionInRule; i++) {
      var elem = ruleElements.get(i);

      if (elem instanceof HasAssignTo elemWithAssignTo) {
        var assignTo = elemWithAssignTo.assignToElement();

        if (assignTo != null) {
          if (assignTo.getAssignToName().equals("mnemonic")) {
            mnemonicElementOfLongest = (AsmGrammarElement) elemWithAssignTo;
          } else {
            var localVarName = assignTo.getAssignToName() + "Tmp";
            var elementAsmType = elemWithAssignTo.getAsmType();
            var localVarDefinition =
                new AsmLocalVarDefinition(localVarName, elemWithAssignTo.copyAndOverwriteAssignTo(
                    new AsmAssignToLocalVar(localVarName, false)), elementAsmType);

            resultGrammarElements.add(localVarDefinition);
            localVarUsages.add(
                new AsmLocalVarUse(new AsmAssignToAttribute(assignTo.getAssignToName(), false),
                    localVarName, elementAsmType, elementAsmType));
          }

        } else {
          // If elem is not an attribute assignment use it as is in the merged rule
          resultGrammarElements.add(elem);
        }
      } else {
        // If elem is not an attribute assignment use it as is in the merged rule
        resultGrammarElements.add(elem);
      }
    }
    return requireNonNull(mnemonicElementOfLongest);
  }

  private AsmAlternative buildLabelDefaultAlternative(List<AsmGrammarElement> ruleElements,
                                                      int immediatePositionInRule,
                                                      AsmGrammarElement mnemonicElementOfLongest,
                                                      List<AsmLocalVarUse> localVarUsages) {
    var immediateOperandElement = ruleElements.get(immediatePositionInRule);
    var immediateOperandName =
        requireNonNull(((HasAssignTo) immediateOperandElement).assignToElement()).getAssignToName();

    var labelAlternativeElements = new ArrayList<AsmGrammarElement>();
    labelAlternativeElements.add(mnemonicElementOfLongest);

    var labelOperand = new AsmRuleInvocation(new AsmAssignToAttribute(immediateOperandName, false),
        getNonTerminalRule("Label"), List.of(), OperandAsmType.instance());

    labelAlternativeElements.add(labelOperand);
    labelAlternativeElements.addAll(localVarUsages);

    var elemsAfterImmediate =
        ruleElements.subList(immediatePositionInRule + 1, ruleElements.size());
    labelAlternativeElements.addAll(elemsAfterImmediate);

    var identifierTokenSet = Set.of(new AsmToken("IDENTIFIER", null));
    var innerLabelAlternative =
        AsmGrammarRuleGenerator.createInstructionAlternative(null, labelAlternativeElements,
            identifierTokenSet);

    var innerLabelAlternatives =
        new AsmAlternatives(List.of(innerLabelAlternative), innerLabelAlternative.asmType());

    var castLabelToInstruction = new AsmGroup(
        new AsmAssignToAttribute("inst", false),
        innerLabelAlternatives,
        false,
        InstructionAsmType.instance()
    );

    return new AsmAlternative(null, identifierTokenSet,
        InstructionAsmType.instance(), false, List.of(castLabelToInstruction));
  }

  private Set<AsmToken> integerRuleFirstTokens() {
    return getNonTerminalRule("Integer").getAlternatives().alternatives().stream().map(
        AsmAlternative::firstTokens).reduce(new HashSet<>(), (acc, tokens) -> {
          acc.addAll(tokens);
          return acc;
        }
    );
  }

  private AsmAlternatives buildConstantAlternatives(
      List<Pair<PrintableInstruction, AsmRuleContext>> pairOfKind,
      List<AsmLocalVarUse> localVarUsages, int immediatePositionInRule,
      AsmAlternative labelAlternative) {

    var constantLocalVarDefinition = new AsmLocalVarDefinition(mergingConstantLocalVar,
        new AsmRuleInvocation(new AsmAssignToLocalVar(mergingConstantLocalVar, false),
            getNonTerminalRule("Integer"), List.of(), ConstantAsmType.instance()),
        ConstantAsmType.instance());

    var constantAlternatives = new ArrayList<AsmAlternative>();

    // Add alternative per instruction variant, predicated with check on parsed Integer value
    for (int i = 0; i < pairOfKind.size(); i++) {
      var generatedElems = pairOfKind.get(i).right().getElements();

      var instructionRuleElements = new ArrayList<AsmGrammarElement>();

      // At index 0: mnemonic = mnemonic_function<>
      instructionRuleElements.add(generatedElems.getFirst());

      // Add already parsed operands
      var attributeElements = generatedElems.stream().filter(
              e -> e instanceof HasAssignTo hat && hat.assignToElement() != null
                  && !requireNonNull(hat.assignToElement()).getAssignToName().equals("mnemonic"))
          .map(e -> (HasAssignTo) e).toList();

      var localVarUsesForInstruction = new ArrayList<AsmGrammarElement>();
      for (int j = 0; j < localVarUsages.size(); j++) {
        var instructionAssignTo = attributeElements.get(j).assignToElement();
        localVarUsesForInstruction.add(
            localVarUsages.get(j).copyAndOverwriteAssignTo(requireNonNull(instructionAssignTo)));
      }
      instructionRuleElements.addAll(localVarUsesForInstruction);

      // Get elem from index to assign to concrete instruction attribute
      var immediateElem = generatedElems.get(immediatePositionInRule);
      var attributeName =
          requireNonNull(((HasAssignTo) immediateElem).assignToElement()).getAssignToName();
      var constantLocalVarUse =
          new AsmLocalVarUse(new AsmAssignToAttribute(attributeName, false),
              mergingConstantLocalVar,
              ConstantAsmType.instance(), OperandAsmType.instance());

      instructionRuleElements.add(constantLocalVarUse);
      instructionRuleElements.addAll(
          generatedElems.subList(immediatePositionInRule + 1, generatedElems.size()));

      // Last alternative with longest instruction is the default case (not guarded by predicate)
      Function semPred = null;
      if (i != pairOfKind.size() - 1) {
        semPred =
            buildMergingSemanticPredicate(pairOfKind.get(i).left(), attributeName);
      }

      // Get tokens from the next elem in the AsmSyntax
      var generatedElements = pairOfKind.get(i).right().elements;
      Set<AsmToken> nextTokens = Set.of(new AsmToken("EOL", null));
      if (generatedElements.size() > immediatePositionInRule + 1) {
        nextTokens = generatedElements.get(immediatePositionInRule + 1).right();
      }

      var instructionAlternative =
          AsmGrammarRuleGenerator.createInstructionAlternative(null, instructionRuleElements,
              nextTokens);

      var castToInstruction = new AsmGroup(
          new AsmAssignToAttribute("inst", false),
          new AsmAlternatives(List.of(instructionAlternative), instructionAlternative.asmType()),
          false,
          InstructionAsmType.instance()
      );

      constantAlternatives.add(
          new AsmAlternative(semPred, nextTokens, InstructionAsmType.instance(), false,
              List.of(castToInstruction)));
    }

    var constantAlternativesElement =
        new AsmAlternatives(constantAlternatives, InstructionAsmType.instance());

    var outerConstantAlternative =
        new AsmAlternative(null, integerRuleFirstTokens(), InstructionAsmType.instance(), false,
            List.of(constantLocalVarDefinition, constantAlternativesElement));


    var outerAlternativeList = List.of(labelAlternative, outerConstantAlternative);
    return new AsmAlternatives(outerAlternativeList, InstructionAsmType.instance());
  }

  private Function buildMergingSemanticPredicate(PrintableInstruction inst,
                                                 String immediateElemName) {
    var fieldOrAccess = inst.getFieldOrAccess(immediateElemName);
    if (fieldOrAccess == null) {
      throw Diagnostic.error(
          "Could not find field or field access with name %s on instruction %s".formatted(
              immediateElemName, inst.identifier().simpleName()), inst.location()).build();
    }

    //  std::optional<llvm::ParsedValue<int64_t>>
    // .value() to get value from optional
    // .Value to get the actual integer value from ParsedValue
    String localVarAccess = mergingConstantLocalVar + ".value().Value";
    Function predicateFunction;

    if (fieldOrAccess.isLeft()) {
      // operand assigned to field
      var field = fieldOrAccess.left();
      predicateFunction = getPredicateFromField(field);
    } else {
      // operand assigned to fieldAccess
      var fieldAccess = fieldOrAccess.right();
      predicateFunction = getPredicateFromFieldAccess(fieldAccess);
    }
    return replaceFieldAccessWithConstantVar(predicateFunction, localVarAccess);
  }

  // FIXME: This does not work in the case where a field is used in the printing
  //        function, but the behavior uses its access function.
  private Function getPredicateFromField(Format.Field field) {
    return field.format().fieldAccesses().stream().filter(
        fa -> fa instanceof NormalizeFieldsToFieldAccessFunctionsPass.GeneratedFieldAccess
            && fa.fieldRefs().contains(field)
    ).findFirst().get().predicate();
  }

  private Function getPredicateFromFieldAccess(Format.FieldAccess fieldAccess) {
    return fieldAccess.predicate();
  }

  private Function replaceFieldAccessWithConstantVar(Function predicate, String argument) {
    var fieldAccess = predicate.behavior().getNodes(FieldAccessRefNode.class).findFirst().get();
    fieldAccess.replace(new FuncParamNode(
        new Parameter(Identifier.noLocation(argument), BitsType.bits(64), 0)));

    return predicate;
  }
}
