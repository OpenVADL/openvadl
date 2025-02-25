package vadl.gcb.valuetypes;

import java.util.List;
import vadl.viam.Register;

/**
 * Extends the {@link Register} with information which a compiler requires.
 */
public abstract class CompilerRegister {
  protected final String name;
  protected final String asmName;
  protected final List<String> altNames;
  protected final int dwarfNumber;
  protected final int hwEncodingValue;

  /**
   * Constructor.
   */
  public CompilerRegister(String name,
                          String asmName,
                          List<String> altNames,
                          int dwarfNumber,
                          int hwEncodingValue) {
    this.name = name;
    this.asmName = asmName;
    this.altNames = altNames;
    this.dwarfNumber = dwarfNumber;
    this.hwEncodingValue = hwEncodingValue;
  }

  public String name() {
    return name;
  }

  public String asmName() {
    return asmName;
  }

  public List<String> altNames() {
    return altNames;
  }

  public int dwarfNumber() {
    return dwarfNumber;
  }

  public int hwEncodingValue() {
    return hwEncodingValue;
  }
}
