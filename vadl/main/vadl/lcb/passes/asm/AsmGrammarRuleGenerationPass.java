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
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
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

              return ctx.builtRule;
            }
        ).toList();

    // TODO: case if assembly description does not exist --> what is default behavior?

    var assemblyDescription = viam.assemblyDescription().get();
    var allRules = new ArrayList<>(assemblyDescription.rules());
    allRules.addAll(generatedRules);
    assemblyDescription.setRules(allRules);

    var instructionRule = (AsmNonTerminalRule) allRules.stream()
        .filter(rule -> rule.simpleName().equals("Instruction"))
        .findFirst()
        .orElseThrow();

    // Add the generated rules as alternatives to the Instruction rule
    instructionRule.getAlternatives().alternatives().addAll(
        generatedRules.stream()
            .map(r -> (AsmNonTerminalRule) r)
            .map(rule -> ruleInvocationInAlternative(rule,
                rule.getAlternatives().alternatives().getFirst()
                    .firstTokens()))
            .toList()
    );

    return null;
  }

  private AsmAlternative ruleInvocationInAlternative(AsmGrammarRule rule,
                                                     Set<AsmToken> firstTokens) {
    var invocation = new AsmRuleInvocation(null, rule, List.of(), rule.getAsmType());
    return new AsmAlternative(null, firstTokens, rule.getAsmType(), false, List.of(invocation));
  }
}
