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
  private boolean insnCount;
  private Tcg_32_64 targetSize;

  /**
   * Constructs a {@link IssConfiguration}.
   */
  public IssConfiguration(GeneralConfiguration generalConfig) {
    super(generalConfig);
    architectureName = "unknown";
    insnCount = false;
    targetSize = Tcg_32_64.i64;
  }

  /**
   * Constructs IssConfiguration.
   *
   * @param insnCount used to determine if the iss generates add instruction for special
   *                            cpu register (QEMU)
   */
  public IssConfiguration(GeneralConfiguration generalConfig, boolean insnCount) {
    super(generalConfig);
    this.architectureName = "unknown";
    this.insnCount = insnCount;
    targetSize = Tcg_32_64.i64;

  }

  public static IssConfiguration from(GeneralConfiguration generalConfig) {
    return new IssConfiguration(generalConfig);
  }

  public static IssConfiguration from(GeneralConfiguration generalConfig, boolean insnCounting) {
    return new IssConfiguration(generalConfig, insnCounting);
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
    return insnCount;
  }

  public void setInsnCounting(boolean insnCounting) {
    this.insnCount = insnCounting;
  }

  public void setTargetSize(Tcg_32_64 targetSize) {
    this.targetSize = targetSize;
  }
}
