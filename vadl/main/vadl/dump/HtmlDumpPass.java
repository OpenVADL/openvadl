package vadl.dump;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.configuration.GeneralConfiguration;
import vadl.dump.entitySuppliers.VdtEntitySupplier;
import vadl.dump.entitySuppliers.ViamEntitySupplier;
import vadl.dump.infoEnrichers.IssEnricherCollection;
import vadl.dump.infoEnrichers.LcbEnricherCollection;
import vadl.dump.infoEnrichers.VdtEnricherCollection;
import vadl.dump.infoEnrichers.ViamEnricherCollection;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;


/**
 * The HTML dump pass emits a debug dump of the VIAM specification in HTML form.
 * The main information source comes from registered {@link DumpEntitySupplier}s that
 * produce {@link DumpEntity}s that get rendered as boxes.
 * The registered {@link InfoEnricher}s enrich the entities by {@link Info} objects.
 * Those info objects are rendered in the entity box depending on their concrete type (e.g.
 * {@link Info.Tag}.
 *
 * <p>To add your own entity supplier, add yours to the {@code entitySuppliers} field.
 * To add a new info enricher, add it to the {@code infoEnrichers} field.
 * Take a look at {@link ViamEnricherCollection} to see an example on how to implement
 * new info enrichers.</p>
 *
 * <p>Note that infos are rendered in the same order as the info enrichers are registered.</p>
 *
 * <p>Also note that we use tailwind css, so all tailwind css classes are available
 * to produced HTML bodies.</p>
 *
 * @see DumpEntitySupplier
 * @see InfoEnricher
 * @see Info
 * @see ViamEntitySupplier
 * @see ViamEnricherCollection
 */
public class HtmlDumpPass extends AbstractTemplateRenderingPass {

  private static final Logger log = LoggerFactory.getLogger(HtmlDumpPass.class);

  /**
   * A function that collects all available entity suppliers.
   * Add your supplier to activate it.
   */
  static Consumer<List<DumpEntitySupplier<?>>> entitySuppliers = suppliers -> {
    suppliers.add(new ViamEntitySupplier());
    suppliers.add(new VdtEntitySupplier());
  };

  /**
   * A function to collect all information enrichers.
   * Add your enricher to activate it.
   */
  static Consumer<List<InfoEnricher>> infoEnrichers = enrichers -> {
    enrichers.addAll(ViamEnricherCollection.all);
    enrichers.addAll(LcbEnricherCollection.all);
    enrichers.addAll(IssEnricherCollection.all);
    enrichers.addAll(VdtEnricherCollection.all);
  };


  /**
   * The config of the HtmlDumpPass.
   * It requires the name of the phase the dump happens in and the output path
   * where the dump should be written to.
   *
   * <p>It holds a dump description that explains what happened since the last dump.</p>
   */
  public static class Config extends GeneralConfiguration {
    private final String phase;
    private final String description;

    /**
     * Construct a configuration.
     *
     * @param generalConfiguration the base configuration
     * @param phase                the phase name used for the emitted file name
     * @param description          the description what happened since the last dump
     */
    public Config(GeneralConfiguration generalConfiguration, String phase, String description) {
      super(generalConfiguration.outputPath(), true);
      this.phase = phase;
      this.description = description;
    }

    public static Config from(GeneralConfiguration generalConfiguration, String phase,
                              String description) {
      return new Config(generalConfiguration, phase, description);
    }
  }

  /**
   * The result of the HTML dump pass.
   * Beside the emitted file path, it also holds the key of the previous pass.
   */
  public static class Result extends AbstractTemplateRenderingPass.Result {
    public final @Nullable PassKey lastPass;

    protected Result(Path emittedFile, @Nullable PassKey lastPass) {
      super(emittedFile);
      this.lastPass = lastPass;
    }
  }

  private final Config config;
  private final int count;
  @Nullable
  private PassKey lastPass = null;

  private static final AtomicInteger dumpCounter = new AtomicInteger();

  /**
   * Constructs the {@link HtmlDumpPass}.
   */
  public HtmlDumpPass(Config config) {
    super(config, "dump");
    this.config = config;
    this.count = dumpCounter.getAndIncrement();
  }

  @Override
  public PassName getName() {
    return new PassName("HTML Dump at phase " + config.phase);
  }

  @Override
  protected String getTemplatePath() {
    return "htmlDump/index.html";
  }

  @Override
  protected String getOutputPath() {
    return count + "_" + config.phase.replace(" ", "_") + ".html";
  }

  @Override
  public Result constructResult() {
    return new Result(getEmittedFile(), lastPass);
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    // find last pass for the result
    lastPass = getLastPass(passResults);

    log.info("HTML dump of phase '{}' at {}dump/{}", config.phase,
        config.outputPath().toUri(),
        getOutputPath());
    // collect suppliers
    var suppliers = new ArrayList<DumpEntitySupplier<?>>();
    entitySuppliers.accept(suppliers);

    // get all top-level entities from all suppliers
    var entities = suppliers.stream()
        .flatMap(e -> e.getEntities(specification, passResults).stream())
        .toList();


    // collect all info enrichers
    var enrichers = new ArrayList<InfoEnricher>();
    infoEnrichers.accept(enrichers);

    // apply all enrichers to all entities (also sub entities)
    for (var entity : entities) {
      applyOnThisAndSubEntities(entity, enrichers, passResults);
    }

    var tocMapList = entities.stream()
        .collect(Collectors.groupingBy(DumpEntity::tocKey))
        .entrySet().stream()
        .sorted(Comparator.comparingInt(a -> a.getKey().rank()))
        .toList();

    var passList = passResults.executedPasses().stream()
        .filter(p -> !(p.pass() instanceof CollectBehaviorDotGraphPass))
        .toList();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());
    var renderDate = formatter.format(Instant.now());

    return Map.of(
        "title",
        "Specification (%s) - at %s".formatted(specification.identifier.name(), config.phase),
        "description", config.description,
        "passes", passList,
        "passDumps", findAllPassDumps(passResults),
        "entries", entities,
        "toc", tocMapList,
        "renderDatetime", renderDate
    );
  }

  /**
   * Applies the given info enrichers to the given entity and all sub entities
   * of the entity recursively.
   */
  private static void applyOnThisAndSubEntities(DumpEntity entity,
                                                List<InfoEnricher> infoEnrichers,
                                                PassResults passResults) {
    for (var subEntity : entity.subEntities()) {
      applyOnThisAndSubEntities(subEntity.subEntity, infoEnrichers, passResults);
    }
    for (var infoEnricher : infoEnrichers) {
      infoEnricher.enrich(entity, passResults);
    }
  }

  private static @Nullable PassKey getLastPass(PassResults passResults) {
    var passes = passResults.executedPasses();
    if (passes.isEmpty()) {
      return null;
    }
    return passes.get(passes.size() - 1)
        .passKey();
  }

  /**
   * Find all pass keys that were dumped directly after execution.
   */
  private static Map<PassKey, Path> findAllPassDumps(PassResults passResults) {
    return passResults.executedPasses().stream()
        .filter(p -> p.pass() instanceof HtmlDumpPass)
        .map(p -> (Result) p.result())
        .filter(Objects::nonNull)
        .collect(Collectors.toMap((r) -> r.lastPass, (r) -> r.emittedFile));
  }

}