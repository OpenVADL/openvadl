package vadl.ast;

interface SyntaxType {
  boolean isSubTypeOf(SyntaxType other);
}

// We're using the ordinal() to construct a two-dimensional alternative to the EnumSet builtin.
@SuppressWarnings("EnumOrdinal")
enum BasicSyntaxType implements SyntaxType {
  STATS("Stats"),
  STAT("Stat"),
  ENCS("Encs"),
  ISA_DEFS("IsaDefs"),
  EX("Ex"),
  LIT("Lit"),
  STR("Str"),
  VAL("Val"),
  BOOL("Bool"),
  INT("Int"),
  BIN("Bin"),
  CALL_EX("CallEx"),
  SYM_EX("SymEx"),
  ID("Id"),
  BIN_OP("BinOp"),
  UN_OP("UnOp"),
  INVALID("InvalidType");

  static final boolean[][] SUBTYPES;

  private final String name;

  BasicSyntaxType(String name) {
    this.name = name;
  }

  String getName() {
    return name;
  }

  @Override
  public boolean isSubTypeOf(SyntaxType other) {
    return other instanceof BasicSyntaxType bst && SUBTYPES[this.ordinal()][bst.ordinal()];
  }

  static {
    SUBTYPES = new boolean[BasicSyntaxType.values().length][BasicSyntaxType.values().length];

    SUBTYPES[STATS.ordinal()   ][STATS.ordinal()   ] = true;

    SUBTYPES[STAT.ordinal()    ][STAT.ordinal()    ] = true;
    SUBTYPES[STAT.ordinal()    ][STATS.ordinal()   ] = true;

    SUBTYPES[ENCS.ordinal()    ][ENCS.ordinal()    ] = true;

    SUBTYPES[ISA_DEFS.ordinal()][ISA_DEFS.ordinal()] = true;

    SUBTYPES[EX.ordinal()      ][EX.ordinal()      ] = true;

    SUBTYPES[LIT.ordinal()     ][LIT.ordinal()     ] = true;
    SUBTYPES[LIT.ordinal()     ][EX.ordinal()      ] = true;

    SUBTYPES[STR.ordinal()     ][STR.ordinal()     ] = true;
    SUBTYPES[STR.ordinal()     ][LIT.ordinal()     ] = true;
    SUBTYPES[STR.ordinal()     ][EX.ordinal()      ] = true;

    SUBTYPES[VAL.ordinal()     ][VAL.ordinal()     ] = true;
    SUBTYPES[VAL.ordinal()     ][LIT.ordinal()     ] = true;
    SUBTYPES[VAL.ordinal()     ][EX.ordinal()      ] = true;

    SUBTYPES[BOOL.ordinal()    ][BOOL.ordinal()    ] = true;
    SUBTYPES[BOOL.ordinal()    ][VAL.ordinal()     ] = true;
    SUBTYPES[BOOL.ordinal()    ][LIT.ordinal()     ] = true;
    SUBTYPES[BOOL.ordinal()    ][EX.ordinal()      ] = true;

    SUBTYPES[INT.ordinal()     ][INT.ordinal()     ] = true;
    SUBTYPES[INT.ordinal()     ][VAL.ordinal()     ] = true;
    SUBTYPES[INT.ordinal()     ][LIT.ordinal()     ] = true;
    SUBTYPES[INT.ordinal()     ][EX.ordinal()      ] = true;

    SUBTYPES[BIN.ordinal()     ][BIN.ordinal()     ] = true;
    SUBTYPES[BIN.ordinal()     ][VAL.ordinal()     ] = true;
    SUBTYPES[BIN.ordinal()     ][LIT.ordinal()     ] = true;
    SUBTYPES[BIN.ordinal()     ][EX.ordinal()      ] = true;

    SUBTYPES[CALL_EX.ordinal() ][CALL_EX.ordinal() ] = true;
    SUBTYPES[CALL_EX.ordinal() ][EX.ordinal()      ] = true;

    SUBTYPES[SYM_EX.ordinal()  ][SYM_EX.ordinal()  ] = true;
    SUBTYPES[SYM_EX.ordinal()  ][CALL_EX.ordinal() ] = true;
    SUBTYPES[SYM_EX.ordinal()  ][EX.ordinal()      ] = true;

    SUBTYPES[ID.ordinal()      ][ID.ordinal()      ] = true;
    SUBTYPES[ID.ordinal()      ][CALL_EX.ordinal() ] = true;
    SUBTYPES[ID.ordinal()      ][SYM_EX.ordinal()  ] = true;
    SUBTYPES[ID.ordinal()      ][EX.ordinal()      ] = true;

    SUBTYPES[BIN_OP.ordinal()  ][BIN_OP.ordinal()  ] = true;

    SUBTYPES[UN_OP.ordinal()   ][UN_OP.ordinal()   ] = true;
  }
}
