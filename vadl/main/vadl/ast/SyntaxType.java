package vadl.ast;

import java.util.List;
import java.util.stream.Collectors;

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

    SUBTYPES[STATS.ordinal()][STATS.ordinal()] = true;

    SUBTYPES[STAT.ordinal()][STAT.ordinal()] = true;
    SUBTYPES[STAT.ordinal()][STATS.ordinal()] = true;

    SUBTYPES[ENCS.ordinal()][ENCS.ordinal()] = true;

    SUBTYPES[ISA_DEFS.ordinal()][ISA_DEFS.ordinal()] = true;

    SUBTYPES[EX.ordinal()][EX.ordinal()] = true;

    SUBTYPES[LIT.ordinal()][LIT.ordinal()] = true;
    SUBTYPES[LIT.ordinal()][EX.ordinal()] = true;

    SUBTYPES[STR.ordinal()][STR.ordinal()] = true;
    SUBTYPES[STR.ordinal()][LIT.ordinal()] = true;
    SUBTYPES[STR.ordinal()][EX.ordinal()] = true;

    SUBTYPES[VAL.ordinal()][VAL.ordinal()] = true;
    SUBTYPES[VAL.ordinal()][LIT.ordinal()] = true;
    SUBTYPES[VAL.ordinal()][EX.ordinal()] = true;

    SUBTYPES[BOOL.ordinal()][BOOL.ordinal()] = true;
    SUBTYPES[BOOL.ordinal()][VAL.ordinal()] = true;
    SUBTYPES[BOOL.ordinal()][LIT.ordinal()] = true;
    SUBTYPES[BOOL.ordinal()][EX.ordinal()] = true;

    SUBTYPES[INT.ordinal()][INT.ordinal()] = true;
    SUBTYPES[INT.ordinal()][VAL.ordinal()] = true;
    SUBTYPES[INT.ordinal()][LIT.ordinal()] = true;
    SUBTYPES[INT.ordinal()][EX.ordinal()] = true;

    SUBTYPES[BIN.ordinal()][BIN.ordinal()] = true;
    SUBTYPES[BIN.ordinal()][VAL.ordinal()] = true;
    SUBTYPES[BIN.ordinal()][LIT.ordinal()] = true;
    SUBTYPES[BIN.ordinal()][EX.ordinal()] = true;

    SUBTYPES[CALL_EX.ordinal()][CALL_EX.ordinal()] = true;
    SUBTYPES[CALL_EX.ordinal()][EX.ordinal()] = true;

    SUBTYPES[SYM_EX.ordinal()][SYM_EX.ordinal()] = true;
    SUBTYPES[SYM_EX.ordinal()][CALL_EX.ordinal()] = true;
    SUBTYPES[SYM_EX.ordinal()][EX.ordinal()] = true;

    SUBTYPES[ID.ordinal()][ID.ordinal()] = true;
    SUBTYPES[ID.ordinal()][CALL_EX.ordinal()] = true;
    SUBTYPES[ID.ordinal()][SYM_EX.ordinal()] = true;
    SUBTYPES[ID.ordinal()][EX.ordinal()] = true;

    SUBTYPES[BIN_OP.ordinal()][BIN_OP.ordinal()] = true;

    SUBTYPES[UN_OP.ordinal()][UN_OP.ordinal()] = true;
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
    return arguments.stream().map(Object::toString).collect(Collectors.joining(", ", "(", ")")) +
        " -> " + resultType;
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
      if (!arguments.get(i).isSubTypeOf(otherProjection.arguments.get(i))) {
        return false;
      }
    }
    return true;
  }
}
