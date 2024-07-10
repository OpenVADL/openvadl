package vadl.error;

import java.util.List;

/**
 * A command line printer for vadl errors.
 */
public class ErrorPrinter {

  /**
   * Prints a list of errors to stdout with ANSI colors.
   *
   * @param errors to print.
   */
  public void print(List<VadlError> errors) {
    for (var error : errors) {
      print(error);
    }
  }

  /**
   * Prints an error to stdout with ANSI colors.
   *
   * @param error to print.
   */
  public void print(VadlError error) {
    // Header
    System.out.printf("%s%serror:%s%s %s%s\n", Ansi.Bold, Ansi.Red, Ansi.Reset, Ansi.Bold,
        error.reason, Ansi.Reset);
    System.out.printf("    %s╭──[%s]\n", Ansi.Cyan, error.location.toConciseString());

    // Print preview
    System.out.println("    │");
    System.out.printf("    │ %sPreview not yet implemented\n", Ansi.Reset);
    System.out.printf("%s    │%s\n\n", Ansi.Cyan, Ansi.Reset);

    // Print description and tip
    if (error.description != null) {
      System.out.printf("%s    %s\n", Ansi.Bold, error.description);
    }
    if (error.description != null && error.tip != null) {
      System.out.println();
    }
    if (error.tip != null) {
      System.out.printf("    %s%sTip:%s %s%s\n", Ansi.Bold, Ansi.Underline, Ansi.Reset, Ansi.Bold,
          error.tip);
    }
    System.out.print(Ansi.Reset);
  }

  static class Ansi {
    static String Reset = "\033[0m";
    static String Bold = "\033[01m";
    static String Underline = "\033[04m";
    static String Black = "\033[30m";
    static String Red = "\033[31m";
    static String Green = "\033[32m";
    static String Orange = "\033[33m";
    static String Blue = "\033[34m";
    static String Purple = "\033[35m";
    static String Cyan = "\033[36m";
    static String Lightgrey = "\033[37m";
    static String Darkgrey = "\033[90m";
    static String Lightred = "\033[91m";
    static String Lightgreen = "\033[92m";
    static String Yellow = "\033[93m";
    static String Lightblue = "\033[94m";
    static String Pink = "\033[95m";
    static String Lightcyan = "\033[96m";
  }
}
