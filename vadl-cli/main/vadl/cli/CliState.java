package vadl.cli;

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
import vadl.error.Diagnostic;
import vadl.utils.SourceLocation;

public class CliState {

  private List<Path> dumpedFiles = new ArrayList<>();

  private static final CliState instance = new CliState();

  private CliState() {
  }

  public static CliState getInstance() {
    return instance;
  }

  /**
   * Dumps should contain their date this method returns the date in a uniform string.
   * Format is YYYY-MM-DD hh:mm:ss
   *
   * @return the current time as a string
   */
  public String getTimeString() {
    var now = LocalDateTime.now(ZoneId.systemDefault());
    return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }

  public void dumpFile(Path outputPath, String fileName, CharSequence content) {
    var folderPath = Paths.get(outputPath.toString(), "dump");
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

    dumpedFiles.add(filePath);
  }

  public List<Path> getDumpedFiles() {
    return dumpedFiles;
  }

  public void printDumpedFiles() {
    if (dumpedFiles.isEmpty()) {
      return;
    }

    System.out.println("\nThe following dumps were created:");
    dumpedFiles.forEach(path -> System.out.printf("\t- %s\n", path));
  }
}
