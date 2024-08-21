package vadl.viam.passes.htmlDump;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.pass.PassKey;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Assembly;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Memory;
import vadl.viam.Parameter;
import vadl.viam.PseudoInstruction;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Relocation;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.passes.htmlDump.suppliers.DefaultSupplierCollection;

public class ViamHtmlDumpPass extends AbstractTemplateRenderingPass {

  public record Config(
      String outDir
  ) {
  }

  public final static List<InfoSupplier> infoSuppliers = List.of(
      DefaultSupplierCollection.DEF_CLASS_SUPPLIER,
      DefaultSupplierCollection.TYPE_SUPPLIER,
      DefaultSupplierCollection.BEHAVIOR_SUPPLIER,
      DefaultSupplierCollection.BEHAVIOR_SUPPLIER_MODAL
  );

  public ViamHtmlDumpPass(Config config) throws IOException {
    super(config.outDir);
  }

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
        "definitions", definitionBuilders,
        "toc", tocMapList
    );
  }
}


class ViamHtmlCreator extends DefinitionVisitor.Empty {

  private final Map<PassKey, Object> passResults;
  private final Map<Definition, HtmlDefinitionBuilder> htmlBuilders =
      new LinkedHashMap<>();

  private ViamHtmlCreator(Map<PassKey, Object> passResults) {
    this.passResults = passResults;

  }

  public static List<HtmlDefinitionBuilder> run(Specification specification,
                                                Map<PassKey, Object> passResults) {
    var creator = new ViamHtmlCreator(passResults);
    // start running through all definitions
    creator.callBackVisitor.visit(specification);

    return new ArrayList<>(creator.htmlBuilders.values());
  }

  private HtmlDefinitionBuilder builderOf(Definition result) {
    ViamError.ensure(htmlBuilders.containsKey(result), "Could not find HTML builder for %s",
        result);
    return htmlBuilders.get(result);
  }

  private Stack<Definition> parents = new Stack<>();

  private void beforeEach(Definition definition) {
    var builder = new HtmlDefinitionBuilder(definition);
    for (var supplier : ViamHtmlDumpPass.infoSuppliers) {
      var info = supplier.produce(definition, passResults);
      if (info != null) {
        builder.addInfo(info);
      }
    }
    htmlBuilders.put(definition, builder);

    // set parent
    if (!parents.isEmpty()) {
      builder.parent(parents.peek());
    }
    parents.push(definition);
  }

  private void afterEach(Definition definition) {
    parents.pop();
  }

  @Override
  public void visit(Specification specification) {
    // do nothing, specificaiton is handled sparately
    htmlBuilders.remove(specification);
  }

  // hidden traversal

  private final ViamHtmlCreator thisRef = this;

  private final DefinitionVisitor.Recursive callBackVisitor = new DefinitionVisitor.Recursive() {
    @Override
    public void beforeTraversal(Definition definition) {
      beforeEach(definition);
      definition.accept(thisRef);
    }

    @Override
    public void afterTraversal(Definition definition) {
      afterEach(definition);
    }
  };
}


class HtmlDefinitionBuilder {
  @Nullable
  private Definition parent;
  private List<HtmlDefinitionBuilder> subDefinitions = new ArrayList<>();
  private List<Info> infos = new ArrayList<>();

  private Definition origin;

  HtmlDefinitionBuilder(Definition origin) {
    this.origin = origin;
  }

  public Definition origin() {
    return origin;
  }


  HtmlDefinitionBuilder parent(Definition val) {
    this.parent = val;
    return this;
  }

  public Info.Tag parent() {
    Objects.requireNonNull(parent);
    return Info.Tag.of("Parent", parent.identifier.name(), "#" + cssIdFor(parent));
  }

  HtmlDefinitionBuilder addInfo(Info val) {
    this.infos.add(val);
    return this;
  }

  public List<Info.Tag> tagInfos() {
    return infos.stream()
        .filter(Info.Tag.class::isInstance)
        .map(Info.Tag.class::cast)
        .toList();
  }

  public List<Info.Expandable> expandableInfos() {
    return infos.stream()
        .filter(Info.Expandable.class::isInstance)
        .map(Info.Expandable.class::cast)
        .toList();
  }

  public List<Info.Modal> modalInfos() {
    return infos.stream()
        .filter(Info.Modal.class::isInstance)
        .map(Info.Modal.class::cast)
        .toList();
  }

  HtmlDefinitionBuilder addSubDef(HtmlDefinitionBuilder val) {
    this.subDefinitions.add(val);
    return this;
  }

  public List<HtmlDefinitionBuilder> subDefinitions() {
    return subDefinitions;
  }

  public String cssId() {
    return cssIdFor(origin);
  }

  public static String cssIdFor(Definition def) {
    return def.identifier.name() + "-" + def.getClass().getSimpleName();
  }
}