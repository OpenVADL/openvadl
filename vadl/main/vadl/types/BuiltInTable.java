package vadl.types;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import vadl.utils.functionInterfaces.TriFunction;
import vadl.viam.Constant;
import vadl.viam.ViamError;

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
      BuiltIn.func("NEG", "-", Type.relation(BitsType.class, BitsType.class),
          (List<Constant.Value> args) -> args.get(0).negate());


  /**
   * {@code function add ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a + b }
   */
  public static final BuiltIn ADD =
      BuiltIn.func("ADD", "+", Type.relation(BitsType.class, BitsType.class, BitsType.class),
          (Constant.Value a, Constant.Value b) -> a.add(b, false).get(0, Constant.Value.class)
      );


  /**
   * {@code function adds( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ADDS =
      BuiltIn.func("ADDS", null, Type.relation(BitsType.class, BitsType.class, TupleType.class),
          (Constant.Value a, Constant.Value b) -> a.add(b, false)
      );


  /**
   * {@code function addc( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ADDC =
      BuiltIn.func("ADDC",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class),
          (Constant.Value a, Constant.Value b, Constant.Value carry) -> a.add(b, carry.bool()));


  /**
   * {@code function ssatadd ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a +| b }
   */
  public static final BuiltIn SSATADD =
      BuiltIn.func("SSATADD", "+|", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function usatadd ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a +| b }
   */
  public static final BuiltIn USATADD =
      BuiltIn.func("USATADD", "+|", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function ssatadds( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SSATADDS =
      BuiltIn.func("SSATADDS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function usatadds( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn USATADDS =
      BuiltIn.func("USATADDS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function ssataddc( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SSATADDC =
      BuiltIn.func("SSATADDC",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function usataddc( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn USATADDC =
      BuiltIn.func("USATADDC",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function sub  ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a - c }
   *
   * @see Constant.Value#subtract(Constant.Value, Constant.Value.SubMode, boolean)
   */
  public static final BuiltIn SUB =
      BuiltIn.func("SUB", "-", Type.relation(BitsType.class, BitsType.class, BitsType.class),
          (Constant.Value a, Constant.Value b) -> a.subtract(b, Constant.Value.SubMode.X86_LIKE,
              false).firstValue()
      );


  /**
   * {@code function subsc( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   *
   * <p>Subtract without carry (subsc) acts as if the carry bit were set.
   *
   * @see Constant.Value#subtract(Constant.Value, Constant.Value.SubMode, boolean)
   */
  public static final BuiltIn SUBSC =
      BuiltIn.func("SUBSC", null, Type.relation(BitsType.class, BitsType.class, TupleType.class),
          (Constant.Value a, Constant.Value b) -> a.subtract(b,
              Constant.Value.SubMode.ARM_LIKE, true));


  /**
   * {@code function subsb( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   *
   * <p>Subtract without borrow (subsb) acts as if the borrow bit were clear.</p>
   *
   * @see Constant.Value#subtract(Constant.Value, Constant.Value.SubMode, boolean)
   */
  public static final BuiltIn SUBSB =
      BuiltIn.func("SUBSB", null, Type.relation(BitsType.class, BitsType.class, TupleType.class),
          (Constant.Value a, Constant.Value b) -> a.subtract(b,
              Constant.Value.SubMode.X86_LIKE, false));


  /**
   * {@code function subc ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   *
   * @see Constant.Value#subtract(Constant.Value, Constant.Value.SubMode, boolean)
   */
  public static final BuiltIn SUBC =
      BuiltIn.func("SUBC",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class),
          (Constant.Value a, Constant.Value b, Constant.Value carry) ->
              a.subtract(b, Constant.Value.SubMode.ARM_LIKE, carry.bool())
      );


  /**
   * {@code function subb ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   *
   * @see Constant.Value#subtract(Constant.Value, Constant.Value.SubMode, boolean)
   */
  public static final BuiltIn SUBB =
      BuiltIn.func("SUBB",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class),
          (Constant.Value a, Constant.Value b, Constant.Value carry) ->
              a.subtract(b, Constant.Value.SubMode.X86_LIKE, carry.bool())
      );


  /**
   * {@code function ssatsub ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a -| b }
   */
  public static final BuiltIn SSATSUB =
      BuiltIn.func("SSATSUB", "-|", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function usatsub ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a -| b }
   */
  public static final BuiltIn USATSUB =
      BuiltIn.func("USATSUB", "-|", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function ssatsubs( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SSATSUBS =
      BuiltIn.func("SSATSUBS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function usatsubs( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn USATSUBS =
      BuiltIn.func("USATSUBS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function ssatsubc( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SSATSUBC =
      BuiltIn.func("SSATSUBC",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function usatsubc( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn USATSUBC =
      BuiltIn.func("USATSUBC",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function ssatsubb( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SSATSUBB =
      BuiltIn.func("SSATSUBB",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function usatsubb( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn USATSUBB =
      BuiltIn.func("USATSUBB",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class), TupleType.class));


  /**
   * {@code function mul ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a * b }
   */
  public static final BuiltIn MUL =
      BuiltIn.func("MUL", "*", Type.relation(BitsType.class, BitsType.class, BitsType.class),
          (Constant.Value a, Constant.Value b) -> a.multiply(b, false)
      );


  /**
   * {@code function muls( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn MULS =
      BuiltIn.func("MULS", null, Type.relation(BitsType.class, BitsType.class, TupleType.class));


  /**
   * {@code function smull   ( a : SInt<N>, b : SInt<N> ) -> SInt<2*N> // <=> a *# b }
   */
  public static final BuiltIn SMULL =
      BuiltIn.func("SMULL", "*#", Type.relation(SIntType.class, SIntType.class, SIntType.class),
          (Constant.Value a, Constant.Value b) -> a.multiply(b, true));


  /**
   * {@code function umull   ( a : UInt<N>, b : UInt<N> ) -> UInt<2*N> // <=> a *# b }
   */
  public static final BuiltIn UMULL =
      BuiltIn.func("UMULL", "*#", Type.relation(UIntType.class, UIntType.class, UIntType.class),
          (Constant.Value a, Constant.Value b) -> a.multiply(b, true));


  /**
   * {@code function sumull  ( a : SInt<N>, b : UInt<N> ) -> SInt<2*N> // <=> a *# b }
   */
  public static final BuiltIn SUMULL =
      BuiltIn.func("SUMULL", "*#", Type.relation(SIntType.class, UIntType.class, SIntType.class));


  /**
   * {@code function smulls  ( a : SInt<N>, b : SInt<N> ) -> ( SInt<2*N>, Status ) }
   */
  public static final BuiltIn SMULLS =
      BuiltIn.func("SMULLS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function umulls  ( a : UInt<N>, b : UInt<N> ) -> ( UInt<2*N>, Status ) }
   */
  public static final BuiltIn UMULLS =
      BuiltIn.func("UMULLS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function sumulls ( a : SInt<N>, b : UInt<N> ) -> ( SInt<2*N>, Status ) }
   */
  public static final BuiltIn SUMULLS =
      BuiltIn.func("SUMULLS", Type.relation(SIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function smod ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a % b }
   */
  public static final BuiltIn SMOD =
      BuiltIn.func("SMOD", "%", Type.relation(SIntType.class, SIntType.class, SIntType.class));


  /**
   * {@code function umod ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a % b }
   */
  public static final BuiltIn UMOD =
      BuiltIn.func("UMOD", "%", Type.relation(UIntType.class, UIntType.class, UIntType.class));


  /**
   * {@code function smods( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SMODS =
      BuiltIn.func("SMODS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function umods( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn UMODS =
      BuiltIn.func("UMODS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


  /**
   * {@code function sdiv ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a / b }
   */
  public static final BuiltIn SDIV =
      BuiltIn.func("SDIV", "/", Type.relation(SIntType.class, SIntType.class, SIntType.class),
          Constant.Value::divide);


  /**
   * {@code function udiv ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a / b }
   */
  public static final BuiltIn UDIV =
      BuiltIn.func("UDIV", "/", Type.relation(UIntType.class, UIntType.class, UIntType.class),
          Constant.Value::divide);


  /**
   * {@code function sdivs( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SDIVS =
      BuiltIn.func("SDIVS", Type.relation(SIntType.class, SIntType.class, TupleType.class));


  /**
   * {@code function udivs( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn UDIVS =
      BuiltIn.func("UDIVS", Type.relation(UIntType.class, UIntType.class, TupleType.class));


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
   * {@code function slth ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a < b }
   */
  public static final BuiltIn SLTH =
      BuiltIn.func("SLTH", "<", Type.relation(SIntType.class, SIntType.class, BoolType.class));


  /**
   * {@code function ulth ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a < b }
   */
  public static final BuiltIn ULTH =
      BuiltIn.func("ULTH", "<", Type.relation(UIntType.class, UIntType.class, BoolType.class));


  /**
   * {@code function sleq ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a <= b }
   */
  public static final BuiltIn SLEQ =
      BuiltIn.func("SLEQ", "<=", Type.relation(SIntType.class, SIntType.class, BoolType.class));


  /**
   * {@code function uleq ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a <= b }
   */
  public static final BuiltIn ULEQ =
      BuiltIn.func("ULEQ", "<=", Type.relation(UIntType.class, UIntType.class, BoolType.class));


  /**
   * {@code function sgth ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a > b }
   */
  public static final BuiltIn SGTH =
      BuiltIn.func("SGTH", ">", Type.relation(SIntType.class, SIntType.class, BoolType.class));


  /**
   * {@code function ugth ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a > b }
   */
  public static final BuiltIn UGTH =
      BuiltIn.func("UGTH", ">", Type.relation(UIntType.class, UIntType.class, BoolType.class));


  /**
   * {@code function sgeq ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a >= b }
   */
  public static final BuiltIn SGEQ =
      BuiltIn.func("SGEQ", ">=", Type.relation(SIntType.class, SIntType.class, BoolType.class));


  /**
   * {@code function ugeq ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a >= b }
   */
  public static final BuiltIn UGEQ =
      BuiltIn.func("UGEQ", ">=", Type.relation(UIntType.class, UIntType.class, BoolType.class));


  ///// SHIFTING //////


  /**
   * {@code function lsl ( a : Bits<N>, b : UInt<M> ) -> Bits<N> // <=> a << b }
   */
  public static final BuiltIn LSL =
      BuiltIn.func("LSL", "<<", Type.relation(BitsType.class, UIntType.class, BitsType.class),
          Constant.Value::lsl);


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
   *
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
      BuiltIn.func("CONCATENATE",
          Type.relation(StringType.class, StringType.class, StringType.class));

  /**
   * Formats the register file index.
   *
   * <p>{@code function register(Bits<N>) -> String<M>}
   */
  public static final BuiltIn REGISTER =
      BuiltIn.func("REGISTER",
          Type.relation(BitsType.class, StringType.class));

  /**
   * Formats value to binary string.
   *
   * <p>{@code function binary(Bits<N>) -> String<M>}
   */
  public static final BuiltIn BINARY =
      BuiltIn.func("BINARY", Type.relation(BitsType.class, StringType.class));

  /**
   * Formats value to decimal string.
   *
   * <p>{@code function decimal(Bits<N>) -> String<M>}
   */
  public static final BuiltIn DECIMAL =
      BuiltIn.func("DECIMAL",
          Type.relation(BitsType.class, StringType.class));

  /**
   * Formats value to hex string.
   *
   * <p>{@code function hex(Bits<N>) -> String<M>}
   */
  public static final BuiltIn HEX =
      BuiltIn.func("HEX", Type.relation(BitsType.class, StringType.class));

  /**
   * Formats value to octal string.
   *
   * <p>{@code function hex(Bits<N>) -> String<M>}
   */
  public static final BuiltIn OCTAL =
      BuiltIn.func("octal", Type.relation(BitsType.class, StringType.class));

  ///// FIELDS /////

  public static final List<BuiltIn> BUILT_INS = List.of(
      // ARITHMETIC

      NEG,

      ADD,
      ADDS,
      ADDC,

      SSATADD,
      USATADD,
      SSATADDS,
      USATADDS,
      SSATADDC,
      USATADDC,

      SUB,
      SUBSC,
      SUBSB,
      SUBC,
      SUBB,

      SSATSUB,
      USATSUB,
      SSATSUBS,
      USATSUBS,
      SSATSUBC,
      USATSUBC,
      SSATSUBB,
      USATSUBB,

      MUL,
      MULS,

      UMULL,
      SMULL,
      SUMULL,
      SMULLS,
      UMULLS,
      SUMULLS,

      SMOD,
      UMOD,
      SMODS,
      UMODS,

      SDIV,
      UDIV,
      SDIVS,
      UDIVS,

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
      SLTH,
      ULTH,
      SLEQ,
      ULEQ,
      SGTH,
      UGTH,
      SGEQ,
      UGEQ,

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
      REGISTER,

      BINARY,
      DECIMAL,
      HEX,
      OCTAL

  );

  public static final HashSet<BuiltIn> commutative = new HashSet<>(List.of(
      // ARITHMETIC
      ADD,
      ADDS,
      ADDC,
      SSATADD,
      USATADD,
      SSATADDS,
      USATADDS,
      SSATADDC,
      USATADDC,
      MUL,
      MULS,
      UMULL,
      UMULLS,
      SMULL,
      SMULLS,
      SUMULL,
      SUMULLS,
      // LOGIC
      AND,
      ANDS,
      OR,
      ORS,
      XOR,
      XORS,
      // Comparisons
      EQU,
      NEQ
  ));

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

    @SuppressWarnings("LineLength")
    private static <T extends Constant, R extends Constant> BuiltIn func(String name,
                                                                         @Nullable String operator,
                                                                         RelationType signature,
                                                                         @Nullable
                                                                         Function<List<T>, R> computeFunction) {
      return new BuiltIn(name, operator, signature, Kind.FUNCTION) {
        @Override
        public Optional<Constant> compute(List<Constant> args) {
          if (computeFunction == null) {
            return super.compute(args);
          }

          var argTypes = args.stream()
              .map(e -> (Class<?>) e.type().getClass())
              .toArray(Class<?>[]::new);
          if (!takes(argTypes)) {
            throw new ViamError("Types of arguments does not match type signature of " + signature)
                .addContext("built-in", this)
                .addContext("constants", List.of(args));
          }

          var typedArgs = args.stream().map(e -> (T) e).toList();
          return Optional.of(computeFunction.apply(typedArgs));
        }
      };
    }

    private static <T extends Constant, U extends Constant, R extends Constant> BuiltIn func(
        String name, @Nullable String operator,
        RelationType signature,
        BiFunction<T, U, R> computeFunction) {
      return func(name, operator, signature,
          (args) -> computeFunction.apply((T) args.get(0), (U) args.get(1)));
    }

    @SuppressWarnings("LineLength")
    private static <A extends Constant, B extends Constant, C extends Constant, R extends Constant> BuiltIn func(
        String name,
        RelationType signature,
        TriFunction<A, B, C, R> computeFunction) {
      return func(name, null, signature,
          (args) -> computeFunction.apply((A) args.get(0), (B) args.get(1), (C) args.get(2)));
    }


    private static BuiltIn func(String name, RelationType signature,
                                @Nullable Function<List<Constant>, Constant> compute) {
      return func(name, null, signature, compute);
    }


    // TODO: Eventually delete these two functions (with no compute function)
    private static BuiltIn func(String name, @Nullable String operator,
                                RelationType signature) {
      return func(name, operator, signature, (Function<List<Constant>, Constant>) null);
    }

    private static BuiltIn func(String name, RelationType signature) {
      return func(name, (String) null, signature, (Function<List<Constant>, Constant>) null);
    }

    // TODO: removed as soon as used
    @SuppressWarnings("unused")
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

    public Optional<Constant> compute(List<Constant> args) {
      logger.atWarn().log("Computation of constants for built-in {} is not implemented", this);
      return Optional.empty();
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

    public List<Class<? extends Type>> argTypeClasses() {
      return signature.argTypeClasses();
    }

    public Class<? extends Type> resultTypeClass() {
      return signature.resultTypeClass();
    }

    /**
     * Describes if a built-in is a function (e.g. {@code add}) or process (e.g. {@code decode}).
     */
    public enum Kind {
      FUNCTION,
      PROCESS
    }

    private static final Logger logger = getLogger(BuiltIn.class);

  }

}
