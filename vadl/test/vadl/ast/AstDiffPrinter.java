package vadl.ast;

public class AstDiffPrinter {
  static String printDiff(Ast actual, Ast expected) {
    if (actual.definitions.size() != expected.definitions.size()) {
      return "Mismatched top-level definitions: Expected %d, Actual %d".formatted(
          expected.definitions.size(), actual.definitions.size());
    }
    for (int i = 0; i < actual.definitions.size(); i++) {
      var actualDef = actual.definitions.get(i);
      var expectedDef = expected.definitions.get(i);
      if (!actualDef.equals(expectedDef)) {
        if (actualDef instanceof ImportDefinition actualImport
            && expectedDef instanceof ImportDefinition expectedImport
            && !actualImport.moduleAst.equals(expectedImport.moduleAst)) {
          return "In import %d:\n%s".formatted(i + 1,
              printDiff(actualImport.moduleAst, expectedImport.moduleAst));
        }
        StringBuilder actualPretty = new StringBuilder();
        StringBuilder expectedPretty = new StringBuilder();
        actualDef.prettyPrint(2, actualPretty);
        expectedDef.prettyPrint(2, expectedPretty);
        return "Top-level definition %d:\nExpected:\n%s\nActual:\n%s\n".formatted(
            i + 1, actualPretty, expectedPretty
        );
      }
    }
    return "Unknown difference";
  }
}
