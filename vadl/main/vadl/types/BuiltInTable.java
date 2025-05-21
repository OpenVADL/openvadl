// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.types;

import static org.slf4j.LoggerFactory.getLogger;
import static vadl.types.Type.constructDataType;

import com.google.common.collect.Streams;
import com.google.errorprone.annotations.FormatMethod;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import vadl.utils.functionInterfaces.TriFunction;
import vadl.viam.Constant;
import vadl.viam.ViamError;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

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
      func("VADL::neg", "-", Type.relation(BitsType.class, BitsType.class))
          .computeUnary(Constant.Value::negate)
          .takesDefault()
          .returnsFirstBitWidth(BitsType.class)
          .build();


  /**
   * {@code function add ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a + b }
   */
  public static final BuiltIn ADD =
      func("VADL::add", "+", Type.relation(BitsType.class, BitsType.class, BitsType.class))
          .compute(
              (Constant.Value a, Constant.Value b) -> a.add(b, false).get(0, Constant.Value.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidth(BitsType.class)
          .build();


  /**
   * {@code function adds( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ADDS =
      func("VADL::adds", null, Type.relation(BitsType.class, BitsType.class, TupleType.class))
          .compute((Constant.Value a, Constant.Value b) -> a.add(b, false))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();


  /**
   * {@code function addc( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ADDC =
      func("VADL::addc",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class))
          .compute(
              (Constant.Value a, Constant.Value b, Constant.Value carry) -> a.add(b, carry.bool()))
          .takesFirstTwoWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();


  /**
   * {@code function ssatadd ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a +| b }
   */
  public static final BuiltIn SSATADD =
      func("VADL::ssatadd", "+|", Type.relation(SIntType.class, SIntType.class, SIntType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidth(SIntType.class)
          .build();


  /**
   * {@code function usatadd ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a +| b }
   */
  public static final BuiltIn USATADD =
      func("VADL::usatadd", "+|", Type.relation(UIntType.class, UIntType.class, UIntType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidth(UIntType.class)
          .build();


  /**
   * {@code function ssatadds( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SSATADDS =
      func("VADL::ssatadds", Type.relation(SIntType.class, SIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(SIntType.class)
          .build();


  /**
   * {@code function usatadds( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn USATADDS =
      func("VADL::usatadds", Type.relation(UIntType.class, UIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(UIntType.class)
          .build();


  /**
   * {@code function ssataddc( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SSATADDC =
      func("VADL::ssataddc",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class),
              TupleType.class))
          .takesFirstTwoWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(SIntType.class)
          .build();


  /**
   * {@code function usataddc( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn USATADDC =
      func("VADL::usataddc",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesFirstTwoWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(UIntType.class)
          .build();


  /**
   * {@code function sub  ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a - c }
   *
   * @see Constant.Value#subtract(Constant.Value, Constant.Value.SubMode, boolean)
   */
  public static final BuiltIn SUB =
      func("VADL::sub", "-", Type.relation(BitsType.class, BitsType.class, BitsType.class))
          .compute(
              (Constant.Value a, Constant.Value b) -> a.subtract(b, Constant.Value.SubMode.X86_LIKE,
                  false).firstValue())
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidth(BitsType.class)
          .build();


  /**
   * {@code function subsc( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   *
   * <p>Subtract without carry (subsc) acts as if the carry bit were set.
   *
   * @see Constant.Value#subtract(Constant.Value, Constant.Value.SubMode, boolean)
   */
  public static final BuiltIn SUBSC =
      func("VADL::subsc", null, Type.relation(BitsType.class, BitsType.class, TupleType.class))
          .compute((Constant.Value a, Constant.Value b) -> a.subtract(b,
              Constant.Value.SubMode.ARM_LIKE, true))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();


  /**
   * {@code function subsb( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   *
   * <p>Subtract without borrow (subsb) acts as if the borrow bit were clear.</p>
   *
   * @see Constant.Value#subtract(Constant.Value, Constant.Value.SubMode, boolean)
   */
  public static final BuiltIn SUBSB =
      func("VADL::subsb", null, Type.relation(BitsType.class, BitsType.class, TupleType.class))
          .compute(
              (Constant.Value a, Constant.Value b) -> a.subtract(b, Constant.Value.SubMode.X86_LIKE,
                  false))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();


  /**
   * {@code function subc ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   *
   * @see Constant.Value#subtract(Constant.Value, Constant.Value.SubMode, boolean)
   */
  public static final BuiltIn SUBC =
      func("VADL::subc",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class))
          .compute((Constant.Value a, Constant.Value b, Constant.Value carry) ->
              a.subtract(b, Constant.Value.SubMode.ARM_LIKE, carry.bool())
          )
          .takesFirstTwoWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();


  /**
   * {@code function subb ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status ) }
   *
   * @see Constant.Value#subtract(Constant.Value, Constant.Value.SubMode, boolean)
   */
  public static final BuiltIn SUBB =
      func("VADL::subb",
          Type.relation(List.of(BitsType.class, BitsType.class, BoolType.class), TupleType.class))
          .compute((Constant.Value a, Constant.Value b, Constant.Value carry) ->
              a.subtract(b, Constant.Value.SubMode.X86_LIKE, carry.bool())
          )
          .takesFirstTwoWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();


  /**
   * {@code function ssatsub ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a -| b }
   */
  public static final BuiltIn SSATSUB =
      func("VADL::ssatsub", "-|", Type.relation(SIntType.class, SIntType.class, SIntType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidth(SIntType.class)
          .build();


  /**
   * {@code function usatsub ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a -| b }
   */
  public static final BuiltIn USATSUB =
      func("VADL::usatsub", "-|", Type.relation(UIntType.class, UIntType.class, UIntType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidth(UIntType.class)
          .build();


  /**
   * {@code function ssatsubs( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SSATSUBS =
      func("VADL::ssatsubs", Type.relation(SIntType.class, SIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(SIntType.class)
          .build();


  /**
   * {@code function usatsubs( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn USATSUBS =
      func("VADL::usatsubs", Type.relation(UIntType.class, UIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(UIntType.class)
          .build();


  /**
   * {@code function ssatsubc( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SSATSUBC =
      func("VADL::ssatsubc",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class),
              TupleType.class))
          .takesFirstTwoWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(SIntType.class)
          .build();


  /**
   * {@code function usatsubc( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn USATSUBC =
      func("VADL::usatsubc",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesFirstTwoWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(UIntType.class)
          .build();


  /**
   * {@code function ssatsubb( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SSATSUBB =
      func("VADL::ssatsubb",
          Type.relation(List.of(SIntType.class, SIntType.class, BoolType.class),
              TupleType.class))
          .takesFirstTwoWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(SIntType.class)
          .build();


  /**
   * {@code function usatsubb( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn USATSUBB =
      func("VADL::usatsubb",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesFirstTwoWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(UIntType.class)
          .build();


  /**
   * {@code function mul ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a * b }
   */
  public static final BuiltIn MUL =
      func("VADL::mul", "*", Type.relation(BitsType.class, BitsType.class, BitsType.class))
          .compute((Constant.Value a, Constant.Value b) -> a.multiply(b, false, false))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidth(BitsType.class)
          .build();


  /**
   * {@code function muls( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn MULS =
      func("VADL::muls", null, Type.relation(BitsType.class, BitsType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();


  /**
   * {@code function smull   ( a : SInt<N>, b : SInt<N> ) -> SInt<2*N> // <=> a *# b }
   */
  public static final BuiltIn SMULL =
      func("VADL::smull", "*#",
          Type.relation(SIntType.class, SIntType.class, SIntType.class)).compute(
              (Constant.Value a, Constant.Value b) -> a.multiply(b, true, true))
          .takesAllWithSameBitWidths()
          .returnsFromFirstAsDataType((a) -> Type.signedInt(2 * a.bitWidth()))
          .build();


  /**
   * {@code function umull   ( a : UInt<N>, b : UInt<N> ) -> UInt<2*N> // <=> a *# b }
   */
  public static final BuiltIn UMULL =
      func("VADL::umull", "*#",
          Type.relation(UIntType.class, UIntType.class, UIntType.class)).compute(
              (Constant.Value a, Constant.Value b) -> a.multiply(b, true, false))
          .takesAllWithSameBitWidths()
          .returnsFromFirstAsDataType((a) -> Type.unsignedInt(2 * a.bitWidth()))
          .build();


  /**
   * {@code function sumull  ( a : SInt<N>, b : UInt<N> ) -> SInt<2*N> // <=> a *# b }
   */
  public static final BuiltIn SUMULL =
      func("VADL::sumull", "*#", Type.relation(SIntType.class, UIntType.class, SIntType.class))
          .takesAllWithSameBitWidths()
          .returnsFromFirstAsDataType((a) -> Type.signedInt(2 * a.bitWidth()))
          .build();


  /**
   * {@code function smulls  ( a : SInt<N>, b : SInt<N> ) -> ( SInt<2*N>, Status ) }
   */
  public static final BuiltIn SMULLS =
      func("VADL::smulls", Type.relation(SIntType.class, SIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .returnsFromFirstAsDataType((a) -> Type.tuple(
              Type.signedInt(2 * a.bitWidth()),
              Type.status()
          ))
          .build();


  /**
   * {@code function umulls  ( a : UInt<N>, b : UInt<N> ) -> ( UInt<2*N>, Status ) }
   */
  public static final BuiltIn UMULLS =
      func("VADL::umulls", Type.relation(UIntType.class, UIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .returnsFromFirstAsDataType((a) -> Type.tuple(
              Type.unsignedInt(2 * a.bitWidth()),
              Type.status()
          ))
          .build();


  /**
   * {@code function sumulls ( a : SInt<N>, b : UInt<N> ) -> ( SInt<2*N>, Status ) }
   */
  public static final BuiltIn SUMULLS =
      func("VADL::sumulls", Type.relation(SIntType.class, UIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .returnsFromFirstAsDataType((a) -> Type.tuple(
              Type.signedInt(2 * a.bitWidth()),
              Type.status()
          ))
          .build();


  /**
   * {@code function smod ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a % b }
   */
  public static final BuiltIn SMOD =
      func("VADL::smod", "%", Type.relation(SIntType.class, SIntType.class, SIntType.class))
          .takesAllWithSameBitWidths()
          .compute((Constant.Value a, Constant.Value b) -> a.modulo(b, true))
          .returnsFirstBitWidth(SIntType.class)
          .build();


  /**
   * {@code function umod ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a % b }
   */
  public static final BuiltIn UMOD =
      func("VADL::umod", "%", Type.relation(UIntType.class, UIntType.class, UIntType.class))
          .takesAllWithSameBitWidths()
          .compute((Constant.Value a, Constant.Value b) -> a.modulo(b, false))
          .returnsFirstBitWidth(UIntType.class)
          .build();


  /**
   * {@code function smods( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SMODS =
      func("VADL::smods", Type.relation(SIntType.class, SIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidth(SIntType.class)
          .build();


  /**
   * {@code function umods( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn UMODS =
      func("VADL::umods", Type.relation(UIntType.class, UIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(UIntType.class)
          .build();


  /**
   * {@code function sdiv ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a / b }
   */
  public static final BuiltIn SDIV =
      func("VADL::sdiv", "/", Type.relation(SIntType.class, SIntType.class, SIntType.class))
          .compute((Constant.Value a, Constant.Value b) -> a.divide(b, true))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidth(SIntType.class)
          .build();


  /**
   * {@code function udiv ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a / b }
   */
  public static final BuiltIn UDIV =
      func("VADL::udiv", "/", Type.relation(UIntType.class, UIntType.class, UIntType.class))
          .compute((Constant.Value a, Constant.Value b) -> a.divide(b, false))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidth(UIntType.class)
          .build();


  /**
   * {@code function sdivs( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn SDIVS =
      func("VADL::sdivs", Type.relation(SIntType.class, SIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(SIntType.class)
          .build();


  /**
   * {@code function udivs( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn UDIVS =
      func("VADL::udivs", Type.relation(UIntType.class, UIntType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(UIntType.class)
          .build();


  ///// LOGICAL //////

  /**
   * {@code function not ( a : Bits<N> ) -> Bits<N> // <=> ~a }
   */
  public static final BuiltIn NOT =
      func("VADL::not", "~", Type.relation(BitsType.class, BitsType.class))
          .compute((List<Constant.Value> args) -> args.get(0).not())
          .takesDefault()
          .returnsFirstBitWidth(BitsType.class)
          .build();


  /**
   * {@code function and ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a & b }
   */
  public static final BuiltIn AND =
      func("VADL::and", "&", Type.relation(BitsType.class, BitsType.class, BitsType.class))
          .compute(Constant.Value::and)
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidth(BitsType.class)
          .build();


  /**
   * {@code function ands( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ANDS =
      func("VADL::ands", Type.relation(BitsType.class, BitsType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();


  /**
   * {@code function xor ( a : Bits<N>, b : Bits<N> ) -> [ SInt<N> | UInt<N> ] // <=> a ^ b }
   */
  public static final BuiltIn XOR =
      func("VADL::xor", "^", Type.relation(BitsType.class, BitsType.class, SIntType.class))
          .takesAllWithSameBitWidths()
          .compute(Constant.Value::xor)
          // as it is not known but effectively irrelevant, we use bits
          .returnsFirstBitWidth(BitsType.class)
          .build();


  /**
   * {@code function xors( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn XORS =
      func("VADL::xors", Type.relation(BitsType.class, BitsType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();


  /**
   * {@code function or ( a : Bits<N>, b : Bits<N> ) -> [ SInt<N> | UInt<N> ] // <=> a | b }
   */
  public static final BuiltIn OR =
      func("VADL::or", "|", Type.relation(BitsType.class, BitsType.class, SIntType.class))
          .takesAllWithSameBitWidths()
          .compute(Constant.Value::or)
          // as it is not known and effectively relevant, we return bits
          .returnsFirstBitWidth(BitsType.class)
          .build();


  /**
   * {@code function ors( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ORS =
      func("VADL::ors", Type.relation(BitsType.class, BitsType.class, TupleType.class))
          .takesAllWithSameBitWidths()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();

  ///// COMPARISON //////


  /**
   * {@code function equ ( a : Bits<N>, b : Bits<N> ) -> Bool // <=> a = b }
   */
  public static final BuiltIn EQU =
      func("VADL::equ", "=", Type.relation(BitsType.class, BitsType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .compute(
              (a, b) -> Constant.Value.fromBoolean(a.asVal().equalValue(b)))
          .returns(Type.bool())
          .build();


  /**
   * {@code function neq ( a : Bits<N>, b : Bits<N> ) -> Bool // <=> a != b }
   */
  public static final BuiltIn NEQ =
      func("VADL::neq", "!=", Type.relation(BitsType.class, BitsType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .compute((a, b) -> Constant.Value.fromBoolean(!a.equals(b)))
          .returns(Type.bool())
          .build();


  /**
   * {@code function slth ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a < b }
   */
  public static final BuiltIn SLTH =
      func("VADL::slth", "<", Type.relation(SIntType.class, SIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .compute((Constant.Value a, Constant.Value b) -> a.lth(b, true))
          .returns(Type.bool())
          .build();


  /**
   * {@code function ulth ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a < b }
   */
  public static final BuiltIn ULTH =
      func("VADL::ulth", "<", Type.relation(UIntType.class, UIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .compute((Constant.Value a, Constant.Value b) -> a.lth(b, false))
          .returns(Type.bool())
          .build();


  /**
   * {@code function sleq ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a <= b }
   */
  public static final BuiltIn SLEQ =
      func("VADL::sleq", "<=",
          Type.relation(SIntType.class, SIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .compute((Constant.Value a, Constant.Value b) -> a.leq(b, true))
          .returns(Type.bool())
          .build();


  /**
   * {@code function uleq ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a <= b }
   */
  public static final BuiltIn ULEQ =
      func("VADL::uleq", "<=",
          Type.relation(UIntType.class, UIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .compute((Constant.Value a, Constant.Value b) -> a.leq(b, false))
          .returns(Type.bool())
          .build();


  /**
   * {@code function sgth ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a > b }
   */
  public static final BuiltIn SGTH =
      func("VADL::sgth", ">",
          Type.relation(SIntType.class, SIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .compute((Constant.Value a, Constant.Value b) -> a.gth(b, true))
          .returns(Type.bool())
          .build();


  /**
   * {@code function ugth ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a > b }
   */
  public static final BuiltIn UGTH =
      func("VADL::ugth", ">",
          Type.relation(UIntType.class, UIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .compute((Constant.Value a, Constant.Value b) -> a.gth(b, false))
          .returns(Type.bool())
          .build();


  /**
   * {@code function sgeq ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a >= b }
   */
  public static final BuiltIn SGEQ =
      func("VADL::sgeq", ">=",
          Type.relation(SIntType.class, SIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .compute((Constant.Value a, Constant.Value b) -> a.geq(b, true))
          .returns(Type.bool())
          .build();


  /**
   * {@code function ugeq ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a >= b }
   */
  public static final BuiltIn UGEQ =
      func("VADL::ugeq", ">=",
          Type.relation(UIntType.class, UIntType.class, BoolType.class))
          .takesAllWithSameBitWidths()
          .compute((Constant.Value a, Constant.Value b) -> a.geq(b, false))
          .returns(Type.bool())
          .build();


  ///// SHIFTING //////


  /**
   * {@code function lsl ( a : Bits<N>, b : UInt<M> ) -> Bits<N> // <=> a << b }
   */
  public static final BuiltIn LSL =
      func("VADL::lsl", "<<", Type.relation(BitsType.class, UIntType.class, BitsType.class))
          .compute(Constant.Value::lsl)
          .takesDefault()
          .returnsFirstBitWidth(BitsType.class)
          .build();


  /**
   * {@code function lsls( a : Bits<N>, b : UInt<M> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn LSLS =
      func("VADL::lsls", Type.relation(BitsType.class, UIntType.class, TupleType.class))
          .takesDefault()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();


  /**
   * {@code function lslc( a : Bits<N>, b : UInt<M>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn LSLC =
      func("VADL::lslc",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesDefault()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();


  /**
   * {@code function asr ( a : SInt<N>, b : UInt<M> ) -> SInt<N> // <=> a >> b }
   */
  public static final BuiltIn ASR =
      func("VADL::asr", ">>", Type.relation(SIntType.class, UIntType.class, SIntType.class))
          .takesDefault()
          .returnsFirstBitWidth(SIntType.class)
          .build();


  /**
   * {@code function lsr ( a : UInt<N>, b : UInt<M> ) -> UInt<N> // <=> a >> b }
   */
  public static final BuiltIn LSR =
      func("VADL::lsr", ">>", Type.relation(UIntType.class, UIntType.class, UIntType.class))
          .takesDefault()
          .compute(Constant.Value::lsr)
          .returnsFirstBitWidth(UIntType.class)
          .build();


  /**
   * {@code function asrs( a : SInt<N>, b : UInt<M> ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn ASRS =
      func("VADL::asrs", Type.relation(SIntType.class, UIntType.class, TupleType.class))
          .takesDefault()
          .returnsFirstBitWidthAndStatus(SIntType.class)
          .build();


  /**
   * {@code function lsrs( a : UInt<N>, b : UInt<M> ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn LSRS =
      func("VADL::lsrs", Type.relation(UIntType.class, UIntType.class, TupleType.class))
          .takesDefault()
          .returnsFirstBitWidthAndStatus(UIntType.class)
          .build();


  /**
   * {@code function asrc( a : SInt<N>, b : UInt<M>, c : Bool ) -> ( SInt<N>, Status ) }
   */
  public static final BuiltIn ASRC =
      func("VADL::asrc",
          Type.relation(List.of(SIntType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesDefault()
          .returnsFirstBitWidthAndStatus(SIntType.class)
          .build();


  /**
   * {@code function lsrc( a : UInt<N>, b : UInt<M>, c : Bool ) -> ( UInt<N>, Status ) }
   */
  public static final BuiltIn LSRC =
      func("VADL::lsrc",
          Type.relation(List.of(UIntType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesDefault()
          .returnsFirstBitWidthAndStatus(UIntType.class)
          .build();


  /**
   * {@code function rol ( a : Bits<N>, b : UInt<M> ) ->Bits<N> }
   */
  public static final BuiltIn ROL =
      func("VADL::rol", "<<>", Type.relation(BitsType.class, UIntType.class, BitsType.class))
          .takesDefault()
          .returnsFirstBitWidth(BitsType.class)
          .build();


  /**
   * {@code function rols( a : Bits<N>, b : UInt<M> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ROLS =
      func("VADL::rols", Type.relation(BitsType.class, UIntType.class, TupleType.class))
          .takesDefault()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();


  /**
   * {@code function rolc( a : Bits<N>, b : UInt<M>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn ROLC =
      func("VADL::rolc",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesDefault()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();


  /**
   * {@code function ror ( a : Bits<N>, b : UInt<M> ) -> Bits<N> }
   */
  public static final BuiltIn ROR =
      func("VADL::ror", "<>>", Type.relation(BitsType.class, UIntType.class, BitsType.class))
          .takesDefault()
          .returnsFirstBitWidth(BitsType.class)
          .build();


  /**
   * {@code function rors( a : Bits<N>, b : UInt<M> ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn RORS =
      func("VADL::rors", Type.relation(BitsType.class, UIntType.class, TupleType.class))
          .takesDefault()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();


  /**
   * {@code function rorc( a : Bits<N>, b : UInt<M>, c : Bool ) -> ( Bits<N>, Status ) }
   */
  public static final BuiltIn RORC =
      func("VADL::rorc",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class),
              TupleType.class))
          .takesDefault()
          .returnsFirstBitWidthAndStatus(BitsType.class)
          .build();


  /**
   * {@code function rrx ( a : Bits<N>, b : UInt<M>, c : Bool ) -> Bits<N> }
   */
  public static final BuiltIn RRX =
      func("VADL::rrx",
          Type.relation(List.of(BitsType.class, UIntType.class, BoolType.class),
              BitsType.class))
          .takesDefault()
          .returnsFirstBitWidth(BitsType.class)
          .build();


  ///// BIT COUNTING //////


  /**
   * Counting one bits.
   *
   * <p>{@code function cob( a : Bits<N> ) -> UInt<N> }
   */
  public static final BuiltIn COB =
      func("VADL::cob", Type.relation(BitsType.class, UIntType.class))
          .takesDefault()
          .returnsFirstBitWidth(UIntType.class)
          .build();


  /**
   * {@code function czb( a : Bits<N> ) -> UInt<N> // counting zero bits }
   */
  public static final BuiltIn CZB =
      func("VADL::czb", Type.relation(BitsType.class, UIntType.class))
          .takesDefault()
          .returnsFirstBitWidth(UIntType.class)
          .build();


  /**
   * Counting leading zeros.
   *
   * <p>{@code function clz( a : Bits<N> ) -> UInt<N>  }
   */
  public static final BuiltIn CLZ =
      func("VADL::clz", Type.relation(BitsType.class, UIntType.class))
          .takesDefault()
          .returnsFirstBitWidth(UIntType.class)
          .build();


  /**
   * Counting leading ones.
   *
   * <p>{@code function clo( a : Bits<N> ) -> UInt<N> }
   */
  public static final BuiltIn CLO =
      func("VADL::clo", Type.relation(BitsType.class, UIntType.class))
          .takesDefault()
          .returnsFirstBitWidth(UIntType.class)
          .build();


  /**
   * Counting leading sign bits (without sign bit).
   *
   * <p>{@code function cls( a : Bits<N> ) -> UInt<N>}
   */
  public static final BuiltIn CLS =
      func("VADL::cls", Type.relation(BitsType.class, TupleType.class))
          .takesDefault()
          .returnsFirstBitWidth(UIntType.class)
          .build();

  /**
   * Counting trailing zeros.
   *
   * <p>{@code function ctz( a : Bits<N> ) -> UInt<N>  }
   */
  public static final BuiltIn CTZ =
      func("VADL::ctz", Type.relation(BitsType.class, UIntType.class))
          .takesDefault()
          .returnsFirstBitWidth(UIntType.class)
          .build();


  /**
   * Counting trailing ones.
   *
   * <p>{@code function cto( a : Bits<N> ) -> UInt<N> }
   */
  public static final BuiltIn CTO =
      func("VADL::cto", Type.relation(BitsType.class, UIntType.class))
          .takesDefault()
          .returnsFirstBitWidth(UIntType.class)
          .build();


  ///// FUNCTIONS //////

  /**
   * TODO: describe function
   *
   * <p>{@code function mnemonic() -> String<N>}
   */
  public static final BuiltIn MNEMONIC =
      func("mnemonic", Type.relation(StringType.class))
          .takesDefault()
          .returns(Type.string())
          .noCompute()
          .build();

  /**
   * Concatenates two strings to a new string.
   *
   * <p>{@code function concat(String<N>, String<M>) -> String<X>}
   */
  public static final BuiltIn CONCATENATE_STRINGS =
      func("VADL::concat",
          Type.relation(StringType.class, StringType.class, StringType.class))
          .takesDefault()
          .returns(Type.string())
          .compute(
              (a, b) -> new Constant.Str(((Constant.Str) a).value() + ((Constant.Str) b).value()))
          .build();

  /**
   * Concatenates two bit values to a new bit value with a length of the sum of the input values.
   *
   * <p>{@code function concat(Bits<N>, Bits<M>) -> Bits<N + M>}
   */
  public static final BuiltIn CONCATENATE_BITS =
      func("VADL::concat",
          Type.relation(BitsType.class, BitsType.class, BitsType.class))
          .takesDefault()
          .returnsFromDataTypes(args -> Type.bits(args.stream().mapToInt(DataType::bitWidth).sum()))
          .compute(Constant.Value::concat)
          .build();

  /**
   * Formats the register file index.
   *
   * <p>{@code function register(Bits<N>) -> String<M>}
   */
  public static final BuiltIn REGISTER =
      func("register",
          Type.relation(BitsType.class, StringType.class))
          .takesDefault()
          .returns(Type.string())
          .build();

  /**
   * Formats value to binary string.
   *
   * <p>{@code function binary(Bits<N>) -> String<M>}
   */
  public static final BuiltIn BINARY =
      func("binary", Type.relation(BitsType.class, StringType.class))
          .takesDefault()
          .returns(Type.string())
          .build();

  /**
   * Formats value to decimal string.
   *
   * <p>{@code function decimal(Bits<N>) -> String<M>}
   */
  public static final BuiltIn DECIMAL =
      func("decimal",
          Type.relation(BitsType.class, StringType.class))
          .takesDefault()
          .returns(Type.string())
          .build();

  /**
   * Formats value to hex string.
   *
   * <p>{@code function hex(Bits<N>) -> String<M>}
   */
  public static final BuiltIn HEX =
      func("hex", Type.relation(BitsType.class, StringType.class))
          .takesDefault()
          .returns(Type.string())
          .build();

  /**
   * Formats value to octal string.
   *
   * <p>{@code function hex(Bits<N>) -> String<M>}
   */
  public static final BuiltIn OCTAL =
      func("octal", Type.relation(BitsType.class, StringType.class))
          .takesDefault()
          .returns(Type.string())
          .build();

  /**
   * Checks if the token at lookahead {@code n} in the AsmParser is equal to a string {@code s}.
   *
   * <p>{@code function laideq(n: UInt<N>,s: String) -> Bool}
   */
  public static final BuiltIn LA_ID_EQ =
      func("laideq", null, Type.relation(UIntType.class, StringType.class, BoolType.class))
          .takesDefault()
          .noCompute()
          .returns(Type.bool())
          .build();

  /**
   * Checks if the token at lookahead {@code n} in the AsmParser is any of the strings in {@code s}.
   *
   * <p>{@code function laidin(n: UInt<N>,s: String...) -> Bool}
   */
  public static final BuiltIn LA_ID_IN =
      func("laidin", null,
          Type.relation(List.of(UIntType.class, StringType.class), true, BoolType.class))
          .takesDefault()
          .noCompute()
          .returns(Type.bool())
          .build();


  ///// MICRO ARCHITECTURE //////

  /**
   * Fetch the next instruction.
   *
   * <p>{@code process fetchNext -> FetchResult}
   */
  public static final BuiltIn FETCH_NEXT =
      proc("fetchNext", null,
          Type.relation(FetchResultType.class))
          .takesDefault()
          .noCompute()
          .returns(MicroArchitectureType.fetchResult())
          .build();

  /**
   * Decode instruction.
   *
   * <p>{@code process decode(fr: FetchResult) -> Instruction}
   */
  public static final BuiltIn DECODE =
      proc("decode", null,
          Type.relation(List.of(FetchResultType.class), InstructionType.class))
          .takesDefault()
          .noCompute()
          .returns(MicroArchitectureType.instruction())
          .build();


  // processes mapping instruction behavior: name(instr: Instruction) -> Instruction
  private static BuiltIn instr(String name) {
    return proc(name, null,
        Type.relation(List.of(InstructionType.class), InstructionType.class))
        .takesDefault()
        .noCompute()
        .returns(MicroArchitectureType.instruction())
        .build();
  }

  /**
   * Execute instruction reads.
   *
   * <p>{@code process read(instr: Instruction) -> Instruction}
   */
  public static final BuiltIn INSTRUCTION_READ = instr("read");

  /**
   * Execute instruction reads.
   *
   * <p>{@code process readOrForward(instr: Instruction) -> Instruction}
   */
  public static final BuiltIn INSTRUCTION_READ_OR_FORWARD = instr("readOrForward");

  /**
   * Execute instruction writes.
   *
   * <p>{@code process write(instr: Instruction) -> Instruction}
   */
  public static final BuiltIn INSTRUCTION_WRITE = instr("write");

  /**
   * Execute instruction computations.
   *
   * <p>{@code process compute(instr: Instruction) -> Instruction}
   */
  public static final BuiltIn INSTRUCTION_COMPUTE = instr("compute");

  /**
   * Execute instruction address calculations.
   *
   * <p>{@code process address(instr: Instruction) -> Instruction}
   */
  public static final BuiltIn INSTRUCTION_ADDRESS = instr("address");

  /**
   * Execute instruction results calculations.
   *
   * <p>{@code process results(instr: Instruction) -> Instruction}
   */
  public static final BuiltIn INSTRUCTION_RESULTS = instr("results");

  /**
   * Instruction verify branching decision.
   *
   * <p>{@code process verify(instr: Instruction) -> Instruction}
   */
  public static final BuiltIn INSTRUCTION_VERIFY = instr("verify");


  /// // FIELDS /////

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
      CTZ,
      CTO,

      // FUNCTIONS

      MNEMONIC,
      CONCATENATE_STRINGS,
      REGISTER,

      BINARY,
      DECIMAL,
      HEX,
      OCTAL,

      // ASM PARSER FUNCTIONS

      LA_ID_IN,
      LA_ID_EQ,

      // MICRO ARCHITECTURE
      FETCH_NEXT,
      DECODE,
      INSTRUCTION_READ,
      INSTRUCTION_WRITE,
      INSTRUCTION_COMPUTE
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

  public static Set<BuiltIn> ASM_PARSER_BUILT_INS =
      Collections.newSetFromMap(new IdentityHashMap<>());

  static {
    ASM_PARSER_BUILT_INS.add(LA_ID_EQ);
    ASM_PARSER_BUILT_INS.add(LA_ID_IN);
  }

  public static Set<BuiltIn> MIA_BUILTINS =
      Collections.newSetFromMap(new IdentityHashMap<>());

  static {
    MIA_BUILTINS.add(FETCH_NEXT);
    MIA_BUILTINS.add(DECODE);
    MIA_BUILTINS.add(INSTRUCTION_READ);
    MIA_BUILTINS.add(INSTRUCTION_WRITE);
    MIA_BUILTINS.add(INSTRUCTION_COMPUTE);
    MIA_BUILTINS.add(INSTRUCTION_ADDRESS);
    MIA_BUILTINS.add(INSTRUCTION_RESULTS);
    MIA_BUILTINS.add(INSTRUCTION_READ_OR_FORWARD);
    MIA_BUILTINS.add(INSTRUCTION_VERIFY);
  }

  public static Stream<BuiltIn> builtIns() {
    return BUILT_INS.stream();
  }

  /**
   * The BuiltIn class represents a built-in function or process in VADL.
   * It contains information about the name, operator, type, and kind of the built-in.
   */
  public abstract static class BuiltIn {
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


    /**
     * Checks whether the given concrete types are valid argument types for this BuiltIn.
     * The default implementation will only check if the type classes are equal to the ones
     * in the built-in definition, and if not, if it is possible to produce a type of the
     * parameter type class that can be trivially cast from the argument type.
     *
     * @param argTypes of the concrete arguments
     * @return true if argument types are correct, false otherwise
     */
    public boolean takes(List<Type> argTypes) {

      if (this.signature().hasVarArgs()) {
        if (argTypes.size() < argTypeClasses().size()) {
          return false;
        }

        // check first n-1 args
        var firstN1Args = argTypes.subList(0, argTypeClasses().size() - 1);
        var firstN1ArgTypeClasses = argTypeClasses().subList(0, argTypeClasses().size() - 1);
        if (!argsCompatible(firstN1Args, firstN1ArgTypeClasses)) {
          return false;
        }

        // check varArgs arg
        var varArgsType = argTypeClasses().get(argTypeClasses().size() - 1);
        var varArgs = argTypes.subList(argTypeClasses().size() - 1, argTypes.size());
        var varArgsTypeClasses = Collections.nCopies(varArgs.size(), varArgsType);
        return argsCompatible(varArgs, varArgsTypeClasses);
      }

      if (argTypeClasses().size() != argTypes.size()) {
        // if the number of arguments is not correct, this can't be true
        return false;
      }

      return argsCompatible(argTypes, argTypeClasses());
    }

    private boolean argsCompatible(List<Type> argTypes,
                                   List<? extends Class<? extends Type>> argTypeClasses) {
      // we check certain properties that must match for ALL built-ins.
      // basically, if the argument type class is another type class then the
      // parameter's one, and there is no way to trivially cast the argument type
      // to a constructed type of the parameter type, we return false.
      // otherwise true.
      // overrides should further constraint the properties of the given types.
      return Streams.zip(argTypes.stream(), argTypeClasses.stream(),
          (argType, argTypeClass) -> {
            if (argType.getClass() == argTypeClass) {
              // if the class is the same, we know that the argument type is correct
              return true;
            }
            if (argType instanceof DataType argDataType) {
              // if the concrete type is a data type we try to construct a data type
              // with the same bit width from the built-ins argument type class.
              // if this fails, we know that the type can't be correct.
              var constructedType = constructDataType(argTypeClass, argDataType.bitWidth());
              if (constructedType != null) {
                // check that the argument type can be trivially cast to the constructed type
                return argDataType.isTrivialCastTo(argDataType);
              }
              return false;
            }
            // if the concrete type is not a data type, we know that the given type is wrong
            // as there is no way of trivially casting the argument type to the parameter type
            return false;
          }
      ).allMatch(p -> p);
    }

    /**
     * Returns the result type of the built-in when called with the given argument types.
     * It assumes that the argument types are valid, such as a call to
     * {@link #takes(List)} would return true.
     *
     * @param argTypes concrete types of argument for call.
     * @return the concrete type that is return by this built-in
     */
    public abstract Type returns(List<Type> argTypes);


    public final boolean matches(RelationType type) {
      return this.signature.equals(type);
    }

    @Override
    public String toString() {
      return name + signature;
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

    /**
     * Creates a {@link BuiltInCall} node from this built-in and the given arguments.
     */
    public BuiltInCall call(ExpressionNode... args) {
      return BuiltInCall.of(this, args);
    }

  }

  private static BuiltInBuilder func(String name, @Nullable String operator,
                                     RelationType signature) {
    return new BuiltInBuilder(name, operator, signature, BuiltIn.Kind.FUNCTION);
  }

  private static BuiltInBuilder func(String name, RelationType signature) {
    return func(name, (String) null, signature);
  }

  private static BuiltInBuilder proc(String name, @Nullable String operator,
                                     RelationType signature) {
    return new BuiltInBuilder(name, operator, signature, BuiltIn.Kind.PROCESS);
  }

  private static class BuiltInBuilder {
    private String name;
    private @Nullable String operator;
    private RelationType signature;
    private BuiltIn.Kind kind;
    @Nullable
    private Function<List<Constant>, Optional<Constant>> computeFunction;
    @Nullable
    private Function<List<Type>, Boolean> takesFunction;
    @Nullable
    private Function<List<Type>, Type> returnsFunction;

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
          (args) -> Optional.of(computeFunction.apply((T) args.get(0)));
      return this;
    }

    public <T extends Constant, R extends Constant> BuiltInBuilder compute(
        Function<List<T>, R> computeFunction) {
      this.computeFunction =
          (args) -> Optional.of(computeFunction.apply(args.stream().map(a -> (T) a).toList()));
      return this;
    }

    public <A extends Constant, B extends Constant, R extends Constant> BuiltInBuilder compute(
        BiFunction<A, B, R> computeFunction) {
      this.computeFunction =
          (args) -> Optional.of(computeFunction.apply((A) args.get(0), (B) args.get(1)));
      return this;
    }

    @SuppressWarnings("LineLength")
    public <A extends Constant, B extends Constant, C extends Constant, R extends Constant> BuiltInBuilder compute(
        TriFunction<A, B, C, R> computeFunction) {
      this.computeFunction =
          (args) -> Optional.of(
              computeFunction.apply((A) args.get(0), (B) args.get(1), (C) args.get(2)));
      return this;
    }

    public BuiltInBuilder noCompute() {
      this.computeFunction = (args) -> Optional.empty();
      return this;
    }

    public BuiltInBuilder takesData(Function<List<DataType>, Boolean> takesFunction) {
      this.takesFunction = (args) -> args.stream().allMatch(DataType.class::isInstance)
          && takesFunction.apply(args.stream().map(DataType.class::cast).toList());
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
      takesData((args) -> !args.isEmpty()
          && args.stream().allMatch(a -> a.bitWidth() == args.get(0).bitWidth()));
      return this;
    }

    public BuiltInBuilder takesFirstTwoWithSameBitWidths() {
      takesData((args) -> args.size() >= 2
          && args.get(0).bitWidth() == args.get(1).bitWidth());
      return this;
    }

    public BuiltInBuilder returns(Type returnType) {
      returns((args) -> returnType);
      return this;
    }

    public BuiltInBuilder returns(Function<List<Type>, Type> returnsFunction) {
      this.returnsFunction = returnsFunction;
      return this;
    }

    public <T extends DataType> BuiltInBuilder returnsFirstBitWidth(Class<T> returnTypeClass) {
      returnsFromFirstAsDataType(
          (firstDataType) -> {
            var result = constructDataType(returnTypeClass, firstDataType.bitWidth());
            Objects.requireNonNull(result);
            return result;
          });
      return this;
    }

    public <T extends DataType> BuiltInBuilder returnsFirstBitWidthAndStatus(
        Class<T> returnTypeClass) {
      returnsFromFirstAsDataType((firstDataType) -> {
        var valType = constructDataType(returnTypeClass, firstDataType.bitWidth());
        Objects.requireNonNull(valType);
        return Type.tuple(valType, Type.status());
      });
      return this;
    }


    public BuiltInBuilder returnsFromFirstAsDataType(Function<DataType, Type> returnFunction) {
      returns((args) -> {
        ensure(!args.isEmpty(), "Expected at least one argument, but found none.");
        var a = args.get(0);
        ensure(a instanceof DataType, "Expected a data type, but found %s", a);
        return returnFunction.apply((DataType) a);
      });
      return this;
    }

    public BuiltInBuilder returnsFromDataTypes(Function<List<DataType>, Type> returnFunction) {
      returns((args) -> {
        ensure(!args.isEmpty(), "Expected at least one argument, but found none.");
        var argDataTypes = args.stream().map(DataType.class::cast).toList();
        return returnFunction.apply(argDataTypes);
      });
      return this;
    }


    BuiltIn build() {

      var takesFunction = this.takesFunction;
      ensure(takesFunction != null,
          "Built-in construction failed: No `takes` function specified for built-in %s",
          name);

      var returnsFunction = this.returnsFunction;
      ensure(returnsFunction != null,
          "Built-in construction failed: No `returns` function specified for built-in %s",
          name);

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
          return computeFunction.apply(args)
              .map(result -> result instanceof Constant.Value value
                  ? value.trivialCastTo(returns(argTypes))
                  : result);
        }

        @Override
        public boolean takes(List<Type> argTypes) {
          // always check general case first
          var generalConstraintsValid = super.takes(argTypes);
          if (generalConstraintsValid) {
            // if general case doesn't fail, then test specific constraints
            return takesFunction.apply(argTypes);
          }
          return false;
        }

        @Override
        public Type returns(List<Type> argTypes) {
          return returnsFunction.apply(argTypes);
        }
      };
    }


    @Contract("false, _, _ -> fail")
    @FormatMethod
    private void ensure(boolean condition, String message, Object... args) {
      if (!condition) {
        throw new ViamError(message.formatted(args))
            .addContext("built-in", this.name)
            .addContext("signature", this.signature)
            .addContext("kind", this.kind)
            .shrinkStacktrace(1);
      }
    }
  }
}
