package vadl.types.asmTypes;

import java.util.HashMap;
import java.util.Map;
import vadl.types.Type;

/**
 * An AsmType is used to type assembly grammar elements in the assembly description.
 *
 * <p>The following types are allowed to be used in the assembly grammar:</p>
 * <ul>
 *   <li>constant</li>
 *   <li>expression</li>
 *   <li>instruction</li>
 *   <li>modifier</li>
 *   <li>operand</li>
 *   <li>register</li>
 *   <li>string</li>
 *   <li>symbol</li>
 *   <li>void</li>
 *   <li>statements</li>
 *   <li>instructions</li>
 *   <li>operands</li>
 * </ul>
 *
 * <p>There is also a special GroupAsmType, used to type sequences of grammar elements.</p>
 */
public interface AsmType {
  /**
   * Mapping of AsmType names to their corresponding instances.
   *
   * @see AsmType
   */
  HashMap<String, AsmType> ASM_TYPES = new HashMap<>(Map.ofEntries(
      Map.entry(ConstantAsmType.instance().name(), ConstantAsmType.instance()),
      Map.entry(ExpressionAsmType.instance().name(), ExpressionAsmType.instance()),
      Map.entry(InstructionAsmType.instance().name(), InstructionAsmType.instance()),
      Map.entry(ModifierAsmType.instance().name(), ModifierAsmType.instance()),
      Map.entry(OperandAsmType.instance().name(), OperandAsmType.instance()),
      Map.entry(RegisterAsmType.instance().name(), RegisterAsmType.instance()),
      Map.entry(StringAsmType.instance().name(), StringAsmType.instance()),
      Map.entry(SymbolAsmType.instance().name(), SymbolAsmType.instance()),
      Map.entry(VoidAsmType.instance().name(), VoidAsmType.instance()),
      Map.entry(StatementsAsmType.instance().name(), StatementsAsmType.instance()),
      Map.entry(InstructionsAsmType.instance().name(), InstructionsAsmType.instance()),
      Map.entry(OperandsAsmType.instance().name(), OperandsAsmType.instance())
  ));

  /**
   * Check if the input string is a valid assembly grammar type.
   * The input string is valid if it is equal to the lowercase string representation
   * of any assembly grammar type.
   *
   * @param input the input string to check
   * @return true if the input string is a valid assembly type, false otherwise
   */
  static boolean isInputAsmType(String input) {
    return ASM_TYPES.containsKey(input);
  }

  /**
   * Get the AsmType corresponding to the given VADL type.
   *
   * @param operationalType the VADL type to get the AsmType for
   * @return the corresponding VADL Type
   * @throws UnsupportedOperationException if there is no AsmType for the given VADL type
   */
  static AsmType getAsmTypeFromOperationalType(Type operationalType) {
    if (operationalType == Type.signedInt(64)) {
      return ConstantAsmType.instance();
    } else if (operationalType == Type.string()) {
      return StringAsmType.instance();
    } else if (operationalType == Type.void_()) {
      return VoidAsmType.instance();
    } else {
      throw new UnsupportedOperationException("There is no VADL type for this AsmType.");
    }
  }

  /**
   * Check whether this AsmType can be cast to the given AsmType.
   *
   * @param to AsmType to be cast to
   * @return whether this AsmType can be cast to the given AsmType
   */
  boolean canBeCastTo(AsmType to);

  /**
   * Get the name of this AsmType.
   *
   * @return name of this AsmType
   */
  String name();

  /**
   * Get the corresponding type of the VADL type system.
   *
   * @return the corresponding VADL type
   * @throws UnsupportedOperationException if there is no VADL type for this AsmType
   */
  default Type toOperationalType() {
    throw new UnsupportedOperationException(
        "This AsmType does not have an corresponding VADL type.");
  }
}