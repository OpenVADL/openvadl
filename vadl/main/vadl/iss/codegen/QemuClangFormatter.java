package vadl.iss.codegen;

import vadl.cppCodeGen.formatting.ClangFormatter;

/**
 * The QemuClangFormatter uses the {@link ClangFormatter} with
 * styles compatible with the QEMU project guidelines.
 */
public class QemuClangFormatter extends ClangFormatter {

  public static final QemuClangFormatter INSTANCE = new QemuClangFormatter();

  private QemuClangFormatter() {
    super("""
        {
          Language: Cpp,
          AlignAfterOpenBracket: Align,
          AlignConsecutiveAssignments: false,
          AlignConsecutiveDeclarations: false,
          AlignEscapedNewlinesLeft: true,
          AlignOperands: true,
          AlignTrailingComments: false,
          AllowAllParametersOfDeclarationOnNextLine: true,
          AllowShortBlocksOnASingleLine: false,
          AllowShortCaseLabelsOnASingleLine: false,
          AllowShortFunctionsOnASingleLine: None,
          AllowShortIfStatementsOnASingleLine: false,
          AllowShortLoopsOnASingleLine: false,
          AlwaysBreakAfterReturnType: None,
          AlwaysBreakBeforeMultilineStrings: false,
          BinPackArguments: true,
          BinPackParameters: true,
          BraceWrapping: {
            AfterControlStatement: false,
            AfterEnum: false,
            AfterFunction: false,
            AfterStruct: false,
            AfterUnion: false,
            BeforeElse: false,
            IndentBraces: false
          },
          BreakBeforeBinaryOperators: None,
          BreakBeforeBraces: Custom,
          BreakBeforeTernaryOperators: false,
          BreakStringLiterals: true,
          ColumnLimit: 80,
          ContinuationIndentWidth: 4,
          Cpp11BracedListStyle: false,
          DerivePointerAlignment: false,
          DisableFormat: false,
          ForEachMacros: [
            CPU_FOREACH,
            CPU_FOREACH_REVERSE,
            CPU_FOREACH_SAFE,
            IOMMU_NOTIFIER_FOREACH,
            QLIST_FOREACH,
            QLIST_FOREACH_ENTRY,
            QLIST_FOREACH_RCU,
            QLIST_FOREACH_SAFE,
            QLIST_FOREACH_SAFE_RCU,
            QSIMPLEQ_FOREACH,
            QSIMPLEQ_FOREACH_SAFE,
            QSLIST_FOREACH,
            QSLIST_FOREACH_SAFE,
            QTAILQ_FOREACH,
            QTAILQ_FOREACH_REVERSE,
            QTAILQ_FOREACH_SAFE,
            QTAILQ_RAW_FOREACH,
            RAMBLOCK_FOREACH
          ],
          IncludeIsMainRegex: $,
          IndentCaseLabels: false,
          IndentWidth: 4,
          IndentWrappedFunctionNames: false,
          KeepEmptyLinesAtTheStartOfBlocks: false,
          MacroBlockBegin: .*_BEGIN$,
          MacroBlockEnd: .*_END$,
          MaxEmptyLinesToKeep: 2,
          PointerAlignment: Right,
          ReflowComments: true,
          SortIncludes: true,
          SpaceAfterCStyleCast: false,
          SpaceBeforeAssignmentOperators: true,
          SpaceBeforeParens: ControlStatements,
          SpaceInEmptyParentheses: false,
          SpacesBeforeTrailingComments: 1,
          SpacesInContainerLiterals: true,
          SpacesInParentheses: false,
          SpacesInSquareBrackets: false,
          Standard: Auto,
          UseTab: Never
        }
        """.stripIndent()
        .replaceAll("\n", "")
    );
  }
}

