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
        if (actualDef instanceof InstructionSetDefinition actualIsa
            && expectedDef instanceof InstructionSetDefinition expectedIsa) {
          return "Top-level definition %d:\n%s".formatted(i, printDiff(actualIsa, expectedIsa));
        }
        StringBuilder actualPretty = new StringBuilder();
        StringBuilder expectedPretty = new StringBuilder();
        actualDef.prettyPrint(2, actualPretty);
        expectedDef.prettyPrint(2, expectedPretty);
        return "Top-level definition %d:\nExpected:\n%s\nActual:\n%s\n".formatted(
            i + 1, expectedPretty, actualPretty
        );
      }
    }
    return "Unknown difference";
  }

  private static String printDiff(InstructionSetDefinition actual,
                                  InstructionSetDefinition expected) {
    if (actual.definitions.size() != expected.definitions.size()) {
      return "Mismatched definitions: Expected %d, Actual %d".formatted(
          expected.definitions.size(), actual.definitions.size());
    }
    if (!actual.annotations.equals(expected.annotations)) {
      StringBuilder actualPretty = new StringBuilder();
      StringBuilder expectedPretty = new StringBuilder();
      actual.annotations.prettyPrint(2, actualPretty);
      expected.annotations.prettyPrint(2, expectedPretty);
      return "ISA definition annotations:\nExpected:\n%s\nActual:\n%s\n"
          .formatted(expectedPretty, actualPretty);
    }
    for (int i = 0; i < actual.definitions.size(); i++) {
      var actualDef = actual.definitions.get(i);
      var expectedDef = expected.definitions.get(i);
      if (!actualDef.equals(expectedDef)) {
        StringBuilder actualPretty = new StringBuilder();
        StringBuilder expectedPretty = new StringBuilder();
        actualDef.prettyPrint(2, actualPretty);
        expectedDef.prettyPrint(2, expectedPretty);
        return "Definition %d in ISA:\nExpected:\n%s\nActual:\n%s\n"
            .formatted(i + 1, expectedPretty, actualPretty);
      }
    }
    StringBuilder actualPretty = new StringBuilder();
    StringBuilder expectedPretty = new StringBuilder();
    actual.prettyPrint(2, actualPretty);
    expected.prettyPrint(2, expectedPretty);
    return "Expected:\n%s\nActual:\n%s\n".formatted(expectedPretty, actualPretty);
  }
}
