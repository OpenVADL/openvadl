package vadl.cli;

import static org.apache.commons.io.file.PathUtils.isEmptyDirectory;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.file.PathUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;

/**
 * The Command does provide the iss subcommand.
 */
@Command(
    name = "iss",
    description = "Generate the ISS (Instruction Set Simulator)",
    mixinStandardHelpOptions = true
)
public class IssCommand extends BaseCommand {


  @CommandLine.Option(names = {"--dry-run"},
      description = "Don't emit generated files.")
  boolean dryRun;

  @CommandLine.Option(names = {"--init"},
      description = "Download and prepare QEMU before generating ISS.")
  boolean init;

  private static String QEMU_VERSION = "9.0.3";
  private static String QEMU_DOWNLOAD_URL =
      "https://github.com/qemu/qemu/archive/refs/tags/v" + QEMU_VERSION + ".tar.gz";
  private static String QEMU_SHA256_CHECKSUM = "e9G8+p+nv31UJz4HvCWCHVC89AT3bMcxRC6GSW9oykc=";

  @Override
  PassOrder passOrder(GeneralConfiguration configuration) throws IOException {
    var issConfig = new IssConfiguration(configuration);
    issConfig.setDryRun(dryRun);
    return PassOrders.iss(issConfig);
  }

  @Override
  public Integer call() {
    if (!setup()) {
      // if setup failed, return exit code 1
      return 1;
    }
    return super.call();
  }

  private boolean setup() {
    try {
      return init();
    } catch (IOException | NoSuchAlgorithmException e) {
      System.err.println("Error: " + e.getMessage());
      return false;
    }
  }

  private boolean init() throws IOException, NoSuchAlgorithmException {
    if (!init) {
      return true;
    }
    var config = getConfig();
    var issOutputPath = config.outputPath().resolve("iss");
    if (!createOutputDirectory(issOutputPath)) {
      return true;
    }

    System.out.println("Downloading QEMU " + QEMU_VERSION + "...");
    var archive = issOutputPath.resolve("qemu-" + QEMU_VERSION + ".tar.gz");
    try {
      downloadFile(QEMU_DOWNLOAD_URL, archive);
      System.out.println("Verifying QEMU checksum...");
      testChecksum(archive);
      System.out.println("Extracting QEMU...");
      extractTarXz(archive, issOutputPath);
      System.out.println("QEMU " + QEMU_VERSION + " is ready, generating ISS...");
    } finally {
      // always delete downloaded archive
      Files.deleteIfExists(archive);
    }
    return true;
  }

  // returns true if we should continue, otherwise false
  private boolean createOutputDirectory(Path issOutputPath) throws IOException {
    if (Files.exists(issOutputPath)) {
      if (isEmptyDirectory(issOutputPath)) {
        // an empty directory does not contain a project yet
        return true;
      }
      if (!isValidQemuDirectory(issOutputPath)) {
        System.out.println("Warning: output/iss exists but does not contain a valid QEMU project.");
        System.out.print("Do you want to overwrite and re-download? (y/N): ");
        if (!confirmUserInput()) {
          System.out.println("Aborting.");
          return false;
        }
        PathUtils.cleanDirectory(issOutputPath);
      } else {
        System.out.println("QEMU is already present. Skipping download.");
        return false;
      }
    } else {
      Files.createDirectories(issOutputPath);
    }
    return true;
  }

  private boolean isValidQemuDirectory(Path dir) throws IOException {
    Path versionFile = dir.resolve("VERSION");

    boolean filesExist = Files.exists(dir.resolve("configure"))
        && Files.exists(dir.resolve("Makefile"))
        && Files.exists(versionFile);
    if (!filesExist) {
      return false;
    }

    try {
      String version = Files.readString(versionFile).trim();
      return QEMU_VERSION.equals(version);
    } catch (IOException e) {
      throw new IOException("Error reading QEMU version file: " + e.getMessage());
    }
  }

  private boolean confirmUserInput() throws IOException {
    byte[] input = new byte[1];
    System.in.read(input);
    return input[0] == 'y' || input[0] == 'Y';
  }

  private void downloadFile(String fileURL, Path savePath) throws IOException {
    URL url = new URL(fileURL);
    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
    httpConn.setRequestProperty("User-Agent", "Mozilla/5.0");
    int responseCode = httpConn.getResponseCode();

    // Create parent directories if they don't exist
    Files.createDirectories(savePath.getParent());

    if (responseCode == HttpURLConnection.HTTP_OK) {
      int contentLength = httpConn.getContentLength();
      ProgressBar progressBar = new ProgressBar(contentLength);

      try (InputStream inputStream = httpConn.getInputStream();
           FileOutputStream outputStream = new FileOutputStream(savePath.toFile())) {

        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
          progressBar.update(bytesRead);
        }
        progressBar.complete();
      }
    } else {
      throw new IOException("Failed to download file: HTTP " + responseCode);
    }
  }

  private void testChecksum(Path archive) throws IOException, NoSuchAlgorithmException {
    var progressBar = new ProgressBar(Files.size(archive));
    var checksum =
        Utils.calculateSHA256Checksum(archive, progressBar::update);
    progressBar.complete();
    if (!QEMU_SHA256_CHECKSUM.equals(checksum)) {
      throw new IOException("Checksum does not match. Downloaded QEMU source code is corrupted.");
    }
  }

  private void extractTarXz(Path archive, Path outputDir) throws IOException {
    try (var fileInputStream = Files.newInputStream(archive);
         var bufferedInputStream = new BufferedInputStream(fileInputStream);
         var xzInputStream = new GzipCompressorInputStream(bufferedInputStream);
         var tarInputStream = new TarArchiveInputStream(xzInputStream)) {

      TarArchiveEntry entry;
      String topLevelDir = null;

      while ((entry = tarInputStream.getNextEntry()) != null) {
        String entryName = entry.getName();

        // Detect top-level directory
        if (topLevelDir == null) {
          int firstSlash = entryName.indexOf('/');
          if (firstSlash != -1) {
            topLevelDir = entryName.substring(0, firstSlash + 1); // "qemu-9.0.3/"
          }
        }

        // Remove top-level directory
        if (topLevelDir != null && entryName.startsWith(topLevelDir)) {
          entryName = entryName.substring(topLevelDir.length());
        }

        if (entryName.isEmpty()) {
          continue; // Skip empty root directory entry
        }

        Path outputPath = outputDir.resolve(entryName);
        if (entry.isDirectory()) {
          Files.createDirectories(outputPath);
        } else {
          Files.createDirectories(outputPath.getParent());
          try (var outputStream = Files.newOutputStream(outputPath)) {
            tarInputStream.transferTo(outputStream);
          }
        }
      }
    }
  }

}
