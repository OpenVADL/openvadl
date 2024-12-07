package vadl.cppCodeGen.formatting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.apache.commons.io.IOUtils;

/**
 * The ClangFormatter uses the clang-format executable to format a given file.
 * If the clang-format executable is not in the PATH, it will be marked as not available.
 * The formatter takes a String {@code style} that may be a style class like
 * {@code LLVM, GOOGLE, ...} or a YAML string like
 * {@code {Language: Cpp, AlignAfterOpenBracket: Align}}.
 */
public class ClangFormatter implements CodeFormatter {
  private static final String CLANG_FORMAT_PATH = "clang-format";

  private final String style;
  private boolean checkedAvailability = false;
  private boolean isAvailable = false;

  protected ClangFormatter(String style) {
    this.style = style;
  }

  @Override
  public boolean isAvailable() {
    if (checkedAvailability) {
      return isAvailable;
    }
    checkedAvailability = true;

    try {
      ProcessBuilder pb = new ProcessBuilder(CLANG_FORMAT_PATH, "--version");
      Process process = pb.start();
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream()))) {
        String output = reader.readLine();
        isAvailable = output != null && output.contains("clang-format");
      }
    } catch (IOException e) {
      isAvailable = false;
    }
    return isAvailable;
  }

  @Override
  public void format(Path file) throws FormatFailureException, NotAvailableException {
    if (!isAvailable()) {
      throw new NotAvailableException("clang-format is not in PATH");
    }

    ProcessBuilder processBuilder = new ProcessBuilder(CLANG_FORMAT_PATH, "--style=" + style, "-i",
        file.toAbsolutePath().toString());

    try {
      Process process = processBuilder.start();
      try {
        int exitCode = process.waitFor();
        if (exitCode != 0) {
          var error = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
          throw new FormatFailureException(
              "clang-format failed with exit code " + exitCode + ": " + error);
        }
      } catch (InterruptedException e) {
        throw new FormatFailureException("clang-format interrupted", e);
      }
    } catch (IOException e) {
      throw new FormatFailureException("Failed to format code", e);
    }
  }

}
