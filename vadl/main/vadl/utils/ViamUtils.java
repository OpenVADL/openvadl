package vadl.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * A set of utility methods that helps when working with the VIAM.
 */
public class ViamUtils {

  /**
   * Finds a Definition by its identifier and type.
   *
   * <p>Example:
   * {@code findDefinitionByIdentAndType(spec,
   * "RV3264IM::RTYPE::imm", Format.Field.class)}
   *
   * @param root            The root Definition to start the search from.
   * @param identifier      The name of the Definition to find.
   * @param definitionClass The type of the Definition to find.
   * @return The first Definition with the given identifier and type, or null if not found.
   */
  @Nullable
  public Definition findDefinitionByIdentAndType(Definition root, String identifier,
                                                 Class<Definition> definitionClass) {
    var result = findDefinitionByFilter(root,
        (def) -> definitionClass.isInstance(def) && def.identifier.name().equals(identifier));

    root.ensure(result.size() > 1,
        "More than 1 definitions with name %s and type %s found in this root definition",
        identifier, definitionClass.getSimpleName());

    if (result.isEmpty()) {
      return null;
    } else {
      return result.stream().findFirst().get();
    }
  }

  /**
   * Finds all Definitions that match the given filter.
   *
   * @param root   The root Definition to start the search from.
   * @param filter The filter Function to check if a Definition should be included in the result.
   * @return A Set of Definitions that match the given filter.
   */
  public static Set<Definition> findDefinitionByFilter(Definition root,
                                                       Function<Definition, Boolean> filter) {
    return new DefinitionVisitor.Recursive() {

      private Set<Definition> allDefs = new HashSet<>();

      public Set<Definition> findAllIn(Definition definition) {
        definition.accept(this);
        return this.allDefs;
      }

      @Override
      public void beforeTraversal(Definition definition) {
        super.beforeTraversal(definition);
        if (filter.apply(definition)) {
          allDefs.add(definition);
        }
      }
    }.findAllIn(root);
  }
}