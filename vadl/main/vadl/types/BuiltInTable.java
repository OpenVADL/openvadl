package vadl.types;

import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.collect.Streams;
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
      func("NEG", "-", Type.relation(BitsType.class, BitsType.class))
          .computeUnary(Constant.Value::negate)
          .takesDefault()
          .build();


  /**
   * {@code function add ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a + b }
   */
  public static final BuiltIn ADD =
      func("ADD", "+", Type.relation(BitsType.class, BitsType.class, BitsType.class))
          .compute(
              (Constant.Value a, Constant.Value b) -> a.add(b, false).get(0, Constant.Value.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function adds( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ADDS =
      func("ADDS", null, Type.relation(BitsType.class, BitsType.class, TupleType.class))
          .compute((Constant.Value a, Constant.Value b) -> a.add(b, false))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function addc( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ADDC =
      func("ADDC",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class))
          .compute(
              (Constant.Value a, Constant.Value b, Constant.Value carry) -> a.add(b, carry.bool()))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function ssatadd ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a +| b }
   */
  public static final BuiltIn SSATADD =
      func("SSATADD", "+|", Type.relation(SIntType.class, SIntType.class, SIntType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function usatadd ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a +| b }
   */
  public static final BuiltIn USATADD =
      func("USATADD", "+|", Type.relation(UIntType.class, UIntType.class, UIntType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function ssatadds( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SSATADDS =
      func("SSATADDS", Type.relation(SIntType.class, SIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function usatadds( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn USATADDS =
      func("USATADDS", Type.relation(UIntType.class, UIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function ssataddc( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SSATADDC =
      func("SSATADDC",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class),
              TupleType.class))
          .takesFirstTwoWithSameBitWidths()
          .build();


  /**
   * {@code function usataddc( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn USATADDC =
      func("USATADDC",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesFirstTwoWithSameBitWidths()
          .build();


  /**
   * {@code function sub  ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a - c }
   *
   * @see Constant.Value#subtract(Constant.Value, Constant.Value.SubMode, boolean)
   */
  public static final BuiltIn SUB =
      func("SUB", "-", Type.relation(BitsType.class, BitsType.class, BitsType.class))
          .compute(
              (Constant.Value a, Constant.Value b) -> a.subtract(b, Constant.Value.SubMode.X86_LIKE,
                  false).firstValue())
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function subsc( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   *
   * <p>Subtract without carry (subsc) acts as if the carry bit were set.
   *
   * @see Constant.Value#subtract(Constant.Value, Constant.Value.SubMode, boolean)
   */
  public static final BuiltIn SUBSC =
      func("SUBSC", null, Type.relation(BitsType.class, BitsType.class, TupleType.class))
          .compute((Constant.Value a, Constant.Value b) -> a.subtract(b,
              Constant.Value.SubMode.ARM_LIKE, true))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function subsb( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   *
   * <p>Subtract without borrow (subsb) acts as if the borrow bit were clear.</p>
   *
   * @see Constant.Value#subtract(Constant.Value, Constant.Value.SubMode, boolean)
   */
  public static final BuiltIn SUBSB =
      func("SUBSB", null, Type.relation(BitsType.class, BitsType.class, TupleType.class))
          .compute(
              (Constant.Value a, Constant.Value b) -> a.subtract(b, Constant.Value.SubMode.X86_LIKE,
                  false))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function subc ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   *
   * @see Constant.Value#subtract(Constant.Value, Constant.Value.SubMode, boolean)
   */
  public static final BuiltIn SUBC =
      func("SUBC",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class))
          .compute((Constant.Value a, Constant.Value b, Constant.Value carry) ->
              a.subtract(b, Constant.Value.SubMode.ARM_LIKE, carry.bool())
          )
          .takesFirstTwoWithSameBitWidths()
          .build();


  /**
   * {@code function subb ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   *
   * @see Constant.Value#subtract(Constant.Value, Constant.Value.SubMode, boolean)
   */
  public static final BuiltIn SUBB =
      func("SUBB",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class))
          .compute((Constant.Value a, Constant.Value b, Constant.Value carry) ->
              a.subtract(b, Constant.Value.SubMode.X86_LIKE, carry.bool())
          )
          .takesFirstTwoWithSameBitWidths()
          .build();


  /**
   * {@code function ssatsub ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a -| b }
   */
  public static final BuiltIn SSATSUB =
      func("SSATSUB", "-|", Type.relation(SIntType.class, SIntType.class, SIntType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function usatsub ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a -| b }
   */
  public static final BuiltIn USATSUB =
      func("USATSUB", "-|", Type.relation(UIntType.class, UIntType.class, UIntType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function ssatsubs( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SSATSUBS =
      func("SSATSUBS", Type.relation(SIntType.class, SIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function usatsubs( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn USATSUBS =
      func("USATSUBS", Type.relation(UIntType.class, UIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function ssatsubc( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SSATSUBC =
      func("SSATSUBC",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class),
              TupleType.class))
          .takesFirstTwoWithSameBitWidths()
          .build();


  /**
   * {@code function usatsubc( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn USATSUBC =
      func("USATSUBC",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesFirstTwoWithSameBitWidths()
          .build();


  /**
   * {@code function ssatsubb( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SSATSUBB =
      func("SSATSUBB",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class),
              TupleType.class))
          .takesFirstTwoWithSameBitWidths()
          .build();


  /**
   * {@code function usatsubb( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn USATSUBB =
      func("USATSUBB",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesFirstTwoWithSameBitWidths()
          .build();


  /**
   * {@code function mul ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a * b }
   */
  public static final BuiltIn MUL =
      func("MUL", "*", Type.relation(BitsType.class, BitsType.class, BitsType.class))
          .compute((Constant.Value a, Constant.Value b) -> a.multiply(b, false)
          )
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function muls( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn MULS =
      func("MULS", null, Type.relation(BitsType.class, BitsType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function smull   ( a : SInt<N>, b : SInt<N> ) -> SInt<2*N> // <=> a *# b }
   */
  public static final BuiltIn SMULL =
      func("SMULL", "*#", Type.relation(SIntType.class, SIntType.class, SIntType.class)).compute(
              (Constant.Value a, Constant.Value b) -> a.multiply(b, true))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function umull   ( a : UInt<N>, b : UInt<N> ) -> UInt<2*N> // <=> a *# b }
   */
  public static final BuiltIn UMULL =
      func("UMULL", "*#", Type.relation(UIntType.class, UIntType.class, UIntType.class)).compute(
              (Constant.Value a, Constant.Value b) -> a.multiply(b, true))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function sumull  ( a : SInt<N>, b : UInt<N> ) -> SInt<2*N> // <=> a *# b }
   */
  public static final BuiltIn SUMULL =
      func("SUMULL", "*#", Type.relation(SIntType.class, UIntType.class, SIntType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function smulls  ( a : SInt<N>, b : SInt<N> ) -> ( SInt<2*N>, Status ) }
   */
  public static final BuiltIn SMULLS =
      func("SMULLS", Type.relation(SIntType.class, SIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function umulls  ( a : UInt<N>, b : UInt<N> ) -> ( UInt<2*N>, Status ) }
   */
  public static final BuiltIn UMULLS =
      func("UMULLS", Type.relation(UIntType.class, UIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function sumulls ( a : SInt<N>, b : UInt<N> ) -> ( SInt<2*N>, Status ) }
   */
  public static final BuiltIn SUMULLS =
      func("SUMULLS", Type.relation(SIntType.class, UIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function smod ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a % b }
   */
  public static final BuiltIn SMOD =
      func("SMOD", "%", Type.relation(SIntType.class, SIntType.class, SIntType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function umod ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a % b }
   */
  public static final BuiltIn UMOD =
      func("UMOD", "%", Type.relation(UIntType.class, UIntType.class, UIntType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function smods( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SMODS =
      func("SMODS", Type.relation(SIntType.class, SIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function umods( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn UMODS =
      func("UMODS", Type.relation(UIntType.class, UIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function sdiv ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a / b }
   */
  public static final BuiltIn SDIV =
      func("SDIV", "/", Type.relation(SIntType.class, SIntType.class, SIntType.class))
          .compute(Constant.Value::divide)
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function udiv ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a / b }
   */
  public static final BuiltIn UDIV =
      func("UDIV", "/", Type.relation(UIntType.class, UIntType.class, UIntType.class))
          .compute(Constant.Value::divide)
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function sdivs( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SDIVS =
      func("SDIVS", Type.relation(SIntType.class, SIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function udivs( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn UDIVS =
      func("UDIVS", Type.relation(UIntType.class, UIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .build();


  ///// LOGICAL //////

  /**
   * {@code function not ( a : Bits<N> ) ->Bits<N> // <=> ~a }
   */
  public static final BuiltIn NOT =
      func("NOT", "~", Type.relation(BitsType.class, BitsType.class))
          .compute((List<Constant.Value> args) -> args.get(0).not())
          .takesDefault()
          .build();


  /**
   * {@code function and ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a & b }
   */
  public static final BuiltIn AND =
      func("AND", "&", Type.relation(BitsType.class, BitsType.class, BitsType.class))
          .compute(Constant.Value::and)
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function ands( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ANDS =
      func("ANDS", Type.relation(BitsType.class, BitsType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function xor ( a : Bits<N>, b : Bits<N> ) -> [ SInt<N> | UInt<N> ] // <=> a ^ b }
   */
  public static final BuiltIn XOR =
      func("XOR", "^", Type.relation(BitsType.class, BitsType.class, SIntType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function xors( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn XORS =
      func("XORS", Type.relation(BitsType.class, BitsType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function or ( a : Bits<N>, b : Bits<N> ) -> [ SInt<N> | UInt<N> ] // <=> a | b }
   */
  public static final BuiltIn OR =
      func("OR", "|", Type.relation(BitsType.class, BitsType.class, SIntType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function ors( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ORS =
      func("ORS", Type.relation(BitsType.class, BitsType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .build();

  ///// COMPARISON //////


  /**
   * {@code function equ ( a : Bits<N>, b : Bits<N> ) -> Bool // <=> a = b }
   */
  public static final BuiltIn EQU =
      func("EQU", "=", Type.relation(BitsType.class, BitsType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function neq ( a : Bits<N>, b : Bits<N> ) -> Bool // <=> a != b }
   */
  public static final BuiltIn NEQ =
      func("NEQ", "!=", Type.relation(BitsType.class, BitsType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function slth ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a < b }
   */
  public static final BuiltIn SLTH =
      func("SLTH", "<", Type.relation(SIntType.class, SIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function ulth ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a < b }
   */
  public static final BuiltIn ULTH =
      func("ULTH", "<", Type.relation(UIntType.class, UIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function sleq ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a <= b }
   */
  public static final BuiltIn SLEQ =
      func("SLEQ", "<=",
          Type.relation(SIntType.class, SIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function uleq ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a <= b }
   */
  public static final BuiltIn ULEQ =
      func("ULEQ", "<=",
          Type.relation(UIntType.class, UIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function sgth ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a > b }
   */
  public static final BuiltIn SGTH =
      func("SGTH", ">",
          Type.relation(SIntType.class, SIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function ugth ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a > b }
   */
  public static final BuiltIn UGTH =
      func("UGTH", ">",
          Type.relation(UIntType.class, UIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function sgeq ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a >= b }
   */
  public static final BuiltIn SGEQ =
      func("SGEQ", ">=",
          Type.relation(SIntType.class, SIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .build();


  /**
   * {@code function ugeq ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a >= b }
   */
  public static final BuiltIn UGEQ =
      func("UGEQ", ">=",
          Type.relation(UIntType.class, UIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .build();


  ///// SHIFTING //////


  /**
   * {@code function lsl ( a : Bits<N>, b : UInt<M> ) -> Bits<N> // <=> a << b }
   */
  public static final BuiltIn LSL =
      func("LSL", "<<", Type.relation(BitsType.class, UIntType.class, BitsType.class))
          .compute(Constant.Value::lsl)
          .takesDefault()
          .build();


  /**
   * {@code function lsls( a : Bits<N>, b : UInt<M> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn LSLS =
      func("LSLS", Type.relation(BitsType.class, UIntType.class, TupleType.class))
          .takesDefault()
          .build();


  /**
   * {@code function lslc( a : Bits<N>, b : UInt<M>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn LSLC =
      func("LSLC",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesDefault()
          .build();


  /**
   * {@code function asr ( a : SInt<N>, b : UInt<M> ) -> SInt<N> // <=> a >> b }
   */
  public static final BuiltIn ASR =
      func("ASR", ">>", Type.relation(SIntType.class, UIntType.class, SIntType.class))
          .takesDefault()
          .build();


  /**
   * {@code function lsr ( a : UInt<N>, b : UInt<M> ) -> UInt<N> // <=> a >> b }
   */
  public static final BuiltIn LSR =
      func("LSR", ">>", Type.relation(UIntType.class, UIntType.class, UIntType.class))
          .takesDefault()
          .build();


  /**
   * {@code function asrs( a : SInt<N>, b : UInt<M> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn ASRS =
      func("ASRS", Type.relation(SIntType.class, UIntType.class, TupleType.class))
          .takesDefault()
          .build();


  /**
   * {@code function lsrs( a : UInt<N>, b : UInt<M> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn LSRS =
      func("LSRS", Type.relation(UIntType.class, UIntType.class, TupleType.class))
          .takesDefault()
          .build();


  /**
   * {@code function asrc( a : SInt<N>, b : UInt<M>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn ASRC =
      func("ASRC",
          Type.relation(List.of(SIntType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesDefault()
          .build();


  /**
   * {@code function lsrc( a : UInt<N>, b : UInt<M>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn LSRC =
      func("LSRC",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesDefault()
          .build();


  /**
   * {@code function rol ( a : Bits<N>, b : UInt<M> ) ->Bits<N> }
   */
  public static final BuiltIn ROL =
      func("ROL", Type.relation(BitsType.class, UIntType.class, BitsType.class))
          .takesDefault()
          .build();


  /**
   * {@code function rols( a : Bits<N>, b : UInt<M> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ROLS =
      func("ROLS", Type.relation(BitsType.class, UIntType.class, TupleType.class))
          .takesDefault()
          .build();


  /**
   * {@code function rolc( a : Bits<N>, b : UInt<M>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ROLC =
      func("ROLC",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesDefault()
          .build();


  /**
   * {@code function ror ( a : Bits<N>, b : UInt<M> ) -> Bits<N> }
   */
  public static final BuiltIn ROR =
      func("ROR", Type.relation(BitsType.class, UIntType.class, BitsType.class))
          .takesDefault()
          .build();


  /**
   * {@code function rors( a : Bits<N>, b : UInt<M> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn RORS =
      func("RORS", Type.relation(BitsType.class, UIntType.class, TupleType.class))
          .takesDefault()
          .build();


  /**
   * {@code function rorc( a : Bits<N>, b : UInt<M>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn RORC =
      func("RORC",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesDefault()
          .build();


  /**
   * {@code function rrx ( a : Bits<N>, b : UInt<M>, c : Bool ) -> Bits<N> }
   */
  public static final BuiltIn RRX =
      func("RRX",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class),
              BitsType.class))
          .takesDefault()
          .build();


  ///// BIT COUNTING //////


  /**
   * Counting one bits.
   *
   * <p>{@code function cob( a : Bits<N> ) -> UInt<N> }
   */
  public static final BuiltIn COB =
      func("COB", Type.relation(BitsType.class, UIntType.class))
          .takesDefault()
          .build();


  /**
   * {@code function czb( a : Bits<N> ) -> UInt<N> // counting zero bits }
   */
  public static final BuiltIn CZB =
      func("CZB", Type.relation(BitsType.class, UIntType.class))
          .takesDefault()
          .build();


  /**
   * Counting leading zeros.
   *
   * <p>{@code function clz( a : Bits<N> ) -> UInt<N>  }
   */
  public static final BuiltIn CLZ =
      func("CLZ", Type.relation(BitsType.class, UIntType.class))
          .takesDefault()
          .build();


  /**
   * Counting leading ones.
   *
   * <p>{@code function clo( a : Bits<N> ) -> UInt<N> }
   */
  public static final BuiltIn CLO =
      func("CLO", Type.relation(BitsType.class, UIntType.class))
          .takesDefault()
          .build();


  /**
   * Counting leading sign bits (without sign bit).
   *
   * <p>{@code function cls( a : Bits<N> ) -> UInt<N>}
   */
  public static final BuiltIn CLS =
      func("CLS", Type.relation(BitsType.class, TupleType.class))
          .takesDefault()
          .build();


  ///// FUNCTIONS //////

  /**
   * TODO: describe function
   *
   * <p>{@code function mnemonic() -> String<N>}
   */
  public static final BuiltIn MNEMONIC =
      func("MNEMONIC", Type.relation(StringType.class))
          .takesDefault()
          .build();

  /**
   * Concatenates two strings to a new string.
   *
   * <p>{@code function concatenate(String<N>, String<M>) -> String<X>}
   */
  public static final BuiltIn CONCATENATE_STRINGS =
      func("CONCATENATE",
          Type.relation(StringType.class, StringType.class, StringType.class))
          .takesDefault()
          .build();

  /**
   * Formats the register file index.
   *
   * <p>{@code function register(Bits<N>) -> String<M>}
   */
  public static final BuiltIn REGISTER =
      func("REGISTER",
          Type.relation(BitsType.class, StringType.class))
          .takesDefault()
          .build();

  /**
   * Formats value to binary string.
   *
   * <p>{@code function binary(Bits<N>) -> String<M>}
   */
  public static final BuiltIn BINARY =
      func("BINARY", Type.relation(BitsType.class, StringType.class))
          .takesDefault()
          .build();

  /**
   * Formats value to decimal string.
   *
   * <p>{@code function decimal(Bits<N>) -> String<M>}
   */
  public static final BuiltIn DECIMAL =
      func("DECIMAL",
          Type.relation(BitsType.class, StringType.class))
          .takesDefault()
          .build();

  /**
   * Formats value to hex string.
   *
   * <p>{@code function hex(Bits<N>) -> String<M>}
   */
  public static final BuiltIn HEX =
      func("HEX", Type.relation(BitsType.class, StringType.class))
          .takesDefault()
          .build();

  /**
   * Formats value to octal string.
   *
   * <p>{@code function hex(Bits<N>) -> String<M>}
   */
  public static final BuiltIn OCTAL =
      func("octal", Type.relation(BitsType.class, StringType.class))
          .takesDefault()
          .build();

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


    public boolean takes(List<Type> types) {
      if (argTypeClasses().size() != types.size()) {
        // if the number of arguments is not correct, this can't be true
        return false;
      }

      // we check certain properties that must match for ALL built-ins.
      // basically, if the argument type class is another type class than the
      // parameter's one, and there is no way to trivially cast the argument type
      // to an constructed type of the parameter type, we return false.
      // otherwise true.
      // overrides should further constraint the properties of the given types.
      return Streams.zip(types.stream(), argTypeClasses().stream(),
          (t, tc) -> {
            if (t.getClass() == tc) {
              // if the class is the same, we know that the argument type is correct
              return true;
            }
            if (t instanceof DataType tDataType) {
              // if the concrete type is a data type we try to construct a data type
              // with the same bit width from the built-ins argument type class.
              // if this fails, we know that the type can't be correct.
              var constructedType = Type.constructDataType(tc, tDataType.bitWidth());
              if (constructedType != null) {
                // check that the argument type can be trivially cast to the constructed type
                return tDataType.isTrivialCastTo(tDataType);
              }
              return false;
            }
            // if the concrete type is not a data type, we know that the given type is wrong
            // as there is no way of trivially casting the argument type to the parameter type
            return false;
          }
      ).allMatch(p -> p);
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

  private static BuiltInBuilder func(String name, @Nullable String operator,
                                     RelationType signature) {
    return new BuiltInBuilder(name, operator, signature, BuiltIn.Kind.FUNCTION);
  }

  private static BuiltInBuilder func(String name, RelationType signature) {
    return func(name, (String) null, signature);
  }

  private static class BuiltInBuilder {
    private String name;
    private @Nullable String operator;
    private RelationType signature;
    private BuiltIn.Kind kind;
    @Nullable
    private Function<List<Constant>, Constant> computeFunction;
    @Nullable
    private Function<List<Type>, Boolean> takesFunction;

    BuiltInBuilder(String name, @Nullable String operator, RelationType signature,
                   BuiltIn.Kind kind) {
      this.name = name;
      this.operator = operator;
      this.signature = signature;
      this.kind = kind;
    }

    public <T extends Constant, R extends Constant> BuiltInBuilder computeUnary(
        Function<T, R> computeFunction) {
      this.computeFunction =
          (args) -> computeFunction.apply((T) args.get(0));
      return this;
    }

    public <T extends Constant, R extends Constant> BuiltInBuilder compute(
        Function<List<T>, R> computeFunction) {
      this.computeFunction =
          (args) -> computeFunction.apply(args.stream().map(a -> (T) a).toList());
      return this;
    }

    public <A extends Constant, B extends Constant, R extends Constant> BuiltInBuilder compute(
        BiFunction<A, B, R> computeFunction) {
      this.computeFunction =
          (args) -> computeFunction.apply((A) args.get(0), (B) args.get(1));
      return this;
    }

    @SuppressWarnings("LineLength")
    public <A extends Constant, B extends Constant, C extends Constant, R extends Constant> BuiltInBuilder compute(
        TriFunction<A, B, C, R> computeFunction) {
      this.computeFunction =
          (args) -> computeFunction.apply((A) args.get(0), (B) args.get(1), (C) args.get(2));
      return this;
    }

    public BuiltInBuilder takes(Function<List<Type>, Boolean> takesFunction) {
      this.takesFunction = takesFunction;
      return this;
    }

    public BuiltInBuilder takesData(Function<List<DataType>, Boolean> takesFunction) {
      this.takesFunction = (args) -> args.stream().allMatch(DataType.class::isInstance)
          && takesFunction.apply(args.stream().map(DataType.class::cast).toList());
      return this;
    }

    public BuiltInBuilder takesData(BiFunction<DataType, DataType, Boolean> takesFunction) {
      this.takesFunction = (args) -> args.size() == 2 &&
          takesFunction.apply((DataType) args.get(0), (DataType) args.get(1));
      return this;
    }

    public BuiltInBuilder takesData(
        TriFunction<DataType, DataType, DataType, Boolean> takesFunction) {
      this.takesFunction = (args) -> args.size() == 3 &&
          takesFunction.apply((DataType) args.get(0), (DataType) args.get(1),
              (DataType) args.get(2));
      return this;
    }

    /**
     * This will use the default implementation of {@link BuiltIn#takes(List)}.
     * So it will compare type classes and checks if an argument is trivially cast
     * to a parameter's type class.
     */
    public BuiltInBuilder takesDefault() {
      this.takesFunction = (args) -> true;
      return this;
    }

    public BuiltInBuilder takesAllWithSameBitWidths() {
      takesData((args) -> !args.isEmpty() &&
          args.stream().allMatch(a -> a.bitWidth() == args.get(0).bitWidth()));
      return this;
    }

    public BuiltInBuilder takesFirstTwoWithSameBitWidths() {
      takesData((args) -> args.size() >= 2 &&
          args.get(0).bitWidth() == args.get(1).bitWidth());
      return this;
    }

    BuiltIn build() {

      var takesFunction = this.takesFunction;
      if (takesFunction == null) {
        throw new ViamError(
            "Built-in construction failed: No takes function specified for built-in %s".formatted(
                name));
      }

      return new BuiltIn(name, operator, signature, kind) {
        @Override
        public Optional<Constant> compute(List<Constant> args) {
          if (computeFunction == null) {
            return super.compute(args);
          }

          var argTypes = args.stream()
              .map(Constant::type)
              .toList();
          if (!takes(argTypes)) {
            throw new ViamError("Types of arguments does not match type signature of " + signature)
                .addContext("built-in", this)
                .addContext("constants", List.of(args));
          }
          return Optional.of(computeFunction.apply(args));
        }

        @Override
        public boolean takes(List<Type> types) {
          // always check general case first
          var generalConstraintsValid = super.takes(types);
          if (generalConstraintsValid) {
            // if general case doesn't fail, then test specific constraints
            return takesFunction.apply(types);
          }
          return generalConstraintsValid;
        }
      };
    }
  }
}
