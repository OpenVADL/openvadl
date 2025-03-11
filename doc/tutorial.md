# VADL Tutorial {#tutorial}

## Getting Started
\lbl{tut_getting_started}

Every \ac{VADL} processor specification is separated into different sections.

Listing \r{riscv-isa} shows a complete \ac{ISA} specification of all RISC-V instructions with immediate operands and branches.
It is a good example to show the most important \ac{VADL} \ac{ISA} features.

\listing{riscv-isa, RISC-V ISA specification for instructions with immediate operands and all branches}
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
| Val     | Generic VADL Value Literal           | `1, 0b001                               ` |
| Bool    | Boolean Literal                      | `true, false                            ` |
| Int     | Integer Literal                      | `1, 2, 3                                ` |
| Bin     | Binary or Hexadecimal Literal        | `0b0111, 0xff                           ` |
| Str     | String Literal                       | `"ADDI"                                 ` |
| CallEx  | Arbitrary Call Expression            | `MEM<2>(rs1),PC.next,abs(X(rs1)),Z(0)(1)` |
| SymEx   | Symbol Expression                    | `rs1, MEM<2>, VADL::add                 ` |
| Id      | Identifier                           | `rs1, ADDI, X                           ` |
| BinOp   | Binary Operator                      | `+, -, *                                ` |
| UnOp    | Unary Operator                       | `-, !                                   ` |
| Stat    | Generic VADL Statement               | `X(rd) := X(rs)                         ` |
| Stats   | List of VADL Statements              | `X(rd) := X(rs) ...                     ` |
| Defs    | List of common VADL Definitions      | `constant b = 8, using Byte = Bits<8>   ` |
| IsaDefs | List of VADL ISA Definition          | `instruction ORI : Itype = { ... } ...  ` |
| Encs    | Element(s) of an Encoding Definition | `opcode = 0b110’0011, none, ...         ` |

Call expressions represent function or method calls, memory accesses or indexed registers accesses with slicing.
The left hand side expression of an assignment also is a call expression.
Additional examples are `X(rs1)(15..0)`, `IntQueue.consume(@BranchIntBase)`, `VADL::add(X(5), X(6) * 2)` and `a(11..8,3..0)`.
A symbol expression consists of an identifier path optionally followed by a vector specification (`<VectorSizeExpression>`).

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

A macro is defined through the keyword `model` followed by the name of the macro, a list of typed arguments in parentheses separated by the comma symbol `","`, the type of the macro after a colon symbol `":"` and after the equal symbol `"="` the body of the macro enclosed in parentheses.
The usage of the model arguments inside the model body is indicated by the dollar symbol `"$"`.
When a model is invoked, the model arguments in the body are substituted by the values passed in the arguments.
Similar to arguments the invocation of a model is indicated by the dollar symbol `"$"`.
The arguments in a model invocation are separated by the semicolon symbol `";"`.
The result of the model invocation in line 51 of Listing \r{riscv-isa} is shown in Listing \r{macro_model_invocation}.


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

To support the conditional application of macros VADL provides an explicitly typed `match`-macro.
It will conditionally insert the match result into the syntax tree.
It can be used inside a `model` definition as well as in any location in a specification.
A match macro is started by the keyword `match` followed by the colon symbol `":"` and the syntax type of the macro.
Enclosed by parentheses is a list of `match` elements separated by a semicolon `";"`.
A `match` element contains a condition followed by the result of the macro after the double arrow symbol `"=>"`. 
For the conditions only comparisons for equality (`"="`) or inequality (`"!="`) between two syntax elements are allowed.
For every `match`-macro a default case has to be provided at the last position, indicated by the underline symbol `"_"`.
When used outside of a model defintion only macro invocations can be used in the comparison.
In the example in Listing \r{match_macro}, a user can switch between 32 and 64 Bit length by setting the appropriate `Arch` to `Arch64`.

\listing{match_macro, Matching on a model}
~~~{.vadl}
model Arch () : Id = {Arch64}
constant AddrWidth = match : Ex ($Arch() = Arch64 => 64; _ => 32)
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
model InstModel (info: BinInstRec)
  : IsaDefs = {
  instruction $info.name : F = {
    X(rd) := X(rs1) $info.op X(rs2)
    }
}

$InstModel( ( SUB ; - ) )
$InstModel( ( ADD ; + ) )
~~~
\endlisting

### Lexical Macro Functions 

While most of the needs are covered by syntactical macros, string and identifier manipulation is best done using lexical macros.
A lexical macro acts on the abstraction level of token streams in contrast to an already parsed AST.
Two use-cases are supported using special macro functions.
Firstly, templates generating instruction behavior and assembly often need the instruction name once in form of an identifier (`Id`) and again in form of a string (`Str`).
This use case is covered by the `IdToStr` function.
This function takes an `Id` typed syntax element and converts it to a `Str` typed syntax element.
Secondly, the `ExtendId` function allows safe identifier manipulation.
This function takes an `Id` typed identifier and an arbitrary number of `Str` typed syntax elements, concatenates them and returns a single `Id` typed syntax element.
Listing \r{lexical_macros} shows a small example of both functions with their typed result as comment.
It is important to note that the context of identifiers generated by lexical macros is strictly separated from the context of the syntactical macros.
Therefore, it is not possible to define or refer to a model name or parameter using a generated identifier.

\listing{lexical_macros, Lexical Macro Examples}
~~~{.vadl}
ExtendId( I, "Am", "An", "Identifier" ) // --> IAmAnIdentifier : Id
IdToStr( IAmAString )                   // --> "IAmAString"    : Str
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

#### Type variance in model-type parameters

If a macro is passed as an argument to a model and assuming that the type for this argument is declared by a `model-type`, then OpenVADL allows the model parameters of the passed macro to be supertypes of the `model-type` parameters and the result type to be a subtype of the `model-type` result.
Listing \r{model_type_parameters} shows a reference to `model Constants` being used as an `IsaDefsFactory`.
The reference is of a valid type because the result type `Defs` is a subtype of `IsaDefs` and the type `Ex` of parameter `size` is a supertype of `Id` (see Figure \r{syntax_type_hierarchy}). 

\listing{model_type_parameters, Valid types in model references}
~~~{.vadl}
instruction set architecture ISA = {
  constant Word = 32

  model-type IsaDefsFactory = (Id) -> IsaDefs

  model Constants(size: Ex): Defs = {
    constant full = $size
    constant half = $size / 2
  }

  model BitDefs(factory: IsaDefsFactory, size: Id): IsaDefs = {
    $factory($size)
  }

  $BitDefs(Constants ; Word)
}
~~~
\endlisting

\listing{higher_order_macro, Instruction Specification applying Higher Order Macros}
~~~{.vadl}
record Instr (id: Id, ass: Str, op: BinOp, opcode: Bin)
record Cond  (str: Str, code: Id, ex: Ex)

model ALImmCondInstr (cond: Cond, instr: Instr) : IsaDefs = {
  instruction ExtendId ($instr.id, $cond.str) : ArLoImm = {
    if ($cond.ex) then
      R(rd) := R(rn) $instr.op imm12
    }
  encoding ExtendId ($instr.id, $cond.str) =
    {cc = cond::$cond.code, op = $instr.opcode, flags = 0}
  assembly ExtendId ($instr.id, $cond.str) =
    ($instr.ass, $cond.str, ' ', register(rd), ',', register(rn), ',', decimal(imm12))
  }

model-type CondInstrModel = (Cond, Instr) -> IsaDefs

model CondInstr (modelid: CondInstrModel, instr: Instr) : IsaDefs = {
  $modelid (( "eq" ; EQ ;  APSR.Z = 0b1 ) ; $instr)
  $modelid (( "ne" ; NE ;  APSR.Z = 0b0 ) ; $instr)
  //...
  }

$CondInstr(ALImmCondInstr ; ( ADD ; "add" ; + ; 0b000'0100 ))
$CondInstr(ALImmCondInstr ; ( SUB ; "sub" ; - ; 0b000'0010 ))
$CondInstr(ALImmCondInstr ; ( AND ; "and" ; & ; 0b000'0000 ))
$CondInstr(ALImmCondInstr ; ( ORR ; "orr" ; | ; 0b000'1100 ))
~~~
\endlisting

