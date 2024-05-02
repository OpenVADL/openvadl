package vadl.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vadl.error.VadlError;
import vadl.error.VadlException;


/**
 * A symbol table to hold variable definition and usages and verify the later.
 */
public class SymbolTable {
  private Map<String, Identifier> definitions = new HashMap<>();
  private List<Identifier> unresolvedUsages = new ArrayList<>();
  private List<VadlError> errors = new ArrayList<>();

  /**
   * Adds a new symbol definition.
   *
   * @param identifier of the symbol being defined.
   */
  public void addDefinition(Identifier identifier) {
    if (definitions.containsKey(identifier.name)) {
      errors.add(new VadlError(
          "Redefinition of variable '%s' is not allowed".formatted(identifier.name),
          identifier.location(),
          "Variables cannot be redefined or reassigned and a variable with the same name was "
              + "already defined in %s".formatted(
              definitions.get(identifier.name).location().toString()),
          null
      ));
      return;
    }

    definitions.put(identifier.name, identifier);
  }

  /**
   * Adds a new usage of a symbol.
   * This always succeeds even if the symbol isn't defined yet because we allow usage before
   * definition.
   *
   * @param identifier of the symbol beeing used.
   */
  public void addUsage(Identifier identifier) {
    if (definitions.containsKey(identifier.name)) {
      return;
    }

    unresolvedUsages.add(identifier);
  }

  /**
   * Verifies that all usages are valid.
   *
   * @throws VadlException if not all usages have matching definitions.
   */
  public void verifyAllUsages() {
    for (var unresovled : unresolvedUsages) {
      if (!definitions.containsKey(unresovled.name)) {
        // FIXME: We could do some fancy stuff here searching the symbol table for similarly named
        // variables and suggesting them.
        errors.add(new VadlError(
            "Cannot find variable '%s'".formatted(unresovled.name),
            unresovled.location(),
            "No variable with such a name exists.",
            null
        ));
      }
    }

    if (!errors.isEmpty()) {
      throw new VadlException(errors);
    }
  }

}
