// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.cli;

import static picocli.CommandLine.ScopeType.INHERIT;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import vadl.ast.Ast;
import vadl.ast.AstDumper;
import vadl.ast.ModelRemover;
import vadl.ast.TypeChecker;
import vadl.ast.Ungrouper;
import vadl.ast.VadlParser;
import vadl.ast.ViamLowering;
import vadl.configuration.GeneralConfiguration;
import vadl.dump.ArtifactTracker;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.error.DiagnosticPrinter;
import vadl.pass.PassManager;
import vadl.pass.PassOrder;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.EditorUtils;
import vadl.utils.SourceLocation;
import vadl.viam.Specification;

/**
 * A base command from which the actual commands can inherit from.
 */
public abstract class BaseCommand implements Callable<Integer> {
  @Parameters(description = "Path to the input VADL specification")
  @LazyInit
  Path input;

  @Option(names = {"-o",
      "--output"}, scope = INHERIT, description = "The output directory (default: \"output\")")
  Path output = Paths.get("output");

  @Option(names = {"-m", "--model"}, scope = INHERIT,
      description = "Override the value of an Id macro model.")
  @Nullable
  Map<String, String> modelOverrides;

  @Option(names = {"--dump"},
      scope = INHERIT,
      description = "Generate all dumps of intermediate representations.")
  boolean dump;

  // @Option(names = "--timings", scope = INHERIT, description = "Write a AST dumps to disc.")
  // boolean printTimings;

  @Option(names = "--expand-macros",
      scope = INHERIT,
      description = "Expand all macros and write them to disk.")
  boolean expandMacros;

  /**
   * Dumps should contain their date this method returns the date in a uniform string.
   * Format is YYYY-MM-DD hh:mm:ss
   *
   * @return the current time as a string
   */
  private String getTimeString() {
    var now = LocalDateTime.now(ZoneId.systemDefault());
    return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }

  /**
   * Dump a file.
   *
   * @param fileName of the dump.
   * @param content  of the dump.
   */
  private void dumpFile(String fileName, CharSequence content) {
    var folderPath = Paths.get(output.toString(), "dump");
    if (!folderPath.toFile().exists()) {
      folderPath.toFile().mkdirs();
    }

    var filePath = Paths.get(folderPath.toString(), fileName);
    try (var writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
      writer.append(content);
    } catch (IOException e) {
      e.printStackTrace();
      throw Diagnostic.error("Unable to write file %s".formatted(filePath.toString()),
              SourceLocation.INVALID_SOURCE_LOCATION)
          .build();
    }

    ArtifactTracker.addDump(filePath);
  }

  /**
   * Parses the input to an AST.
   *
   * @return the AST.
   */
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

  /**
   * Dump the source code with all macros expaned.
   *
   * @param ast to be expanded.
   */
  private void dumpExpaned(Ast ast) {
    if (!expandMacros) {
      return;
    }

    var content =
        new StringBuilder(
            "// Sourcecode with expanded macros on %s\n\n".formatted(getTimeString()));
    content.append(ast.prettyPrint());
    dumpFile("expanded-macros.vadl", content);
  }

  /**
   * Dump the AST before it gets enriched with types.
   *
   * @param ast to be dumped.
   */
  private void dumpUntyped(Ast ast) {
    if (!dump) {
      return;
    }

    var content =
        new StringBuilder(
            "// AST Dump without types generated on %s\n".formatted(getTimeString()));
    content.append("// The file contains a dump of the AST with all macros expanded but, before "
        + "the type-checker has run.\n\n");
    content.append(new AstDumper().dump(ast));
    dumpFile("ast-dump-untyped.txt", content);
  }

  /**
   * Dump the AST after it was enriched with types.
   *
   * @param ast to be dumped.
   */
  private void dumpTyped(Ast ast) {
    if (!dump) {
      return;
    }

    var content =
        new StringBuilder(
            "// AST Dump with types generated on %s\n".formatted(getTimeString()));
    content.append("// The file contains a dump of the AST with all macros expanded and "
        + "validated by the typechecker.\n\n");
    content.append(new AstDumper().dump(ast));
    dumpFile("ast-dump-typed.txt", content);
  }

  /**
   * Parses, typechecks and lowers the input according to the arguments and
   * returns a parsed VIAM specification.
   *
   * <p>If an error occurs, a diagnostic will be thrown.
   *
   * @return the viam specification
   */
  private Specification parseToVIAM() {
    var ast = parseToAst();
    dumpExpaned(ast);
    dumpUntyped(ast);
    var typeChecker = new TypeChecker();
    typeChecker.verify(ast);
    dumpTyped(ast);
    var viamGenerator = new ViamLowering();
    var spec = viamGenerator.generate(ast);
    return spec;
  }

  protected void printDumps(String message) {
    if (ArtifactTracker.getDumpPaths().isEmpty()) {
      return;
    }

    System.out.println(message);
    for (var path : ArtifactTracker.getDumpPaths()) {
      if (EditorUtils.isIntelliJIDE()) {
        var uri = path.toAbsolutePath().toUri();
        System.out.printf("\t- %s\n", uri);
      } else {
        System.out.printf("\t- %s\n", path);
      }
    }
  }

  protected void printArtifacts(String message) {
    if (ArtifactTracker.getArtifactPathsPaths().isEmpty()) {
      return;
    }
    System.out.println(message);
    for (var path : ArtifactTracker.getArtifactPathsPaths()) {
      if (EditorUtils.isIntelliJIDE()) {
        var uri = path.toAbsolutePath().toUri();
        System.out.printf("\t- %s\n", uri);
      } else {
        System.out.printf("\t- %s\n", path);
      }
    }
  }

  // lazy evaluated config, do NOT use this directly.
  // use getConfig() instead.
  @Nullable
  private GeneralConfiguration config;

  /**
   * Generate a general configuration from the arguments.
   *
   * @return the configuration.
   */
  protected GeneralConfiguration getConfig() {
    if (config == null) {
      config = new GeneralConfiguration(output, dump);
    }
    return config;
  }

  abstract PassOrder passOrder(GeneralConfiguration configuration) throws IOException;

  @SuppressWarnings("EmptyCatch")
  @Override
  public Integer call() {
    if (!input.toFile().exists()) {
      System.out.printf("\033[01m\033[31merror:\033[0m\033[01m Cannot find file: %s\033[0m\n",
          input);
      return 1;
    }

    int returnVal = 0;
    try {
      var viam = parseToVIAM();
      var passOrder = passOrder(getConfig());
      var passManager = new PassManager();
      passManager.add(passOrder);
      passManager.run(viam);

    } catch (Diagnostic d) {
      System.out.println(new DiagnosticPrinter().toString(d));
      returnVal = 1;
    } catch (DiagnosticList d) {
      System.out.println(new DiagnosticPrinter().toString(d));
      returnVal = 1;
    } catch (RuntimeException | IOException | DuplicatedPassKeyException e) {
      System.out.println("""
                                       ___ ___    _   ___ _  _      \s
                                      / __| _ \\  /_\\ / __| || |   \s
                                     | (__|   / / _ \\\\__ \\ __ |  \s  
                                      \\___|_|_\\/_/ \\_\\___/_||_| \s
                                                                    \s
                                   🔥 The vadl compiler crashed 🔥  \s
          
          This shouldn't have happened, please open an issue with the stacktrace below at:
          https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/new
          """);

      printDumps("\nBefore the crash, the following dumps were generated:");

      // Dirty hack to avoid stdout and stderr getting mixed in IntelliJ (flushing wasn't enough).
      try {
        System.out.flush();
        Thread.sleep(10);
      } catch (InterruptedException ignored) {
        // ignored
      }
      e.printStackTrace();
      return 1;
    }

    if (!DeferredDiagnosticStore.isEmpty()) {
      System.out.println(new DiagnosticPrinter().toString(DeferredDiagnosticStore.getAll()));

      // Only exit abnormally if any diagnostic message is an error.
      if (DeferredDiagnosticStore.getAll().stream()
          .anyMatch(diagnostic -> diagnostic.level == Diagnostic.Level.ERROR)) {
        returnVal = 1;
      }
    }

    printArtifacts(returnVal == 0
        ? "\nThe following artifacts were generated:"
        : "\nEven though some errors occurred, the following artifacts were generated:");

    printDumps(returnVal == 0
        ? "\nThe following dumps were generated:"
        : "\nEven though some errors occurred, the following dumps were generated:");

    return returnVal;
  }

}
