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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import vadl.configuration.GeneralConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.annotations.AsmGenerateRulesAnno;
import vadl.viam.asm.AsmToken;
import vadl.viam.asm.elements.AsmAlternative;
import vadl.viam.asm.elements.AsmRuleInvocation;
import vadl.viam.asm.rules.AsmGrammarRule;
import vadl.viam.asm.rules.AsmNonTerminalRule;
import vadl.viam.graph.control.ReturnNode;

/**
 * A pass that generates assembly grammar rules per instruction,
 * based on their assembly printing function.
 */
public class AsmGrammarRuleGenerationPass extends Pass {

  public AsmGrammarRuleGenerationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("AsmGrammarRuleGenerationPass");
  }

  @CheckForNull
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    var assemblyDescription = viam.assemblyDescription().get();

    var shouldGenerateRulesAnno = assemblyDescription.annotation(AsmGenerateRulesAnno.class);
    if (shouldGenerateRulesAnno == null || !shouldGenerateRulesAnno.shouldGenerateRules()) {
      return null;
    }

    var generatedRules = viam.isa().get().ownInstructions().stream()
        .filter(instruction -> instruction.simpleName().equals("ADD"))
        .map(
            instruction -> {
              var returnNodes =
                  instruction.assembly().function().behavior().getNodes(ReturnNode.class).toList();
              var returnNode = returnNodes.getFirst();

              var ruleGenerator = new AsmGrammarRuleGenerator();
              var ctx = new AsmRuleContext(instruction);
              AsmGrammarRuleGeneratorDispatcher.dispatch(ruleGenerator, ctx, returnNode);

              return new Pair<>(instruction, ctx.builtRule);
            }
        ).toList();


    var instructionRule = (AsmNonTerminalRule) assemblyDescription.rules().stream()
        .filter(rule -> rule.simpleName().equals("Instruction"))
        .findFirst()
        .orElseThrow();

    var conflictingRules = new ArrayList<Pair<Instruction, AsmGrammarRule>>();
    computeConflictingRules(instructionRule, generatedRules, conflictingRules);

    var nonConflictingRules = generatedRules.stream()
        .filter(p -> !conflictingRules.contains(p))
        .toList();

    // Add invocations of the generated rules to the alternatives of the Instruction rule
    instructionRule.getAlternatives().alternatives().addAll(
        nonConflictingRules.stream()
            .map(r -> (AsmNonTerminalRule) r.right())
            .map(rule -> ruleInvocationInAlternative(rule,
                rule.getAlternatives().alternatives().getFirst()
                    .firstTokens()))
            .toList()
    );

    // Add the generated rules to the assembly description
    var allRules = new ArrayList<>(assemblyDescription.rules());
    allRules.addAll(nonConflictingRules.stream().map(Pair::right).toList());
    assemblyDescription.setRules(allRules);

    return null;
  }

  private void computeConflictingRules(AsmNonTerminalRule instructionRule,
                                       List<Pair<Instruction, AsmGrammarRule>>
                                           generatedRules,
                                       ArrayList<Pair<Instruction, AsmGrammarRule>>
                                           conflictingRules) {
    for (var alternative : instructionRule.getAlternatives().alternatives()) {
      for (var generatedPair : generatedRules) {
        var generatedRule = (AsmNonTerminalRule) generatedPair.right();
        for (var generatedAlternative : generatedRule.getAlternatives()
            .alternatives()) {

          var intersection = alternative.firstTokens().stream().filter(
              token -> generatedAlternative.firstTokens().contains(token)
          ).toList();

          if (!intersection.isEmpty()) {
            var conflictingRule = ((AsmRuleInvocation) alternative.elements().getFirst()).rule();
            conflictingRules.add(generatedPair);

            reportWarningForConflictingRule(generatedPair, intersection, conflictingRule);
          }
        }
      }
    }
  }

  private void reportWarningForConflictingRule(Pair<Instruction, AsmGrammarRule> generatedPair,
                                               List<AsmToken> intersection,
                                               AsmGrammarRule conflictingRule) {
    DeferredDiagnosticStore.add(
        Diagnostic.warning("Cannot generate assembly grammar rule for instruction: "
                + generatedPair.left().simpleName(), generatedPair.left())
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
}
