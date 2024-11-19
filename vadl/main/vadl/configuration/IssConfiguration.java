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
  private Tcg_32_64 targetSize;

  public IssConfiguration(GeneralConfiguration generalConfig) {
    super(generalConfig);
    architectureName = "unknown";
    targetSize = Tcg_32_64.i64;
  }

  public static IssConfiguration from(GeneralConfiguration generalConfig) {
    return new IssConfiguration(generalConfig);
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

  public void setTargetSize(Tcg_32_64 targetSize) {
    this.targetSize = targetSize;
  }
}
