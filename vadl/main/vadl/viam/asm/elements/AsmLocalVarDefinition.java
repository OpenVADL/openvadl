package vadl.viam.asm.elements;

import javax.annotation.Nullable;
import vadl.types.asmTypes.AsmType;

/**
 * Represents a local variable definition within a grammar rule.
 */
public record AsmLocalVarDefinition(String localVarName,
                                    @Nullable AsmGrammarElement asmLiteral, AsmType asmType)
    implements AsmGrammarElement {
}
