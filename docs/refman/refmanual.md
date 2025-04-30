# VADL Reference Manual {#refmanual}

<!-- SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at> -->
<!-- SPDX-License-Identifier: CC-BY-4.0                          -->

## General Language Features

## Type System

\lbl{langref_type_system}

\ac{VADL}'s type system is inspired by the type system of the hardware construction language Chisel.
The main two primitive data types are `Bool` and `Bits<N>`.
`Bool` represents Boolean typed data (see Section \r{refman_literals}).
A vector is defined by appending the length of the vector in angle brackets to a type.
`Bits<N>` represents an arbitrary bit vector type of length \f$N\f$.

Indexing is used to acces an element of a vector.
The index is enclosed in parentheses.
If `a` is defined as \n
`constant a : Bits<16> = 1023`, \n
then with the index expression `a(3)` the element with index `3` is selected.
The lowest index is zero, the highest index is the length minus one.

Multiple elements of a bit vector can be extracted by slicing.
In \ac{VADL} the most significant bit -- the bit with the highest index -- comes first.
With slicing a range of indices is specified by the higher index connected to the lower index by the range symbol `..`
surrounded by parentheses.
The index can be any expression which can be evaluated to a constant value during parsing of a \ac{VADL} specification.
Multiple indices and ranges can be combined in a single slice specification by separating indices and ranges by a comma.
The following examples show different ways for the specification of slices:

```{.vadl}
a(7..0)            // extracts the lowest 8 bit
a(7,6,5,4,3,2,1,0) // equal to a(7..0), concatenation of single bits
a(11..8,3..0)      // concatenates two 4 bit ranges
a(3..0,11..8)      // reversed order to above example
a(16-1,5+1..0)     // concatenates the highest bit with the 7 lowest bits
```

To express explicitly signed and unsigned arithmetic operations \ac{VADL} provides two sub-types of `Bits<N>` --
`SInt<N>` and `UInt<N>`.
`SInt<N>` represents a signed two's complement integer type of length \f$N\f$.
The length \f$N\f$ includes both the sign-bit and data bits.
`UInt<N>` represents an unsigned integer type of length \f$N\f$.

For all bit-vector based types `Bits`, `SInt` and `UInt` \ac{VADL} will try to infer the bit size from the surrounding
usage.
But for definitions, a concrete bit size has to be specified in order to determine the actual size of, e.g., a register.
In contrast to Chisel the size of the resulting bit vector of an operation is identical to the size of the source
operands.
An exception is multiplication where two versions are available, one with a result with the same size and one with a
double sized result.

An additional `String` type is available which only can be used in an assembly specification and the macro system.

The operator `as` does explicit type casting between different types.
There is no change in the bit vector representation if the size of source and result vector are equal.
The bit vector is truncated if the result type is smaller than the source type.
A truncating cast to `Bool` is defined as a comparison with zero:

`Bits<N> as Bool, N > 1 => Bits<N> != 0` \n
`SInt<N> as Bool, N > 1 => SInt<N> != 0` \n
`UInt<N> as Bool, N > 1 => UInt<N> != 0`

The vector is sign or zero extended if the result type is larger than the source type.

Zero or sign extension is defined by the following explicit type casting rules:

`Bits<N> as Bits<M>, M > N => zero extension` \n
`Bits<N> as UInt<M>, M > N => zero extension` \n
`Bits<N> as SInt<M>, M > N => sign extension`

`UInt<N> as Bits<M>, M > N => zero extension` \n
`UInt<N> as UInt<M>, M > N => zero extension` \n
`UInt<N> as SInt<M>, M > N => zero extension`

`SInt<N> as Bits<M> , M > N => sign extension` \n
`SInt<N> as UInt<M> , M > N => sign extension` \n
`SInt<N> as SInt<M> , M > N => sign extension`

If a `Bool` is casted to a bit vector with a length larger than `1`, `false` is represented as `0` and `true` is
represented as `1` which is equivalent to zero extension.
For `Bool` and bit vectors with the same length the following implicit type casting rules apply:

`Bits<1> <=> Bool`

`SInt<N> <=> Bits<N>` \n
`UInt<N> <=> Bits<N>`

For arithmetic operations and bitwise operations except shift and rotate \ac{VADL} supports the following implicit
type casting rules from `Bits<N>`:

`SInt<N> o SInt<N> -> SInt<N>` \n
`SInt<N> o Bits<N> -> SInt<N>` \n
`Bits<N> o SInt<N> -> SInt<N>`

`UInt<N> o UInt<N> -> UInt<N>` \n
`UInt<N> o Bits<N> -> UInt<N>` \n
`Bits<N> o UInt<N> -> UInt<N>`

### Literals and Type Inference

\lbl{refman_literals}

For the type `Bool` there exist the two boolean literals `true` (value `1` as `Bits<1>`) and `false` (value `0` as `Bits<1>`).

Binary literals start with `0b` and hexadecimal literals start with `0x`.
Binary, decimal and hexadecimal literals represent signed integers with an arbitrary length, they are implemented as a `BigInteger`.
In the evaluation of constant expressions no truncation can happen.
The apostrophe can be used to make the representation more comprehensible (see Listing \r{lst_literals}).

\listing{lst_literals, VADL Binary and Decimal Literals}

~~~{.vadl}
constant binLit = 0b1'0011       // has the value 19
constant hexLit = 0x000f         // has the value 15

constant decLit = 4              // has the value  4
constant decEx  = 4 * 3 + 1      // has the value 13

constant bitEx  = binLit + decEx // has the value 32
~~~

\endlisting


### Tensors

\lbl{refman_tensors}

Tensors are multi-dimensional arrays with a uniform type.
A one dimensional tensor commonly is called vector.
A two dimensional tensor often is referred as matrix.
A three dimensional tensor can be imagined as a cube.
In \ac{VADL} tensors are specified by vectors of vectors with a bit vector for the innermost dimension.
When indexing tensors the index of every dimension has to be enclosed separately in parentheses.
The outermost index is the first one, the innermost index is the last one.
When tuples are used to initialize a tensor, the highest index comes first.
This is different to an initializer in the programming language `C++`,
but fits better to the specification of bit vectors with highest bit first.
It is quite natural if every value is written in a single line.
Listing \r{lst_tensordef} gives some examples for the definition and usage of tensors.
OpenVADL currently supports slicing only for bit vectors (the innermost dimension).
In the future it is planned to allow slicing on the higher dimension levels too.

\listing{lst_tensordef, VADL Tensor Definitions and Usage}

~~~{.vadl}
using Dim_1_a = Bits<16>
using Dim_2_a = Dim_1_a<4>
using Dim_3_a = Dim_2_a<2>
using Dim_3_b = Bits<2><4><16>         // equivalent to Dim_3_a

constant d2 : Dim_2_a = (3, 2, 1, 0)   // specified with highest index first
constant d3 : Dim_3_a = ((7, 6, 5, 4), // d3(1)
                         (3, 2, 1, 0)) // d3(0)

constant a = d2(3)                     // is 3 as Dim_1_a (Bits<16>)
constant b = d2(3)(15)                 // is 0 as Bits<1>
constant c = d3(0)                     // is (3, 2, 1, 0) as Dim_2_a 
constant d = d3(0)(3)                  // is 3 as Dim_1_a (Bits<16>) 
constant e = d3(0)(3)(15)              // is 0 as Bits<1>
constant f = let x = d3(0) as Bits<64> in x(15..0)  // is 0, is d3(0)(0)
constant g = let x = d3(0) as Bits<64> in x(63..48) // is 3, is d3(0)(3)
constant h = let x = d3(1) as Bits<64> in x(63..48) // is 7, is d3(1)(3)

constant i = 0xfedc'da98'7654'3210     // Bits<64> value
//            |63                0|    // bit positions
//            |j(3)|    |    |j(0)|    // tensor elements
constant j = i as Dim_2_a

instruction set architecture test = {
register file  X : Bits<2> -> Dim_1_a  // 4 registers with Bit<16>
alias register Y : Dim_2_a  = X        // also 4 registers with Bit <16>
alias register Z : Bits<64> = X        // a single 64 bit register

format F : Bits<8> = {opcode : Bits<8>}
instruction instr1 : F =
    X(3) := Y(3)                       // are identical registers
instruction instr2 : F =
    X(3) := Z(63..48)                  // are the identical bits
}
~~~

\endlisting

## Expressions and Operator Precedence

\lbl{expr_precedence}

The behavior of instructions is described by expressions consisting of operations on bit vectors.
These operations can be selected either using binary and unary operators or by calling \ac{VADL}'s builtin functions.
To avoid excessive usage of parentheses an operator precedence inspired by `C++` has been defined as shown in the
following table (operators with lower precedence level bind stronger):

| precedence |     symbols      |                                                                                             |
|:----------:|:----------------:|:--------------------------------------------------------------------------------------------|
|     16     |       `.`        | dot, ordered sequence in group expression                                                   |
|     15     |       `,`        | comma, set union for operation, unordered sequence in group\n expression, always in `{` `}` |
|     14     |       `..`       | range in bit fields, range in group expressions                                             |
|     13     |       \|\|       | logical or                                                                                  |
|     12     |       `&&`       | logical and                                                                                 |
|     11     |   `∈ ∉ in !in`   | set operators (if `&` is intersection, `,` is union, `>=` and `<=` are subset)              |
|     10     |        \|        | bitwise or, alternative in group expression or assembly grammar                             |
|     9      |       `^`        | bitwise exclusive or                                                                        |
|     8      |       `&`        | bitwise and, intersection in set expression                                                 |
|     7      |      `= !=`      | equality operators                                                                          |
|     6      |   `< <= > >=`    | relational operators                                                                        |
|     5      | `<< >> <<> <>>`  | shift left, shift right, rotate left, rotate right                                          |
|     4      | `+ - +`\| ` -`\| | addition, subtraction, with saturation                                                      |
|     3      |    `* / % *#`    | multiply, divide, modulo, multiply with double wide result                                  |
|     2      |       `as`       | type cast                                                                                   |
|     1      |     `- ~ !`      | negate, bitwise not, logical not (unary operators)                                          |

In \ac{VADL} additionally to expressions on bit vectors there are expressions in the assembly grammar
which use the `"|"` operator and regular expressions for defining groups for \ac{VLIW} architectures
which use the `"."`, `","` and `"|"` operator.

## Arithmetic Operations

\ac{VADL} provides a set of pre-defined basic mathematical built-in functions.
Many of them can be accessed by using binary or unary operators, all others have to be invoked by a function call
in the built-in name space `VADL`.
Listing \r{math_status_example} shows a let expression which uses binary infix operators (`infixExpr`),
an equivalent second let expression which uses function calls (`callExpr`) and a third let expression
which calls a built-in function with a double result, the result of the operation,
and a bit field structure with the status bits for this operation (`result, status`).

\listing{math_status_example, Arithmetic with Status Flags}

~~~{.vadl}
let infixExpr = X(5) + X(6) * 2 in {}
let callExpr  = VADL::add( X(5), VADL::mul(X(6), 2)) in {}

let result, status = VADL::adds( X(5), X(6) ) in {
  // 'result'          contains the addition result
  // 'status'          contains all status flags
  // 'status.zero'     contains the zero flag as 'Bool' type
  // 'status.carry'    contains the carry flag as 'Bool' type
  // 'status.overflow' contains the overflow flag as 'Bool' type
  // 'status.negative' contains the negative flag as 'Bool' type
}
~~~

\endlisting

All pre-defined built-in functions define the actual semantics of the available \ac{VADL} operations.
Each available unary or infix operator maps to the corresponding built-in function.

The complete list of the supported built-in functions is given in Listings \r{basic_math_arithmetic},
\r{basic_math_logical}, \r{basic_math_comparison}, \r{basic_math_shifting} and \r{basic_math_bit_counting}.

Some built-ins have a carry (e.g. `addc`) and carry with status (e.g. `adds`) version to
represent ternary operations where the carry is part of the mathematical operation.
While the carry flag is well-defined for addition, there are two common ways to use the carry flag
for subtraction operations.

The first uses the bit as a borrow flag, setting it if `a<b` when computing `a-b`, and a borrow must be performed.
If `a>=b`, the bit is cleared. The subtract with borrow (`subb`) built-in function will compute `a-b-C = a-(b+C)`,
while a subtract without borrow (`subsb`) acts as if the borrow bit were clear.
The 8080, 6800, Z80, 8051, x86 and 68k families of instruction set architectures use a borrow bit.

The second uses the identity that `-x = not(x)+1` directly (i.e. without storing the carry bit inverted) and
computes `a-b` as `a+not(b)+1`.
The carry flag is set according to this addition, and subtract with carry (`subc`) computes `a+not(b)+C`,
while subtract without carry  (`subsc`) acts as if the carry bit were set.
The result is that the carry bit is set if `a>=b`, and clear if `a<b`.
The System/360, 6502, MSP430, COP8, ARM and PowerPC instruction set architectures use this convention.
The 6502 is a particularly well-known example because it does not have a subtract without carry operation,
so programmers must ensure that the carry flag is set before every subtract operation where a borrow is not required.

\listing{basic_math_arithmetic, VADL Arithmetic Operations}

~~~{.vadl}
function neg ( a : Bits<N> ) -> Bits<N> // <=> -a

function add ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a + b
function adds( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status )
function addc( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status )

// saturated add requires SInt/UInt variants, as the saturation value depends on the type
function ssatadd ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a +| b
function usatadd ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a +| b
function ssatadds( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status )
function usatadds( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status )
function ssataddc( a : SInt<N>, b : SInt<N>, c : Bool ) -> ( SInt<N>, Status )
function usataddc( a : UInt<N>, b : UInt<N>, c : Bool ) -> ( UInt<N>, Status )

function sub  ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a - c
function subsc( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status )            // carry
function subsb( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status )            // borrow
function subc ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status )  // carry
function subb ( a : Bits<N>, b : Bits<N>, c : Bool ) -> ( Bits<N>, Status )  // borrow

// saturated sub requires SInt/UInt variants, as the saturation value depends on the type
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

// multiplication with double sized result requires SInt/UInt variants
function smull   ( a : SInt<N>, b : SInt<N> ) -> SInt<2*N> // <=> a *# b
function umull   ( a : UInt<N>, b : UInt<N> ) -> UInt<2*N> // <=> a *# b
function sumull  ( a : SInt<N>, b : UInt<N> ) -> SInt<2*N> // <=> a *# b
function smulls  ( a : SInt<N>, b : SInt<N> ) -> ( SInt<2*N>, Status )
function umulls  ( a : UInt<N>, b : UInt<N> ) -> ( UInt<2*N>, Status )
function sumulls ( a : SInt<N>, b : UInt<N> ) -> ( SInt<2*N>, Status )

// division and remainder (modulo) require SInt/UInt variants

function sdiv ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a / b
function udiv ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a / b
function sdivs( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status )
function udivs( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status )

function smod ( a : SInt<N>, b : SInt<N> ) -> SInt<N> // <=> a % b
function umod ( a : UInt<N>, b : UInt<N> ) -> UInt<N> // <=> a % b
function smods( a : SInt<N>, b : SInt<N> ) -> ( SInt<N>, Status )
function umods( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status )
~~~

\endlisting

## Logical Operations

\listing{basic_math_logical, VADL Logical Operations}

~~~{.vadl}
function not ( a : Bits<N> ) -> Bits<N> // <=> ~a, !a if N == 1 or Bool

function and ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a & b, a && b if N == 1 or Bool
function ands( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status )

function xor ( a : Bits<N>, b : Bits<N> ) -> Bits<N> // <=> a ^ b
function xors( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status )

function or ( a : Bits<N>, b : Bits<N> ) -> Bits<N>  // <=> a | b, a || b if N ==1 or Bool
function ors( a : Bits<N>, b : Bits<N> ) -> ( Bits<N>, Status )
~~~

\endlisting

## Comparison Operation

\listing{basic_math_comparison, VADL Arithmetic Comparison Operations}

~~~{.vadl}
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

## Shift and Rotate Operations

Listing \r{basic_math_shifting} lists the primitives for shift and rotate operations.
Shift and rotate operations move the bits of operand `a` left or right by the number of bits specified by operand
`b % N` of type `UInt<M>`.
Shift operations to the left fill the low bit positions with zeros and operand `a` and the result are of type `Bits<N>`.
Shift operations to the right fill the high bit positions with the sign bit for arithmetic shifts (`SInt`) and with
zeros for logical shifts (`UInt`).
`M` has to be smaller or equal to `N`.

For instructions which set the status register (`*s`, `*c`, `rrx`) the carry flag is set to the last bit shifted out.
The carry flag is unchanged if the shift/rotate amount is `0`.

Rotate left (right) provides the operand `a` rotated by a variable number of bits.
The bits that are rotated off the left (right) end are inserted into the vacated bit positions on the right (left).
Rotate right with extend ( `rrx`) moves the bits of a register to the right by one bit.
It copies the carry flag into the highest bit position of the result and sets the carry flag to lowest bit position of
operand `a`.

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
function rrx ( a : Bits<N>, c : Bool ) -> Bits<N>
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


## Assembly Directives

\lbl{table_assembly_directives}


| directive                 | explanation                                                                                                              |
|:--------------------------|:-------------------------------------------------------------------------------------------------------------------------|
| ABORT                     | stops the assembly                                                                                                       |
| ADDRSIG                   |                                                                                                                          |
| ADDRSIG_SYM               |                                                                                                                          |
| ALIGN32_BYTE("align32")   |                                                                                                                          |
| ALIGN32_POW2("align32")   |                                                                                                                          |
| ALIGN_BYTE("align")       | .align [alignment [, fill_value, [, max_skip_count]]]                                                                    |
| ALIGN_POW2("align")       | .align [exponent  [, fill_value, [, max_skip_count]]]                                                                    |
| ALTMACRO                  |                                                                                                                          |
| ASCII                     | '.ascii "a", "b", "c"'; zero or more string literals                                                                     |
| ASCIZ                     | like ASCII, but each string is followed by a zero byte                                                                   |
| BALIGN                    | .balign  [alignment [, fill_value, [, max_skip_count]]]                                                                  |
| BALIGNL                   | .balignl [alignment [, fill_value, [, max_skip_count]]]                                                                  |
| BALIGNW                   | .balignw [alignment [, fill_value, [, max_skip_count]]]                                                                  |
| BUNDLE_ALIGN_MODE         | .bundle_align_mode abs-expr                                                                                              |
| BUNDLE_LOCK               | .bundle_lock; used with .bundle_unlock to control bundle padding                                                         |
| BUNDLE_UNLOCK             | .bundle_unlock; used with .bundle_lock to control bundle padding                                                         |
| BYTE                      | .byte  expr* [, expr]*; zero or more 8-bit  integer expressions                                                          |
| BYTE2("2byte")            | .short expr* [, expr]*; zero or more 16-bit integer expressions                                                          |
| BYTE4("4byte")            | .word  expr* [, expr]*; zero or more 32-bit integer expressions                                                          |
| BYTE8("8byte")            | .quad  expr* [, expr]*; zero or more 64-bit integer expressions                                                          |
| CFI_ADJUST_CFA_OFFSET     | .cfi_adjust_cfa_offset offset; modifies a rule for computing CFA                                                         |
| CFI_B_KEY_FRAME           |                                                                                                                          |
| CFI_DEF_CFA               |                                                                                                                          |
| CFI_DEF_CFA_OFFSET        | .cfi_def_cfa_offset offset; modifies a rule for computing CFA                                                            |
| CFI_DEF_CFA_REGISTER      | .cfi_def_cfa_register register; modifies a rule for computing CFA                                                        |
| CFI_ENDPROC               | .cfi_endproc; is used at the end of a function where it closes its unwind entry previously opened by .cfi_startproc      |
| CFI_ESCAPE                | .cfi_escape expression[, ...];  allows the user to add arbitrary bytes to the unwind info                                |
| CFI_LLVM_DEF_ASPACE_CFA   |                                                                                                                          |
| CFI_LSDA                  | .cfi_lsda encoding [, exp];  defines LSDA and its encoding                                                               |
| CFI_MTE_TAGGED_FRAME      |                                                                                                                          |
| CFI_OFFSET                | .cfi_offset register, offset; previous value of register is saved at offset offset from CFA                              |
| CFI_PERSONALITY           | .cfi_personality encoding [, exp]; defines personality routine and its encoding                                          |
| CFI_REGISTER              | .cfi_register register1, register2; previous value of register1 is saved in register register2                           |
| CFI_REL_OFFSET            | .cfi_rel_offset register, offset; previous value of register is saved at offset offset from the current CFA register     |
| CFI_REMEMBER_STATE        | .cfi_remember_state; pushes the set of rules for every register onto an implicit stack                                   |
| CFI_RESTORE               | .cfi_restore register; rule for register is now the same as it was at the beginning of the function                      |
| CFI_RESTORE_STATE         | .cfi_restore_state; pops the set of rules off the stack and places them in the current row                               |
| CFI_RETURN_COLUMN         | .cfi_return_column register; change return column register                                                               |
| CFI_SAME_VALUE            | .cfi_same_value register; current value of register is the same like in the previous frame                               |
| CFI_SECTIONS              | .cfi_sections section_list; used to specify which sections CFI directives should emit                                    |
| CFI_SIGNAL_FRAME          | .cfi_signal_frame; mark current function as signal trampoline                                                            |
| CFI_STARTPROC             | .cfi_startproc [simple]; used at the beginning of each function that should have an entry in .eh_frame                   |
| CFI_UNDEFINED             |                                                                                                                          |
| CFI_WINDOW_SAVE           | .cfi_window_save; SPARC register window has been saved                                                                   |
| CODE16                    |                                                                                                                          |
| CODE16GCC                 |                                                                                                                          |
| COLD                      |                                                                                                                          |
| COMM                      | .comm symbol , length;  declares a common symbol named symbol                                                            |
| COMMON                    |                                                                                                                          |
| CV_DEF_RANGE              |                                                                                                                          |
| CV_FILE                   |                                                                                                                          |
| CV_FILECHECKSUMS          |                                                                                                                          |
| CV_FILECHECKSUM_OFFSET    |                                                                                                                          |
| CV_FPO_DATA               |                                                                                                                          |
| CV_FUNC_ID                |                                                                                                                          |
| CV_INLINE_LINETABLE       |                                                                                                                          |
| CV_INLINE_SITE_ID         |                                                                                                                          |
| CV_LINETABLE              |                                                                                                                          |
| CV_LOC                    |                                                                                                                          |
| CV_STRING                 |                                                                                                                          |
| CV_STRINGTABLE            |                                                                                                                          |
| DC                        | .dc expressions; evaluate zero or more expressions and assemble results as 16-bit values                                 |
| DCB                       | .dcb number [,fill]; emits number copies of fill, each of 2 bytes                                                        |
| DCB_B("dcb.b")            | .dcb.b number [,fill]; emits number copies of fill, each of 1 byte                                                       |
| DCB_D("dcb.d")            | .dcb.d number [,fill]; emits number copies of fill, each of size of double-precision floating point values               |
| DCB_L("dcb.l")            | .dcb.l number [,fill]; emits number copies of fill, each of 4 byte                                                       |
| DCB_S("dcb.s")            | .dcb.s number [,fill]; emits number copies of fill, each of size of single-precision floating point values               |
| DCB_W("dcb.w")            | .dcb.w number [,fill]; emits number copies of fill, each of 2 bytes                                                      |
| DCB_X("dcb.x")            | .dcb.x number [,fill]; emits number copies of fill, each of size of double-precision floating point values               |
| DC_A_CODEPOINTER4("dc.a") |                                                                                                                          |
| DC_A_CODEPOINTER8("dc.a") |                                                                                                                          |
| DC_B("dc.b")              | .dc.b expressions; evaluate zero or more expressions and assemble results as 8-bit values                                |
| DC_D("dc.d")              | .dc.d expressions; evaluate zero or more expressions and assemble results double precision floating-point values         |
| DC_L("dc.l")              | .dc.b expressions; evaluate zero or more expressions and assemble results as 32-bit values                               |
| DC_S("dc.s")              | .dc.s expressions; evaluate zero or more expressions and assemble results single precision floating-point values         |
| DC_W("dc.w")              | .dc.w expressions; evaluate zero or more expressions and assemble results as 16-bit values                               |
| DC_X("dc.x")              | .dc.x expressions; evaluate zero or more expressions and assemble results as long double precision floating-point values |
| DOUBLE                    | .double flonums; assembles zero or morefloating point numbers                                                            |
| DS                        | .ds number [,fill]; emits number copies of fill, each of 2 bytes                                                         |
| DS_B("ds.b")              | .ds.b number [,fill]; emits number copies of fill, each of 1 byte                                                        |
| DS_D("ds.d")              | .ds.d number [,fill]; emits number copies of fill, each of 8 bytes                                                       |
| DS_L("ds.l")              | .ds.l number [,fill]; emits number copies of fill, each of 4 bytes                                                       |
| DS_P("ds.p")              | .ds.p number [,fill]; emits number copies of fill, each of size of packed-decimal floating-point values                  |
| DS_S("ds.s")              | .ds.s number [,fill]; emits number copies of fill, each of 4 bytes                                                       |
| DS_W("ds.w")              | .ds.w number [,fill]; emits number copies of fill, each of 2 bytes                                                       |
| DS_X("ds.x")              | .ds.x number [,fill]; emits number copies of fill, each of size of long double precision floating-point values           |
| ELSE                      | .else; is part of the support for conditional assembly                                                                   |
| ELSEIF                    | .elseif; is part of the as support for conditional assembly                                                              |
| END                       | .end; marks the end of the assembly file                                                                                 |
| ENDIF                     | .endif; is part of the as support for conditional assembly                                                               |
| ENDM                      |                                                                                                                          |
| ENDMACRO                  |                                                                                                                          |
| ENDR                      |                                                                                                                          |
| EQU                       | .equ symbol, expression; sets value of symbol to an expression                                                           |
| EQUIV                     | .equiv symbol, expression; sets value of symbol to an expression, signal error if symbol already defined                 |
| ERR                       | .err; emit an error                                                                                                      |
| ERROR                     | .error "string"; emits an error with a message                                                                           |
| EXITM                     | .exitm; exit from the current macro definition                                                                           |
| EXTERN                    | .extern;                                                                                                                 |
| FILE                      | .file string; start a new logical file                                                                                   |
| FILL                      | .fill repeat, size, value;                                                                                               |
| FLOAT                     | .float flonums; assembles zero or more flonums, separated by commas                                                      |
| GLOBAL                    | .global symbol; makes the symbol visible to linker                                                                       |
| GLOBL                     | .globl symbol; makes the symbol visible to linker                                                                        |
| IF                        | .if absolute expression; assembles the following section of code if the argument is non-zero                             |
| IFB                       | .ifb text; assembles the following section of code if the operand is blank (empty)                                       |
| IFC                       | .ifc string1,string2; assembles the following section of code if the two strings are the same                            |
| IFDEF                     | .ifdef symbol; assembles the following section of code if the specified symbol has been defined.                         |
| IFEQ                      | .ifeq absolute expression; assembles the following section of code if the argument is zero                               |
| IFEQS                     | .ifeqs string1,string2; assembles the following section of code if the two strings are the same                          |
| IFGE                      | .ifge absolute expression; assembles the following section of code if the argument is greater than or equal to zero      |
| IFGT                      | .ifgt absolute expression; assembles the following section of code if the argument is greater than zero                  |
| IFLE                      | .ifle absolute expression; assembles the following section of code if the argument is less than or equal to zero         |
| IFLT                      | .iflt absolute expression; assembles the following section of code if the argument is less than zero                     |
| IFNB                      | .ifnb text; assembles the following section of code if the operand is non-blank (non-empty)                              |
| IFNC                      | .ifnc string1,string2; assembles the following section of code if the two strings are not the same                       |
| IFNDEF                    | .ifndef symbol;  assembles the following section of code if the specified symbol has not been defined                    |
| IFNE                      | .ifne absolute expression; assembles the following section of code if the argument is not equal to zero                  |
| IFNES                     | .ifnes string1,string2; assembles the following section of code if the two strings are not the same                      |
| IFNOTDEF                  | .ifnotdef symbol; Assembles the following section of code if the specified symbol has not been defined                   |
| INCBIN                    | .incbin "file"[,skip[,count]]; includes file verbatim at the current location                                            |
| INCLUDE                   |                                                                                                                          |
| INT                       | .int expressions; emit zero or more numbers                                                                              |
| IRP                       | .irp symbol,value...; evaluate a sequence of statements assigning different values to symbol                             |
| IRPC                      | .irpc symbol,values...; evaluate a sequence of statements assigning different values to symbol                           |
| LAZY_REFERENCE            |                                                                                                                          |
| LCOMM                     | .lcomm symbol , length; reverse length bytes of a local common denoted by symbol                                         |
| LINE                      | .line line-number; change logical line number                                                                            |
| LOC                       | .loc fileno lineno [column] [options]                                                                                    |
| LONG                      | .long expressions; emit zero or more numbers                                                                             |
| LTO_DISCARD               |                                                                                                                          |
| LTO_SET_CONDITIONAL       |                                                                                                                          |
| MACRO                     | .macro allows to define macros that generate assembly output                                                             |
| MACROS_OFF                |                                                                                                                          |
| MACROS_ON                 |                                                                                                                          |
| MEMTAG                    |                                                                                                                          |
| NOALTMACRO                |                                                                                                                          |
| NO_DEAD_STRIP             |                                                                                                                          |
| OCTA                      | .octa bignums; emit zero or more bignums as 16-byte integers                                                             |
| ORG                       | .org new-lc , fill; avance location counter to new-lc                                                                    |
| P2ALIGN                   | .p2align [abs-expr[, abs-expr[, abs-expr]]]; pad the location counter to a particular storage boundary                   |
| P2ALIGNL                  | .p2alignl [abs-expr[, abs-expr[, abs-expr]]]; pad the location counter to a particular storage boundary                  |
| P2ALIGNW                  | .p2alignw [abs-expr[, abs-expr[, abs-expr]]]; pad the location counter to a particular storage boundary                  |
| PRINT                     | .print "string"; print to stdout during assembly                                                                         |
| PRIVATE_EXTERN            |                                                                                                                          |
| PSEUDO_PROBE              |                                                                                                                          |
| PURGEM                    | .purgem name; undefine the macro name                                                                                    |
| QUAD                      | .quad expressions; emits zero or more 8-byte integers                                                                    |
| REFERENCE                 |                                                                                                                          |
| RELOC                     | .reloc offset, reloc_name[, expression]; generate relocation with given parameters                                       |
| REP                       |                                                                                                                          |
| REPT                      | .rept count; repeat the sequence of lines between the .rept directive and the next .endr directive count times.          |
| SET                       | .set symbol, expression; set value of symbol to an expression                                                            |
| SHORT                     | .short expressions; usually the same as .word                                                                            |
| SINGLE                    | .single flonums; assembly zero or more flonums (same as .float)                                                          |
| SKIP                      | .skip size [,fill]; emits size bytes of value fill                                                                       |
| SLEB128                   | .sleb128 expressions; “signed little endian base 128” used by DWARF symbolic debugging                                   |
| SPACE                     | .space size [,fill]                                                                                                      |
| STABS                     | .stabs string , type , other , desc , value; emit symbols to be used by symbolic debugger                                |
| STRING                    | .string "str" [, "str2"]*; emit strings to the object file                                                               |
| SYMBOL_RESOLVER           |                                                                                                                          |
| ULEB128                   | .uleb128 expressions; “unsigned little endian base 128” used by DWARF symbolic debugging                                 |
| VALUE                     |                                                                                                                          |
| WARNING                   | .warning "string"; emits a warning                                                                                       |
| WEAK_DEFINITION           |                                                                                                                          |
| WEAK_DEF_CAN_BE_HIDDEN    |                                                                                                                          |
| WEAK_REFERENCE            |                                                                                                                          |
| ZERO                      | .zero size; emit `size` amount of 0-valued bytes                                                                         |


## Assembly Grammar Rule Types

| type          | explanation                                                                                                 |
|:--------------|:------------------------------------------------------------------------------------------------------------|
| @constant     | represents an integer value.                                                                                |
| @expression   | represents a complex expression for an immediate operand. The `Expression` default rule is of this type.    |
| @instruction  | represents an entire machine or pseudo instruction. It needs to consist of at least the `mnemonic` operand. |
| @modifier     | represents a modifier defined in the `modifier` mappings of the assembly description.                       |
| @operand      | represents a machine operand, which is used to build an instruction in the parser.                          |
| @operands     | represents a sequence of `@operands`.                                                                       |
| @register     | represents a register of the instruction set architecture corresponding to the assembly description.        |
| @statements   | represents a sequence of `@instruction`, where each instruction is followed by an `EOL`.                    |
| @string       | represents a sequence of characters. Most terminal rules are of this type.                                  |
| @symbol       | represents a reference to a symbol, such as an assembly label.                                              |
| @void         | represents the empty type. For rules like `EOL`.                                                            |


## Assembly Grammar Rule Type Casts

| from                       | to           | explanation                                                               |
|:---------------------------|:-------------|:--------------------------------------------------------------------------|
| @instruction               | @statements  | create a sequence of statements containing a single instruction           |
| @operands                  | @instruction | create an instruction from a sequence of operands                         |
| @operand                   | @instruction | create an instruction from a single operand                               |
| @operand                   | @operands    | create a sequence of operands containing a single operand                 |
| @register                  | @operand     | wrap register in an operand                                               |
| @constant                  | @operand     | wrap constant in an operand                                               |
| @string                    | @operand     | wrap string in an operand                                                 |
| @expression                | @operand     | wrap expression in an operand                                             |
| @symbol                    | @operand     | wrap symbol in an operand                                                 |
| @modifier with @expression | @operand     | create a new expression from a modified expression and wrap in an operand |
| @constant                  | @register    | interpret integer as register index                                       |
| @string                    | @register    | interpret string value as register name                                   |
| @string                    | @modifier    | the cast string needs to be a defined modifier                            |
| @string                    | @symbol      | interpret string value as symbol                                          |
| any                        | @void        | drop all data                                                             |

## Assembly Terminal Rules

\listing{assembly_terminals, Assembly Terminal Rules}
~~~{.vadl}
@string   | AMP            | "&"
@string   | AMPAMP         | "&&"
@string   | AT             | "@"
@string   | BACKSLASH      | "\ "
@string   | CARET          | "^"
@string   | COLON          | ":"
@string   | COMMA          | ","
@string   | DOLLAR         | "$"
@string   | DOT            | "."
@void     | EOL            | "\\r\\n?|\\n"
@string   | EQUAL          | "="
@string   | EQUALEQUAL     | "=="
@string   | EXCLAIM        | "!"
@string   | EXCLAIMEQUAL   | "!="
@string   | GREATER        | ">"
@string   | GREATEREQUAL   | ">="
@string   | GREATERGREATER | ">>"
@string   | HASH           | "#"
@string   | IDENTIFIER     | "[a-zA-Z_.][a-zA-Z0-9_$.@]*"
@constant | INTEGER        | "0b[01]+|0[0-7]+|[1-9][0-9]*|0x[0-9a-fA-F]+"
@string   | LBRAC          | "["
@string   | LCURLY         | "{"
@string   | LESS           | "<"
@string   | LESSEQUAL      | "<="
@string   | LESSGREATER    | "<>"
@string   | LESSLESS       | "<<"
@string   | LPAREN         | "("
@string   | MINUS          | "-"
@string   | MINUSGREATER   | "->"
@string   | PERCENT        | "%"
@string   | PIPE           | "|"
@string   | PIPEPIPE       | "||"
@string   | PLUS           | "+"
@string   | RBRAC          | "]"
@string   | RCURLY         | "}"
@string   | RPAREN         | ")"
@string   | SLASH          | "/"
@string   | STAR           | "*"
@string   | STRING         | "\\\".*\\\""
@string   | TILDE          | "~"
~~~
\endlisting


## Assembly Nonterminal Rules
The following grammar rules are default rules that can be used in other rule definitions.
Default grammar rules can be overwritten by defining a new rule with the exact same name.

- Expression: An expression can be a signed integer, a complex expression (e.g., `2 + 3`) or a symbol reference (e.g., `.foo`).
- Identifier: Identifier is used to allow any string.
- ImmediateOperand: ImmediateOperand is an expression with a cast to @operand.
- Instruction: The instruction default rule is an alternative over all grammar rules with the type @instruction.
- Label: Label is a symbol reference (e.g., `.foo`).
- Natural: Natural is an unsigned integer number.
- Integer: Integer is a signed integer number.
- Register: Register is any of the registers defined in the \ac{ISA} or an register alias of the \ac{ABI}.
- Statement: Statement is the Instruction default rule followed by an End-Of-Line token.

\listing{assembly_nonterminals, Assembly Nonterminal Rules}
~~~{.vadl}
 Expression        | @expression
 Identifier        | @string
 ImmediateOperand  | @operand
 Instruction       | @instruction
 Label             | @symbol
 Natural           | @constant
 Integer           | @constant
 Register          | @register
 Statement         | @instruction
~~~
\endlisting
