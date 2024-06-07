package vadl.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.error.VadlError;
import vadl.error.VadlException;


/**
 * A symbol table to hold variable definition and usages and verify the later.
 */
class SymbolTable {
  private final Map<String, Macro> macros = new HashMap<>();
  private final Map<String, Identifier> symbolDefinitions = new HashMap<>();
  private final List<Identifier> unresolvedUsages = new ArrayList<>();
  private final List<VadlError> errors = new ArrayList<>();

  /**
   * Adds a new symbol definition.
   *
   * @param identifier of the symbol being defined.
   */
  void addDefinition(Identifier identifier) {
    if (symbolDefinitions.containsKey(identifier.name)) {
      errors.add(new VadlError(
          "Redefinition of variable '%s' is not allowed".formatted(identifier.name),
          identifier.location(),
          "Variables cannot be redefined or reassigned and a variable with the same name was "
              + "already defined in %s".formatted(
              symbolDefinitions.get(identifier.name).location().toString()),
          null
      ));
      return;
    }

    symbolDefinitions.put(identifier.name, identifier);
  }

  /**
   * Adds a new usage of a symbol.
   * This always succeeds even if the symbol isn't defined yet because we allow usage before
   * definition.
   *
   * @param identifier of the symbol beeing used.
   */
  void addUsage(Identifier identifier) {
    if (symbolDefinitions.containsKey(identifier.name)) {
      return;
    }

    unresolvedUsages.add(identifier);
  }

  /**
   * Verifies that all usages are valid.
   *
   * @throws VadlException if not all usages have matching definitions.
   */
  void verifyAllUsages() {
    for (var unresovled : unresolvedUsages) {
      if (!symbolDefinitions.containsKey(unresovled.name)) {
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

  void addMacro(Macro macro) {
    if (macros.containsKey(macro.name().name)) {
      errors.add(new VadlError(
          "Redefinition of macro '%s' is not allowed".formatted(macro.name().name),
          macro.name().location(),
          "A macro with that name was already defined in: %s".formatted(
              macros.get(macro.name().name).name().location().toString()),
          null
      ));
    }

    macros.put(macro.name().name, macro);
  }

  @Nullable
  Macro getMacro(String name) {
    return macros.get(name);
  }
}
