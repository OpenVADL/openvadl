package vadl.configuration;

/**
 * This record defines some gcb specific LCB configuration.
 */
public class GcbConfiguration extends GeneralConfiguration {
  public GcbConfiguration(GeneralConfiguration generalConfiguration) {
    super(generalConfiguration);
  }

  public static GcbConfiguration from(GeneralConfiguration generalConfiguration) {
    return new GcbConfiguration(generalConfiguration);
  }
}
