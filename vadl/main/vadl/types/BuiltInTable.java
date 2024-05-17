package vadl.types;

import java.util.List;
import javax.annotation.Nullable;

public class BuiltInTable {

  ///// ARITHMETIC //////

  /**
   * {@code function neg( a : Bits<N> ) -> Bits<N> // <=> -a }
   */
  public static final BuiltIn NEG =
      new BuiltIn("NEG", "-", Type.relation(BitsType.class, BitsType.class));


  /**
   * {@code function add ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a + b }
   */
  public static final BuiltIn ADD =
      new BuiltIn("ADD", "+", Type.relation(BitsType.class, BitsType.class, BitsType.class));


  /**
   * {@code function adds( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ADDS =
      new BuiltIn("ADDS", Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function addc( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ADDC =
      new BuiltIn("ADDC",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class));


  /**
   * {@code function satadd ( a : SInt<N>, b : SInt<N> ) -> SInt<N> }
   */
  public static final BuiltIn SATADD_SS =
      new BuiltIn("SATADD", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function satadd ( a : UInt<N>, b : UInt<N> ) -> UInt<N> }
   */
  public static final BuiltIn SATADD_UU =
      new BuiltIn("SATADD", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function satadds( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SATADDS_SS =
      new BuiltIn("SATADDS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function satadds( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn SATADDS_UU =
      new BuiltIn("SATADDS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function sataddc( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SATADDC_SS =
      new BuiltIn("SATADDC",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function sataddc( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn SATADDC_UU =
      new BuiltIn("SATADDC",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function sub  ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a - c }
   */
  public static final BuiltIn SUB =
      new BuiltIn("SUB", "-", Type.relation(BitsType.class, BitsType.class, BitsType.class));


  /**
   * {@code function subsc( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn SUBSC =
      new BuiltIn("SUBSC", Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function subsb( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn SUBSB =
      new BuiltIn("SUBSB", Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function subc ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn SUBC =
      new BuiltIn("SUBC",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class));


  /**
   * {@code function subb ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn SUBB =
      new BuiltIn("SUBB",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class));


  /**
   * {@code function satsub ( a : SInt<N>, b : SInt<N> ) -> SInt<N> }
   */
  public static final BuiltIn SATSUB_SS =
      new BuiltIn("SATSUB", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function satsub ( a : UInt<N>, b : UInt<N> ) -> UInt<N> }
   */
  public static final BuiltIn SATSUB_UU =
      new BuiltIn("SATSUB", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function satsubs( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SATSUBS_SS =
      new BuiltIn("SATSUBS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function satsubs( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn SATSUBS_UU =
      new BuiltIn("SATSUBS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function satsubc( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SATSUBC_SS =
      new BuiltIn("SATSUBC",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function satsubc( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn SATSUBC_UU =
      new BuiltIn("SATSUBC",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function satsubb( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SATSUBB_SS =
      new BuiltIn("SATSUBB",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function satsubb( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn SATSUBB_UU =
      new BuiltIn("SATSUBB",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function mul ( a : SInt<N>, b : SInt<N> ) -> SInt<2*N> // <=> a * b }
   */
  public static final BuiltIn MUL_SS =
      new BuiltIn("MUL", "*", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function mul ( a : UInt<N>, b : UInt<N> ) -> UInt<2*N> // <=> a * b }
   */
  public static final BuiltIn MUL_UU =
      new BuiltIn("MUL", "*", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function muls( a : SInt<N>, b : SInt<N> ) -> ( SInt<2*N>, Status ) }
   */
  public static final BuiltIn MULS_SS =
      new BuiltIn("MULS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function muls( a : UInt<N>, b : UInt<N> ) -> ( UInt<2*N>, Status ) }
   */
  public static final BuiltIn MULS_UU =
      new BuiltIn("MULS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function mod ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a % b }
   */
  public static final BuiltIn MOD_SS =
      new BuiltIn("MOD", "%", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function mod ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a % b }
   */
  public static final BuiltIn MOD_UU =
      new BuiltIn("MOD", "%", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function mods ( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn MODS_SS =
      new BuiltIn("MODS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function mods ( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn MODS_UU =
      new BuiltIn("MODS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function div ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a / b }
   */
  public static final BuiltIn DIV_SS =
      new BuiltIn("DIV", "/", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function div ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a / b }
   */
  public static final BuiltIn DIV_UU =
      new BuiltIn("DIV", "/", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function divs( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn DIVS_SS =
      new BuiltIn("DIVS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function divs( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn DIVS_UU =
      new BuiltIn("DIVS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  ///// LOGICAL //////

  /**
   * {@code function not ( a : Bits<N> ) ->Bits<N> // <=> ~a }
   */
  public static final BuiltIn NOT =
      new BuiltIn("NOT", "~", Type.relation(BitsType.class, BitsType.class));


  /**
   * {@code function and ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a & b }
   */
  public static final BuiltIn AND =
      new BuiltIn("AND", "&", Type.relation(BitsType.class, BitsType.class, BitsType.class));


  /**
   * {@code function ands( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ANDS =
      new BuiltIn("ANDS", Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function xor ( a : Bits<N>, b : Bits<N> ) -> [ SInt<N> | UInt<N> ] // <=> a ^ b }
   */
  public static final BuiltIn XOR =
      new BuiltIn("XOR", "^", Type.relation(BitsType.class, BitsType.class, SIntType.class));


  /**
   * {@code function xors( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn XORS =
      new BuiltIn("XORS", Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function or ( a : Bits<N>, b : Bits<N> ) -> [ SInt<N> | UInt<N> ] // <=> a | b }
   */
  public static final BuiltIn OR =
      new BuiltIn("OR", "|", Type.relation(BitsType.class, BitsType.class, SIntType.class));


  /**
   * {@code function ors( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ORS =
      new BuiltIn("ORS", Type.relation(BitsType.class, BitsType.class, TupleType.class));

  ///// COMPARISON //////


  /**
   * {@code function equ ( a : Bits<N>, b : Bits<N> ) -> Bool // <=> a = b }
   */
  public static final BuiltIn EQU =
      new BuiltIn("EQU", "=", Type.relation(BitsType.class, BitsType.class, BoolType.class));


  /**
   * {@code function neq ( a : Bits<N>, b : Bits<N> ) -> Bool // <=> a != b }
   */
  public static final BuiltIn NEQ =
      new BuiltIn("NEQ", "!=", Type.relation(BitsType.class, BitsType.class, BoolType.class));


  /**
   * {@code function lth ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a < b }
   */
  public static final BuiltIn LTH_SS =
      new BuiltIn("LTH", "<", Type.relation(SIntType.class, SIntType.class, BoolType.class));


  /**
   * {@code function lth ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a < b }
   */
  public static final BuiltIn LTH_UU =
      new BuiltIn("LTH", "<", Type.relation(UIntType.class, UIntType.class, BoolType.class));


  /**
   * {@code function leq ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a <= b }
   */
  public static final BuiltIn LEQ_SS =
      new BuiltIn("LEQ", "<=", Type.relation(SIntType.class, SIntType.class, BoolType.class));


  /**
   * {@code function leq ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a <= b }
   */
  public static final BuiltIn LEQ_UU =
      new BuiltIn("LEQ", "<=", Type.relation(UIntType.class, UIntType.class, BoolType.class));


  /**
   * {@code function gth ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a > b }
   */
  public static final BuiltIn GTH_SS =
      new BuiltIn("GTH", ">", Type.relation(SIntType.class, SIntType.class, BoolType.class));


  /**
   * {@code function gth ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a > b }
   */
  public static final BuiltIn GTH_UU =
      new BuiltIn("GTH", ">", Type.relation(UIntType.class, UIntType.class, BoolType.class));


  /**
   * {@code function geq ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a >= b }
   */
  public static final BuiltIn GEQ_SS =
      new BuiltIn("GEQ", ">=", Type.relation(SIntType.class, SIntType.class, BoolType.class));


  /**
   * {@code function geq ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a >= b }
   */
  public static final BuiltIn GEQ_UU =
      new BuiltIn("GEQ", ">=", Type.relation(UIntType.class, UIntType.class, BoolType.class));

  ///// SHIFTING //////


  /**
   * {@code function lsl ( a : Bits<N>, b : UInt<M> ) -> Bits<N> // <=> a << b }
   */
  public static final BuiltIn LSL =
      new BuiltIn("LSL", "<<", Type.relation(BitsType.class, UIntType.class, BitsType.class));


  /**
   * {@code function lsls( a : Bits<N>, b : UInt<M> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn LSLS =
      new BuiltIn("LSLS", Type.relation(BitsType.class, UIntType.class, TupleType.class));


  /**
   * {@code function lslc( a : Bits<N>, b : UInt<M>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn LSLC =
      new BuiltIn("LSLC",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function asr ( a : SInt<N>, b : UInt<M> ) -> SInt<N> // <=> a >> b }
   */
  public static final BuiltIn ASR =
      new BuiltIn("ASR", ">>", Type.relation(SIntType.class, UIntType.class, SIntType.class));


  /**
   * {@code function lsr ( a : UInt<N>, b : UInt<M> ) -> UInt<N> // <=> a >> b }
   */
  public static final BuiltIn LSR =
      new BuiltIn("LSR", ">>", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function asrs( a : SInt<N>, b : UInt<M> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn ASRS =
      new BuiltIn("ASRS", Type.relation(SIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function lsrs( a : UInt<N>, b : UInt<M> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn LSRS =
      new BuiltIn("LSRS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function asrc( a : SInt<N>, b : UInt<M>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn ASRC =
      new BuiltIn("ASRC",
          Type.relation(List.of(SIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function lsrc( a : UInt<N>, b : UInt<M>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn LSRC =
      new BuiltIn("LSRC",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function rol ( a : Bits<N>, b : UInt<M> ) ->Bits<N> }
   */
  public static final BuiltIn ROL =
      new BuiltIn("ROL", Type.relation(BitsType.class, UIntType.class, BitsType.class));


  /**
   * {@code function rols( a : Bits<N>, b : UInt<M> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ROLS =
      new BuiltIn("ROLS", Type.relation(BitsType.class, UIntType.class, TupleType.class));


  /**
   * {@code function rolc( a : Bits<N>, b : UInt<M>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ROLC =
      new BuiltIn("ROLC",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function ror ( a : Bits<N>, b : UInt<M> ) -> Bits<N> }
   */
  public static final BuiltIn ROR =
      new BuiltIn("ROR", Type.relation(BitsType.class, UIntType.class, BitsType.class));


  /**
   * {@code function rors( a : Bits<N>, b : UInt<M> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn RORS =
      new BuiltIn("RORS", Type.relation(BitsType.class, UIntType.class, TupleType.class));


  /**
   * {@code function rorc( a : Bits<N>, b : UInt<M>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn RORC =
      new BuiltIn("RORC",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function rrx ( a : Bits<N>, b : UInt<M>, c : Bool ) -> Bits<N> }
   */
  public static final BuiltIn RRX =
      new BuiltIn("RRX",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class), BitsType.class));


  ///// BIT COUNTING //////


  /**
   * {@code function cob( a : Bits<N> ) -> UInt<N> // counting one bits }
   */
  public static final BuiltIn COB =
      new BuiltIn("COB", Type.relation(BitsType.class, UIntType.class));


  /**
   * {@code function czb( a : Bits<N> ) -> UInt<N> // counting zero bits }
   */
  public static final BuiltIn CZB =
      new BuiltIn("CZB", Type.relation(BitsType.class, UIntType.class));


  /**
   * {@code function clz( a : Bits<N> ) -> UInt<N> // counting leading zeros }
   */
  public static final BuiltIn CLZ =
      new BuiltIn("CLZ", Type.relation(BitsType.class, UIntType.class));


  /**
   * {@code function clo( a : Bits<N> ) -> UInt<N> // counting leading ones }
   */
  public static final BuiltIn CLO =
      new BuiltIn("CLO", Type.relation(BitsType.class, UIntType.class));


  /**
   * {@code function cls( a : Bits<N> ) -> UInt<N> // counting leading sign bits (without sign bit) }
   */
  public static final BuiltIn CLS =
      new BuiltIn("CLS", Type.relation(BitsType.class, TupleType.class));


  ///// FUNCTIONS //////

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
      CLS

  );

  public static class BuiltIn {
    private final String name;
    private final @Nullable String operator;
    protected final RelationType type;

    private BuiltIn(String name, @Nullable String operator,
                    RelationType type) {
      this.name = name;
      this.operator = operator;
      this.type = type;
    }

    private BuiltIn(String name,
                    RelationType type) {
      this(name, null, type);
    }

    public String name() {
      return name;
    }

    @Nullable
    public String operator() {
      return operator;
    }

    /**
     * Determines if the built takes the specified argument type classes.
     *
     * @param args the argument type classes to check
     * @return true if the method takes the specified argument classes, false otherwise
     */
    public final boolean takes(Class<?>... args) {
      if (type.argTypeClass().size() != args.length) {
        return false;
      }

      for (int i = 0; i < args.length; i++) {
        if (type.argTypeClass().get(i) != args[i]) {
          return false;
        }
      }
      return true;
    }

    @Override
    public String toString() {
      return "VADL::" + name + type;
    }
  }

}
