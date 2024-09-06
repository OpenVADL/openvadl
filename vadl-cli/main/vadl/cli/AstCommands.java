package vadl.cli;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import vadl.ast.Ast;
import vadl.ast.AstDumper;
import vadl.ast.ModelRemover;
import vadl.ast.Ungrouper;
import vadl.ast.VadlParser;
import vadl.error.VadlException;

class AstCommands {

  static int checkSyntax(Main main) {
    try {
      Ast ast = parse(main.input, main.modelOverrides);
      if (main.printPassStatistics) {
        VadlParser.printPassTimings(ast);
      }
      return 0;
    } catch (IOException e) {
      // TODO Log to slf4j
      e.printStackTrace();
      return 1;
    } catch (VadlException vadlException) {
      // TODO Log details to slf4j
      System.out.println("Errors during parsing - " + vadlException.getMessage());
      return 1;
    }
  }

  @Command(name = "dump-ast", description = "Dumps an AST representation of the VADL specification")
  static class DumpAstCommand implements Callable<Integer> {

    @ParentCommand
    @LazyInit
    Main main;

    @Parameters(description = "Path to the input VADL specification")
    @LazyInit
    Path input;

    @Option(names = {"--output", "-o"},
        description = "Output path for dumped AST contents, or - for stdout")
    @Nullable
    Path output;

    @Override
    public Integer call() {
      try {
        Ast ast = parse(input, main.modelOverrides);
        String dump = new AstDumper().dump(ast);
        if (output != null) {
          writeToPath(dump, output);
        }
        if (main.printPassStatistics) {
          VadlParser.printPassTimings(ast);
        }
        return 0;
      } catch (IOException e) {
        // TODO Log to slf4j
        e.printStackTrace();
        return 1;
      } catch (VadlException vadlException) {
        // TODO Log details to slf4j
        System.out.println("Errors during parsing - " + vadlException.getMessage());
        return 1;
      }
    }
  }

  @Command(name = "expand-ast", description = "Expands all macros in the given VADL specification")
  static class ExpandAstCommand implements Callable<Integer> {

    @ParentCommand
    @LazyInit
    Main main;

    @Parameters(description = "Path to the input VADL specification")
    @LazyInit
    Path input;

    @Option(names = {"--output", "-o"},
        description = "Output path for pretty-printed expanded AST, or - for stdout")
    @Nullable
    Path output;

    @Override
    public Integer call() {
      try {
        Ast ast = parse(input, main.modelOverrides);
        String prettified = ast.prettyPrint();
        if (output != null) {
          writeToPath(prettified, output);
        }
        if (main.printPassStatistics) {
          VadlParser.printPassTimings(ast);
        }
        return 0;
      } catch (IOException e) {
        // TODO Log to slf4j
        e.printStackTrace();
        return 1;
      } catch (VadlException vadlException) {
        // TODO Log details to slf4j
        System.out.println("Errors during parsing - " + vadlException.getMessage());
        return 1;
      }
    }
  }

  private static Ast parse(Path input, @Nullable Map<String, String> modelOverrides)
      throws IOException {
    Ast ast = VadlParser.parse(input, Objects.requireNonNullElseGet(modelOverrides, Map::of));
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
