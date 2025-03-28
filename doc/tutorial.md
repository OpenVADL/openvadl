# VADL Tutorial {#tutorial}

## Getting Started
\lbl{tut_getting_started}

Every \ac{VADL} processor specification is separated into different sections.

Listing \r{riscv_isa} shows a complete \ac{ISA} specification of all RISC-V instructions with immediate operands and branches.
It is a good example to show the most important \ac{VADL} \ac{ISA} features.

\listing{riscv_isa, RISC-V ISA specification for instructions with immediate operands and all branches}
~~~{.vadl}
instruction set architecture RV32I = {

  constant Size = 32                     // architecture size is 32 bits

  using Byte    = Bits<  8 >             // 8 bit Byte
  using Inst    = Bits< 32 >             // instruction word type
  using Regs    = Bits<Size>             // register word type 
  using Index   = Bits<  5 >             // 5 bit register index type for 32 registers
  using Addr    = Regs                   // address type is equal to the register type

  [zero : X(0)]                          // register with index 0 always is zero
  register file    X : Index -> Regs     // integer register file with 32 registers
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
The \ac{ISA} section specifies basic architecture elements like registers, program counter and memory model and the instructions with their behavior, encoding and assembly representatioin.
In line 3 a constant with the decimal value `32` for the register size is defined.

Lines 5 to 9 declare user defined types.
\ac{VADL} supports bit vector types (for details see section \r{langref_type_system}).
The basic type is `Bits`.
There exist two subtypes representing signed (`SInt`) and unsigned (`UInt`) two's complement integers.
The size of the bit vector is specified in angle brackets (`<N>`).

Line 12 demonstrates the definition of a register file.
Here we have a mapping of a 5 bit register index to the register type `Regs`, a bit vector type with a bit width of `32`.
Annotations can be used to detail a definition and are available for most of the \ac{ISA} elements.
Here the annotation is used to declare that the register `X(0)`is a zero register, a register where a write has no effect and when read always returns zero (see line 11).

The memory model is defined as a mapping from the `32` address `Addr` to a byte of eight bits.
Additionally to the memory model a memory consistency model can be dedeclared with an annotation.
One of the possible predefined consistency models is the weak memory consistency model of the RISC-V (`rvWeakMemoryOrdering`)

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
A macro definition starts with the keyword `model` followed by the typed arguments and the result type of the macro (line 36).
There exist syntactic types like `Id` (identifier), `BinOp` (binary operator), `Bin` (binary constant) or `IsaDefs` (\ac{ISA} definitions).
An instantiation of a macro or the substitution of a macro argument are indicated by the dollar sign.

An `instruction` defines the behavior of an instruction (line 37).
After the equality symbol `"="` the behavior is defined by a single statement or a list of statements in parantheses.
Assignment statements use the symbol `":="` to separate the target on the left hand side from the expression on the right hand side.
The precedence of all operators is listed in a table in section \r{expr_precedence}.
A conditional statement is shown in line 45.

The `encoding` sets the fields in an instruction word which are constant for the given instruction (line 39).
The `assembly` specifies the assembly language syntax for the instruction with a string expression (line 40).

By packing these three definitions into a macro, an instruction with behavior, encoding and assembly can be specified in a single line.
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
Usually, a \ac{VADL} processor specification has some common definitions in the beginning, followed by the main sections which describe the \ac{ISA} or the \ac{MiA}.
On line 1, a constant `MLen` with the value 64 is defined.
Type aliases can be defined with the keyword `using` as shown on lines 3 to 5.
On line 7, a function is defined that compares two values of type `SIntM` and returns the result of the comparison as a value of type `Bool`.
`import` allows the import of \ac{VADL} specification parts from separate files.
On line 9, a specification named `RV3264IM` is imported from a file called `rv3264im.vadl` by setting the model `ArchSize` to `Arch64`.
In this example, `RV3264IM` refers to another \ac{ISA} specification.

An `instruction set architecture` specification can extend another \ac{ISA} specification and optionally pass macros (line 11).
Section \r{isa_section} contains a detailed description of the \ac{ISA} specification.
Lines 13 to 19 demonstrate the definition of the `application binary interface` (see Section \r{abi_section}), the `assembly description` (see Section \r{assembly_description_section}), the `processor` specification (see Section \r{processor_section}).
On line 17 a \ac{MiA} named `FiveStage` is defined for the \ac{ISA} `RV64IM` (see Section \r{mia_section}).


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

Constant definitions start with the keyword `constant` followed by the name of the constant, an optional type after the colon symbol `":"`, the equal symbol `"="` and an expression which has to be evaluated at parse time (see Listing \r{constants_literals}).
The evaluation of the expression is done with a signed integer type with unlimited size (internally the parser uses a Java `BigInteger` type for the evaluation).
Therefore, the constant `twice` as expected has the value `64`.
The constants `min1one` and `min1two` are of the fixed size type `SInt<64>`.
They cannot be used in expressions with unlimited size any more, the expression evaluaton is done on type `SInt<64>` and the operands must have the type `SInt<64>`.

Literals can also be specified as binary or hexadecimal numbers.
A single quote symbol can be inserted into numbers to make them more readable.
The constant `binsize` represents the same value `32` as the constant `size`.
Equally to the constant `min1two` the constant `min1one` has the value minus one (`-1`) as the very large hexadecimal constant with a positive integer value is casted to a signed integer of the same size resulting in the negative value.

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
The definition of `addEx2` in Listing \r{constants_expressions} shows the call of the function `add` from the `VADL` builtin namespace.
This is equivalent to the usage of the `"+"` operator in the definition of `addEx1`.
A `let` expression defines the binding of an expression to a name.
The name then can be used in the expression after the keyword `in`.
In the constant definition of `letEx` the value `16` is bound to the name `size` which is used in the addition `size + 16` after `in`.
VADL has nested scoping of name spaces.
A `let` expression starts a new scope.
Therefore, the definition of `size` in the `let` expression hides the definition of the constant `size` in line 1 of Listing \r{constants_expressions}.
An `if` expression can be used in a constant definition if the condition can be evaluated at parse time.
An `if` expression always needs an `else` part.
As `letEx` has the value `32` the value of `ifEx` is `5`.
A `match` expression can be used to specify a multi way selection.
The expression after the keyword `match` is checked for equality with a list of expressions after the keyword `with` included in braces and separated by a comma symbol `","`.
The expressions in the list are evaluated sequentially, the expression which matches first is selected and the result of the evaluated expression after the arrow symbol `"=>"` gives the result of the whole `match` expression.
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
An enumeration is defined by the keyword `enumeration` followed by the name of the enumeration, an optional type after the colon symbol `":"` and an equality symbol `"="`.
Then follows a list of names enclosed in braces and separated by a comma symbol `","`.
The first name has the value `0`, every further name has the value of its predecessor incremented by one.
Optionally a constant expression can be assigned to the name after the equality symbol `"="`.
Line 7 of Listing \r{enumerations} shows the use of an enumeration element with the added name space in front separated by `"::"` to the name.


### Type Definitions (using)

\listing{using, Type Definitions (using)}
~~~{.vadl}
using Bits32    = Bits<32>       // a 32 bit vector
using Vector4   = Bits32<4>      // a 4 element vector of bit vectors 32 bit wide
using Matrix2   = Vector4<2>     // a 2 element times 4 element matrix of bit vectors 32 bit wide
using Matrix2_4 = Bits<2><4><16> // the same as above
using SInt32    = SInt<32>       // a 32 bit two's complement signed integer
using UInt32    = UInt<32>       // a 32 bit unsigned integer
~~~
\endlisting

The type system is explained in detail in the reference manual (see Section \r{langref_type_system}).
In VADL it is possible to define bit vectors of arbitrary length.
The basic types are Bits, SInt and UInt which can be used to form vectors.
Types are defined by the keyword `using` followed by the name of the type, the equality symbol `"="` and the type literal.
The type literal is comprised of a name optionally followed by a number of vector sizes in angle brackets.
Listing \r{using} shows some type definitions and their meaning in the comments.


### Functions

### Formats


## Macro System
\lbl{tut_macro_system}

### Syntax Types

\ac{VADL} exhibits a syntactical macro system.
The advantage of a syntactical macro system compared to a lexical macro system is the type safety.
There exists a set of syntax types which cover syntactical elements like an expression or an identifier.
The syntax types are designed to have a one-to-one relation to parser rules.
This already provides a partially ordered subtype relation.
The following table lists all core syntax types with a description and examples:

|  Type   | Description                          | Examples                                  |
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

Call expressions represent function or method calls, memory accesses or indexed registers accesses with slicing and field accesses.
The left hand side expression of an assignment statement also is a call expression.
Additional examples are `X(rs1)(15..0)`, `IntQueue.consume(@BranchIntBase)`, `VADL::add(X(5), X(6) * 2)` and `a(11..8,3..0)`.
A symbol expression consists of an identifier path optionally followed by a vector specification (`<VectorSizeExpression>`).
`Stats`, `Defs`, `IsaDefs` and `Encs` require at least one element of the specified type.

Figure \r{syntax_type_hierarchy} displays the subtype relation between the presented core types.
The macro type system provides an implicit up-casting of the value types.
For example, if a model expects a value of type `Val`, any subtype, i.e. `Bool`, `Int` or `Bin` will be accepted as argument.

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

top     -- isadefs ;
top     -- stats   ;
top     -- encs    ;
top     -- ex      ;
top     -- binop   ;
top     -- unop    ;
isadefs -- defs    ;
stats   -- stat    ;
ex      -- lit     ;
lit     -- str     ;
lit     -- val     ;
val     -- bool    ;
val     -- int     ;
val     -- bin     ;
ex      -- callex  ;
callex  -- symex   ;
symex   -- id      ;
}
\enddot
\endfigure{syntax_type_hierarchy, Syntax Types Hierarchy in the OpenVADL macro system}

### Macro Definition (model)

A macro is defined through the keyword `model` followed by the name of the macro, a list of typed arguments in parentheses separated by the comma symbol `","`, the type of the macro after a colon symbol `":"` and after the equal symbol `"="` the body of the macro enclosed in braces.
The usage of the model arguments inside the model body is indicated by the dollar symbol `"$"`.
When a model is invoked, the model arguments in the body are substituted by the values passed in the arguments.
Similar to arguments the invocation of a model is indicated by the dollar symbol `"$"`.
The arguments in a model invocation are separated by the semicolon symbol `";"`.
The result of the model invocation in line 8 of Listing \r{macro_model_definition} is shown in Listing \r{macro_model_invocation}.

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
In the example in Listing \r{match_macro}, a user can switch between a 32 and 64 bit address width by setting the appropriate model `Arch` to the identifier `Arch64`.

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
A syntax tuple is specified the same way a model argument list is provided, i.e. syntax elements are separated by `";"` and enclosed inside parentheses.
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

While most of the needs are covered by syntactical macros, string and identifier manipulation is best done using lexical macros.
A lexical macro acts on the abstraction level of token streams in contrast to an already parsed AST.
Two use-cases are supported using special macro functions.
Firstly, templates generating instruction behavior and assembly often need the instruction name once in form of an identifier (`Id`) and again in form of a string (`Str`).
This use case is covered by the `IdToStr` function.
This function takes an `Id` typed syntax element and converts it to a `Str` typed syntax element.
Secondly, the `ExtendId` function allows safe identifier manipulation.
This function takes an arbitrary number of `Id` or `Str` typed syntax elements, converts `Id` typed elements to `Str`, concatenates them and returns a single `Id` typed syntax element.
Listing \r{lexical_macros} shows a small example of both functions with their typed result as comment.
It is important to note that the context of identifiers generated by lexical macros is strictly separated from the context of the syntactical macros.
Therefore, it is not possible to define or refer to a model name or parameter using a generated identifier.

\listing{lexical_macros, Lexical Macro Examples}
~~~{.vadl}
ExtendId( "", I, "Am", An, "Identifier" ) // --> IAmAnIdentifier : Id
IdToStr( IAmAString )                     // --> "IAmAString"    : Str
~~~
\endlisting

### Higher Order Macros (model-type)

Higher order macros are macros which generate macros or which take macros as arguments.
In the macro expansion system of OpenVADL, model instances are expanded immediately at the site they are declared. This allows the usage of models that produce models. 

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
Because the `$BinExFactory` instance is evaluated immediately after it is parsed, the produced model `Addition` is known to the parser and can be used in the definition of the `ADD` instruction. 

#### Macros as Macro arguments

When using a macro as an argument of a macro, it is necessary to specify the signature of the passed macro in the argument type declaration (e.g. `(Ex, Ex) -> Ex` in Listing \r{higher_order_model_definition}).
As an alternative with better readability the signature can be declared in a separate type definition with the keyword `model-type` followed by the signature after the equal symbol `=`.

The model `BinExStat` takes a macro of type `BinExType` as an argument and returns a statement.
When the model `BinExStat` is invoked with the model `AddExp` as an argument, an assignment statement with an addition on the right hand side is generated.

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

If a macro is passed as an argument to a model and assuming that the type for this argument is declared by a `model-type`, then OpenVADL allows the model parameters of the passed macro to be supertypes of the `model-type` parameters and the result type to be a subtype of the `model-type` result.
Listing \r{model_type_parameters} shows a reference to `model Constants` being used as an `IsaDefsFactory`.
The reference is of a valid type because the result type `Defs` is a subtype of `IsaDefs` and the type `Ex` of parameter `size` is a supertype of `Id` (see Listing \r{syntax_type_hierarchy}). 

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

The ARM architecture AArch32 has a register file called `R` consisting of 16 registers which are 32 bits wide (see Listing \r{higher_order_macro}).
Conditions are specified by boolean expressions on flags of the status register `APSR`, e.g. the zero flag `Z`.
Every instruction can be executed conditionally.
There are 15 different conditions which are described by an enumeration in the specification and encoded by the `cc` field in an instruction word which is 32 bits wide.
Arithmetic/logic instructions, which have an immediate value as second source operand, share a common instruction encoding specified in the `ArLoImm` instruction format.

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

As in the AArch32 architecture every instruction can be executed conditionally, a basic instruction exists in 15 variants for 15 different conditions.
This problem can be solved smartly by an extension macro design pattern using higher-order macros as demonstrated in Listing \r{higher_order_macro}.

To reduce the number of macro arguments record types are defined for an instruction and a condition.
The `Inst` record type definition groups the four arguments describing an instruction together.
The `Cond` record type definition consists of a string representing the extension of the assembly name, the identifier of the enumeration of the condition encoding and a boolean expression for condition evaluation. 

Now 15 different instructions with a unique identifier have to be created.
This can be handled with the lexical macro function `ExtendId` by appending the extension string of the condition to the identifier.

The final problem is that there is a set of models which describe different kinds of conditional instructions and all these models should be called 15 times for the 15 different conditions.
This can be solved by the higher-order model `CondInstr`, which takes an instruction model (e.g. `ALImmCondInstr`) as first argument.
The instruction model is then called 15 times with an argument list which has been extended by the conditions.
In the above example the 4 macro calls expand to 60 different instructions.
The AArch32 architecture has instructions with a lot of additional variants like setting the status register, shifted operands or complex addressing modes.
This leads to a specification with multiple higher-order macro arguments.

### Macro Usage for Configuration

VADL provides the possibility of passing configuration information to the macro system using the command line.
Currently, this mechanism is kept very simple and is restricted to elements of type `Id`.
To prepare a configurable macro variable a default model of type `Id` has to be defined.
Listing \r{macro_configuration} shows such a variable of name `Size`, with the default setting `Arch32`.
Without any passed configurations the instantiation of `Size` results in the identifier `Arch32`.
If VADL receives the command line option `-m` or `--model` followed by the string `"Size=Arch64"`, the value of `Arch` is overridden.
If `Arch` is instantiated given the previous command line option, it would result in `Arch64`.
In combination with conditional expansion, see Section \r{macro_match} and Listing \r{match_macro}, this simple mechanism already provides powerful configuration capabilities.

\listing{macro_configuration, Macro Configuration Variable}
~~~{.vadl}
model Size() : Id = { Arch32 }
~~~
\endlisting

Similarly to model passing in the command line it is possible to pass models as an argument to import declarations as demonstrated in Listing \r{macro_import}.

\listing{macro_import, Import with Macro Argument}
~~~{.vadl}
import rv3264im::RV3264I with ("Size=Arch64")
~~~
\endlisting
