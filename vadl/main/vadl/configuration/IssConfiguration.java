package vadl.configuration;

import vadl.iss.passes.tcgLowering.Tcg_32_64;

/**
 * The configurations required to control the generation of the ISS (QEMU).
 * Some settings may be added by the {@link vadl.iss.passes.IssConfigurationPass}
 * if they are not statically available.
 */
public class IssConfiguration extends GeneralConfiguration {

  // is set by the IssConfigurationPass
  private String architectureName;
  private boolean insn_counting;
  private Tcg_32_64 targetSize;

  /**
   * Constructs a {@link IssConfiguration}.
   */
  public IssConfiguration(GeneralConfiguration generalConfig) {
    super(generalConfig);
    architectureName = "unknown";
    insn_counting = false;
    targetSize = Tcg_32_64.i64;
  }

  /**
   * @param insn_counting used to determine if the iss generates add instruction for special
   *                            cpu register (QEMU)
   */
  public IssConfiguration(GeneralConfiguration generalConfig, boolean insn_counting) {
    super(generalConfig);
    this.architectureName = "unknown";
    this.insn_counting = insn_counting;
    targetSize = Tcg_32_64.i64;

  }

  public static IssConfiguration from(GeneralConfiguration generalConfig) {
    return new IssConfiguration(generalConfig);
  }

  public static IssConfiguration from(GeneralConfiguration generalConfig, boolean insn_counting) {
    return new IssConfiguration(generalConfig, insn_counting);
  }

  public String architectureName() {
    return architectureName;
  }

  public Tcg_32_64 targetSize() {
    return targetSize;
  }

  public void setArchitectureName(String architectureName) {
    this.architectureName = architectureName;
  }

  public boolean isInsnCounting() {
    return insn_counting;
  }

  public void setInsnCounting(boolean insn_counting) {
    this.insn_counting = insn_counting;
  }

  public void setTargetSize(Tcg_32_64 targetSize) {
    this.targetSize = targetSize;
  }
}
