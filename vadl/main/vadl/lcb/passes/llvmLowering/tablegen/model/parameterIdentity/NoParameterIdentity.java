package vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity;

/**
* Some parameters like constants have no parameter identity.
*/
public class NoParameterIdentity extends ParameterIdentity {
  @Override
  public String render() {
    return "";
  }
}
