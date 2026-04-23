// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static picocli.CommandLine.MaxValuesExceededException;
import static picocli.CommandLine.ScopeType.INHERIT;
import static picocli.CommandLine.TypeConversionException;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import vadl.ast.Ast;
import vadl.ast.AstDumper;
import vadl.ast.ModelRemover;
import vadl.ast.SpecStatAnalyser;
import vadl.ast.TypeChecker;
import vadl.ast.Ungrouper;
import vadl.ast.VadlParser;
import vadl.ast.ViamLowering;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.dump.ArtifactTracker;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.error.DiagnosticPrinter;
import vadl.pass.PassManager;
import vadl.pass.PassOrder;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.DiskVirtualFileSystem;
import vadl.utils.EditorUtils;
import vadl.utils.SourceLocation;
import vadl.utils.VirtualFileSystem;
import vadl.viam.Specification;

/**
 * A base command from which the actual commands can inherit from.
 */
public abstract class BaseCommand implements Callable<Integer> {

  @Nullable
  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;

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
      arity = "0..1",
      fallbackValue = "always",
      paramLabel = "<mode>",
      description = "Generate all dumps of intermediate representations. "
          + "Valid values: ${COMPLETION-CANDIDATES}",
      parameterConsumer = DumpModeConverter.class,
      converter = DumpModeConverter.class,
      completionCandidates = DumpModeConverter.class)

  DumpMode dump = DumpMode.NONE;

  @Option(names = "--timings", scope = INHERIT,
      description = "Print timings of the phases of the compiler")
  boolean showTimings;

  @Option(names = "--timings-csv",
      scope = INHERIT,
      hidden = true,
      description = "Write pass timings to <output>/timings.csv")
  boolean writeTimingsCsv;

  @Option(names = "--spec-stats",
      scope = INHERIT,
      hidden = true,
      description = "Print the statistics of the specification")
  boolean showSpecStats;

  @Option(names = "--expand-macros",
      scope = INHERIT,
      description = "Expand all macros and write them to disk.")
  boolean expandMacros;

  @Option(names = "--with-stacktrace",
      scope = INHERIT,
      description = "Debug option to show the OpenVADL stacktrace of an emitted error."
  )
  boolean showStacktrace;

  /**
   * A list of timings. Will only be filled when the timings should be recorded.
   */
  private final List<Timing> timings = new ArrayList<>();

  private record Timing(String name, long durationMs) {
  }

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

  private void withTimings(String title, Runnable runnable) {
    var startTime = System.currentTimeMillis();
    runnable.run();
    timings.add(new Timing(title, System.currentTimeMillis() - startTime));
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
    Ast ast = VadlParser.parse(input, new DiskVirtualFileSystem(),
        Objects.requireNonNullElseGet(modelOverrides, Map::of));
    Ungrouper.ungroup(ast);
    ModelRemover.removeModels(ast);
    return ast;
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

    withTimings("Expanded Macros Dump", () -> {
      var content =
          new StringBuilder(
              "// Sourcecode with expanded macros on %s\n\n".formatted(getTimeString()));
      content.append(ast.prettyPrint());
      dumpFile("expanded-macros.vadl", content);
    });
  }

  /**
   * Dump the AST before it gets enriched with types.
   *
   * @param ast to be dumped.
   */
  private void dumpUntyped(Ast ast) {
    if (dump != DumpMode.ALWAYS) {
      return;
    }

    withTimings("Untyped AST Dump", () -> {
      var content =
          new StringBuilder(
              "// AST Dump without types generated on %s\n".formatted(getTimeString()));
      content.append("// The file contains a dump of the AST with all macros expanded but, before "
          + "the type-checker has run.\n\n");
      content.append(new AstDumper().dump(ast));
      dumpFile("ast-dump-untyped.txt", content);
    });
  }

  /**
   * Dump the AST after it was enriched with types.
   *
   * @param ast to be dumped.
   */
  private void dumpTyped(Ast ast) {
    if (dump != DumpMode.ALWAYS) {
      return;
    }

    withTimings("Typed AST Dump", () -> {
      var content =
          new StringBuilder(
              "// AST Dump with types generated on %s\n".formatted(getTimeString()));
      content.append("// The file contains a dump of the AST with all macros expanded and "
          + "validated by the typechecker.\n\n");
      content.append(new AstDumper().dump(ast));
      dumpFile("ast-dump-typed.txt", content);
    });
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
    printSpecStats(ast, new DiskVirtualFileSystem());
    ast.timingRecorder.passTimings.forEach(
        t -> timings.add(new Timing(t.description(), t.durationNS() / 1000_000)));
    ast.timingRecorder.passTimings.clear();
    dumpExpaned(ast);
    dumpUntyped(ast);

    TypeChecker.verify(ast);
    ast.timingRecorder.passTimings.forEach(
        t -> timings.add(new Timing(t.description(), t.durationNS() / 1000_000)));
    ast.timingRecorder.passTimings.clear();
    dumpTyped(ast);

    var spec = ViamLowering.generate(ast);
    ast.timingRecorder.passTimings.forEach(
        t -> timings.add(new Timing(t.description(), t.durationNS() / 1000_000)));

    return spec;
  }

  protected void printPaths(String message, List<Path> pathList) {
    if (pathList.isEmpty()) {
      return;
    }

    System.out.println(message);
    for (var path : pathList) {
      if (EditorUtils.isIntelliJIDE()) {
        var uri = path.toUri();
        System.out.printf("\t- %s\n", uri);
      } else {
        System.out.printf("\t- %s\n", path);
      }
    }
  }

  protected void printSpecStats(Ast ast, VirtualFileSystem fileSystem) {
    if (!showSpecStats) {
      return;
    }

    System.out.println("\nSpecification Statistics:");
    var stats = SpecStatAnalyser.run(ast, fileSystem);
    System.out.printf("\t- %-30s %6d\n", "Files:", stats.files);
    System.out.printf("\t- %-30s %6d\n", "Lines of Code:", stats.linesOfCode);
    System.out.printf("\t- %-30s %6d\n", "Function Definitions:", stats.functionDefinitions);
    System.out.printf("\t- %-30s %6d\n", "Format Definitions:", stats.formatDefinitions);
    System.out.printf("\t- %-30s %6d\n", "Instruction Definitions:", stats.instructionDefinition);
    System.out.printf("\t- %-30s %6d\n", "Total Definitions:", stats.totalDefinitions);
    System.out.printf("\t- %-30s %6d\n", "Total Statements:", stats.totalStatements);
    System.out.printf("\t- %-30s %6d\n", "Total Expressions:", stats.totalExpressions);
  }

  protected void printTimings() {
    if (!showTimings) {
      return;
    }

    System.out.println("\nTimings:");
    timings.forEach(t -> {
      System.out.printf("\t- %-40s %5dms\n", t.name + ":", t.durationMs);
    });
  }

  private void writeTimingsCsv() {
    if (!writeTimingsCsv) {
      return;
    }
    var sb = new StringBuilder("pass,duration_ms\n");
    timings.forEach(t -> sb.append(t.name()).append(',').append(t.durationMs()).append('\n'));
    var csvPath = output.resolve("timings.csv");
    try {
      Files.writeString(csvPath, sb, StandardCharsets.UTF_8);
    } catch (IOException e) {
      System.err.printf("Warning: could not write timings CSV to %s: %s%n",
          csvPath, e.getMessage());
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
    if (config != null) {
      return config;
    }

    config = new GeneralConfiguration(input, output, dump);

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
      final var totalStartTime = System.nanoTime();
      var viam = parseToVIAM();
      var passOrder = passOrder(getConfig());
      var passManager = new PassManager();
      passManager.add(passOrder);
      passManager.run(viam);
      var result = passManager.getPassResults();
      result.executedPasses()
          .forEach(p -> timings.add(new Timing(p.pass().getName().value(), p.durationMs())));
      timings.add(new Timing("Total", (System.nanoTime() - totalStartTime) / 1_000_000));


    } catch (TypeConversionException | MaxValuesExceededException e) {
      // Re-throw to let Picoli handle it
      throw e;
    } catch (Diagnostic d) {
      System.out.println(new DiagnosticPrinter(new DiskVirtualFileSystem()).toString(d));
      if (showStacktrace) {
        System.out.println(getStackTrace(d));
      }
      returnVal = 1;
    } catch (DiagnosticList d) {
      System.out.println(new DiagnosticPrinter(new DiskVirtualFileSystem()).toString(d));
      if (showStacktrace) {
        System.out.println(getStackTrace(d));
      }
      returnVal = 1;
    } catch (RuntimeException | IOException | DuplicatedPassKeyException e) {
      System.out.println("""
                                       ___ ___    _   ___ _  _      
                                      / __| _ \\  /_\\ / __| || |   
                                     | (__|   / / _ \\\\__ \\ __ |    
                                      \\___|_|_\\/_/ \\_\\___/_||_| 
          
                                 🔥 The OpenVADL compiler crashed 🔥  
          
          This shouldn't have happened, please open an issue with the stacktrace below at:
          https://github.com/OpenVADL/open-vadl/issues/new
          """);

      printPaths("\nBefore the crash, the following dumps were generated:",
          ArtifactTracker.getDumpPaths());
      System.out.println();

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
      System.out.println(new DiagnosticPrinter(new DiskVirtualFileSystem()).toString(
          DeferredDiagnosticStore.getAll()));

      // Only exit abnormally if any diagnostic message is an error.
      if (DeferredDiagnosticStore.getAll().stream()
          .anyMatch(diagnostic -> diagnostic.level == Diagnostic.Level.ERROR)) {
        returnVal = 1;
      }
    }

    printPaths(returnVal == 0
            ? "\nThe following artifacts were generated:"
            : "\nEven though some errors occurred, the following artifacts were generated:",
        ArtifactTracker.getArtifactPathsPaths());

    printPaths(returnVal == 0
            ? "\nThe following dumps were generated:"
            : "\nEven though some errors occurred, the following dumps were generated:",
        ArtifactTracker.getDumpPaths()
    );

    printTimings();
    writeTimingsCsv();

    return returnVal;
  }
}
