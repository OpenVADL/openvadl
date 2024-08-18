package vadl.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import javax.annotation.Nullable;

public class FileUtils {

  public static File writeToTempFile(String content, String prefix, @Nullable String suffix)
      throws IOException {
    var tempFile = File.createTempFile(prefix, suffix);
    tempFile.deleteOnExit();
    var writer = new FileWriter(tempFile);
    writer.write(content);
    writer.close();
    return tempFile;
  }

  public static Path copyDirToTempDir(URI dirPath, String prefix) throws IOException {
    // Create a temporary directory
    Path tempDir = Files.createTempDirectory(prefix);
    var tempFile = tempDir.toFile();
    // delete temp afterwards
    tempFile.deleteOnExit();

    copyDir(dirPath, tempDir);

    return tempDir;
  }

  public static void copyDir(URI source, Path destination) throws IOException {
    if (source.getScheme().equals("jar")) {
      copyDirInJar(source, destination);
    } else {
      copyNormalDir(new File(source), destination.toFile());
    }
  }

  private static void copyNormalDir(File from, File to) throws IOException {
    org.testcontainers.shaded.org.apache.commons.io.FileUtils.copyDirectory(
        from, to
    );
  }

  private static void copyDirInJar(URI jarDir, Path target) throws IOException {
    if (!jarDir.getScheme().equals("jar")) {
      throw new IllegalArgumentException("jarDir must be a jar file");
    }

    try (var fs = FileSystems.newFileSystem(jarDir, Collections.emptyMap())) {

      var pathInJar = fs.getPath(jarDir.toString().split("!")[1]);
      Files.walkFileTree(pathInJar, new SimpleFileVisitor<>() {

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            throws IOException {
          Path currentTarget = target.resolve(pathInJar.relativize(dir).toString());
          Files.createDirectories(currentTarget);
          return FileVisitResult.CONTINUE;
        }

        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          var relativePathInFs = pathInJar.relativize(file);
          var targetFilePath = target.resolve(relativePathInFs.toString());
          Files.copy(file, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }
}
