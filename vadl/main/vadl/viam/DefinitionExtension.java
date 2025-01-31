package vadl.viam;

import javax.annotation.Nullable;

/**
 * Definition extensions allow passes to attach information to specific definitions.
 * Later on, this information can be directly accessed from a given VIAM definition.
 */
public abstract class DefinitionExtension<T extends Definition> {

  // set automatically when added to definition
  @Nullable
  private T extendingDefinition;

  public DefinitionExtension() {
  }

  /**
   * Returns the class of definition this extension extends.
   */
  public abstract Class<? extends Definition> extendsDefClass();

  /**
   * Returns the definition extended by this extension.
   * This must be called AFTER the extension was added to the definition.
   */
  public T extendingDef() {
    if (extendingDefinition == null) {
      throw new IllegalStateException("Extension not yet added to definition.");
    }
    return extendingDefinition;
  }

  /**
   * Sets the definition extended by this. This is only called by the
   * {@link Definition#attachExtension(DefinitionExtension)}.
   */
  protected void setExtendingDefinition(T def) {
    if (def == null) {
      throw new IllegalArgumentException("Extension already added to definition.");
    }
    extendingDefinition = def;
  }

}
