package vadl.iss.passes.decode;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.decode.dto.ArgumentSet;
import vadl.iss.passes.decode.dto.Field;
import vadl.iss.passes.decode.dto.Format;
import vadl.iss.passes.decode.dto.Pattern;
import vadl.iss.passes.decode.dto.QemuDecodeLoweringPassResult;
import vadl.iss.passes.decode.dto.QemuDecodeResolveSymbolPassResult;
import vadl.iss.passes.decode.dto.SourceMapping;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Identifier;
import vadl.viam.Specification;

public class QemuDecodeSymbolResolvingPass extends AbstractIssPass {

  public QemuDecodeSymbolResolvingPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("QEMU Decode Symbol Resolving");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    final var prevResult = passResults.lastNullableResultOf(QemuDecodeLoweringPass.class);
    if (!(prevResult instanceof QemuDecodeLoweringPassResult res)) {
      return null;
    }

    // Every argument set is identified by its source format definition
    final var distinctArgSets =
        resolveNames(res.argSets(), a -> a.getSource().identifier, ArgumentSet::setName);

    // Every pattern is identified by its source instruction definition
    final var distinctPatterns =
        resolveNames(res.patterns(), p -> p.getSource().identifier, Pattern::setName);

    // Every format instance will receive its own name (there is a one-to-many relationship between
    // VADL format and QEMU format)
    var distinctFormats =
        resolveNames(res.formats(), Object::hashCode, e -> e.getSource().simpleName(),
            Format::setName);

    // We reuse fields if they have the same bit patterns, to avoid duplicate definitions
    final var allFields = res.argSets().stream()
        .flatMap(a -> a.getFields().stream())
        .toList();
    final var distinctFields =
        resolveNames(allFields, Field::getSlices, f -> f.getSource().identifier.simpleName(),
            Field::setName);

    return new QemuDecodeResolveSymbolPassResult(distinctPatterns, distinctFormats, distinctArgSets,
        distinctFields);
  }

  private static <T extends SourceMapping> Collection<T> resolveNames(Collection<T> entities,
                                                                      Function<T, Identifier> identExtractor,
                                                                      BiConsumer<T, String> nameSetter) {
    return resolveNames(entities, identExtractor, e -> e.getSource().identifier.simpleName(),
        nameSetter);
  }

  /**
   * Group the given entities by their grouping condition and assign them a unique name, based on
   * the given suggestion.
   *
   * @param entities
   * @param groupingConditionExtractor
   * @param nameSuggestionGetter
   * @param nameSetter
   * @param <T>
   * @param <I>
   * @return
   */
  private static <T, I> Collection<T> resolveNames(Collection<T> entities,
                                                   Function<T, I> groupingConditionExtractor,
                                                   Function<T, String> nameSuggestionGetter,
                                                   BiConsumer<T, String> nameSetter) {

    final Map<String, T> distinct = new LinkedHashMap<>();
    final Map<I, String> symbols = new LinkedHashMap<>();

    for (T e : entities) {
      final I i = groupingConditionExtractor.apply(e);

      String name = symbols.get(i);
      if (name != null) {
        // Already resolved
        nameSetter.accept(e, name);
        distinct.computeIfAbsent(name, k -> e);
        continue;
      }

      // First we try the simple name of the instruction
      name = nameSuggestionGetter.apply(e);
      if (!symbols.containsValue(name)) {
        symbols.put(i, name);
        nameSetter.accept(e, name);
        distinct.computeIfAbsent(name, k -> e);
        continue;
      }

      // Find a unique name by appending an index
      int j = 1;
      while (symbols.containsValue(name)) {
        name = nameSuggestionGetter.apply(e) + "_" + j;
        j++;
      }

      symbols.put(i, name);
      nameSetter.accept(e, name);
      distinct.computeIfAbsent(name, k -> e);
    }

    return distinct.values();
  }
}
