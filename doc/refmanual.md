# VADL Reference Manual {#refmanual}

## General Language Features

### Type System
\lbl{langref_type_system}

\ac{VADL}'s type system is inspired by the hardware construction language Chisel.
The main two primitive data types are `Bool` and `Bits<`\f$N\f$`>`.
`Bool` represents a Boolean typed data (see Section \r{refman_boolean_literal}).
`Bits<`\f$N\f$`>` represents an arbitrary bit vector type of length \f$N\f$.

To express explicitly signed and unsigned arithmetic operations \ac{VADL} provides two sub-types of `Bits<`\f$N\f$`>` -- `SInt<`\f$N\f$`>` and `UInt<`\f$N\f$`>`.
`SInt<`\f$N\f$`>` represents a signed two's complement integer type of length \f$N\f$. 
The length \f$N\f$ includes both the sign-bit and data bits.
`UInt`\f$N\f$`>` represents an unsigned integer type of length \f$N\f$.

For all bit-vector based types `Bits`, `SInt`, and `UInt` -- \ac{VADL} will try to infer the bit size of the surrounding usage.
But for definitions, a concrete bit size has to be specified in order to determine the actual size of, e.g., a register.
In contrast to Chisel the size of the resulting bit vector of an operation is identical to the size of the source operands.
An exception is the multiplication where two versions are available, one with a result with the same size and one with a double sized result.

An additional `String` type is available which is used in the assembly specification and the macro system.

The operator `as` does explicit type casting between different types.
There is no change in the bit vector representation, if the size of source and result vector are equal.
The vector is truncated if the result type is smaller than the source type.
The vector is sign or zero extended if the result type is larger than the source type.

Zero or sign extension is defined by the following explicit type casting rules:

\listing{type_extension_rules, VADL Type Extension Rules}
~~~{.vadl}
Bits<M> as Bits<N>, N > M => zero extension
Bits<M> as UInt<N>, N > M => zero extension
Bits<M> as SInt<N>, N > M => sign extension

UInt<M> as Bits<N>, N > M => zero extension
UInt<M> as UInt<N>, N > M => zero extension
UInt<M> as SInt<N>, N > M => zero extension

SInt<M> as Bits<N>, N > M => sign extension
SInt<M> as UInt<N>, N > M => sign extension
SInt<M> as SInt<N>, N > M => sign extension
~~~
\endlisting

\ac{VADL} supports the following implicit type casting rules:

* `Bits<1>` \f$\Longleftrightarrow\f$ `Bool`
* `Bits<`\f$N\f$`>` \f$\Longrightarrow\f$ `SInt<`\f$M\f$`>` \f$| ~ N = M\f$
* `Bits<`\f$N\f$`>` \f$\Longrightarrow\f$ `Bits<`\f$M\f$`>` \f$| ~ N \leq M\f$
* `UInt<`\f$N\f$`>` \f$\Longrightarrow\f$ `Bits<`\f$M\f$`>` \f$| ~ N \leq M\f$
* `SInt<`\f$N\f$`>` \f$\Longrightarrow\f$ `Bits<`\f$M\f$`>` \f$| ~ N \leq M ~ \land ~ N > 1\f$


\listing{basic_math_arithmetic, VADL Arithmetic Operations}
~~~{.vadl}
function neg ( a : Bits<N> ) -> Bits<N> // <=> -a

function add ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a + b
function adds( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status )
function addc( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status )

// satadd requires SInt/UInt differentiation, 
// as the saturation value depends on the type
function ssatadd ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a +| b
function usatadd ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a +| b
function ssatadds( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status )
function usatadds( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status )
function ssataddc( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status )
function usataddc( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status )

function sub  ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a - c
function subsc( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status )
function subsb( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status )
function subc ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status )
function subb ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status )

function ssatsub ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a -| b
function usatsub ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a -| b
function ssatsubs( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status )
function usatsubs( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status )
function ssatsubc( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status )
function usatsubc( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status )
function ssatsubb( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status )
function usatsubb( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status )

function mul ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a * b
function muls( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status )

function smull   ( a : SInt<N>, b : SInt<N> ) -> SInt<2*N> // <=> a *# b
function umull   ( a : UInt<N>, b : UInt<N> ) -> UInt<2*N> // <=> a *# b
function sumull  ( a : SInt<N>, b : UInt<N> ) -> SInt<2*N> // <=> a *# b
function smulls  ( a : SInt<N>, b : SInt<N> ) -> ( SInt<2*N>, Status )
function umulls  ( a : UInt<N>, b : UInt<N> ) -> ( UInt<2*N>, Status )
function sumulls ( a : SInt<N>, b : UInt<N> ) -> ( SInt<2*N>, Status )

function smod ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a % b
function umod ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a % b
function smods( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status )
function umods( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status )

function sdiv ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a / b
function udiv ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a / b
function sdivs( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status )
function udivs( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status )
~~~
\endlisting

## Logical Operations
\listing{basic_math_logicl, VADL Logical Operations}
~~~{.vadl}
function not ( a : Bits<N> ) ->Bits<N> // <=> ~a, !a if N == 1

function and ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a & b, a && b if N == 1
function ands( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status )

function xor ( a : Bits<N>, b : Bits<N> ) -> [ SInt<N> | UInt<N> ] // <=> a ^ b
function xors( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status )

function or ( a : Bits<N>, b : Bits<N> ) -> [ SInt<N> | UInt<N> ] // <=> a | b, a || b if N ==1
function ors( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status )
~~~
\endlisting

## Comparison Operation
\listing{basic_math_comparison, VADL Arithmetic Comparison Operations}
~~~{.vadl}
// TODO: does (2 as SInt<2>) = (2 as UInt<2>) hold?
function equ ( a : Bits<N>, b : Bits<N> ) -> Bool // <=> a = b

function neq ( a : Bits<N>, b : Bits<N> ) -> Bool // <=> a != b

function slth ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a < b
function ulth ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a < b

function sleq ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a <= b
function uleq ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a <= b


function sgth ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a > b
function ugth ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a > b 

function sgeq ( a : SInt<N>, b : SInt<N> ) -> Bool // <=> a >= b 
function ugeq ( a : UInt<N>, b : UInt<N> ) -> Bool // <=> a >= b  
~~~
\endlisting

## Shifting Operations
\listing{basic_math_shifting, VADL Shifting Operations}
~~~{.vadl}
M <= N
function lsl ( a : Bits<N>, b : UInt<M> ) -> Bits<N> // <=> a << b
function lsls( a : Bits<N>, b : UInt<M> ) -> ( Bits<N>, Status )
function lslc( a : Bits<N>, b : UInt<M>, c : Bool ) -> ( Bits<N>, Status )

function asr ( a : SInt<N>, b : UInt<M> ) -> SInt<N> // <=> a >> b
function lsr ( a : UInt<N>, b : UInt<M> ) -> UInt<N> // <=> a >> b
function asrs( a : SInt<N>, b : UInt<M> ) -> ( SInt<N>, Status )
function lsrs( a : UInt<N>, b : UInt<M> ) -> ( UInt<N>, Status )
function asrc( a : SInt<N>, b : UInt<M>, c : Bool ) -> ( SInt<N>, Status )
function lsrc( a : UInt<N>, b : UInt<M>, c : Bool ) -> ( UInt<N>, Status )

function rol ( a : Bits<N>, b : UInt<M> ) ->Bits<N> // <=> a <<> b
function rols( a : Bits<N>, b : UInt<M> ) -> ( Bits<N>, Status )
function rolc( a : Bits<N>, b : UInt<M>, c : Bool ) -> ( Bits<N>, Status )

function ror ( a : Bits<N>, b : UInt<M> ) -> Bits<N> // <=> a <>> b
function rors( a : Bits<N>, b : UInt<M> ) -> ( Bits<N>, Status )
function rorc( a : Bits<N>, b : UInt<M>, c : Bool ) -> ( Bits<N>, Status )
function rrx ( a : Bits<N>, b : UInt<M>, c : Bool ) -> Bits<N>
~~~
\endlisting


## Bit Counting Operations
\listing{basic_math_bit_counting, VADL Bit Counting Operations}
~~~{.vadl}
function cob( a : Bits<N> ) -> UInt<N> // counting one bits
function czb( a : Bits<N> ) -> UInt<N> // counting zero bits
function clz( a : Bits<N> ) -> UInt<N> // counting leading zeros
function clo( a : Bits<N> ) -> UInt<N> // counting leading ones
function cls( a : Bits<N> ) -> UInt<N> // counting leading sign bits (without sign bit)
~~~
\endlisting
