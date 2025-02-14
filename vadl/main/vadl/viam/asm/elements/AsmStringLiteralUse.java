package vadl.viam.asm.elements;

import javax.annotation.Nullable;
import vadl.types.asmTypes.AsmType;

/**
 * Represents the usage of a string literal in a grammar rule.
 */
public record AsmStringLiteralUse(@Nullable AsmAssignTo assignToElement,
                                  String value, AsmType asmType) implements AsmGrammarElement {
}
