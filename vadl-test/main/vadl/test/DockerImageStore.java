package vadl.test;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * Singleton class to manage multiple {@link ImageFromDockerfile}. The benefit of storing
 * existing images is that once an image has been build then it does not need to be built again.
 */
public class DockerImageStore {
  private static final Map<String, ImageFromDockerfile> store = new HashMap<>();

  /**
   * Returns {@code true} when an {@link ImageFromDockerfile} exists with the given {@code key}.
   */
  public static boolean containsImage(String key) {
    return store.containsKey(key);
  }

  /**
   * Get an image with a given {@code key}.
   */
  @Nullable
  public static ImageFromDockerfile getImage(String key) {
    return store.get(key);
  }

  /**
   * Put the given {@code image} into the store.
   */
  public static void setImage(String key, ImageFromDockerfile image) {
    store.put(key, image);
  }

  /**
   * Check whether the store has already stored an {@code image} with the {@code key}.
   * If yes, then return the cached image. If not, then return the original image but store it
   * into the cache.
   */
  public static ImageFromDockerfile replaceWithCachedImage(String key, ImageFromDockerfile image) {
    if (containsImage(key)) {
      return getImage(key);
    }

    setImage(key, image);
    return image;
  }

  /**
   * Clears any existing saved images.
   */
  public static void clear() {
    store.clear();
  }
}
