package vadl.dump;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A global store to record which artifacts and dumps were generated to later inform the user about.
 *
 * <p>Often the user requests certain dumps. However, in certain error conditions, some dumps might
 * not be created (for example, if the typechecker fails, all passes further down the road cannot
 * that depend on types cannot be executed and cannot produce any dumps).
 */
public class ArtifactTracker {
  private static final List<Path> artifactPaths = new ArrayList<>();
  private static final List<Path> dumpPaths = new ArrayList<>();

  private ArtifactTracker() {
  }

  /**
   * Add a path of a dump to be recorded.
   * The path should be relative to the working directory.
   *
   * @param path to be stored.
   */
  public static void addDump(Path path) {
    dumpPaths.add(path);
  }

  public static List<Path> getDumpPaths() {
    return dumpPaths;
  }

  /**
   * Add a path of a artifact to be recorded.
   * The path should be relative to the working directory.
   *
   * @param path to be stored.
   */
  public static void addArtifact(Path path) {
    artifactPaths.add(path);
  }

  public static List<Path> getArtifactPathsPaths() {
    return artifactPaths;
  }
}
