package vadl.dump;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import vadl.dump.supplier.ViamEntitySupplier;
import vadl.pass.PassKey;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;
import vadl.dump.supplier.ViamEnricherCollection;


public class HtmlDumpPass extends AbstractTemplateRenderingPass {

  static SupplierCollection<DumpEntitySupplier<?>> entitySuppliers =
      new SupplierCollection<DumpEntitySupplier<?>>()
          .add(new ViamEntitySupplier());

  static SupplierCollection<InfoEnricher> infoEnricher = new SupplierCollection<InfoEnricher>()
      .addAll(ViamEnricherCollection.all);

  public record Config(
      String phase,
      String outputPath
  ) {
  }

  private final Config config;

  public HtmlDumpPass(Config config) throws IOException {
    super(config.outputPath);
    this.config = config;
  }

  @Override
  protected String getTemplatePath() {
    return "htmlDump/index.html";
  }

  @Override
  protected String getOutputPath() {
    return config.phase.replace(" ", "_") + ".html";
  }

  @Override
  protected Map<String, Object> createVariables(Map<PassKey, Object> passResults,
                                                Specification specification) {
    var suppliers = entitySuppliers.suppliers();
    var entities = suppliers.stream()
        .flatMap(e -> e.getEntities(specification, passResults).stream())
        .toList();


    for (var entity : entities) {
      applyOnThisAndSubEntities(entity, infoEnricher, passResults);
    }

    var tocMapList = entities.stream()
        .collect(Collectors.groupingBy(DumpEntity::tocKey))
        .entrySet().stream()
        .sorted(Comparator.comparingInt(a -> a.getKey().rank()))
        .toList();

    var passList = passResults.keySet().stream()
        .map(PassKey::value)
        .toList();

    return Map.of(
        "entries", entities,
        "toc", tocMapList,
        "title",
        "Specification (%s) - at %s".formatted(specification.identifier.name(), config.phase),
        "passes", passList
    );
  }

  private static void applyOnThisAndSubEntities(DumpEntity entity,
                                                SupplierCollection<InfoEnricher> infoEnrichers,
                                                Map<PassKey, Object> passResults) {
    for (var subEntity : entity.subEntities()) {
      applyOnThisAndSubEntities(subEntity.subEntity, infoEnrichers, passResults);
    }
    for (var iSup : infoEnrichers.suppliers()) {
      iSup.enrich(entity, passResults);
    }
  }

}

class SupplierCollection<T> {
  private final List<T> suppliers = new ArrayList<>();

  SupplierCollection() {
  }

  static <T> SupplierCollection<T> init() {
    return new SupplierCollection<T>();
  }

  SupplierCollection<T> addAll(Collection<T> suppliers) {
    this.suppliers.addAll(suppliers);
    return this;
  }

  SupplierCollection<T> add(T supplier) {
    suppliers.add(supplier);
    return this;
  }

  public List<T> suppliers() {
    return this.suppliers;
  }
}