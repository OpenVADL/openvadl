package vadl.test;

import java.util.HashMap;
import java.util.Map;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * This is a cached representation of a {@link ImageFromDockerfile}.
 * The testcontainer implementation remembers when an image has been already built.
 * The goal of this class is to get an already existing instance of a prebuild image when
 * the docker file and the build args are the same.
 */
public class CachedImageFromDockerfile extends ImageFromDockerfile {
  private static final Map<String, Map<Map<String, String>, ImageFromDockerfile>> cache =
      new HashMap<>();

  /**
   * Constructor.
   */
  public CachedImageFromDockerfile(String image, boolean b) {
    super(image, b);
  }

  @Override
  public ImageFromDockerfile withBuildArg(String key, String value) {
    var args = this.getBuildArgs();
    args.put(key, value);

    if (getDockerFilePath().isPresent()) {
      var dockerFile = cache.get(getDockerFilePath().get());
      if (dockerFile != null && dockerFile.containsKey(args)) {
        return dockerFile.get(args);
      }
    }

    return super.withBuildArg(key, value);
  }
}
