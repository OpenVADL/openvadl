package vadl.types;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * The BuiltInTable class represents a collection of built-in functions and operations in VADL.
 * It provides information about the name, operator, and supported types of each built-in.
 *
 * <p>The current implementation of the {@link BuiltIn} is limited,
 * e.g. no {@code compute} method exists.
 */
@SuppressWarnings("SummaryJavadoc")
public class BuiltInTable {

  ///// ARITHMETIC //////

  /**
   * {@code function neg( a : Bits<N> ) -> Bits<N> // <=> -a }
   */
  public static final BuiltIn NEG =
      BuiltIn.func("NEG", "-", Type.relation(BitsType.class, BitsType.class));


  /**
   * {@code function add ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a + b }
   */
  public static final BuiltIn ADD =
      BuiltIn.func("ADD", "+", Type.relation(BitsType.class, BitsType.class, BitsType.class));


  /**
   * {@code function adds( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ADDS =
      BuiltIn.func("ADDS", Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function addc( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ADDC =
      BuiltIn.func("ADDC",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class));


  /**
   * {@code function satadd ( a : SInt<N>, b : SInt<N> ) -> SInt<N> }
   */
  public static final BuiltIn SATADD_SS =
      BuiltIn.func("SATADD", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function satadd ( a : UInt<N>, b : UInt<N> ) -> UInt<N> }
   */
  public static final BuiltIn SATADD_UU =
      BuiltIn.func("SATADD", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function satadds( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SATADDS_SS =
      BuiltIn.func("SATADDS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function satadds( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn SATADDS_UU =
      BuiltIn.func("SATADDS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function sataddc( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SATADDC_SS =
      BuiltIn.func("SATADDC",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function sataddc( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn SATADDC_UU =
      BuiltIn.func("SATADDC",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function sub  ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a - c }
   */
  public static final BuiltIn SUB =
      BuiltIn.func("SUB", "-", Type.relation(BitsType.class, BitsType.class, BitsType.class));


  /**
   * {@code function subsc( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn SUBSC =
      BuiltIn.func("SUBSC", Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function subsb( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn SUBSB =
      BuiltIn.func("SUBSB", Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function subc ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn SUBC =
      BuiltIn.func("SUBC",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class));


  /**
   * {@code function subb ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn SUBB =
      BuiltIn.func("SUBB",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class));


  /**
   * {@code function satsub ( a : SInt<N>, b : SInt<N> ) -> SInt<N> }
   */
  public static final BuiltIn SATSUB_SS =
      BuiltIn.func("SATSUB", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function satsub ( a : UInt<N>, b : UInt<N> ) -> UInt<N> }
   */
  public static final BuiltIn SATSUB_UU =
      BuiltIn.func("SATSUB", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function satsubs( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SATSUBS_SS =
      BuiltIn.func("SATSUBS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function satsubs( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn SATSUBS_UU =
      BuiltIn.func("SATSUBS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function satsubc( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SATSUBC_SS =
      BuiltIn.func("SATSUBC",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function satsubc( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn SATSUBC_UU =
      BuiltIn.func("SATSUBC",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function satsubb( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SATSUBB_SS =
      BuiltIn.func("SATSUBB",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function satsubb( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn SATSUBB_UU =
      BuiltIn.func("SATSUBB",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function mul ( a : SInt<N>, b : SInt<N> ) -> SInt<2*N> // <=> a * b }
   */
  public static final BuiltIn MUL_SS =
      BuiltIn.func("MUL", "*", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function mul ( a : UInt<N>, b : UInt<N> ) -> UInt<2*N> // <=> a * b }
   */
  public static final BuiltIn MUL_UU =
      BuiltIn.func("MUL", "*", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function muls( a : SInt<N>, b : SInt<N> ) -> ( SInt<2*N>, Status ) }
   */
  public static final BuiltIn MULS_SS =
      BuiltIn.func("MULS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function muls( a : UInt<N>, b : UInt<N> ) -> ( UInt<2*N>, Status ) }
   */
  public static final BuiltIn MULS_UU =
      BuiltIn.func("MULS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function mod ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a % b }
   */
  public static final BuiltIn MOD_SS =
      BuiltIn.func("MOD", "%", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function mod ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a % b }
   */
  public static final BuiltIn MOD_UU =
      BuiltIn.func("MOD", "%", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function mods ( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn MODS_SS =
      BuiltIn.func("MODS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function mods ( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn MODS_UU =
      BuiltIn.func("MODS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function div ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a / b }
   */
  public static final BuiltIn DIV_SS =
      BuiltIn.func("DIV", "/", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function div ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a / b }
   */
  public static final BuiltIn DIV_UU =
      BuiltIn.func("DIV", "/", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function divs( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn DIVS_SS =
      BuiltIn.func("DIVS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function divs( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn DIVS_UU =
      BuiltIn.func("DIVS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  ///// LOGICAL //////

  /**
   * {@code function not ( a : Bits<N> ) ->Bits<N> // <=> ~a }
   */
  public static final BuiltIn NOT =
      BuiltIn.func("NOT", "~", Type.relation(BitsType.class, BitsType.class));


  /**
   * {@code function and ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a & b }
   */
  public static final BuiltIn AND =
      BuiltIn.func("AND", "&", Type.relation(BitsType.class, BitsType.class, BitsType.class));


  /**
   * {@code function ands( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ANDS =
      BuiltIn.func("ANDS", Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function xor ( a : Bits<N>, b : Bits<N> ) -> [ SInt<N> | UInt<N> ] // <=> a ^ b }
   */
  public static final BuiltIn XOR =
      BuiltIn.func("XOR", "^", Type.relation(BitsType.class, BitsType.class, SIntType.class));


  /**
   * {@code function xors( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn XORS =
      BuiltIn.func("XORS", Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function or ( a : Bits<N>, b : Bits<N> ) -> [ SInt<N> | UInt<N> ] // <=> a | b }
   */
  public static final BuiltIn OR =
      BuiltIn.func("OR", "|", Type.relation(BitsType.class, BitsType.class, SIntType.class));


  /**
   * {@code function ors( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ORS =
      BuiltIn.func("ORS", Type.relation(BitsType.class, BitsType.class, TupleType.class));

  ///// COMPARISON //////


  /**
   * {@code function equ ( a : Bits<N>, b : Bits<N> ) -> Bool // <=> a = b }
   */
  public static final BuiltIn EQU =
      BuiltIn.func("EQU", "=", Type.relation(BitsType.class, BitsType.class, BoolType.class));


  /**
   * {@code function neq ( a : Bits<N>, b : Bits<N> ) -> Bool // <=> a != b }
   */
  public static final BuiltIn NEQ =
      BuiltIn.func("NEQ", "!=", Type.relation(BitsType.class, BitsType.class, BoolType.class));


  /**
   * {@code function lth ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a < b }
   */
  public static final BuiltIn LTH_SS =
      BuiltIn.func("LTH", "<", Type.relation(SIntType.class, SIntType.class, BoolType.class));


  /**
   * {@code function lth ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a < b }
   */
  public static final BuiltIn LTH_UU =
      BuiltIn.func("LTH", "<", Type.relation(UIntType.class, UIntType.class, BoolType.class));


  /**
   * {@code function leq ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a <= b }
   */
  public static final BuiltIn LEQ_SS =
      BuiltIn.func("LEQ", "<=", Type.relation(SIntType.class, SIntType.class, BoolType.class));


  /**
   * {@code function leq ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a <= b }
   */
  public static final BuiltIn LEQ_UU =
      BuiltIn.func("LEQ", "<=", Type.relation(UIntType.class, UIntType.class, BoolType.class));


  /**
   * {@code function gth ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a > b }
   */
  public static final BuiltIn GTH_SS =
      BuiltIn.func("GTH", ">", Type.relation(SIntType.class, SIntType.class, BoolType.class));


  /**
   * {@code function gth ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a > b }
   */
  public static final BuiltIn GTH_UU =
      BuiltIn.func("GTH", ">", Type.relation(UIntType.class, UIntType.class, BoolType.class));


  /**
   * {@code function geq ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a >= b }
   */
  public static final BuiltIn GEQ_SS =
      BuiltIn.func("GEQ", ">=", Type.relation(SIntType.class, SIntType.class, BoolType.class));


  /**
   * {@code function geq ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a >= b }
   */
  public static final BuiltIn GEQ_UU =
      BuiltIn.func("GEQ", ">=", Type.relation(UIntType.class, UIntType.class, BoolType.class));

  ///// SHIFTING //////


  /**
   * {@code function lsl ( a : Bits<N>, b : UInt<M> ) -> Bits<N> // <=> a << b }
   */
  public static final BuiltIn LSL =
      BuiltIn.func("LSL", "<<", Type.relation(BitsType.class, UIntType.class, BitsType.class));


  /**
   * {@code function lsls( a : Bits<N>, b : UInt<M> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn LSLS =
      BuiltIn.func("LSLS", Type.relation(BitsType.class, UIntType.class, TupleType.class));


  /**
   * {@code function lslc( a : Bits<N>, b : UInt<M>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn LSLC =
      BuiltIn.func("LSLC",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function asr ( a : SInt<N>, b : UInt<M> ) -> SInt<N> // <=> a >> b }
   */
  public static final BuiltIn ASR =
      BuiltIn.func("ASR", ">>", Type.relation(SIntType.class, UIntType.class, SIntType.class));


  /**
   * {@code function lsr ( a : UInt<N>, b : UInt<M> ) -> UInt<N> // <=> a >> b }
   */
  public static final BuiltIn LSR =
      BuiltIn.func("LSR", ">>", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function asrs( a : SInt<N>, b : UInt<M> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn ASRS =
      BuiltIn.func("ASRS", Type.relation(SIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function lsrs( a : UInt<N>, b : UInt<M> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn LSRS =
      BuiltIn.func("LSRS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function asrc( a : SInt<N>, b : UInt<M>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn ASRC =
      BuiltIn.func("ASRC",
          Type.relation(List.of(SIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function lsrc( a : UInt<N>, b : UInt<M>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn LSRC =
      BuiltIn.func("LSRC",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function rol ( a : Bits<N>, b : UInt<M> ) ->Bits<N> }
   */
  public static final BuiltIn ROL =
      BuiltIn.func("ROL", Type.relation(BitsType.class, UIntType.class, BitsType.class));


  /**
   * {@code function rols( a : Bits<N>, b : UInt<M> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ROLS =
      BuiltIn.func("ROLS", Type.relation(BitsType.class, UIntType.class, TupleType.class));


  /**
   * {@code function rolc( a : Bits<N>, b : UInt<M>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ROLC =
      BuiltIn.func("ROLC",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function ror ( a : Bits<N>, b : UInt<M> ) -> Bits<N> }
   */
  public static final BuiltIn ROR =
      BuiltIn.func("ROR", Type.relation(BitsType.class, UIntType.class, BitsType.class));


  /**
   * {@code function rors( a : Bits<N>, b : UInt<M> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn RORS =
      BuiltIn.func("RORS", Type.relation(BitsType.class, UIntType.class, TupleType.class));


  /**
   * {@code function rorc( a : Bits<N>, b : UInt<M>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn RORC =
      BuiltIn.func("RORC",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function rrx ( a : Bits<N>, b : UInt<M>, c : Bool ) -> Bits<N> }
   */
  public static final BuiltIn RRX =
      BuiltIn.func("RRX",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class), BitsType.class));


  ///// BIT COUNTING //////


  /**
   * Counting one bits.
   *
   * <p>{@code function cob( a : Bits<N> ) -> UInt<N> }
   */
  public static final BuiltIn COB =
      BuiltIn.func("COB", Type.relation(BitsType.class, UIntType.class));


  /**
   * {@code function czb( a : Bits<N> ) -> UInt<N> // counting zero bits }
   */
  public static final BuiltIn CZB =
      BuiltIn.func("CZB", Type.relation(BitsType.class, UIntType.class));


  /**
   * Counting leading zeros.
   *
   * <p>{@code function clz( a : Bits<N> ) -> UInt<N>  }
   */
  public static final BuiltIn CLZ =
      BuiltIn.func("CLZ", Type.relation(BitsType.class, UIntType.class));


  /**
   * Counting leading ones.
   *
   * <p>{@code function clo( a : Bits<N> ) -> UInt<N> }
   */
  public static final BuiltIn CLO =
      BuiltIn.func("CLO", Type.relation(BitsType.class, UIntType.class));


  /**
   * Counting leading sign bits (without sign bit).
   *
   * <p>{@code function cls( a : Bits<N> ) -> UInt<N>}
   */
  public static final BuiltIn CLS =
      BuiltIn.func("CLS", Type.relation(BitsType.class, TupleType.class));


  ///// FUNCTIONS //////

  /**
   * TODO: describe function
   * <p>{@code function mnemonic() -> String<N>}
   */
  public static final BuiltIn MNEMONIC =
      BuiltIn.func("MNEMONIC", Type.relation(StringType.class));

  /**
   * Concatenates two strings to a new string.
   *
   * <p>{@code function concatenate(String<N>, String<M>) -> String<X>}
   */
  public static final BuiltIn CONCATENATE_STRINGS =
      BuiltIn.func("CONCATENATE", "++",
          Type.relation(StringType.class, StringType.class, StringType.class));

  /**
   * Formats the register file index.
   *
   * <p>{@code function register(Bits<N>) -> String<M>}
   */
  public static final BuiltIn REGISTER =
      BuiltIn.func("REGISTER",
          Type.relation(BitsType.class, StringType.class));

  ///// FIELDS /////

  public static final List<BuiltIn> BUILT_INS = List.of(
      // ARITHMETIC

      NEG,

      ADD,
      ADDS,
      ADDC,

      SATADD_SS,
      SATADD_UU,
      SATADDS_SS,
      SATADDS_UU,
      SATADDC_SS,
      SATADDC_UU,

      SUB,
      SUBSC,
      SUBSB,
      SUBC,
      SUBB,

      SATSUB_SS,
      SATSUB_UU,
      SATSUBS_SS,
      SATSUBS_UU,
      SATSUBC_SS,
      SATSUBC_UU,
      SATSUBB_SS,
      SATSUBB_UU,

      MUL_SS,
      MUL_UU,
      MULS_SS,
      MULS_UU,

      MOD_SS,
      MOD_UU,
      MODS_SS,
      MODS_UU,

      DIV_SS,
      DIV_UU,
      DIVS_SS,
      DIVS_UU,

      // LOGICAL

      NOT,
      AND,
      ANDS,
      XOR,
      XORS,
      OR,
      ORS,

      // Comparison

      EQU,
      NEQ,
      LTH_SS,
      LTH_UU,
      LEQ_SS,
      LEQ_UU,
      GTH_SS,
      GTH_UU,
      GEQ_SS,
      GEQ_UU,

      // Shifting

      LSL,
      LSLS,
      LSLC,
      ASR,
      LSR,
      ASRS,
      LSRS,
      ASRC,
      LSRC,
      ROL,
      ROLS,
      ROLC,
      ROR,
      RORS,
      RORC,
      RRX,

      // Bitwise Counting

      COB,
      CZB,
      CLZ,
      CLO,
      CLS,

      // FUNCTIONS

      MNEMONIC,
      CONCATENATE_STRINGS,
      REGISTER

  );

  public static Stream<BuiltIn> builtIns() {
    return BUILT_INS.stream();
  }

  /**
   * The BuiltIn class represents a built-in function or process in VADL.
   * It contains information about the name, operator, type, and kind of the built-in.
   */
  public static class BuiltIn {
    private final String name;
    private final @Nullable String operator;
    private final RelationType signature;
    private final Kind kind;

    private BuiltIn(String name, @Nullable String operator,
                    RelationType signature, Kind kind) {
      this.name = name;
      this.operator = operator;
      this.signature = signature;
      this.kind = kind;
    }

    private static BuiltIn func(String name, @Nullable String operator,
                                RelationType signature) {
      return new BuiltIn(name, operator, signature, Kind.FUNCTION);
    }

    private static BuiltIn func(String name, RelationType signature) {
      return new BuiltIn(name, null, signature, Kind.FUNCTION);
    }

    private static BuiltIn proc(String name, RelationType signature) {
      return new BuiltIn(name, null, signature, Kind.PROCESS);
    }

    public String name() {
      return name;
    }

    @Nullable
    public String operator() {
      return operator;
    }

    public Kind kind() {
      return kind;
    }

    public RelationType signature() {
      return signature;
    }

    /**
     * Determines if the built takes the specified argument type classes.
     *
     * @param args the argument type classes to check
     * @return true if the method takes the specified argument classes, false otherwise
     */
    public final boolean takes(Class<?>... args) {
      return takes(Arrays.stream(args).toList());
    }

    /**
     * Determines if the built takes the specified argument type classes.
     *
     * @param args the argument type classes to check
     * @return true if the method takes the specified argument classes, false otherwise
     */
    public final boolean takes(List<Class<?>> args) {
      if (signature.argTypeClasses().size() != args.size()) {
        return false;
      }

      for (int i = 0; i < args.size(); i++) {
        if (signature.argTypeClasses().get(i) != args.get(i)) {
          return false;
        }
      }
      return true;
    }

    public final boolean matches(RelationType type) {
      return this.signature.equals(type);
    }

    @Override
    public String toString() {
      return "VADL::" + name + signature;
    }

    /**
     * Describes if a built-in is a function (e.g. {@code add}) or process (e.g. {@code decode}).
     */
    public enum Kind {
      FUNCTION,
      PROCESS
    }
  }

}
