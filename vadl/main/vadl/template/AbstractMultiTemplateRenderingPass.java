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

package vadl.template;

import static vadl.viam.ViamError.ensure;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import vadl.OpenVadlProperties;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.formatting.CodeFormatter;
import vadl.dump.ArtifactTracker;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.Pass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This pass allows rendering one or more files from a template.
 * By overriding the {@link #createRenderInputs(PassResults, Specification)},
 * subtypes may specify the output path and variables used during rendering for each
 * rendered file.
 *
 * @see Result
 */
public abstract class AbstractMultiTemplateRenderingPass extends Pass {
  private static final TemplateEngine templateEngine = templateEngine();
  private static final Logger log = LoggerFactory.getLogger(
      AbstractMultiTemplateRenderingPass.class);

  /**
   * The result of a rendering pass.
   * It contains the list of emitted files.
   * A custom rendering pass can extend this class and override the
   * {@link #constructResult(List)} to customize the pass result.
   */
  public static class Result {
    private final List<Path> emittedFiles;

    /**
     * Constructs the result.
     *
     * @param emittedFiles the paths to the files that were rendered/emitted
     */
    protected Result(List<Path> emittedFiles) {
      this.emittedFiles = emittedFiles;
    }

    public List<Path> emittedFiles() {
      return emittedFiles;
    }
  }

  /**
   * The rendering input.
   * Each output file is constructed from one rendering input file.
   *
   * @param outputPath the file's output path (relative to the generator's output directory).
   * @param variables  the variables used for rendering the template.
   */
  public record RenderInput(
      String outputPath,
      Map<String, Object> variables
  ) {
  }

  /**
   * Path prefix where the template gets stored to.
   */
  protected final String subDir;

  public AbstractMultiTemplateRenderingPass(GeneralConfiguration configuration, String subDir) {
    super(configuration);
    this.subDir = subDir;
  }

  /**
   * Get the path for the template which should be rendered.
   */
  protected abstract String getTemplatePath();

  /**
   * Set the line comment that should be used to start the copyright notice.
   */
  protected String lineComment() {
    return "//";
  }

  /**
   * Enable/Disable the copyright notice at the start of the file.
   */
  protected boolean enableCopyright() {
    return true;
  }

  /**
   * Construct the rendering inputs used during template rendering.
   * Each render input corresponds to one rendered file.
   *
   * @return a list of rendering inputs, one per output file.
   */
  protected abstract List<RenderInput> createRenderInputs(final PassResults passResults,
                                                          Specification specification);

  /**
   * Allows a subclass defining a code formatter that formats the emitted file.
   * See {@link IssTemplateRenderingPass#getFormatter()} for an example.
   */
  public @Nullable CodeFormatter getFormatter() {
    return null;
  }

  @Nonnull
  @Override
  public Result execute(final PassResults passResults, Specification viam)
      throws IOException {

    var inputs = createRenderInputs(passResults, viam);
    var emittedFiles = new ArrayList<Path>();
    for (var input : inputs) {
      var finalFilePath = createOutputPath(configuration(), subDir, input.outputPath);
      emittedFiles.add(finalFilePath);

      var writer = createFileWriter(finalFilePath);
      if (this.subDir.equals("dump")) {
        ArtifactTracker.addDump(finalFilePath);
      } else {
        ArtifactTracker.addArtifact(finalFilePath);
      }

      renderTemplate(input.variables, writer);
      formatRenderedFile(finalFilePath);
    }


    return constructResult(emittedFiles);
  }

  /**
   * Allows subtypes of this pass to construct their own result.
   */
  protected Result constructResult(List<Path> emittedFiles) {
    return new Result(emittedFiles);
  }

  private void renderTemplate(Map<String, Object> vars,
                              Writer writer) throws IOException {
    var ctx = new Context();
    // check if variables have correct type.
    // for rendering, only primitive types, maps, and lists are valid.
    try {
      vars = VariableNormalizer.normalizeAndCheckVariables(vars);
    } catch (IllegalRenderTypeException e) {
      log.error("Illegal render type during rendering of {} in {}", getTemplatePath(),
          this.getClass().getSimpleName(), e);
      throw new RuntimeException(e);
    }
    vars.forEach(ctx::setVariable);

    // Wrap the original writer to prepend the copyright notice.
    try (
        Writer wrappedWriter = new PrependingWriter(writer, getCopyrightNotice())) {

      // if copyright is enabled, we use the wrapped writer.
      var actualWriter = enableCopyright() ? wrappedWriter : writer;
      templateEngine.process(getTemplatePath(), ctx, actualWriter);
      actualWriter.flush();
    }
  }

  private void formatRenderedFile(Path filePath) {
    var formatter = getFormatter();
    if (formatter != null) {
      try {
        formatter.format(filePath);
      } catch (CodeFormatter.NotAvailableException | CodeFormatter.FormatFailureException e) {
        log.debug("Failed to apply code formatter: {}", e.getMessage());
      }
    }
  }

  private Path createOutputPath(GeneralConfiguration configuration, String subDir,
                                String outputPath) {
    return Path.of(configuration.outputPath().toString(), subDir, outputPath);
  }

  private FileWriter createFileWriter(Path filePath)
      throws IOException {
    var file = filePath.toFile();

    if (!file.getParentFile().exists()) {
      ensure(file.getParentFile().mkdirs(), "Cannot create parent directories of %s", file);
    }

    if (!file.exists()) {
      ensure(file.createNewFile(), "Cannot create new file %s", file);
    }

    return new FileWriter(file, Charset.defaultCharset());
  }

  @SuppressWarnings("LineLength")
  private String getCopyrightNotice() {
    String version = OpenVadlProperties.getVersion();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm:ss")
        .withZone(ZoneOffset.UTC);
    String timestamp = formatter.format(Instant.now()) + " UTC";
    StringBuilder sb = new StringBuilder();
    return sb.append(lineComment())
        .append(" This file is machine generated by OpenVADL ")
        .append(version).append(" on ").append(timestamp)
        .append("\n").append(lineComment())
        .append(" and is therefore not copyrightable and in the public domain.\n")
        .append(lineComment())
        .append(
            " It is recommended to not modify this file, but the sources from which it was generated.")
        .append("\n\n") // double new line to have some space
        .toString();
  }

  private static TemplateEngine templateEngine() {
    TemplateEngine templateEngine = new TemplateEngine();
    templateEngine.addTemplateResolver(templateResolver());
    return templateEngine;
  }

  private static ITemplateResolver templateResolver() {
    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("/templates/");
    templateResolver.setTemplateMode(TemplateMode.TEXT);
    templateResolver.setCharacterEncoding("UTF8");
    templateResolver.setCheckExistence(true);
    templateResolver.setCacheable(false);
    return templateResolver;
  }


}
