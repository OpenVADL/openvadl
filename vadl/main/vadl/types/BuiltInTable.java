package vadl.types;

import java.util.List;
import javax.annotation.Nullable;

public class BuiltInTable {

  ///// ARITHMETIC //////

  /**
   * {@code function neg( a : Bits<N> ) -> Bits<N> // <=> -a }
   */
  public final static BuiltIn NEG =
      new BuiltIn("NEG", "-", Type.relation(BitsType.class, BitsType.class));


  /**
   * {@code function add ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a + b }
   */
  public final static BuiltIn ADD =
      new BuiltIn("ADD", "+", Type.relation(BitsType.class, BitsType.class, BitsType.class));


  /**
   * {@code function adds( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public final static BuiltIn ADDS =
      new BuiltIn("ADDS", Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function addc( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public final static BuiltIn ADDC =
      new BuiltIn("ADDC",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class));


  /**
   * {@code function satadd ( a : SInt<N>, b : SInt<N> ) -> SInt<N> }
   */
  public final static BuiltIn SATADD_SS =
      new BuiltIn("SATADD", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function satadd ( a : UInt<N>, b : UInt<N> ) -> UInt<N> }
   */
  public final static BuiltIn SATADD_UU =
      new BuiltIn("SATADD", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function satadds( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public final static BuiltIn SATADDS_SS =
      new BuiltIn("SATADDS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function satadds( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public final static BuiltIn SATADDS_UU =
      new BuiltIn("SATADDS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function sataddc( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public final static BuiltIn SATADDC_SS =
      new BuiltIn("SATADDC",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function sataddc( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public final static BuiltIn SATADDC_UU =
      new BuiltIn("SATADDC",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function sub  ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a - c }
   */
  public final static BuiltIn SUB =
      new BuiltIn("SUB", "-", Type.relation(BitsType.class, BitsType.class, BitsType.class));


  /**
   * {@code function subsc( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public final static BuiltIn SUBSC =
      new BuiltIn("SUBSC", Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function subsb( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public final static BuiltIn SUBSB =
      new BuiltIn("SUBSB", Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function subc ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public final static BuiltIn SUBC =
      new BuiltIn("SUBC",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class));


  /**
   * {@code function subb ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public final static BuiltIn SUBB =
      new BuiltIn("SUBB",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class));


  /**
   * {@code function satsub ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a - c }
   */
  public final static BuiltIn SATSUB_SS =
      new BuiltIn("SATSUB", "-", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function satsub ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a - b }
   */
  public final static BuiltIn SATSUB_UU =
      new BuiltIn("SATSUB", "-", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function satsubs( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public final static BuiltIn SATSUBS_SS =
      new BuiltIn("SATSUBS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function satsubs( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public final static BuiltIn SATSUBS_UU =
      new BuiltIn("SATSUBS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function satsubc( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public final static BuiltIn SATSUBC_SS =
      new BuiltIn("SATSUBC",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function satsubc( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public final static BuiltIn SATSUBC_UU =
      new BuiltIn("SATSUBC",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function satsubb( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public final static BuiltIn SATSUBB_SS =
      new BuiltIn("SATSUBB",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function satsubb( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public final static BuiltIn SATSUBB_UU =
      new BuiltIn("SATSUBB",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function mul ( a : SInt<N>, b : SInt<N> ) -> SInt<2*N> // <=> a * b }
   */
  public final static BuiltIn MUL_SS =
      new BuiltIn("MUL", "*", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function mul ( a : UInt<N>, b : UInt<N> ) -> UInt<2*N> // <=> a * b }
   */
  public final static BuiltIn MUL_UU =
      new BuiltIn("MUL", "*", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function muls( a : SInt<N>, b : SInt<N> ) -> ( SInt<2*N>, Status ) }
   */
  public final static BuiltIn MULS_SS =
      new BuiltIn("MULS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function muls( a : UInt<N>, b : UInt<N> ) -> ( UInt<2*N>, Status ) }
   */
  public final static BuiltIn MULS_UU =
      new BuiltIn("MULS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function mod ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a % b }
   */
  public final static BuiltIn MOD_SS =
      new BuiltIn("MOD", "%", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function mod ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a % b }
   */
  public final static BuiltIn MOD_UU =
      new BuiltIn("MOD", "%", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function mods ( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public final static BuiltIn MODS_SS =
      new BuiltIn("MODS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function mods ( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public final static BuiltIn MODS_UU =
      new BuiltIn("MODS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function div ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a / b }
   */
  public final static BuiltIn DIV_SS =
      new BuiltIn("DIV", "/", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function div ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a / b }
   */
  public final static BuiltIn DIV_UU =
      new BuiltIn("DIV", "/", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function divs( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public final static BuiltIn DIVS_SS =
      new BuiltIn("DIVS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function divs( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public final static BuiltIn DIVS_UU =
      new BuiltIn("DIVS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  ///// LOGICAL //////
  
  /**
   * {@code function not ( a : Bits<N> ) ->Bits<N> // <=> ~a }
   */
  public final static BuiltIn NOT =
      new BuiltIn("NOT", "~", Type.relation(BitsType.class, BitsType.class));


  /**
   * {@code function and ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a & b }
   */
  public final static BuiltIn AND =
      new BuiltIn("AND", "&", Type.relation(BitsType.class, BitsType.class, BitsType.class));


  /**
   * {@code function ands( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public final static BuiltIn ANDS =
      new BuiltIn("ANDS", Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function xor ( a : Bits<N>, b : Bits<N> ) -> [ SInt<N> | UInt<N> ] // <=> a ^ b }
   */
  public final static BuiltIn XOR =
      new BuiltIn("XOR", "^", Type.relation(BitsType.class, BitsType.class, SIntType.class));


  /**
   * {@code function xors( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public final static BuiltIn XORS =
      new BuiltIn("XORS", Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function or ( a : Bits<N>, b : Bits<N> ) -> [ SInt<N> | UInt<N> ] // <=> a | b }
   */
  public final static BuiltIn OR =
      new BuiltIn("OR", "|", Type.relation(BitsType.class, BitsType.class, SIntType.class));


  /**
   * {@code function ors( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public final static BuiltIn ORS =
      new BuiltIn("ORS", Type.relation(BitsType.class, BitsType.class, TupleType.class));

  ///// COMPARISON //////

  ///// SHIFTING //////

  ///// BIT COUNTING //////

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
      ORS

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
