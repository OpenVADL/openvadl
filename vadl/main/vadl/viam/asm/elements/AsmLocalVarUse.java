package vadl.viam.asm.elements;

import javax.annotation.Nullable;
import vadl.types.asmTypes.AsmType;

/**
 * Represents the usage of a local variable in a grammar rule.
 */
public record AsmLocalVarUse(@Nullable AsmAssignTo assignToElement,
                             String invokedLocalVar,
                             AsmType invokedLocalVarType,
                             AsmType asmType)
    implements AsmGrammarElement {
}
