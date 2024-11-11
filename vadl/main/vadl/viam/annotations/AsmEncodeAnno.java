package vadl.viam.annotations;

import java.util.List;
import vadl.viam.Annotation;
import vadl.viam.Assembly;
import vadl.viam.Format;

/**
 * AsmEncodeAnno is a subclass of Annotation that provides the functionality to associate
 * a list of fields to encode with an Assembly definition.
 *
 * <p>Example:<pre>{@code
 *  [encode imm20 ]
 *  assembly LUI = (mnemonic, ' ', register( rd ), ',', hex( imm20 ))
 * }</pre></p>
 */
public class AsmEncodeAnno extends Annotation<Assembly> {

  private final List<Format.FieldAccess> fieldsToEncode;

  public AsmEncodeAnno(List<Format.FieldAccess> fieldsToEncode) {
    this.fieldsToEncode = fieldsToEncode;
  }

  public List<Format.FieldAccess> fieldsToEncode() {
    return fieldsToEncode;
  }

  @Override
  public Class<Assembly> parentDefinitionClass() {
    return Assembly.class;
  }
}
