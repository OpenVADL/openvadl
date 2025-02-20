package vadl.template;

import static vadl.viam.ViamError.ensure;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
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
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.formatting.CodeFormatter;
import vadl.dump.ArtifactTracker;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This is a {@link Pass} to emit files for the generators.
 * Every file which should be emitted will extend from this class.
 */
public abstract class AbstractTemplateRenderingPass extends Pass {
  private static final TemplateEngine templateEngine = templateEngine();
  private static final Logger log = LoggerFactory.getLogger(AbstractTemplateRenderingPass.class);

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

  /**
   * The result of a rendering pass.
   */
  public static class Result {
    public final Path emittedFile;

    /**
     * Constructs the result.
     *
     * @param emittedFile the path to the file that was rendered/emitted
     */
    protected Result(Path emittedFile) {
      this.emittedFile = emittedFile;
    }

    public Path emittedFile() {
      return emittedFile;
    }
  }

  /**
   * Path prefix where the template gets stored to.
   */
  private final String subDir;

  /**
   * Get the path for the template which should be rendered.
   */
  protected abstract String getTemplatePath();

  /**
   * Get the path where the file will be written to.
   */
  protected abstract String getOutputPath();

  /**
   * The map with the variables for the template. This method has access to the {@code passResults}
   * from the passes which have run before.
   */
  protected abstract Map<String, Object> createVariables(final PassResults passResults,
                                                         Specification specification);

  public AbstractTemplateRenderingPass(GeneralConfiguration configuration, String subDir) {
    super(configuration);
    this.subDir = subDir;
  }

  @Override
  public PassName getName() {
    return new PassName("EmitFile " + getOutputPath());
  }

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

    var finalFilePath = createOutputPath(configuration(), subDir, getOutputPath());
    var writer = createFileWriter(finalFilePath);
    if (this.subDir.equals("dump")) {
      ArtifactTracker.addDump(finalFilePath);
    } else {
      ArtifactTracker.addArtifact(finalFilePath);
    }

    renderTemplate(passResults, viam, writer);
    formatRenderedFile(finalFilePath);

    return constructResult();
  }

  /**
   * Allows subtypes of this pass to construct their own result.
   */
  public Result constructResult() {
    return new Result(getEmittedFile());
  }

  /**
   * Get the path of the emitted file.
   */
  public Path getEmittedFile() {
    // TODO: This is very sub optimal as it assumes some implementation of the createWriter
    //   We have to refactor this! However, we definitely should not store the whole template
    //   string for every pass in memory!
    return Path.of(configuration().outputPath() + "/" + subDir + "/" + getOutputPath())
        .toAbsolutePath();
  }

  /**
   * Renders the template into a {@link StringWriter}. Additionally, this will not create a
   * folder in the output path.
   */
  public String renderToString(final PassResults passResults, Specification viam) {
    var stringWriter = new StringWriter();
    var ctx = new Context();
    // Map the variables into thymeleaf's context
    createVariables(passResults, viam).forEach(ctx::setVariable);
    templateEngine.process(getTemplatePath(), ctx, stringWriter);
    return stringWriter.toString();
  }

  private void renderTemplate(final PassResults passResults, Specification viam,
                              Writer writer) throws IOException {
    var ctx = new Context();
    // Map the variables into thymeleaf's context
    var vars = createVariables(passResults, viam);
    // check if variables have correct type.
    // for rendering, only primitive types, maps, and lists are valid.
    try {
      vars = VariableChecker.checkVariables(vars);
    } catch (IllegalRenderTypeException e) {
      log.error("Illegal render type during rendering of {} in {}", getTemplatePath(),
          this.getClass().getSimpleName(), e);
      throw new RuntimeException(e);
    }
    vars.forEach(ctx::setVariable);
    templateEngine.process(getTemplatePath(), ctx, writer);
    writer.flush();
    writer.close();
  }

  private void formatRenderedFile(Path filePath) {
    var formatter = getFormatter();
    if (formatter != null) {
      try {
        formatter.format(filePath);
      } catch (CodeFormatter.NotAvailableException | CodeFormatter.FormatFailureException e) {
        log.warn("Failed to apply code formatter: {}", e.getMessage());
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

}
