package vadl.configuration;

/**
 * This configuration holds information for all passes.
 */
public class GeneralConfiguration {
  private final String outputPath;
  private final boolean doDump;

  public GeneralConfiguration(String outputPath, boolean doDump) {
    this.outputPath = outputPath;
    this.doDump = doDump;
  }

  public String outputPath() {
    return outputPath;
  }

  public boolean doDump() {
    return doDump;
  }
}
