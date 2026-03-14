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

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.types.asmTypes.InstructionAsmType;
import vadl.utils.Pair;
import vadl.utils.SourceLocation;
import vadl.viam.AssemblyDescription;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.PrintableInstruction;
import vadl.viam.Specification;
import vadl.viam.annotations.AsmGenerateRulesAnno;
import vadl.viam.asm.AsmToken;
import vadl.viam.asm.elements.AsmAlternative;
import vadl.viam.asm.elements.AsmAlternatives;
import vadl.viam.asm.elements.AsmAssignToAttribute;
import vadl.viam.asm.elements.AsmGroup;
import vadl.viam.asm.elements.AsmRuleInvocation;
import vadl.viam.asm.rules.AsmGrammarRule;
import vadl.viam.asm.rules.AsmNonTerminalRule;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;

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

  public AsmGrammarRuleGenerationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("AsmGrammarRuleGenerationPass");
  }

  private int namingSequence = 0;

  @CheckForNull
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    // TODO: Later when rule generation is the default,
    //  create an assembly description if it does not exist
    if (viam.assemblyDescription().isEmpty()) {
      return null;
    }
    var assemblyDescription = viam.assemblyDescription().get();

    var shouldGenerateRulesAnno = assemblyDescription.annotation(AsmGenerateRulesAnno.class);
    if (shouldGenerateRulesAnno == null || !shouldGenerateRulesAnno.shouldGenerateRules()) {
      return null;
    }

    var generatedInstructionRules = viam.isa().get().ownInstructions().stream()
        .map(instruction -> mapToGeneratedRuleContextPair(instruction, assemblyDescription));

    var generatedPseudoRules = viam.isa().get().ownPseudoInstructions().stream()
        .map(instruction -> mapToGeneratedRuleContextPair(instruction, assemblyDescription));

    var generatedPairs = Stream.concat(generatedInstructionRules, generatedPseudoRules).toList();

    var instructionRule = getNonTerminalRule(assemblyDescription, "Instruction");

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
      PrintableInstruction instruction, AssemblyDescription assemblyDescription) {
    var returnNodes =
        instruction.assembly().function().behavior().getNodes(ReturnNode.class).toList();
    var returnNode = returnNodes.getFirst();

    var ruleGenerator = new AsmGrammarRuleGenerator(instruction,
        getNonTerminalRule(assemblyDescription, "Register"),
        getNonTerminalRule(assemblyDescription, "ImmediateOperand")
    );
    var ctx = new AsmRuleContext();
    AsmGrammarRuleGeneratorDispatcher.dispatch(ruleGenerator, ctx, returnNode);

    return new Pair<>(instruction, ctx);
  }

  /**
   * Compute LL(1) conflicts between user written grammar rules and rules generated in this pass.
   * A conflict arises when the {@link vadl.viam.asm.elements.AsmAlternative#firstTokens()} sets
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
  private List<Pair<PrintableInstruction, AsmRuleContext>> computeConflictingRules(
      AsmNonTerminalRule instructionRule,
      List<Pair<PrintableInstruction, AsmRuleContext>> generated) {
    var conflicting = new ArrayList<Pair<PrintableInstruction, AsmRuleContext>>();

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

  private AsmNonTerminalRule getNonTerminalRule(AssemblyDescription ad, String name) {
    var nonTerminal = ad.rules().stream().filter(rule -> rule.simpleName().equals(name)).findFirst()
        .orElseThrow();
    return (AsmNonTerminalRule) nonTerminal;
  }

  private List<AsmGrammarRule> mergeOverlappingRules(
      List<Pair<PrintableInstruction, AsmRuleContext>> generated) {
    var groupedByFirstToken =
        new HashMap<AsmToken, List<Pair<PrintableInstruction, AsmRuleContext>>>();
    // "ADD" -> ADD, ADD_S, ADD_L ---> "ADD" -> [ADD_S],[ADD,ADD_L]

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


    var groupedByFirstTokenThenBySyntax = new HashMap<AsmToken,
        HashMap<AsmSyntax, List<Pair<PrintableInstruction, AsmRuleContext>>>>();

    groupedByFirstToken.forEach((token, pairs) -> {

      var pairsGroupedBySyntax =
          new HashMap<AsmSyntax, List<Pair<PrintableInstruction, AsmRuleContext>>>();

      pairs.forEach(pair -> {
        var syntax = syntaxOf(pair.right());
        pairsGroupedBySyntax.putIfAbsent(syntax, new ArrayList<>());
        pairsGroupedBySyntax.get(syntax).add(pair);
      });

      groupedByFirstTokenThenBySyntax.put(token, pairsGroupedBySyntax);
    });

    var rulesAfterMerging = new ArrayList<AsmGrammarRule>();

    groupedByFirstTokenThenBySyntax.forEach((token, syntaxKinds) -> {

      if (syntaxKinds.size() == 1) {
        // TODO: Implement handling of cases where there is more than one generated instruction
        //       per syntax kind
        var generatedPairs = syntaxKinds.values().iterator().next();
        if (generatedPairs.size() == 1) {
          rulesAfterMerging.addAll(
              generatedPairs.stream().map(pair -> pair.right().builtRule).toList());
        }
        return;
      }

      var sortedByTokenSetSizeDesc = syntaxKinds.entrySet().stream().sorted(
          Comparator.comparingInt((Map.Entry<AsmSyntax, ?> e) -> e.getKey().numberOfTokenSets())
              .reversed()).toList();

      var alternatives = new ArrayList<AsmAlternative>();

      for (int i = 0; i < sortedByTokenSetSizeDesc.size() - 1; i++) {
        var currentKind = sortedByTokenSetSizeDesc.get(i);
        var nextKind = sortedByTokenSetSizeDesc.get(i + 1);

        var nonOverlapIndex = currentKind.getKey().firstNonOverlappingIndex(nextKind.getKey());
        var nonOverlapTokens = currentKind.getKey().getTokenSets().get(nonOverlapIndex);

        var laIdInArgs = nonOverlapTokens.stream().map(AsmToken::getStringLiteral)
            .filter(Objects::nonNull).toList();

        var semPredFunction = buildSemPredFunction(nonOverlapIndex - 1, laIdInArgs);

        // TODO: Implement handling of cases where there is more than one generated instruction
        //       per syntax kind
        var builtRuleAlternative = ((AsmNonTerminalRule) currentKind.getValue().getFirst()
            .right().builtRule).getAlternatives().alternatives().getFirst();

        alternatives.add(
            wrapInGroupWithCastToInstructionAsmType(builtRuleAlternative, semPredFunction));
      }

      // add last alternative without semantic predicate
      var lastKind = sortedByTokenSetSizeDesc.getLast();
      var lastBuiltAlternative =
          ((AsmNonTerminalRule) lastKind.getValue().getFirst().right().builtRule).getAlternatives()
              .alternatives().getFirst();

      alternatives.add(wrapInGroupWithCastToInstructionAsmType(lastBuiltAlternative, null));


      rulesAfterMerging.add(buildRuleWithMergedAlternatives(token, alternatives));
    });

    return rulesAfterMerging;
  }

  private AsmAlternative wrapInGroupWithCastToInstructionAsmType(AsmAlternative alternative,
                                                                 @Nullable
                                                                 Function semPredFunction) {
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
        Identifier.noLocation(firstToken.getStringLiteral() + "_MERGED_RULE"),
        new AsmAlternatives(alternatives, InstructionAsmType.instance()),
        InstructionAsmType.instance(),
        SourceLocation.INVALID_SOURCE_LOCATION
    );
  }

  private Function buildSemPredFunction(int lookupIndex, List<String> args) {
    var indexNode = new ConstantNode(
        Constant.Value.fromInteger(BigInteger.valueOf(lookupIndex), Type.unsignedInt(64)));
    var argNodes = Stream.concat(
        Stream.of(indexNode),
        args.stream().map(arg -> new ConstantNode(new Constant.Str(arg)))
    ).toList();

    var builtinCallNode =
        new AsmBuiltInCall(BuiltInTable.LA_ID_IN, new NodeList<>(argNodes), Type.bool());

    var graph = new Graph("generated_sem_pred_behavior" + namingSequence++);
    graph.addWithInputs(new ReturnNode(builtinCallNode));

    return new Function(Identifier.noLocation("sem_pred_" + namingSequence++),
        new vadl.viam.Parameter[0], Type.bool(), graph);
  }

  private AsmSyntax syntaxOf(AsmRuleContext ctx) {
    List<Set<AsmToken>> tokenSets = ctx.elements.stream()
        .map(Pair::right)
        .filter(set -> !set.isEmpty())
        .toList();
    return new AsmSyntax(tokenSets);
  }
}
