package vadl.lcb.passes;

import vadl.viam.Annotation;
import vadl.viam.Assembly;

/**
* Annotation to indicate that the immediate in the {@link Assembly} needs to be emitted
* before printing.
*/
public class EncodeAssemblyImmediateAnnotation extends Annotation<Assembly> {
  @Override
  public Class<Assembly> parentDefinitionClass() {
    return Assembly.class;
  }
}
