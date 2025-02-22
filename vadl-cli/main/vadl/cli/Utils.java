package vadl.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.function.Consumer;
import javax.annotation.Nullable;

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