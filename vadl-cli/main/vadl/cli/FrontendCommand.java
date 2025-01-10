package vadl.cli;

import static picocli.CommandLine.ScopeType.INHERIT;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import vadl.ast.Ast;
import vadl.ast.AstDumper;
import vadl.ast.ModelRemover;
import vadl.ast.TypeChecker;
import vadl.ast.Ungrouper;
import vadl.ast.VadlParser;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.error.DiagnosticPrinter;
import vadl.utils.SourceLocation;
import vadl.viam.Specification;

/**
 * The Command does provide the check subcommand but is also used by the backend commands to invoke
 * the frontend.
 */
@CommandLine.Command(
    name = "check",
    description = "Verify the correctness of a VADL file without generating anything.",
    mixinStandardHelpOptions = true
)
public class FrontendCommand implements Callable<Integer> {

  @CommandLine.Parameters(description = "Path to the input VADL specification")
  @LazyInit
  Path input;

  @Option(names = {"-o",
      "--output"}, scope = INHERIT, description = "The output directory (default: \"output\")")
  Path output = Paths.get("output");

  @Option(names = {"-m", "--model"}, scope = INHERIT,
      description = "Override the value of an Id model.")
  @Nullable
  Map<String, String> modelOverrides;

  @Option(names = {"--dump-ast"}, scope = INHERIT, description = "Write a AST dumps to disc.")
  boolean dumpAST;

//  @Option(names = "--timings", scope = INHERIT, description = "Write a AST dumps to disc.")
//  boolean printTimings;

  @Option(names = "--expand-macros", scope = INHERIT, description = "Expand all macros and write them to disc.")
  boolean expandMacros;

  private CliState state = CliState.getInstance();

  private Ast parseToAst() {
    try {
      Ast ast = VadlParser.parse(input, Objects.requireNonNullElseGet(modelOverrides, Map::of));
      new Ungrouper().ungroup(ast);
      new ModelRemover().removeModels(ast);
      return ast;
    } catch (IOException e) {
      throw Diagnostic.error("Cannot open file", SourceLocation.INVALID_SOURCE_LOCATION)
          .description("%s", Objects.requireNonNullElse(e.getMessage(), ""))
          .build();
    }
  }

  private void dumpExpaned(Ast ast) {
    if (!expandMacros) {
      return;
    }

    var content =
        new StringBuilder(
            "// Sourcecode with expanded macros on %s\n\n".formatted(state.getTimeString()));
    content.append(ast.prettyPrint());
    state.dumpFile(output, "expanded-macros.vadl", content);
  }

  private void dumpUntyped(Ast ast) {
    if (!dumpAST) {
      return;
    }

    var content =
        new StringBuilder(
            "// AST Dump without types generated on %s\n".formatted(state.getTimeString()));
    content.append("// The file contains a dump of the AST with all macros expanded but, before "
        + "the type-checker has run.\n\n");
    content.append(new AstDumper().dump(ast));
    state.dumpFile(output, "ast-dump-untyped.txt", content);
  }

  private void dumpTyped(Ast ast) {
    if (!dumpAST) {
      return;
    }

    var content =
        new StringBuilder(
            "// AST Dump with types generated on %s\n".formatted(state.getTimeString()));
    content.append("// The file contains a dump of the AST with all macros expanded and "
        + "validated by the typechecker.\n\n");
    content.append(new AstDumper().dump(ast));
    state.dumpFile(output, "ast-dump-typed.txt", content);
  }

  /**
   * Parses, typechecks and lowers the input according to the arguments and
   * returns a parsed VIAM specification.
   *
   * <p> If an error occurs, a diagnostic will be thrown.
   *
   * @return the viam specification
   */
  @Nullable
  public Specification parseToVIAM() {
    var ast = parseToAst();
    dumpExpaned(ast);
    dumpUntyped(ast);
    var typeChecker = new TypeChecker();
    typeChecker.verify(ast);
    dumpTyped(ast);

    // FIXME: add lowering and make not nullable;
    return null;
  }

  @Override
  public Integer call() {
    var returnVal = 0;
    try {
      parseToVIAM();
      System.out.println("All files checked ✨");
    } catch (Diagnostic d) {
      new DiagnosticPrinter().print(d);
      returnVal = 1;
    } catch (DiagnosticList d) {
      new DiagnosticPrinter().print(d);
      returnVal = 1;
    }

    state.printDumpedFiles();
    return returnVal;
  }

}
