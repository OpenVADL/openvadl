package vadl.template;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Map;
import javax.annotation.Nullable;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassResults;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This is a {@link Pass} to emit files for the generators.
 * Every file which should be emitted will extend from this class.
 */
public abstract class AbstractTemplateRenderingPass extends Pass {
  private static final TemplateEngine templateEngine = templateEngine();

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
   * Path prefix where the template gets stored to.
   */
  private final String outputPathPrefix;

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

  public AbstractTemplateRenderingPass(String outputPathPrefix) throws IOException {
    this.outputPathPrefix = outputPathPrefix;
  }

  @Override
  public PassName getName() {
    return new PassName("EmitFile");
  }

  private FileWriter createFileWriter() throws IOException {
    var file = new File(outputPathPrefix + "/" + getOutputPath());

    if (!file.getParentFile().exists()) {
      ensure(file.getParentFile().mkdirs(), "Cannot create parent directories");
    }

    if (!file.exists()) {
      ensure(file.createNewFile(), "Cannot create new file");
    }

    return new FileWriter(file, Charset.defaultCharset());
  }

  @Nullable
  @Override
  public Object execute(final PassResults passResults, Specification viam)
      throws IOException {
    renderTemplate(passResults, viam, createFileWriter());

    // This pass emits files and does not need to store data.
    return null;
  }

  /**
   * Renders the template into a {@link StringWriter}. Additionally, this will not create a
   * folder in the output path.
   */
  public void renderToString(final PassResults passResults, Specification viam,
                             StringWriter writer) {
    renderTemplate(passResults, viam, writer);
  }

  private void renderTemplate(final PassResults passResults, Specification viam,
                              Writer writer) {
    var ctx = new Context();

    // Map the variables into thymeleaf's context
    createVariables(passResults, viam).forEach(ctx::setVariable);

    templateEngine.process(getTemplatePath(), ctx, writer);
  }
}
