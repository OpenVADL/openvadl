package vadl.viam.passes.htmlDump;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import vadl.pass.PassKey;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

public abstract class HtmlDumpPass extends AbstractTemplateRenderingPass {

  public HtmlDumpPass(String outputPathPrefix) throws IOException {
    super(outputPathPrefix);
  }

  public abstract List<InfoSupplier> getInfoSuppliers();


  @Override
  protected String getTemplatePath() {
    return "viamDump/index.html";
  }

  @Override
  protected String getOutputPath() {
    return "index.html";
  }

  @Override
  protected Map<String, Object> createVariables(Map<PassKey, Object> passResults,
                                                Specification specification) {
    var definitionBuilders = ViamHtmlCreator.run(specification, passResults);
    var tocMapList = definitionBuilders.stream()
        .collect(Collectors.groupingBy(d -> d.origin().getClass()))
        .entrySet().stream().toList();

    return Map.of(
        "entries", definitionBuilders,
        "toc", tocMapList
    );
  }
}
