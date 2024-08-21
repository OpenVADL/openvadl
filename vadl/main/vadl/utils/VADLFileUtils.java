package vadl.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;

/**
 * A collection of useful methods to handle files.
 *
 * <p>The name prefixes VADL to prevent confusion with Apache's
 * {@link org.apache.commons.io.FileUtils}.
 */
public class VADLFileUtils {


  /**
   * Writes the given content to a temporary file.
   *
   * @param content the content to write to the file
   * @param prefix  the prefix of the temporary file name
   * @param suffix  the suffix of the temporary file name
   * @return the created temporary file
   */
  public static File writeToTempFile(String content, String prefix, @Nullable String suffix)
      throws IOException {
    var tempFile = File.createTempFile(prefix, suffix);
    tempFile.deleteOnExit();
    var writer = new FileWriter(tempFile);
    writer.write(content);
    writer.close();
    return tempFile;
  }

  /**
   * Copies a directory to a temporary directory with an optional file transformer.
   * This method can also be used for directories inside a JAR (like the classpath/resource).
   *
   * @param dirPath the URI of the directory to copy
   * @param prefix  the prefix for the name of the temporary directory
   * @throws IOException if there is an I/O error during the copying process
   */
  public static Path copyDirToTempDir(URI dirPath, String prefix) throws IOException {
    return copyDirToTempDir(dirPath, prefix, null);
  }

  /**
   * Copies a directory to a temporary directory with an optional file transformer.
   * This method can also be used for directories inside a JAR (like the classpath/resource).
   *
   * @param dirPath         the URI of the directory to copy
   * @param prefix          the prefix for the name of the temporary directory
   * @param fileTransformer the consumer function to transform each file in the directory (optional)
   * @return the path of the created temporary directory
   */
  public static Path copyDirToTempDir(URI dirPath, String prefix,
                                      @Nullable
                                      Consumer<Pair<BufferedReader, Writer>> fileTransformer)
      throws IOException {
    // Create a temporary directory
    Path tempDir = Files.createTempDirectory(prefix);
    var tempFile = tempDir.toFile();
    // delete temp dir on jvm shutdown
    // (file.deleteOnExit is not enough as it only deletes if no files in dir)
    deleteDirectoryOnExit(tempFile);

    copyDir(dirPath, tempDir, fileTransformer);

    return tempDir;
  }

  /**
   * Copies a directory from the given source URI to the specified destination path.
   * Supports copying directories inside a JAR file.
   *
   * @param source      the source URI of the directory to copy
   * @param destination the destination path to copy the directory to
   */
  public static void copyDir(URI source, Path destination) throws IOException {
    copyDir(source, destination, null);
  }

  /**
   * Copies a directory from the given source URI to the specified destination path.
   * Supports copying directories inside a JAR file.
   *
   * @param source          the source URI of the directory to copy
   * @param destination     the destination path to copy the directory to
   * @param fileTransformer the consumer function to transform each file in the directory (optional)
   */
  public static void copyDir(URI source, Path destination,
                             @Nullable Consumer<Pair<BufferedReader, Writer>> fileTransformer)
      throws IOException {
    if (source.getScheme().equals("jar")) {
      copyDirInJar(source, destination, fileTransformer);
    } else {
      copyNormalDir(new File(source), destination.toFile(), fileTransformer);
    }
  }

  private static void copyNormalDir(File from, File to,
                                    @Nullable
                                    Consumer<Pair<BufferedReader, Writer>> fileTransformer)
      throws IOException {
    if (fileTransformer == null) {
      // use apache utility function for normal copy (assume to be more perfomant)
      FileUtils.copyDirectory(
          from, to
      );
    } else {
      // use custom walktree dir copy implementation with file transformer
      walktreeDirCopy(from.toPath(), to.toPath(), fileTransformer);
    }
  }

  private static void copyDirInJar(URI jarDir, Path target,
                                   @Nullable Consumer<Pair<BufferedReader, Writer>> fileTransformer)
      throws IOException {
    if (!jarDir.getScheme().equals("jar")) {
      throw new IllegalArgumentException("jarDir must be a jar file");
    }

    try (var fs = FileSystems.newFileSystem(jarDir, Collections.emptyMap())) {
      var pathInJar = fs.getPath(jarDir.toString().split("!")[1]);
      walktreeDirCopy(pathInJar, target, fileTransformer);
    }
  }

  private static void walktreeDirCopy(Path sourceDir, Path destinationDir,
                                      @Nullable
                                      Consumer<Pair<BufferedReader, Writer>> fileTransformer)
      throws IOException {
    Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {

      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
          throws IOException {
        Path currentTarget = destinationDir.resolve(sourceDir.relativize(dir).toString());
        Files.createDirectories(currentTarget);
        return FileVisitResult.CONTINUE;
      }

      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        var relativePathInFs = sourceDir.relativize(file);
        var targetFilePath = destinationDir.resolve(relativePathInFs.toString());

        if (fileTransformer == null) {
          // if there is no fileTransformer, we just copy the file.
          Files.copy(file, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        } else {
          // if the file transformer is given, we apply it to the copy process
          var isr = Files.newInputStream(file);
          var reader = new BufferedReader(new InputStreamReader(isr));
          try (var outFileWriter = new FileWriter(targetFilePath.toFile())) {
            fileTransformer.accept(Pair.of(reader, outFileWriter));
          }
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /// DELETION API ///

  // A set of directories to delete on exit
  private static final Set<File> dirsToDeleteOnExit = new HashSet<>();
  // a thread that is applied as a shutdown hook.
  // it will delete all directories in the dirsToDeleteOnExit set
  private static final Thread deleteExecutorThread = new Thread(() -> {
    var errors = new ArrayList<IOException>();
    for (var dir : dirsToDeleteOnExit) {
      try {
        deleteDirectoryOrFile(dir);
      } catch (IOException e) {
        errors.add(e);
      }
    }
    if (!errors.isEmpty()) {
      throw new RuntimeException("Failed to delete directories: " + errors, errors.get(0));
    }
  });

  /**
   * Deletes the specified directory on JVM shutdown (including sub-dirs and files).
   *
   * @param dir the directory to be deleted
   */
  public static synchronized void deleteDirectoryOnExit(File dir) {
    if (dirsToDeleteOnExit.isEmpty()) {
      // If it is the first registration, set the hook
      Runtime.getRuntime().addShutdownHook(deleteExecutorThread);
    }

    dirsToDeleteOnExit.add(dir);
  }

  /**
   * Deletes the specified directory or file.
   *
   * @param dir the directory or file to be deleted
   */
  public static void deleteDirectoryOrFile(File dir) throws IOException {
    if (!dir.exists()) {
      return;
    }
    if (dir.isFile()) {
      dir.delete();
      return;
    }
    FileUtils.deleteDirectory(dir);
  }

  /**
   * Creates a directory and schedules it for deletion.
   */
  public static Path createTempDirectory(String prefix) throws IOException {
    var directory = Files.createTempDirectory("OpenVADL-" + prefix);
    deleteDirectoryOnExit(new File(String.valueOf(directory)));
    return directory;
  }

}


