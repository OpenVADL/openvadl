package vadl.ast;

import java.util.List;
import java.util.stream.Collectors;

interface SyntaxType {
  boolean isSubTypeOf(SyntaxType other);

  String print();
}

// We're using the ordinal() to construct a two-dimensional alternative to the EnumSet builtin.
@SuppressWarnings("EnumOrdinal")
enum BasicSyntaxType implements SyntaxType {
  STATS("Stats"),
  STAT("Stat"),
  ENCS("Encs"),
  COMMON_DEFS("Defs"),
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

  static final boolean[][] IS_SUBTYPE;

  private final String name;

  BasicSyntaxType(String name) {
    this.name = name;
  }

  String getName() {
    return name;
  }

  /**
   * Returns whether the current object is a subtype of another.
   * Note, they are always a subtype of themselves.
   *
   * <p>Here is the complete structure for basic types:
   * <pre>{@code
   *                                        T
   *          +---------+--------+---------/ \-------+-----------------------------+------+
   *          |         |        |                   |                             |      |
   *          |         |        |                   |                             |      |
   *          |         |        |                   |                             |      |
   *          |         |        |                   |                             |      |
   *          |         |        |                   |                             |      |
   *          |         |        |            +------Ex------------+               |      |
   *          |         |     IsaDefs         |                    |               |      |
   *          |         |        |            |                    |               |      |
   *          |         |        |        +--Lit-----+           CallEx            |      |
   *        Stats       |        |        |          |             |               |      |
   *          |         |       Defs      |          |             |               |     UnOp
   *          |         |                 |     +-- Val----+      SymEx            |
   *          |       Encs               Str    |    |     |       |               |
   *        Stat                                |    |     |       |             BinOp
   *                                           Bool  |    Bin      Id
   *                                                 |
   *                                                Int
   * }</pre>
   *
   * @param other the type to check against.
   */
  @Override
  public boolean isSubTypeOf(SyntaxType other) {
    return other instanceof BasicSyntaxType bst && IS_SUBTYPE[this.ordinal()][bst.ordinal()];
  }

  @Override
  public String print() {
    return name;
  }


  static {
    IS_SUBTYPE = new boolean[BasicSyntaxType.values().length][BasicSyntaxType.values().length];

    IS_SUBTYPE[STATS.ordinal()][STATS.ordinal()] = true;

    IS_SUBTYPE[STAT.ordinal()][STAT.ordinal()] = true;
    IS_SUBTYPE[STAT.ordinal()][STATS.ordinal()] = true;

    IS_SUBTYPE[ENCS.ordinal()][ENCS.ordinal()] = true;

    IS_SUBTYPE[COMMON_DEFS.ordinal()][COMMON_DEFS.ordinal()] = true;
    IS_SUBTYPE[COMMON_DEFS.ordinal()][ISA_DEFS.ordinal()] = true;

    IS_SUBTYPE[ISA_DEFS.ordinal()][ISA_DEFS.ordinal()] = true;

    IS_SUBTYPE[EX.ordinal()][EX.ordinal()] = true;

    IS_SUBTYPE[LIT.ordinal()][LIT.ordinal()] = true;
    IS_SUBTYPE[LIT.ordinal()][EX.ordinal()] = true;

    IS_SUBTYPE[STR.ordinal()][STR.ordinal()] = true;
    IS_SUBTYPE[STR.ordinal()][LIT.ordinal()] = true;
    IS_SUBTYPE[STR.ordinal()][EX.ordinal()] = true;

    IS_SUBTYPE[VAL.ordinal()][VAL.ordinal()] = true;
    IS_SUBTYPE[VAL.ordinal()][LIT.ordinal()] = true;
    IS_SUBTYPE[VAL.ordinal()][EX.ordinal()] = true;

    IS_SUBTYPE[BOOL.ordinal()][BOOL.ordinal()] = true;
    IS_SUBTYPE[BOOL.ordinal()][VAL.ordinal()] = true;
    IS_SUBTYPE[BOOL.ordinal()][LIT.ordinal()] = true;
    IS_SUBTYPE[BOOL.ordinal()][EX.ordinal()] = true;

    IS_SUBTYPE[INT.ordinal()][INT.ordinal()] = true;
    IS_SUBTYPE[INT.ordinal()][VAL.ordinal()] = true;
    IS_SUBTYPE[INT.ordinal()][LIT.ordinal()] = true;
    IS_SUBTYPE[INT.ordinal()][EX.ordinal()] = true;

    IS_SUBTYPE[BIN.ordinal()][BIN.ordinal()] = true;
    IS_SUBTYPE[BIN.ordinal()][VAL.ordinal()] = true;
    IS_SUBTYPE[BIN.ordinal()][LIT.ordinal()] = true;
    IS_SUBTYPE[BIN.ordinal()][EX.ordinal()] = true;

    IS_SUBTYPE[CALL_EX.ordinal()][CALL_EX.ordinal()] = true;
    IS_SUBTYPE[CALL_EX.ordinal()][EX.ordinal()] = true;

    IS_SUBTYPE[SYM_EX.ordinal()][SYM_EX.ordinal()] = true;
    IS_SUBTYPE[SYM_EX.ordinal()][CALL_EX.ordinal()] = true;
    IS_SUBTYPE[SYM_EX.ordinal()][EX.ordinal()] = true;

    IS_SUBTYPE[ID.ordinal()][ID.ordinal()] = true;
    IS_SUBTYPE[ID.ordinal()][CALL_EX.ordinal()] = true;
    IS_SUBTYPE[ID.ordinal()][SYM_EX.ordinal()] = true;
    IS_SUBTYPE[ID.ordinal()][EX.ordinal()] = true;

    IS_SUBTYPE[BIN_OP.ordinal()][BIN_OP.ordinal()] = true;

    IS_SUBTYPE[UN_OP.ordinal()][UN_OP.ordinal()] = true;
  }
}

/**
 * A record type is a composite type of other syntax types.
 * A record type is considered a subtype of another record type, iif all composite entries of the
 * type are a subtype of the other record's corresponding entry.
 */
class RecordType implements SyntaxType {

  String name;
  List<Entry> entries;

  RecordType(String name, List<Entry> entries) {
    this.name = name;
    this.entries = entries;
  }

  @Override
  public String toString() {
    return name + " " + entries.stream().map(entry -> entry.type.toString())
        .collect(Collectors.joining(",", "(", ")"));
  }

  @Override
  public boolean isSubTypeOf(SyntaxType other) {
    if (!(other instanceof RecordType otherRecord)) {
      return false;
    }
    if (otherRecord.entries.size() != entries.size()) {
      return false;
    }
    for (int i = 0; i < entries.size(); i++) {
      if (!entries.get(i).type().isSubTypeOf(otherRecord.entries.get(i).type())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String print() {
    return name;
  }

  SyntaxType findEntry(String name) {
    for (Entry entry : entries) {
      if (entry.name().equals(name)) {
        return entry.type();
      }
    }
    return BasicSyntaxType.INVALID;
  }

  record Entry(String name, SyntaxType type) {
  }
}


/**
 * A projection type describes an operation that converts multiple arguments to a new value.
 * A projection type is considered a subtype of another projection type, iif the result type of
 * the projection type is a subtype of the other projection type AND all argument types of the
 * projection types are a subtype of the other projection type's respective argument.
 */
class ProjectionType implements SyntaxType {
  List<SyntaxType> arguments;
  SyntaxType resultType;

  ProjectionType(List<SyntaxType> arguments, SyntaxType resultType) {
    this.arguments = arguments;
    this.resultType = resultType;
  }

  @Override
  public String toString() {
    return arguments.stream().map(Object::toString).collect(Collectors.joining(", ", "(", ")"))
        + " -> " + resultType;
  }

  @Override
  public boolean isSubTypeOf(SyntaxType other) {
    if (!(other instanceof ProjectionType otherProjection)) {
      return false;
    }
    if (!resultType.isSubTypeOf(otherProjection.resultType)) {
      return false;
    }
    if (arguments.size() != otherProjection.arguments.size()) {
      return false;
    }
    for (int i = 0; i < arguments.size(); i++) {
      if (!otherProjection.arguments.get(i).isSubTypeOf(arguments.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String print() {
    return arguments.stream().map(SyntaxType::print).collect(Collectors.joining(",", "(", ")"))
        + " -> " + resultType.print();
  }
}
