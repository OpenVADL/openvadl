package vadl.viam.annotations;

import vadl.viam.Annotation;
import vadl.viam.MicroProcessor;

/**
 * This annotation might be set on {@link MicroProcessor} definitions to mark it
 * HTIF (Host-Target Interface) compatible.
 * This will ensure that the generated ISS supports the HTIF protocol, so users can
 * use the tohost/fromhost variables in their ELF binary.
 */
public class EnableHtifAnno extends Annotation<MicroProcessor> {

  @Override
  public Class<MicroProcessor> parentDefinitionClass() {
    return MicroProcessor.class;
  }
}
