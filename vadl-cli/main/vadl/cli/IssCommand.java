// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.cli;

import static org.apache.commons.io.file.PathUtils.isEmptyDirectory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
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

  @CommandLine.Option(
      names = "--init",
      description = "Download and prepare QEMU before generating ISS.")
  private boolean init;

  private static final String QEMU_VERSION = "9.2.2";
  private static final String QEMU_DOWNLOAD_URL =
      "https://github.com/qemu/qemu/archive/refs/tags/v" + QEMU_VERSION + ".tar.gz";
  private static final String QEMU_SHA256_CHECKSUM = "E6zBznDb31pHb/Zt7nV9GSKrlFVPSWzvYgEZWQ9/cQI=";
  // used to show progress bar when downloading
  private static final int QEMU_TAR_BYTE_SIZE = 39_845_342;

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

    // by setting this variable using the command line, we could support
    // QEMU initialization without user interaction.
    var force = false;
    var config = getConfig();
    var issOutputPath = config.outputPath().resolve("iss");
    if (!createOutputDirectory(issOutputPath, force)) {
      return true;
    }

    if (!force && !checkUserSourceTrust()) {
      // abort complete execution if the user does not trust the QEMU source
      return false;
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

  // check if the user trusts the download URL
  private boolean checkUserSourceTrust() throws IOException {
    if (!Utils.getUserConfirmation(
        "OpenVADL is about to download QEMU from the following URL:\n" + QEMU_DOWNLOAD_URL
            + "\nDo you trust this source?")) {
      System.out.println("Download aborted by the user.");
      return false;
    }
    return true;
  }

  // returns true if we should continue, otherwise false
  private boolean createOutputDirectory(Path issOutputPath, boolean force) throws IOException {
    if (Files.exists(issOutputPath)) {
      if (isEmptyDirectory(issOutputPath)) {
        // an empty directory does not contain a project yet
        return true;
      }

      if (!isValidQemuDirectory(issOutputPath)) {
        System.out.println("Warning: output/iss exists but does not contain a valid QEMU project.");
        if (!force && !Utils.getUserConfirmation("Do you want to overwrite and re-download?")) {
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

  private void downloadFile(String fileURL, Path savePath) throws IOException {
    URL url = new URL(fileURL);
    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
    httpConn.setRequestProperty("User-Agent", "Mozilla/5.0");
    int responseCode = httpConn.getResponseCode();

    // Create parent directories if they don't exist
    Files.createDirectories(savePath.getParent());

    if (responseCode == HttpURLConnection.HTTP_OK) {
      int contentLength = httpConn.getContentLength();
      // fallback to QEMU_TAR_BYTE_SIZE
      contentLength = contentLength == -1 ? QEMU_TAR_BYTE_SIZE : contentLength;
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
    // var progressBar = new ProgressBar(Files.size(archive));
    var checksum =
        Utils.calculateSHA256Checksum(archive, null);
    // progressBar.complete();
    if (!QEMU_SHA256_CHECKSUM.equals(checksum)) {
      throw new IOException("Checksum does not match. Downloaded QEMU source code is corrupted.");
    }
  }

  private void extractTarXz(Path archive, Path outputDir) throws IOException {
    TarGzExtractor.extractTarGz(archive, outputDir, 1);
  }

}
