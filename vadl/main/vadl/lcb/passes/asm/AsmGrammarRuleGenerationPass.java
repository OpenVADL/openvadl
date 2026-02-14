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
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import vadl.configuration.GeneralConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.AssemblyDescription;
import vadl.viam.PrintableInstruction;
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

    if (viam.assemblyDescription().isEmpty()) {
      return null;
    }
    var assemblyDescription = viam.assemblyDescription().get();

    var shouldGenerateRulesAnno = assemblyDescription.annotation(AsmGenerateRulesAnno.class);
    if (shouldGenerateRulesAnno == null || !shouldGenerateRulesAnno.shouldGenerateRules()) {
      return null;
    }

    // TODO: preprocess printing functions / detect unsupported forms

    var generatedInstructionRules = viam.isa().get().ownInstructions().stream()
        // TODO: remove filter
//        .filter(instruction -> instruction.simpleName().equals("ADD")
//            || instruction.simpleName().equals("ANDI"))
        .map(instruction -> mapToGeneratedRulePair(instruction, assemblyDescription));

    var generatedPseudoRules = viam.isa().get().ownPseudoInstructions().stream()
        // TODO: remove filter
//        .filter(instruction -> instruction.simpleName().equals("JR")
//            || instruction.simpleName().equals("J"))
        .map(instruction -> mapToGeneratedRulePair(instruction, assemblyDescription));

    var generatedRules = Stream.concat(generatedInstructionRules, generatedPseudoRules).toList();

    var instructionRule = getNonTerminalRule(assemblyDescription, "Instruction");

    var conflictingRules = computeConflictingRules(instructionRule, generatedRules);

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

  private Pair<PrintableInstruction, AsmGrammarRule> mapToGeneratedRulePair(
      PrintableInstruction instruction, AssemblyDescription assemblyDescription) {
    var returnNodes =
        instruction.assembly().function().behavior().getNodes(ReturnNode.class).toList();
    var returnNode = returnNodes.getFirst();

    var ruleGenerator = new AsmGrammarRuleGenerator(
        getNonTerminalRule(assemblyDescription, "Register"),
        getNonTerminalRule(assemblyDescription, "ImmediateOperand")
    );
    var ctx = new AsmRuleContext(instruction);
    AsmGrammarRuleGeneratorDispatcher.dispatch(ruleGenerator, ctx, returnNode);

    return new Pair<>(instruction, ctx.builtRule);
  }

  private List<Pair<PrintableInstruction, AsmGrammarRule>> computeConflictingRules(
      AsmNonTerminalRule instructionRule,
      List<Pair<PrintableInstruction, AsmGrammarRule>> generated) {
    var conflicting = new ArrayList<Pair<PrintableInstruction, AsmGrammarRule>>();

    for (var alternative : instructionRule.getAlternatives().alternatives()) {
      for (var generatedPair : generated) {
        var generatedRule = (AsmNonTerminalRule) generatedPair.right();
        for (var generatedAlternative : generatedRule.getAlternatives()
            .alternatives()) {

          var intersection = alternative.firstTokens().stream().filter(
              token -> generatedAlternative.firstTokens().contains(token)
          ).toList();

          if (!intersection.isEmpty()) {
            var conflictingRule = ((AsmRuleInvocation) alternative.elements().getFirst()).rule();
            conflicting.add(generatedPair);

            reportWarningForConflictingRule(generatedPair, intersection, conflictingRule);
          }
        }
      }
    }
    return conflicting;
  }

  private void reportWarningForConflictingRule(
      Pair<PrintableInstruction, AsmGrammarRule> generatedPair,
      List<AsmToken> intersection,
      AsmGrammarRule conflictingRule) {
    DeferredDiagnosticStore.add(
        Diagnostic.warning("Cannot generate assembly grammar rule for instruction: "
                + generatedPair.left().identifier().simpleName(), generatedPair.left().assembly())
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
}
