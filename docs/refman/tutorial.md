# VADL Tutorial {#tutorial}

## Getting Started

\lbl{tut_getting_started}

Listing \r{riscv_isa} shows a complete \ac{ISA} specification of all RISC-V instructions with immediate operands and all branch instructions.
It is a good example to show the most important \ac{VADL} \ac{ISA} features.

\listing{riscv_isa, RISC-V ISA specification for instructions with immediate operands and all branch instructions}
~~~{.vadl}
instruction set architecture RV32I = {

  constant Size = 32                     // architecture size is 32 bits

  using Byte    = Bits<  8 >             // 8 bit Byte
  using Inst    = Bits< 32 >             // instruction word type
  using Regs    = Bits<Size>             // register word type 
  using Index   = Bits<  5 >             // 5 bit register index type for 32 registers
  using Addr    = Regs                   // address type is equal to the register type

  [zero : X(0)]                          // register with index 0 always is zero
  register         X : Index -> Regs     // integer register file with 32 registers
  [rvWeakMemoryOrdering]                 // RISC V weak memory ordering
  memory         MEM : Addr  -> Byte     // byte addressed memory
  program counter PC : Addr              // PC points to the start of an instruction

  format Itype  : Inst =                 // immediate instruction format
    { imm       : Bits<12>               // [31..20] 12 bit immediate value
    , rs1       : Index                  // [19..15] source register index
    , funct3    : Bits<3>                // [14..12] 3 bit function code
    , rd        : Index                  // [11..7]  destination register index
    , opcode    : Bits<7>                // [6..0]   7 bit operation code
    , immS      = imm as SInt<Size>      // sign extended immediate value
    }

  format Btype : Inst =                  // branch instruction format
    { imm    [31, 7, 30..25, 11..8]      // 12 bit immediate value
    , rs2    [24..20]                    // 2nd source register index
    , rs1    [19..15]                    // 1st source register index
    , funct3 [14..12]                    // 3 bit function code
    , opcode [6..0]                      // 7 bit operation code
    , immS   = imm as SInt<Size> << 1    // sign extended and shifted immediate value immS
    }

  // macro for immediate instructions with name, operator, function code and operand type
  model ItypeInstr (name : Id, op : BinOp, funct3 : Bin, type: Id) : IsaDefs = {
    instruction $name : Itype =
       X(rd) := (X(rs1) as $type $op immS as $type) as Regs
    encoding $name = {opcode = 0b001'0011, funct3 = $funct3}
    assembly $name = (mnemonic, " ", register(rd), ",", register(rs1), ",", decimal(imm))
    }

  model BtypeInstr (name : Id, relOp : BinOp, funct3 : Bin, lhsTy : Id) : IsaDefs = {
    instruction $name : Btype =          // conditional branch instructions
      if X(rs1) as $lhsTy $relOp X(rs2) then
        PC := PC + immS
    encoding $name = {opcode = 0b110'0011, funct3 = $funct3}
    assembly $name = (mnemonic, " ", register(rs1), ",", register(rs2), ",", decimal(imm))
    }

  $ItypeInstr (ADDI ; +  ; 0b000 ; SInt) // add immediate
  $ItypeInstr (ANDI ; &  ; 0b111 ; SInt) // and immediate
  $ItypeInstr (ORI  ; |  ; 0b110 ; SInt) // or immediate
  $ItypeInstr (XORI ; ^  ; 0b100 ; SInt) // exclusive or immediate
  $ItypeInstr (SLTI ; <  ; 0b010 ; SInt) // set less than immediate
  $ItypeInstr (SLTIU; <  ; 0b011 ; UInt) // set less than immediate unsigned

  $BtypeInstr (BEQ  ; =  ; 0b000 ; Bits) // branch equal
  $BtypeInstr (BNE  ; != ; 0b001 ; Bits) // branch not equal
  $BtypeInstr (BGE  ; >= ; 0b101 ; SInt) // branch greater or equal
  $BtypeInstr (BGEU ; >= ; 0b111 ; UInt) // branch greater or equal unsigned
  $BtypeInstr (BLT  ; <  ; 0b100 ; SInt) // branch less than
  $BtypeInstr (BLTU ; <  ; 0b110 ; UInt) // branch less than unsigned
}
~~~
\endlisting

Line 1 defines an `instruction set architecture` with the name `RV32I`.
The \ac{ISA} section specifies basic architecture elements like registers, program counter and memory model and the
instructions with their behavior, encoding and assembly representatioin.
In line 3 a constant with the decimal value `32` for the register size is defined.

Lines 5 to 9 declare user defined types.
\ac{VADL} supports bit vector types (for details see section \r{langref_type_system}).
The basic type is `Bits`.
There exist two subtypes representing signed (`SInt`) and unsigned (`UInt`) two's complement integers.
The size of the bit vector is specified in angle brackets (`<N>`).

Line 12 demonstrates the definition of a register file.
Here we have a mapping of a 5 bit register index to the register type `Regs`, a bit vector type with a bit width of
`32`.
Annotations can be used to detail a definition and are available for most of the \ac{ISA} elements.
Here the annotation is used to declare that the register `X(0)`is a zero register, a register where a write has no
effect and when read always returns zero (see line 11).

The memory model is defined as a mapping from the `32` address `Addr` to a byte of eight bits.
Additionally to the memory model a memory consistency model can be dedeclared with an annotation.
One of the possible predefined consistency models is the weak memory consistency model of the RISC-V (
`rvWeakMemoryOrdering`)

The declaration of a program counter is required in every \ac{ISA} specification (line 15).
The program counter is implicitely incremented by the size of an instruction if it is not modified in the instruction.
If not changed by an annotation the program counter points to the start of the defined instruction.

A format definition is used to specify bitfields with named and typed member fields.
There are two different variants for format specification.
The first one defines bitfields with a name followed by a colon `":"` and a type (line 17 to 24).
The second one defines bitfields with a name and a list of subranges in square brackets (line 26 to 33).
It is possible to define accsess functions to format fields (line 23).
The infix operator `as` casts the left value to the type on the right side.
The effects of truncation and extension effects are detailed in section \r{langref_type_system}.

Usually, many instruction definitions are quite similar.
\ac{VADL} supports type safe syntactic macro templates to avoid copying and modifying specifications.
A macro definition starts with the keyword `model` followed by the typed arguments and the result type of the macro (
line 36).
There exist syntactic types like `Id` (identifier), `BinOp` (binary operator), `Bin` (binary constant) or `IsaDefs` (
\ac{ISA} definitions).
An instantiation of a macro or the substitution of a macro argument are indicated by the dollar sign.

An `instruction` defines the behavior of an instruction (line 37).
After the equality symbol `"="` the behavior is defined by a single statement or a list of statements in parantheses.
Assignment statements use the symbol `":="` to separate the target on the left hand side from the expression on the
right hand side.
The precedence of all operators is listed in a table in section \r{expr_precedence}.
A conditional statement is shown in line 45.

The `encoding` sets the fields in an instruction word which are constant for the given instruction (line 39).
The `assembly` specifies the assembly language syntax for the instruction with a string expression (line 40).

By packing these three definitions into a macro, an instruction with behavior, encoding and assembly can be specified in
a single line.
This macro is invoked six times for all RISC-V instructions with immediate operands (lines 51 to 56).

## Structure of a VADL Processor Specification

\listing{pdl_overview, Structure of a VADL Processor Specification}
~~~{.vadl}
constant MLen = 64

using BitsM = Bits<MLen>
using SIntM = SInt<MLen>
using UIntM = UInt<MLen>

function lessthan (a: SIntM, b: SIntM) -> Bool = a < b

import rv3264im::RV3264IM with ("ArchSize=Arch64")

instruction set architecture RV64IM extending RV3264IM with ("ArchSize=Arch64") = {}

application binary interface ABI for RV64IM = {}

assembly description Assemble implements RV64IM for ABI = {}

micro architecture FiveStage for RV64IM = {}

processor CPU implements RV64IM with FiveStage for ABI = {}
~~~
\endlisting

Listing \r{pdl_overview} shows the main elements of a \ac{VADL} processor specification.
Usually, a \ac{VADL} processor specification has some common definitions in the beginning, followed by the main sections
which describe the \ac{ISA} or the \ac{MiA}.
On line 1, a constant `MLen` with the value 64 is defined.
Type aliases can be defined with the keyword `using` as shown on lines 3 to 5.
On line 7, a function is defined that compares two values of type `SIntM` and returns the result of the comparison as a
value of type `Bool`.
`import` allows the import of \ac{VADL} specification parts from separate files.
On line 9, a specification named `RV3264IM` is imported from a file called `rv3264im.vadl` by setting the model
`ArchSize` to `Arch64`.
In this example, `RV3264IM` refers to another \ac{ISA} specification.

An `instruction set architecture` specification can extend another \ac{ISA} specification and optionally pass macros (
line 11).
Section \r{tut_isa_definition} contains a detailed description of the \ac{ISA} specification.
Lines 13 to 19 demonstrate the definition of the `application binary interface` (see Section \r{tut_abi_definition}), the
`assembly description` (see Section \r{tut_asm_definition}), the `processor` specification (see Section
\r{tut_prc_definition}).
On line 17 a \ac{MiA} named `FiveStage` is defined for the \ac{ISA} `RV64IM` (see Section \r{tut_mia_definition}).

## Common Definitions

\lbl{common_definitions}

### Constants, Literals and Expressions

\listing{constants_literals, Constants and Literals}
~~~{.vadl}
constant size    = 32                                 // value: 32
constant twice   = size * 2                           // value: 64
constant binsize = 0b10'0000                          // value: 32
constant min1one = 0xffff'ffff'ffff'ffff as SInt<64>  // value: -1 as SInt<64>
constant min1two : SInt<64> = -1                      // value: -1 as SInt<64>
~~~
\endlisting

Constant definitions start with the keyword `constant` followed by the name of the constant, an optional type after the
colon symbol `":"`, the equal symbol `"="` and an expression which has to be evaluated at parse time (see Listing
\r{constants_literals}).
The evaluation of the expression is done with a signed integer type with unlimited size (internally the parser uses a
Java `BigInteger` type for the evaluation).
Therefore, the constant `twice` as expected has the value `64`.
The constants `min1one` and `min1two` are of the fixed size type `SInt<64>`.
They cannot be used in expressions with unlimited size any more, the expression evaluaton is done on type `SInt<64>` and
the operands must have the type `SInt<64>`.

Literals can also be specified as binary or hexadecimal numbers.
A single quote symbol can be inserted into numbers to make them more readable.
The constant `binsize` represents the same value `32` as the constant `size`.
Equally to the constant `min1two` the constant `min1one` has the value minus one (`-1`) as the very large hexadecimal
constant with a positive integer value is casted to a signed integer of the same size resulting in the negative value.

\listing{constants_expressions, Constant Expressions}
~~~{.vadl}
constant size    = 32                                 // value: 32
constant addEx1  = size + 32                          // value: 64
constant addEx2  = VADL::add(size, 32)                // value: 64
constant letEx   = let size = 16 in size + 16         // value: 32
constant ifEx    = if letEx = 32 then 5 else 6        // value: 5
constant matchEx = match letEx with {32 => 5, _ => 6} // value: 5
constant width   = match true with                    // value: 5
                     { size = 32 => 5
                     , size > 32 => 6,
                     _           => 4}
~~~
\endlisting

Constant expressions can be quite complex, they can contain function calls, `let`, `if` and `match` expressions.
The definition of `addEx2` in Listing \r{constants_expressions} shows the call of the function `add` from the `VADL`
builtin namespace.
This is equivalent to the usage of the `"+"` operator in the definition of `addEx1`.
A `let` expression defines the binding of an expression to a name.
The name then can be used in the expression after the keyword `in`.
In the constant definition of `letEx` the value `16` is bound to the name `size` which is used in the addition
`size + 16` after `in`.
VADL has nested scoping of name spaces.
A `let` expression starts a new scope.
Therefore, the definition of `size` in the `let` expression hides the definition of the constant `size` in line 1 of
Listing \r{constants_expressions}.
An `if` expression can be used in a constant definition if the condition can be evaluated at parse time.
An `if` expression always needs an `else` part.
As `letEx` has the value `32` the value of `ifEx` is `5`.
A `match` expression can be used to specify a multi way selection.
The expression after the keyword `match` is checked for equality with a list of expressions after the keyword `with`
included in braces and separated by a comma symbol `","`.
The expressions in the list are evaluated sequentially, the expression which matches first is selected and the result of
the evaluated expression after the arrow symbol `"=>"` gives the result of the whole `match` expression.
A `match` expression must contain a catch all expression (denoted by the underscore symbol `"_"`) as last entry.
In `if` and `match` expressions the types of the different alternatives must be identical.

### Enumerations

\listing{enumerations, Enumerations (RISC-V control and status register indices)}
~~~{.vadl}
enumeration CsrDef : Bits<12> =   // defined control and status register indices
  { ustatus                       //  0 0x000  User mode restricted view of mstatus
  , uie      =   4                //  4 0x004  User mode Interrupt Enable
  , utvec                         //  5 0x005  User mode Trap VECtor base address
  , uscratch =  64                // 64 0x040  User mode SCRATCH
  , uepc                          // 65 0x041  User mode Exception Program Counter
  , ucause   =  CsrDef::uepc + 1  // 66 0x042  User mode exception CAUSE
  , utval                         // 67 0x043  User mode Trap VALue
  }
~~~
\endlisting

Enumerations are used to assign names to expressions in an own name space.
An enumeration is defined by the keyword `enumeration` followed by the name of the enumeration, an optional type after
the colon symbol `":"` and an equality symbol `"="`.
Then follows a list of names enclosed in braces and separated by a comma symbol `","`.
The first name has the value `0`, every further name has the value of its predecessor incremented by one.
Optionally a constant expression can be assigned to the name after the equality symbol `"="`.
Line 7 of Listing \r{enumerations} shows the use of an enumeration element with the added name space in front separated
by `"::"` to the name.

### Type Alias Definitions (using)

\listing{using, Type Alias Definitions (using)}
~~~{.vadl}
using Bits32    = Bits<32>       // a bit vector 32 bit wide
using Vector4   = Bits32<4>      // a 4 element vector of bit vectors 32 bit wide
using Matrix2   = Vector4<2>     // a 2 element times 4 element matrix of bit vectors 32 bit wide
using Matrix2_4 = Bits<2><4><32> // a type equivalent to Matrix2
using SInt32    = SInt<32>       // a 32 bit two's complement signed integer
using UInt32    = UInt<32>       // a 32 bit unsigned integer
~~~
\endlisting

The type system is explained in detail in the reference manual (see Section \r{langref_type_system}).
In VADL it is possible to declare bit vectors of arbitrary length.
The basic types are Bits, SInt and UInt which can be used to form vectors.
Type aliases are defined by the keyword `using` followed by the alias name of the type, the equality symbol `"="` and the type literal.
The type literal is comprised of the name of a basic type, a type alias or a format optionally followed by a number of vector sizes in angle brackets.
Listing \r{using} shows some type declarations and their meaning in the comments.

### Functions

\listing{functions, Functions (RISC-V control and status register indices)}
~~~{.vadl}
function size -> SInt<32> = 32

enumeration CsrDef  : Bits<12> = // defined control and status register indices
  { ustatus  =   0               // 0x000  User mode restricted view of mstatus
  , uie      =   4               // 0x004  User mode Interrupt Enable
  // ...              
  , uip      =  68               // 0x044  User mode Interrupt Pending
  }

enumeration CsrImpl : Bits<12> = // implemented control and status register indices
  { ustatus                      // 0x000  User mode restricted view of mstatus
  , uie                          // 0x004  User mode Interrupt Enable
  // ...              
  , uip                          // 0x044  User mode Interrupt Pending
  }

function CsrDefToImpl (csr : Bits<12>) -> Bits<12> = // map defined index to implemented index
  match csr with
    { CsrDef::ustatus  => CsrImpl::ustatus        // 0x000  User mode restricted view of mstatus
    , CsrDef::uie      => CsrImpl::uie            // 0x004  User mode Interrupt Enable
    // ...
    , _                => CsrImpl::uip            // 0x044  User mode Interrupt Pending
    }
~~~
\endlisting

Functions in VADL are pure if they do not read registers or memory.
Functions cannot write registers or memory.
As long as functions do not read registers which have an effect when read, they do not have side effects.
As VADL specifications have to be translated to specifications in a hardware description language or to patterns for the instruction selector of a compiler, neither recursive calls nor higher order functions are allowed.
A function is defined by the keyword `function` followed by the function's name, optionally a parameter list in parentheses, the arrow symbol `"->"`, the return type of the function, the equality symbol `"="` and an expression.

In line 1 of Listing \r{functions} shows the definition of the parameter less function `size` which always will return the value `32`.
In line 17 a function with one argument of type `Bits<12>` is defined which maps two different enumerations to each other.

### Formats

\listing{formats, Formats (RISC-V I-Type and B-Type)}
~~~{.vadl}
using Index = UInt<5>
using Inst  = Bits<32>

format Itype  : Inst =                 // immediate instruction format
  { imm       : Bits<12>               // [31..20] 12 bit immediate value
  , rs1       : Index                  // [19..15] source register index
  , funct3    : Bits<3>                // [14..12] 3 bit function code
  , rd        : Index                  // [11..7]  destination register index
  , opcode    : Bits<7>                // [6..0]   7 bit operation code
  , immS      = imm as SInt<32>        // sign extended immediate value
  }

format Btype : Inst =                  // branch instruction format
  { imm    [31, 7, 30..25, 11..8]      // 12 bit immediate value
  , rs2    [24..20] : Index            // 2nd source register index
  , rs1    [19..15] : Index            // 1st source register index
  , funct3 [14..12]                    // 3 bit function code
  , opcode [6..0]                      // 7 bit operation code
  , immS   = imm as SInt<32> << 1      // sign extended and shifted immediate value immS
  }
~~~
\endlisting

A `format` definition names bit fields of a bit vector and is used to describe instruction formats or system registers.
It starts with the keyword `format` followed by the name of the format, the colon symbol `":"`, a type literal, the equal symbol `"="` and a list of format fields enclosed in braces and separated by the comma symbol `","`.
There exist two variants to define a bit field.

The first one, demonstrated with the definition of the format `Itype` in Listing \r{formats}, defines a bit field with its name followed by the colon symbol `":"` and a type literal.
Examples are the fields `rs1` and `funct3` of `Itype`.
Format definitions start with the most significant bits.
Therefore, the field `imm` occupies the bits from position `31` to `20`.

The second one uses a bit slice notation (see the format `Btype` in Listing \r{formats}).
A slice is defined as a concatenation of single bits and bit ranges in any order.
A bit range starts with index of the highest bit of the range, then follows the range symbol `".."` and it ends with the index of the lowest bit of the range.
Additionally it is possible to add a type literal to a slice separated by the colon symbol `":"`.

Bit fields are not allowed to overlap.
Every bit inside a format has to be covered by a field definition.
It is possible to use nested format definitions.

It is possible to define access functions to bit fields.
They are defined by the name of the access function followed by the equality symbol `"="` and an expression which can use any field name within the format definition.
Every format has its own name space.


## Macro System

\lbl{tut_macro_system}

### Syntax Types

\ac{VADL} exhibits a syntactical macro system.
The advantage of a syntactical macro system compared to a lexical macro system is the type safety.
There exists a set of syntax types which cover syntactical elements like an expression or an identifier.
The syntax types are designed to have a one-to-one relation to parser rules.
This already provides a partially ordered subtype relation.
The following table lists all core syntax types with a description and examples:

| Type    | Description                          |                 Examples                  |
|:--------|:-------------------------------------|:-----------------------------------------:|
| Ex      | Generic VADL Expression              | `X(rs1) + X(rs2) * 2                    ` |
| Lit     | Generic VADL Literal                 | `1, "ADDI"                              ` |
| Val     | Generic VADL Value Literal           | `true, 1, 0b001, 0x00ff                 ` |
| Bool    | Boolean Literal                      | `true, false                            ` |
| Int     | Integer Literal                      | `1, 2, 3                                ` |
| Bin     | Binary or Hexadecimal Literal        | `0b0111, 0xff                           ` |
| Str     | String Literal                       | `"ADDI"                                 ` |
| CallEx  | Arbitrary Call Expression            | `MEM<2>(rs1),PC.next,abs(X(rs1)),Z(0)(1)` |
| SymEx   | Symbol Expression                    | `rs1, MEM<2>, VADL::add                 ` |
| Id      | Identifier                           | `rs1, ADDI, X                           ` |
| BinOp   | Binary Operator                      | `+, -, *, &&, +`\|`, <>>, !in           ` |
| UnOp    | Unary Operator                       | `-, ~, !                                ` |
| Stat    | Generic VADL Statement               | `X(rd) := X(rs)                         ` |
| Stats   | List of VADL Statements              | `X(rd) := X(rs) ...                     ` |
| Defs    | List of common VADL Definitions      | `constant b = 8 * 4 ...                 ` |
| IsaDefs | List of VADL ISA Definition          | `instruction ORI : Itype = { ... } ...  ` |
| Encs    | Element(s) of an Encoding Definition | `opcode = 0b110'0011, none, ...         ` |

Call expressions represent function or method calls, memory accesses or indexed registers accesses with slicing and
field accesses.
The left hand side expression of an assignment statement also is a call expression.
Additional examples are `X(rs1)(15..0)`, `IntQueue.consume(@BranchIntBase)`, `VADL::add(X(5), X(6) * 2)` and
`a(11..8,3..0)`.
A symbol expression consists of an identifier path optionally followed by a vector specification (
`<VectorSizeExpression>`).
`Stats`, `Defs`, `IsaDefs` and `Encs` require at least one element of the specified type.

Figure \r{syntax_type_hierarchy} displays the subtype relation between the presented core types.
The macro type system provides an implicit up-casting of the value types.
For example, if a model expects a value of type `Val`, any subtype, i.e. `Bool`, `Int` or `Bin` will be accepted as
argument.

\figure{ht!}
\dot
graph example {
node  [shape=none];

top     [ label="┳"       ];
isadefs [ label="IsaDefs" ];
defs    [ label="Defs"    ];
stats   [ label="Stats"   ];
stat    [ label="Stat"    ];
encs    [ label="Encs"    ];
ex      [ label="Ex"      ];
lit     [ label="Lit"     ];
str     [ label="Str"     ];
val     [ label="Val"     ];
bool    [ label="Bool"    ];
int     [ label="Int"     ];
bin     [ label="Bin"     ];
callex  [ label="CallEx"  ];
symex   [ label="SymEx"   ];
id      [ label="Id"      ];
binop   [ label="BinOp"   ];
unop    [ label="UnOp"    ];

top -- isadefs ;
top -- stats ;
top -- encs ;
top -- ex ;
top -- binop ;
top -- unop ;
isadefs -- defs ;
stats -- stat ;
ex -- lit ;
lit -- str ;
lit -- val ;
val -- bool ;
val -- int ;
val -- bin ;
ex -- callex ;
callex -- symex ;
symex -- id ;
}
\enddot
\endfigure{syntax_type_hierarchy, Syntax Types Hierarchy in the OpenVADL macro system}

### Macro Definition (model)

A macro is defined through the keyword `model` followed by the name of the macro, a list of typed arguments in
parentheses separated by the comma symbol `","`, the type of the macro after a colon symbol `":"` and after the equal
symbol `"="` the body of the macro enclosed in braces.
The usage of the model arguments inside the model body is indicated by the dollar symbol `"$"`.
When a model is invoked, the model arguments in the body are substituted by the values passed in the arguments.
Similar to arguments the invocation of a model is indicated by the dollar symbol `"$"`.
The arguments in a model invocation are separated by the semicolon symbol `";"`.
The result of the model invocation in line 8 of Listing \r{macro_model_definition} is shown in Listing
\r{macro_model_invocation}.

\listing{macro_model_definition, Model Definition and Invocation}
~~~{.vadl}
  model ItypeInstr (name : Id, op : BinOp, funct3 : Bin, type: Id) : IsaDefs = {
    instruction $name : Itype =
       X(rd) := (X(rs1) as $type $op immS as $type) as Regs
    encoding $name = {opcode = 0b001'0011, funct3 = $funct3}
    assembly $name = (mnemonic, " ", register(rd), ",", register(rs1), ",", decimal(imm))
    }

  $ItypeInstr (ADDI ; +  ; 0b000 ; SInt) // add immediate
~~~
\endlisting

\listing{macro_model_invocation, Result of Model Invocation}
~~~{.vadl}
    instruction ADDI : Itype =
       X(rd) := (X(rs1) as SInt + immS as SInt) as Regs
    encoding ADDI = {opcode = 0b001'0011, funct3 = 0b000}
    assembly ADDI = (mnemonic, " ", register(rd), ",", register(rs1), ",", decimal(imm))
~~~
\endlisting

### Conditional Macro (match)

\lbl{macro_match}

VADL provides an explicitly typed `match`-macro to support the conditional application of macros.
It will conditionally insert the match result into the syntax tree.
It can be used inside a `model` definition as well as in any location in a specification.
A match macro is started by the keyword `match` followed by the colon symbol `":"` and the syntax type of the macro.
Enclosed by parentheses is a list of `match` elements separated by a semicolon `";"`.
A `match` element contains a condition followed by the result of the macro after the double arrow symbol `"=>"`.
For the conditions only comparisons for equality (`"="`) or inequality (`"!="`) between two syntax elements are allowed.
For every `match`-macro a default case has to be provided at the last position, indicated by the underline symbol `"_"`.
When used outside of a model definition only macro invocations can be used in the comparison.
In the example in Listing \r{match_macro}, a user can switch between a 32 and 64 bit address width by setting the
appropriate model `Arch` to the identifier `Arch64`.

\listing{match_macro, Matching on a model}
~~~{.vadl}
model Arch () : Id = {Arch64}
constant AddrWidth = match : Int ($Arch() = Arch64 => 64; _ => 32)
using Addr = Bits<AddrWidth>
~~~
\endlisting

Listing \r{divide_by_null} shows a `model` that optionally wraps an operation into a zero check.
It demonstrates the usage of multiple conditions separated by the comma symbol `","` for the same `match`-result.
In this example multiple conditions are applied to two operators (`"/"` and `"%"`).
The `match`-macro is used inside a model definition and uses the model parameters in the conditions.

\listing{divide_by_null, Divide-by-null safeguard}
~~~{.vadl}
model SafeOp(left: Id, op: BinOp, right: Id): Ex = {
  match : Ex
  ( $op = /, $op = % 
      => if $right = 0 then 0 else $left $op $right
  ; _ => $left $op $right
  )
}
~~~
\endlisting

### Syntax Type Composition (record)

In real world processor specifications the number of model arguments can become quite large.
Model types can be grouped together in a `record` to reduce the number of arguments.
Listing \r{record_definition} shows a `record` definition used for type composition.
In this particular case the record definition composes an `Id` and `BinOp` type to the new type `BinInstRec`.
The body of a record definition consists of a parameter list providing typed fields.
Listing \r{record_application} shows how the record is initialized and the fields `name` and `op` are accessed.
Passing a record type argument can be either done by reference or by creating a syntax tuple.
A syntax tuple is specified the same way a model argument list is provided, i.e. syntax elements are separated by `";"`
and enclosed inside parentheses.
Accessing the passed elements is done using the record's name followed by a `"."` and the desired field.
Accesses of sub-records can be arbitrary chained together.
Furthermore, it is important to note that records are treated as type tuples.
Their field names do not affect the type and are only used to access the internal elements.

\listing{record_definition, Record Example}
~~~{.vadl}
record BinInstRec ( name: Id, op: BinOp )
~~~
\endlisting

\listing{record_application, Record Application}
~~~{.vadl}
model InstModel (info: BinInstRec) : IsaDefs = {
  instruction $info.name : F =
    X(rd) := X(rs1) $info.op X(rs2)
}

$InstModel( ( SUB ; - ) )
$InstModel( ( ADD ; + ) )
~~~
\endlisting

### Lexical Macro Functions (ExtendId, IdToStr)

While most of the needs are covered by syntactical macros, string and identifier manipulation is best done using lexical
macros.
A lexical macro acts on the abstraction level of token streams in contrast to an already parsed AST.
Two use-cases are supported using special syntax type converting functions.
Firstly, templates generating instruction behavior and assembly often need the instruction name once in form of an
identifier (`Id`) and again in form of a string (`Str`).
This use case is covered by the `IdToStr` function (will be renamed to `AsStr`).
This function takes an `Id` typed syntax element and converts it to a `Str` typed syntax element.
Secondly, the `ExtendId` function allows safe identifier manipulation (will be renamed to `AsId`).
This function takes an arbitrary number of `Id` or `Str` typed syntax elements, converts `Id` typed elements to `Str`,
concatenates them and returns a single `Id` typed syntax element.
Listing \r{lexical_macros} shows a small example of both functions with their typed result as comment.
It is important to note that the context of identifiers generated by lexical macros is strictly separated from the
context of the syntactical macros.
Therefore, it is not possible to define or refer to a model name or parameter using a generated identifier.

\listing{lexical_macros, Lexical Macro Examples}
~~~{.vadl}
ExtendId( "", I, "Am", An, "Identifier" ) // --> IAmAnIdentifier : Id
IdToStr( IAmAString )                     // --> "IAmAString"    : Str
~~~
\endlisting

### Higher Order Macros (model-type)

Higher order macros are macros which generate macros or which take macros as arguments.
In the macro expansion system of OpenVADL, model instances are expanded immediately at the site they are declared. This
allows the usage of models that produce models.

\listing{model_producing_model, A model-producing model}
~~~{.vadl}
model BinExFactory(binExName: Id, op: BinOp): IsaDefs = {
  model $binExName(left: Ex, right: Ex): Ex = { 
    $left $op $right
  }
}
$BinExFactory(Addition ; +)
instruction ADD : RType = X(rd) := $Addition(X(rs1) ; X(rs2))
~~~
\endlisting

Listing \r{model_producing_model} shows the model `BinExFactory` which in turn produces a model.
Because the `$BinExFactory` instance is evaluated immediately after it is parsed, the produced model `Addition` is known
to the parser and can be used in the definition of the `ADD` instruction.

#### Macros as Macro arguments

When using a macro as an argument of a macro, it is necessary to specify the signature of the passed macro in the
argument type declaration (e.g. `(Ex, Ex) -> Ex` in Listing \r{higher_order_model_definition}).
As an alternative with better readability the signature can be declared in a separate type definition with the keyword
`model-type` followed by the signature after the equal symbol `=`.

The model `BinExStat` takes a macro of type `BinExType` as an argument and returns a statement.
When the model `BinExStat` is invoked with the model `AddExp` as an argument, an assignment statement with an addition
on the right hand side is generated.

\listing{higher_order_model_definition, Higher-Order Macro Passing a Macro as Argument}
~~~{.vadl}
model-type BinExType = (Ex, Ex) -> Ex

model BinExStat (binEx : BinExType) : Stat = {
    X(rd) := $binEx(X(rs1) ; X(rs2))
  }

model AddExp (rhs: Ex, lhs : Ex) : Ex = {
  $rhs + $lhs
  }

$BinExStat(AddExp)
~~~
\endlisting

#### Type variance in model-type parameters

If a macro is passed as an argument to a model and assuming that the type for this argument is declared by a
`model-type`, then OpenVADL allows the model parameters of the passed macro to be supertypes of the `model-type`
parameters and the result type to be a subtype of the `model-type` result.
Listing \r{model_type_parameters} shows a reference to `model Constants` being used as an `IsaDefsFactory`.
The reference is of a valid type because the result type `Defs` is a subtype of `IsaDefs` and the type `Ex` of parameter
`size` is a supertype of `Id` (see Listing \r{syntax_type_hierarchy}).

\listing{model_type_parameters, Valid types in model references}
~~~{.vadl}
instruction set architecture ISA = {
  constant wordSize = 32

  model-type IsaDefsFactory = (Id) -> IsaDefs

  model Constants(size: Ex): Defs = {
    constant full = $size
    constant half = $size / 2
  }

  model BitDefs(factory: IsaDefsFactory, size: Id): IsaDefs = {
    $factory($size)
  }

  $BitDefs(Constants ; wordSize)
}
~~~
\endlisting

#### Design Patterns for Using Higher-Order Macros

The ARM architecture AArch32 has a register file called `R` consisting of 16 registers which are 32 bits wide (see
Listing \r{higher_order_macro}).
Conditions are specified by boolean expressions on flags of the status register `APSR`, e.g. the zero flag `Z`.
Every instruction can be executed conditionally.
There are 15 different conditions which are described by an enumeration in the specification and encoded by the `cc`
field in an instruction word which is 32 bits wide.
Arithmetic/logic instructions, which have an immediate value as second source operand, share a common instruction
encoding specified in the `ArLoImm` instruction format.

\listing{higher_order_macro, AArch32 -- Instruction Specification applying Higher Order Macros}
~~~{.vadl}
instruction set architecture AArch32 = {

using Word = Bits<32>

register file R: Bits<4> -> Word
format   Status: Bits<1> = {Z : Bits<1>}
register   APSR: Status

enumeration cond: Bits<4> =
  { EQ  // equal           Z == 1
  , NE  // not equal       Z == 0
  //...
  , AL  // always
  }

format ArLoImm: Word =  // arithmetic/logic immediate format
  { cc    [31..28]      // condition
  , op    [27..21]      // opcode
  , flags [20]          // set status register
  , rn    [19..16]      // source register
  , rd    [15..12]      // destination register
  , imm12 [11..0]       // 12 bit immediate
  }

record Instr (id: Id, ass: Str, op: BinOp, opcode: Bin)
record Cond  (str: Str, code: Id, ex: Ex)

model ALImmCondInstr (cond: Cond, instr: Instr) : IsaDefs = {
  instruction ExtendId ($instr.id, $cond.str) : ArLoImm =
    if ($cond.ex) then
      R(rd) := R(rn) $instr.op imm12 as Word
  encoding ExtendId ($instr.id, $cond.str) =
    {cc = cond::$cond.code, op = $instr.opcode, flags = 0}
  assembly ExtendId ($instr.id, $cond.str) =
    ($instr.ass, $cond.str, ' ', register(rd), ',', register(rn), ',', decimal(imm12))
  }

model-type CondInstrModel = (Cond, Instr) -> IsaDefs

model CondInstr (instrModelId: CondInstrModel, instr: Instr) : IsaDefs = {
  $instrModelId (( "eq" ; EQ ;  APSR.Z = 0b1 ) ; $instr)
  $instrModelId (( "ne" ; NE ;  APSR.Z = 0b0 ) ; $instr)
  //...
  }

$CondInstr(ALImmCondInstr ; ( ADD ; "add" ; + ; 0b000'0100 ))
$CondInstr(ALImmCondInstr ; ( SUB ; "sub" ; - ; 0b000'0010 ))
$CondInstr(ALImmCondInstr ; ( AND ; "and" ; & ; 0b000'0000 ))
$CondInstr(ALImmCondInstr ; ( ORR ; "orr" ; | ; 0b000'1100 ))
}
~~~
\endlisting

As in the AArch32 architecture every instruction can be executed conditionally, a basic instruction exists in 15
variants for 15 different conditions.
This problem can be solved smartly by an extension macro design pattern using higher-order macros as demonstrated in
Listing \r{higher_order_macro}.

To reduce the number of macro arguments record types are defined for an instruction and a condition.
The `Inst` record type definition groups the four arguments describing an instruction together.
The `Cond` record type definition consists of a string representing the extension of the assembly name, the identifier
of the enumeration of the condition encoding and a boolean expression for condition evaluation.

Now 15 different instructions with a unique identifier have to be created.
This can be handled with the lexical macro function `ExtendId` by appending the extension string of the condition to the
identifier.

The final problem is that there is a set of models which describe different kinds of conditional instructions and all
these models should be called 15 times for the 15 different conditions.
This can be solved by the higher-order model `CondInstr`, which takes an instruction model (e.g. `ALImmCondInstr`) as
first argument.
The instruction model is then called 15 times with an argument list which has been extended by the conditions.
In the above example the 4 macro calls expand to 60 different instructions.
The AArch32 architecture has instructions with a lot of additional variants like setting the status register, shifted
operands or complex addressing modes.
This leads to a specification with multiple higher-order macro arguments.

### Macro Usage for Configuration

VADL provides the possibility of passing configuration information to the macro system using the command line.
Currently, this mechanism is kept very simple and is restricted to elements of type `Id`.
To prepare a configurable macro variable a default model of type `Id` has to be defined.
Listing \r{macro_configuration} shows such a variable of name `Size`, with the default setting `Arch32`.
Without any passed configurations the instantiation of `Size` results in the identifier `Arch32`.
If VADL receives the command line option `-m` or `--model` followed by the string `"Size=Arch64"`, the value of `Arch`
is overridden.
If `Arch` is instantiated given the previous command line option, it would result in `Arch64`.
In combination with conditional expansion, see Section \r{macro_match} and Listing \r{match_macro}, this simple
mechanism already provides powerful configuration capabilities.

\listing{macro_configuration, Macro Configuration Variable}
~~~{.vadl}
model Size() : Id = { Arch32 }
~~~
\endlisting

Similarly to model passing in the command line it is possible to pass models as an argument to import declarations as
demonstrated in Listing \r{macro_import}.

\listing{macro_import, Import with Macro Argument}
~~~{.vadl}
import rv3264im::RV3264I with ("Size=Arch64")
~~~
\endlisting

## Instruction Set Architecture Definition

\lbl{tut_isa_definition}

\listing{lst_isa_definition, Instruction Set Architecture Definition with some common Definitions}
~~~{.vadl}
instruction set architecture RV32base = {}

instruction set architecture RV32I extending RV32base = {

  constant Size = 32                     // architecture size is 32 bits

  using Inst    = Bits< 32 >             // instruction word type
  using Regs    = Bits<Size>             // register word type 
  using Index   = Bits<  5 >             // 5 bit register index type for 32 registers
  using Addr    = Regs                   // address type is equal to the register type

  format Itype  : Inst =                 // immediate instruction format
    { imm       : Bits<12>               // [31..20] 12 bit immediate value
    , rs1       : Index                  // [19..15] source register index
    , funct3    : Bits<3>                // [14..12] 3 bit function code
    , rd        : Index                  // [11..7]  destination register index
    , opcode    : Bits<7>                // [6..0]   7 bit operation code
    , immS      = imm as SInt<Size>      // sign extended immediate value
    }
}
~~~
\endlisting

An instruction set architecture definition is the main part of a processor specification.
It starts with the three keywords `instruction set architecture` followed by the name of the \ac{ISA}, the equality symbol `"="` and the definition of the instruction set enclosed in braces (see line 1 of Listing \r{lst_isa_definition}).
Optionally it is possible to extend an existing \ac{ISA} with the keyword `extending` followed by the name of the \ac{ISA} to extend (shown in line 3).
Currently it is only possible to extend one \ac{ISA}.
Discussions are ongoing how to support extending multiple \acp{ISA}.

At the beginning of an \ac{ISA} section usually there is a set of common definitions (`constant`, `enumeration`, `using`, `function` and `format`).
These are followed by the definition of \ac{ISA} elements like registers or instructions, which are usually generated by macros.


### Program Counter Declaration

Declaring a program counter (\ac{PC}) is required to define branch instructions or relative addressing (see a declaration in line 7 of Listing \r{lst_program_counter}).
If an instruction does not explicitly modify the \ac{PC}, it is implicitly incremented by the instruction size in each execution cycle.

\listing{lst_program_counter, Program Counter Definition and Use}
~~~{.vadl}
instruction set architecture RV32I = {

  using Addr = Bits<32>      // 32 bit address space

  [next next]                // PC points to the end of the following instruction
  [next]                     // PC points to the end of the current instruction
  program counter PC : Addr  // PC points to the start of the current instruction (default)

  instruction BranchAndLink : BType = {
    let retaddr = PC.next in
      PC    := PC.current + offset
      X(rd) := retaddr
  }
}
~~~
\endlisting

In most architectures, the \ac{PC} points to the start of the current instruction when used to compute a relative branch address.
Therefore, this also is the default behavior in a VADL processor specification when no annotation is added to the \ac{PC} definition.
This behavior can be changed by adding the annotation `[next]`, which lets the \ac{PC} point to the end of the current instruction when the \ac{PC} is read.
When the \ac{PC} is written, it always points to the begin of an instruction.
The ARM AArch32 architecture has the peculiar behavior that the \ac{PC} points to the end of the following instruction when used to compute the branch target address.
This behavior can be specified by the annotation `[next next]`.
It is required that the following instruction has the same size as the current instruction.

If an instruction does not explicitly modify the \ac{PC}, it is implicitly incremented by the instruction size in each execution cycle.

The read value of the \ac{PC} as defined by the annotation can be overruled by using one of the builtin member methods for the program counter: `current`, `next`, and `nextnext` (see lines 10 and 11 of Listing \r{lst_program_counter}).
Independent of any \ac{PC} annotation the method `current` always returns the start of the current instruction, the method `next` always returns the end of the current instruction, and the method `nextnext` the end of the following instruction.

### Register Declaration

Listing \r{register_declaration} demonstrates two ways to declare a register file or a multidimensional register.
In line 6 a register file named `S` is declared by a relation which maps a five bit sized `Index` to a bit vector of type `Word` using the relation symbol `"->"`.
In line 13 the register `Y` with the same layout as `S` is declared as a 32 element vector of type `Word`.
The first way of declaration allows only register files where the number of registers is a power of two, the second way allows an arbitrary number of registers.
Both ways allow the declaration of multidimensional registers. 

\listing{register_declaration, Register Declaration and Register Alias}
~~~{.vadl}
instruction set architecture ISA = {
  using Index       = Bits<5>             // register file index for 32 registers
  using Word        = Bits<64>            // word size
  using Addr        = Word                // address is word size

  register        S : Index -> Word       // general purpose register file, S31 SP
  [const : X(31) = 0]                     // X31 is zero register ZR
  [zero  : X(31)]                         // X31 is zero register ZR
  alias register  X = S                   // general purpose register file, X31 ZR
  alias register SP : Addr = S(31)        // stack pointer
  alias register LR : Addr = S(30)        // link register contains return address
  alias register ZR : Word = X(31)        // zero register
  register        Y : Word<32>            // alternative specification instead of arrow syntax
  alias register  Z : Bits<32><2><16> = Y // Y is interpreted as 32 registers with two 16 bit parts
}
~~~
\endlisting

In \ac{RISC} architectures it is common to use a certain register as a zero register, a register that ignores values assigned to it and when read, always returns zero.
In \ac{VADL} such registers are described by an annotation enclosed in square brackets.
The more generic version in line 7 allows to bind any constant with a certain register using a `const` annotation and specifying the constant after the equality symbol `"="`.
The specific version in line 8 only allows the constant `0` with the `zero` annotation.

It is possible to declare an alias of a register.
The alias name follows the two keywords `alias` and `register` followed by an optional type literal after the colon symbol `":"` and the register which is aliased after the equality symbol `"="`.
The alias can be to single register, a certain register of a register file or a complete register file.
The only requirement is that both registers have the same number of bits.
It is allowed that the alias register has other annotations than the aliased register as demonstrated with the registers `X` and `S`.
The alias register inherits all attributes from the aliased register.
It is possible to make the \ac{PC} an alias of a register using the keywords `alias program counter`.

Listing \r{partial_register_access} shows the declaration of a status register with some bit fields.
It is possible that these bit fields can be accessed directly (partial read and write) or the register can be accessed only as a whole (full read and write).
Partial read and write is the default behavior.
Then the fields can be directly accessed, e.g. `flags.N`.
When the register can only be accessed as a whole, then indexing or slicing is necessary to extract fields and concatenation is necessary to write the register.
The behavior of the register is controlled by annotations.
Additionally, it can be specified, that a register read or write has a side effect.

\listing{partial_register_access, Partial Register Access}
~~~{.vadl}
instruction set architecture ISA = {

  format StatusFlags: Bits<32> = // flag register format
    { N   : Bits< 1>             // negative
    , Z   : Bits< 1>             // zero
    , C   : Bits< 1>             // carry
    , V   : Bits< 1>             // overflow
    , Res : Bits<28>             // reserved
    }

  [full write]
  [full read]
  [partial write]                // default behavior
  [partial read]                 // default behavior
  [side effect write]
  [side effect read]
  [side effect]
  register flags : StatusFlags   // status register
}
~~~
\endlisting


### Memory Declaration

The characteristics of different memories are declared with the keyword `memory` followed by the name of the memory, the colon symbol `":"`, and a relation from the address type to the memory cell type.
The memory relation is specified by the type literal for the address space followed by the relation operator `"->"` and the type literal for a memory cell (see the declaration of a memory named `Mem` in Listing \r{memory_declaration}).
The memory declaration only describes the mapping of an address space to a memory cell, it does not specify the pyhiscal memory available in a processor.

\listing{memory_declaration, Memory Declaration}
~~~{.vadl}
instruction set architecture ISA = {

  using Byte = Bits<8>
  using Addr = Bits<64>

  [ordering : sequentialConsistency] // memory consistency model is sequential consistency
  [ordering : totalStoreOrder]       // memory consistency model is total store order
  [ordering : rvWeakMemoryOrdering]  // memory consistency model is RISC-V weak memory ordering
  [translate VMEM]                   // address translation with the process called VMEM
  [raise ExceptionName : Condition]  // when Condition is met then raise exception ExceptionName
  [bigEndian]                        // big endian memory access
  [littleEndian : Condition]         // if Condition met little endian else big endian access
  [instruction]                      // instruction memory only
  [data]                             // data memory only 
  memory Mem : Addr -> Byte          // byte addressed memory with 64 bit address space
}
~~~
\endlisting

The memory characteristics can be detailed with different annotations.
If no annotation is defined, the memory serves both as data and instruction memory, the memory access is carried out in little endian mode, there is no address translation and the memory consistency model is sequential consistency.
With the annotation `[data]` a memory is only used for data.
With the annotation `[instruction]` a memory is only used for instructions.
With the annotation `[bigEndian]` a memory is only accessed in big endian mode.
If a processor supports bi-endianess, the endianess is selected by the condition evaluated to true, e.g. dependent on a system register.
If the condition is evaluated to false, the opposite endianess is selected.
Exceptions like alignment errors could be specified in every memory accessing instruction directly.
But this violates the principle of separation of concerns.
With the `raise` annotation the throwing of an exception is declared together with the memory.
The `translate` annotation connects the specified address translation process with a memory.
There exist different memory consistency models which are specified with the `ordering` annotation. 


### Instruction Definition

Listing \r{instruction_definition} presents an instruction definition in line 11.
An instruction definition starts with the keyword `instruction` followed by the unique name of the instruction, a type literal (usually the name of a format specification) after the colon symbol `":"` and a statement after the equality symbol `"="` that defines the behavior of the instruction.
Every instruction definition needs a corresponding encoding and assembly definition.
All field and access function names of the instruction's format are visible inside the instruction and inside encoding and assembly definitions.

\listing{instruction_definition, Instruction Definition with Let\, Block and Assignment Statement}
~~~{.vadl}
instruction set architecture ISA = {
  using Index = Bits<5>          // 5 bit register index
  using BWord = Bits<64>         // 64 bit word
  using SWord = SInt<64>         // 64 bit signed integer word

  register status : Bits<4>      // 4 bit status register: negative, zero, carry, overflow
  register X  : Index -> BWord   // 32 registers which are 64 bit wide
  memory MEM  : BWord -> Bits<8> // byte addressed memory in a 64 bit address space

  format MemT : Bits<32> =       // signed offset load / store format
    { opc     : Bits<10>         // opcode
    , off12   : SInt<12>         // signed offset
    , rn      : Index            // base register
    , rt      : Index            // source/target register
    , off     = off12 as SWord   // sign extended 64 bit offset
    }

  [operation : memop]            // belongs to operation set memop
  [require : rn != rt]           // base and target register must be different
  instruction LDUP : MemT =      // 64 bit load instruction with base register update
    let addr = X(rn) + off in {  // access address is base register plus offset
      X(rt) := MEM<8>(addr)      // load 8 byte sized vector from address addr
      X(rn) := addr              // write back of the updated base register
    }

  [operation : aluop, addop]     // belongs to the two operation sets aluop and addop
  instruction ADDIS : MemT =     // 64 bit add immediate instruction setting status register
    let res, fl = VADL::adds(X(rn), off) in {
      X(rt) := res
      status := (fl.negative,fl.zero,fl.carry,fl.overflow)
    }
}
~~~
\endlisting

There are restrictions on the execution order of the statements.
The statements have a sequential semantics, but the OpenVADL compiler must be able to reorder the operations to comply with the restrictions.
All register and memory reads are done in parallel at the beginning of the instruction's execution cycle.
All register and memory writes are done in parallel after all reads at the end of the execution cycle.
It is forbidden that a certain register or memory cell is written twice.
There is no order on the execution of writes and the result would be undefined if the same register is written twice.

Therefore, there exist annotations which specify restrictions on used resources.
With the `require` annotation it is possible to specify constraints which will be checked by OpenVADL's generators and the decoder.
In the example in Listing \r{instruction_definition} a constraint is specified which requires that the indices of the base and the target register are different.
This constraint is additionally checked by the decoder, after the decoding necessary to determine the correct instruction is completed.
There are no restrictions on the used relational operators for the `require` annotation.

Instructions can be grouped into multiple sets used for specifying characteristics of `VLIW` instructions or \ac{MiA} elements.
A set of instructions is named `operation` and can be defined by an annotation as shown in Listing \r{instruction_definition} or in an `operation` definition (see Section \r{tut_operation_definition})

#### Let Statement and Status Flags

A `let` statement is used to define the instruction `LDUP` in Listing \r{instruction_definition}.
An identifier follows the keyword `let` at the start of the statement. 
The `let` statement binds the expression after the equality symbol `"="` with the identifier which enables the use of the result of the expression in the statement after the keyword `in`.
This identifier is only visible in the scope defined by the statement after `in`.
It is common that the statement after `in` is a block statement which is also the case in the current example.

Some \ac{VADL} builtin functions return both the result of the computation and a status of the computation.
The status consists of the four subfields `.negative`, `.zero`, `.carry` and `.overflow`.
Such functions only can be called with a special `let` statement which allows two names separated by the comma symbol `","` as demonstrated in the definition of the instruction `ADDIS` in Listing \r{instruction_definition}.
The \ac{VADL} builtin functions have a separated name space which is designated with the path specifyer `VADL::`.
The first name in the special let denotes the result of the computation, the second name denotes the status of the computation.
In the `ADDIS` instruction the status elements are selected by their subfield names, concatenated to a bit string of the type `Bits<4>` and assigned to the register named `status` which also is of type `Bits<4>`.


#### Block Statement

A block statement groups multiple statements together by enclosing them in braces giving a single statement.
An empty block statement is a valid statement.


#### Assignment Statement

The instruction definitions `LDUP` and `ADDIS` in Listing \r{instruction_definition} show some assignment statements which allow the assignment of a value to a register or to memory.
On the left hand side of the assignment operator `":="` a call expression restricted to a register or memory access is expected, on the right hand side any expression is allowed.
A register file access usually is indexed.
A memory access is always indexed and sometimes is a vector access denoted by the vector size in angle brackets.
On the left hand side the register or memory is always written, on the right hand side it is always read.


#### If Statement


#### Match Statement


#### Forall Statement and Expression


#### Raise Statement and Exception Definition

\ac{VADL} has special notations to mark exceptional behavior.
Technically, these notations are not necessary, as every exceptional behavior can be described with the base \ac{ISA} language constructs.
However, neither a human reader nor the compiler generator can distinguish normal behavior from exceptional behavior.
Therefore, it is required that exceptional behavior is marked by the keyword `raise` as shown in Listing \r{raise_exception} at line 32.
The code after the keyword `raise` can be any (block) statement or the call of an exception defined elsewhere.
After the execution of the exception code the whole instruction is exited, no other statements are executed anymore.

\listing{raise_exception, Exception Handling (simplified MIPS)}
~~~{.vadl}
instruction set architecture MIPSIV = {

  using IWord        = Bits<32>        // 32 bit instruction word
  using RWord        = Bits<64>        // 64 bit register word
  using Address      = RWord           // 64 bit register word
  using Index        = Bits<5>         // register index for 32 registers

  [next]                               // PC points to the next following instruction
  program counter PC : Address         // program pointer
  register       EPC : Address         // saved exception program counter

  [zero : GPR(0)]                      // zero register
  register       GPR : Index -> RWord  // general purpose registers

  format R_Type      : IWord =         // register 3 operand instruction word
    { opcode : Bits<6>                 // operation code
    , rs     : Index                   // 1st source register
    , rt     : Index                   // 2nd source register
    , rd     : Index                   // destination register
    , shamt  : Bits<5>                 // unsigned shift amount
    , funct  : Bits<6>                 // function code
    }

  exception Overflow = {               // overflow exception
    EPC := PC - 4                      // save exception raising PC
    PC  := 0xFFFF'FFFF'8000'0180       // set PC to the exception handler address
    }

  instruction add : R_Type = {         // add with overflow
    let result, status = VADL::adds(GPR(rs), GPR(rt)) in {
      if status.overflow then
        raise Overflow                 // raise exits the instruction after Overflow is executed
      GPR(rd) := result
      }
    }
  }
~~~
\endlisting

Exception raising code is often quite similar.
Exceptions can be specified similarly to functions to enable code reuse.
In contrast to functions, exceptions do not return an expression but have side effects caused by assignment statements (see lines 24 to 27).
Nevertheless, it must be guaranteed that reads to a register or memory location precede all writes.

To specify exceptional behavior like overflow, the basic \ac{VADL} built-in functions exist in a version which returns a status like the occurence of an overflow during the computation.
These built-in functions are used to specify instructions that handle operations with overflow as demonstrated in the example in Listing \r{raise_exception}.


#### Lock Statement


### Encoding Definition


### Assembly Definition


### Pseudo Instructions


### Operation Definition

\lbl{tut_operation_definition}


### Group Definition


### Process Definition


## Application Binary Interface Definition

\lbl{tut_abi_definition}


## Assembly Description Definition

\lbl{tut_asm_definition}

## Micro Architecture Definition

\lbl{tut_mia_definition}

## Processor Definition

\lbl{tut_prc_definition}
