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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

class Utils {

  static String calculateSHA256Checksum(Path filePath, @Nullable Consumer<Integer> progress)
      throws IOException, NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] buffer = new byte[4096];
    int bytesRead;

    try (InputStream fis = Files.newInputStream(filePath);
         DigestInputStream dis = new DigestInputStream(fis, digest)) {

      while ((bytesRead = dis.read(buffer)) != -1) {
        if (progress != null) {
          progress.accept(bytesRead);
        }
      }
    }

    byte[] hashBytes = digest.digest();
    return Base64.getEncoder().encodeToString(hashBytes);
  }

  /**
   * Prompts the user with a message and returns true if the user confirms positively.
   *
   * @param message The message to display to the user.
   * @return true if the user inputs 'y' or 'yes' (case-insensitive), false otherwise.
   */
  static boolean getUserConfirmation(String message) throws IOException {
    Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    System.out.print(message + " (y/n): ");
    try {
      String input = scanner.nextLine().trim().toLowerCase();
      return input.equals("y") || input.equals("yes");
    } catch (Exception e) {
      throw new IOException(e.getMessage(), e);
    }
  }
}

class ProgressBar {
  private final long total;
  private long progress = 0;

  public ProgressBar(long total) {
    this.total = total > 0 ? total : 1; // Avoid division by zero
  }

  /**
   * Updates the progress bar by the progress made in this step.
   * E.g. if we download a file, update(20) means that we read 20 bytes in this step.
   *
   * @param progressStep progress made in this update step.
   */
  public void update(int progressStep) {
    progress += progressStep;
    int percent = (int) ((progress / (double) total) * 100);
    percent = Math.min(Math.max(percent, 0), 100); // Clamp percent between 0 and 100
    int filledLength = percent / 2;
    String bar = "=".repeat(filledLength) + " ".repeat(50 - filledLength);
    System.out.print("\r[" + bar + "] " + percent + "%");
    System.out.flush(); // Ensure immediate console output
  }

  public void complete() {
    update((int) (total - progress));
    System.out.println();
  }
}

class TarGzExtractor {

  /**
   * Extracts the contents of a .tar.gz archive to a specified output directory.
   * <p>
   * This method handles both regular files and symbolic links, preserving
   * the original file permissions. It also provides the option to strip a
   * specified number of leading path components from each entry, similar
   * to the {@code --strip-components} option in the {@code tar} command-line
   * utility.
   * </p>
   *
   * @param archive         the path to the .tar.gz file to be extracted
   * @param outputDir       the directory where the contents of the archive
   *                        will be extracted
   * @param stripComponents the number of leading path components to remove
   *                        from each entry's name
   * @throws IOException              if an I/O error occurs during extraction, such as
   *                                  reading from the archive or writing to the output
   *                                  directory
   * @throws SecurityException        if a security manager exists and denies
   *                                  write access to the output directory or any
   *                                  extracted files
   * @throws IllegalArgumentException if the {@code stripComponents} parameter
   *                                  is negative
   * @implNote This method requires the Apache Commons Compress library
   *     (version 1.23.0 or later) for handling tar and gzip formats.
   *     Ensure that the environment supports POSIX file attribute
   *     views; otherwise, setting file permissions may not be
   *     supported and could result in an
   *     {@link UnsupportedOperationException}.
   * @see <a href="https://commons.apache.org/proper/commons-compress/">Apache Commons Compress</a>
   */
  public static void extractTarGz(Path archive, Path outputDir, int stripComponents)
      throws IOException {
    try (InputStream fileInputStream = Files.newInputStream(archive);
         BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
         GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(
             bufferedInputStream);
         TarArchiveInputStream tarInputStream = new TarArchiveInputStream(gzipInputStream)) {

      TarArchiveEntry entry;
      while ((entry = tarInputStream.getNextEntry()) != null) {
        String entryName = stripLeadingComponents(entry.getName(), stripComponents);
        if (entryName.isEmpty()) {
          continue; // Skip entries that are stripped to an empty name
        }

        Path outputPath = outputDir.resolve(entryName).normalize();
        if (!outputPath.startsWith(outputDir)) {
          throw new IOException("Entry is outside of the target dir: " + entryName);
        }

        if (entry.isDirectory()) {
          Files.createDirectories(outputPath);
        } else if (entry.isSymbolicLink()) {
          Path linkTarget = Paths.get(entry.getLinkName());
          Files.createDirectories(outputPath.getParent());
          Files.createSymbolicLink(outputPath, linkTarget);
        } else {
          Files.createDirectories(outputPath.getParent());
          try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = tarInputStream.read(buffer)) != -1) {
              outputStream.write(buffer, 0, bytesRead);
            }
          }
          setPermissions(outputPath, entry.getMode());
        }
      }
    }
  }

  private static String stripLeadingComponents(String entryName, int stripComponents) {
    String[] parts = entryName.split("/", -1); // Use split with a negative limit
    if (parts.length <= stripComponents) {
      return "";
    }
    return String.join("/", Arrays.copyOfRange(parts, stripComponents, parts.length));
  }

  private static void setPermissions(Path path, int mode) throws IOException {
    Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);

    if ((mode & 0400) != 0) {
      permissions.add(PosixFilePermission.OWNER_READ);
    }
    if ((mode & 0200) != 0) {
      permissions.add(PosixFilePermission.OWNER_WRITE);
    }
    if ((mode & 0100) != 0) {
      permissions.add(PosixFilePermission.OWNER_EXECUTE);
    }
    if ((mode & 0040) != 0) {
      permissions.add(PosixFilePermission.GROUP_READ);
    }
    if ((mode & 0020) != 0) {
      permissions.add(PosixFilePermission.GROUP_WRITE);
    }
    if ((mode & 0010) != 0) {
      permissions.add(PosixFilePermission.GROUP_EXECUTE);
    }
    if ((mode & 0004) != 0) {
      permissions.add(PosixFilePermission.OTHERS_READ);
    }
    if ((mode & 0002) != 0) {
      permissions.add(PosixFilePermission.OTHERS_WRITE);
    }
    if ((mode & 0001) != 0) {
      permissions.add(PosixFilePermission.OTHERS_EXECUTE);
    }

    Files.setPosixFilePermissions(path, permissions);
  }
}