package vadl.cli;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.annotation.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import vadl.ast.Ast;
import vadl.ast.AstDumper;
import vadl.ast.ModelRemover;
import vadl.ast.Ungrouper;
import vadl.ast.VadlParser;

class AstCommands {

  @Command(name = "dump-ast", description = "Dumps an AST representation of the VADL specification")
  static class DumpAstCommand implements Runnable {

    @Parameters(description = "Path to the input VADL specification")
    @LazyInit
    Path input;

    @Option(names = {"--output", "-o"}, description = "Output path for dumped AST contents")
    @Nullable
    Path output;

    @Option(names = {"-p", "--pass-stats"}, description = "Print performance statistics of passes")
    boolean printPassStatistics;

    @Override
    public void run() {
      try {
        Ast ast = parse(input);
        String dump = new AstDumper().dump(ast);
        if (output != null) {
          writeToPath(dump, output);
        }
        if (printPassStatistics) {
          VadlParser.printPassTimings(ast);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Command(name = "expand-ast", description = "Expands all macros in the given VADL specification")
  static class ExpandAstCommand implements Runnable {

    @Parameters(description = "Path to the input VADL specification")
    @LazyInit
    Path input;

    @Option(names = {"--output", "-o"}, description = "Output path for pretty-printed expanded AST, or - for stdout")
    @Nullable
    Path output;

    @Option(names = {"-p", "--pass-stats"}, description = "Print performance statistics of passes")
    boolean printPassStatistics;

    @Override
    public void run() {
      try {
        Ast ast = parse(input);
        String prettified = ast.prettyPrint();
        if (output != null) {
          writeToPath(prettified, output);
        }
        if (printPassStatistics) {
          VadlParser.printPassTimings(ast);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static Ast parse(Path input) throws IOException {
    Ast ast = VadlParser.parse(input, Map.of());
    new Ungrouper().ungroup(ast);
    new ModelRemover().removeModels(ast);
    return ast;
  }

  private static void writeToPath(String string, Path output) throws IOException {
    if (output.toString().equals("-")) {
      System.out.println(string);
    } else {
      Files.writeString(output, string, StandardCharsets.UTF_8);
    }
  }
}
