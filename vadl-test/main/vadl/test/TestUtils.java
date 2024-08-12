package vadl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import vadl.utils.ViamUtils;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Format;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Resource;
import vadl.viam.Specification;

public class TestUtils {

  /**
   * Finds a {@link Resource} with the given name in the specified {@link Specification}.
   *
   * @param name the name of the resource to find
   * @param spec the specification containing the resource
   * @return the resource with the given name
   * @throws AssertionError if the number of resources found with the given name is not equal to 1
   */
  public static Resource findResourceByName(String name, Specification spec) {
    var r = spec.definitions()
        .filter(InstructionSetArchitecture.class::isInstance)
        .map(InstructionSetArchitecture.class::cast)
        .flatMap(i -> Stream.concat(i.registers().stream(), i.registerFiles().stream()))
        .filter(i -> i.identifier.name().equals(name))
        .toList();

    assertEquals(1, r.size(), "Wrong number of resources with name " + name + " found");
    return r.get(0);
  }


  /**
   * Finds a {@link Format} with the given name in the specified {@link Specification}.
   *
   * @param name the name of the format to find
   * @param spec the specification containing the formats
   * @return the format with the given name
   * @throws AssertionError if the number of formats found with the given name is not equal to 1
   */
  public static Format findFormatByName(String name, Specification spec) {
    var formats = spec.findAllFormats()
        .filter(f -> f.identifier.name().equals(name))
        .toList();
    assertEquals(1, formats.size(), "Wrong number of formats with name " + name + " found");
    return formats.get(0);
  }

  /**
   * Finds a Definition with the given name in the specified Specification.
   *
   * @param name the name of the Definition to find
   * @param spec the Specification containing the Definitions
   * @return the Definition with the given name
   * @throws AssertionError if the number of Definitions found with the given name is not equal to 1
   */
  public static <T extends Definition> T findDefinitionByNameIn(String name, Definition spec,
                                                                Class<T> definitionClass) {
    var result = filterAllDefinitionsIn(spec,
        (def) -> definitionClass.isInstance(def) && def.identifier.name().equals(name));

    assertEquals(1, result.size(), "Wrong number of definitions with name " + name + " found");
    //noinspection unchecked
    return (T) result.toArray()[0];
  }


  private static Set<Definition> filterAllDefinitionsIn(Definition def,
                                                        Function<Definition, Boolean> filter) {
    return ViamUtils.findDefinitionByFilter(def, filter);
  }
}
