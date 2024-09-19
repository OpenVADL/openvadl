package vadl.configuration;

public class IssConfiguration extends GeneralConfiguration {

  // is set by the IssConfigurationPass
  private String architectureName;

  public IssConfiguration(GeneralConfiguration generalConfig) {
    super(generalConfig);
    architectureName = "unknown";
  }

  public static IssConfiguration from(GeneralConfiguration generalConfig) {
    return new IssConfiguration(generalConfig);
  }

  public String architectureName() {
    return architectureName;
  }

  public void setArchitectureName(String architectureName) {
    this.architectureName = architectureName;
  }
}
