package vadl.gcb.valuetypes;

import java.util.List;
import vadl.viam.Register;
import vadl.viam.RegisterFile;

/**
 * Like a {@link CompilerRegister} but it is the concrete implementation which are not indexed.
 * This distinction is important since not all {@link CompilerRegister} are indexed e.g. PC.
 * A {@link GeneralCompilerRegister} is exactly for registers like PC which are not defined over a
 * {@link RegisterFile}.
 */
public class GeneralCompilerRegister extends CompilerRegister {
  public GeneralCompilerRegister(Register register,
                                 String asmName,
                                 List<String> altNames,
                                 int dwarfNumber) {
    super(generateName(register), asmName, altNames, dwarfNumber, 0);
  }

  /**
   * Generate the internal compiler name from a {@link Register}.
   */
  public static String generateName(Register register) {
    return register.simpleName();
  }
}
