package vadl.ast;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

public class ParserMain {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.out.println("Arguments: <path.to.vadl>");
      System.exit(1);
    }
    var file = args[0];
    VadlParser.parse(Paths.get(file), Map.of());
  }
}
