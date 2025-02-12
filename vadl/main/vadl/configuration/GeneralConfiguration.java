package vadl.configuration;

import java.nio.file.Path;

/**
 * This configuration holds information for all passes.
 */
public class GeneralConfiguration {
  private final Path outputPath;
  private final boolean doDump;
  private boolean dryRun = false;


  public GeneralConfiguration(Path outputPath, boolean doDump) {
    this.outputPath = outputPath;
    this.doDump = doDump;
  }

  public GeneralConfiguration(GeneralConfiguration generalConfig) {
    this(generalConfig.outputPath, generalConfig.doDump);
  }

  public Path outputPath() {
    return outputPath;
  }

  public boolean doDump() {
    return doDump;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }
}
