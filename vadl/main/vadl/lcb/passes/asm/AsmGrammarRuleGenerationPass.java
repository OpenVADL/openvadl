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

    // Add invocations of the generated rules to the alternatives of the Instruction rule
    instructionRule.getAlternatives().alternatives().addAll(
        nonConflictingRules.stream()
            .map(r -> (AsmNonTerminalRule) r.right().builtRule)
            .map(rule -> ruleInvocationInAlternative(rule,
                rule.getAlternatives().alternatives().getFirst()
                    .firstTokens()))
            .toList()
    );

    // Add the generated rules to the assembly description
    var allRules = new ArrayList<>(assemblyDescription.rules());
    allRules.addAll(nonConflictingRules.stream().map(p -> p.right().builtRule).toList());
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
}
