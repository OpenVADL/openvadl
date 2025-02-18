package vadl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import vadl.ast.Ast;
import vadl.ast.ModelRemover;
import vadl.ast.TypeChecker;
import vadl.ast.Ungrouper;
import vadl.ast.VadlParser;
import vadl.ast.ViamLowering;
import vadl.error.Diagnostic;
import vadl.viam.Specification;

class OpenVadlTestFrontend implements TestFrontend {

  private Specification specification;
  private String logs = "";

  @Override
  public boolean runSpecification(URI vadlFile) {
    try {
      Ast ast = VadlParser.parse(Path.of(vadlFile));
      {
        // FIXME: These two passes must be part of the VadlParser parse API.
        new Ungrouper().ungroup(ast);
        new ModelRemover().removeModels(ast);
      }
      var typeChecker = new TypeChecker();
      typeChecker.verify(ast);
      var viamGenerator = new ViamLowering();
      specification = viamGenerator.generate(ast);
      return true;
    } catch (Diagnostic e) {
      // FIXME: Proper print to string
      var stringWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(stringWriter));
      logs = e.getMessage() + "\n" + stringWriter;
      return false;
    } catch (Exception e) {
      var stringWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(stringWriter));
      logs = e + "\n" + stringWriter;
      return false;
    }
  }

  @Override
  public Specification getViam() {
    return Objects.requireNonNull(specification);
  }

  @Override
  public String getLogAsString() {
    return logs;
  }


  static class Provider extends TestFrontend.Provider {

    public TestFrontend createFrontend() {
      return new OpenVadlTestFrontend();
    }

  }
}
