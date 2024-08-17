package vadl.viam.passes.htmlDump;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

public class ViamHtmlDumpPass extends AbstractTemplateRenderingPass {

  public ViamHtmlDumpPass() throws IOException {
    super("artifacts");
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
    return Map.of();
  }
}


class ViamHtmlCreator implements DefinitionVisitor {

  private record SemiResult(
      @Nullable String body,
      @Nullable String parent,
      @Nullable String type,
      @Nullable List<SemiResult> subDefinitions,
      Definition origin
  ) {
  }

  private List<SemiResult> semiResults = new ArrayList<>();

  private ViamHtmlCreator() {
  }

  public static String run(Specification specification) {
    var creator = new ViamHtmlCreator();
    // start running through all definitions
    creator.callBackVisitor.visit(specification);

    return "";
  }

  @Override
  public void visit(Specification specification) {
    // do nothing, specificaiton is handled sparately
  }

  @Override
  public void visit(InstructionSetArchitecture instructionSetArchitecture) {

  }

  @Override
  public void visit(Instruction instruction) {

  }

  @Override
  public void visit(Assembly assembly) {

  }

  @Override
  public void visit(Encoding encoding) {

  }

  @Override
  public void visit(Encoding.Field encodingField) {

  }

  @Override
  public void visit(Format format) {

  }

  @Override
  public void visit(Format.Field formatField) {

  }

  @Override
  public void visit(Format.FieldAccess formatFieldAccess) {

  }

  @Override
  public void visit(Function function) {

  }

  @Override
  public void visit(Parameter parameter) {

  }

  @Override
  public void visit(PseudoInstruction pseudoInstruction) {

  }

  @Override
  public void visit(Register register) {

  }

  @Override
  public void visit(RegisterFile registerFile) {

  }

  @Override
  public void visit(Memory memory) {

  }

  @Override
  public void visit(Relocation relocation) {

  }

  private final ViamHtmlCreator thisRef = this;

  private final DefinitionVisitor.Recursive callBackVisitor = new DefinitionVisitor.Recursive() {
    @Override
    public void beforeTraversal(Definition definition) {
      definition.accept(thisRef);
    }
  };
}
