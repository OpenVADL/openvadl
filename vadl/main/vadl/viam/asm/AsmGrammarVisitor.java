package vadl.viam.asm;

import vadl.viam.asm.elements.AsmAlternative;
import vadl.viam.asm.elements.AsmAlternatives;
import vadl.viam.asm.elements.AsmAssignToAttribute;
import vadl.viam.asm.elements.AsmAssignToLocalVar;
import vadl.viam.asm.elements.AsmFunctionInvocation;
import vadl.viam.asm.elements.AsmGroup;
import vadl.viam.asm.elements.AsmLocalVarDefinition;
import vadl.viam.asm.elements.AsmLocalVarUse;
import vadl.viam.asm.elements.AsmOption;
import vadl.viam.asm.elements.AsmRepetition;
import vadl.viam.asm.elements.AsmRuleInvocation;
import vadl.viam.asm.elements.AsmStringLiteralUse;
import vadl.viam.asm.rules.AsmBuiltinRule;
import vadl.viam.asm.rules.AsmGrammarRule;
import vadl.viam.asm.rules.AsmNonTerminalRule;
import vadl.viam.asm.rules.AsmTerminalRule;

/**
 * Interface for visiting all classes of the asm grammar.
 */
public interface AsmGrammarVisitor {
  void visit(AsmGrammarRule rule);

  void visit(AsmBuiltinRule rule);

  void visit(AsmNonTerminalRule rule);

  void visit(AsmTerminalRule rule);

  void visit(AsmAlternative element);

  void visit(AsmAlternatives element);

  void visit(AsmAssignToAttribute element);

  void visit(AsmAssignToLocalVar element);

  void visit(AsmFunctionInvocation element);

  void visit(AsmGroup element);

  void visit(AsmLocalVarDefinition element);

  void visit(AsmLocalVarUse element);

  void visit(AsmOption element);

  void visit(AsmRepetition element);

  void visit(AsmRuleInvocation element);

  void visit(AsmStringLiteralUse element);
}
