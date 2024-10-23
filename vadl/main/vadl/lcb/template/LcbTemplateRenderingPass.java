package vadl.lcb.template;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.RegisterFile;

/**
 * Abstracts the subdir under the output.
 */
public abstract class LcbTemplateRenderingPass extends AbstractTemplateRenderingPass {
  public LcbTemplateRenderingPass(GeneralConfiguration configuration) throws IOException {
    super(configuration, "lcb");
  }

  public LcbConfiguration lcbConfiguration() {
    return (LcbConfiguration) configuration();
  }

  protected String renderRegister(RegisterFile registerFile, int addr) {
    return registerFile.identifier.simpleName() + addr;
  }

  protected static <T> Predicate<T> distinctByKey(
      Function<? super T, ?> keyExtractor) {

    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
}
