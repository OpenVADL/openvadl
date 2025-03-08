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

  // macro for immediate instructions with name, operator, function code and type
  model ItypeInstr (name : Id, op : BinOp, funct3 : Bin, type: Id) : IsaDefs = {
    instruction $name : Itype =
       X(rd) := (X(rs1) as $type $op immS as $type) as Regs
    encoding $name = {opcode = 0b001'0011, funct3 = $funct3}
    assembly $name = (mnemonic," ",register(rd),",",register(rs1),",",decimal(imm))
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
The first one defines bitfields with a name followed by a colon `:` and a type (line 17 to 24).
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
After the symbol `=` the behavior is defined by a single statement or a list of statements in parantheses.
Assignment statements use the symbol `:=` to separate the target on the left hand side from the expression on the right hand side.
The precedence of all operators is listed in a table in section \r{expr_precedence}.
A conditional statement is shown in line 45.

The `encoding` sets the fields in an instruction word which are constant for the given instruction (line 39).
The `assembly` specifies the assembly language syntax for the instruction with a string expression (line 40).

By packing these three definitions into a macro, an instruction with behavior, encoding and assembly can be specified in a single line.
This macro is invoked six times for all RISC-V instructions with immediate operands (lines 51 to 56).

## Macro System
\lbl{tut_macro_system}

\ac{VADL} exhibits a syntactical macro system.
The advantage of a syntactical macro system compared to a lexical macro system is the type safety.
There exist a set of syntax types which cover syntactical elements like an expression or an identifier.
The syntax types are designed to have a one-to-one relation to parser rules.
This already provides a partially ordered subtype relation.
The following table lists all core syntax types with a description and examples:

|  Type   | Description                          | Examples                                |
|:--------|:-------------------------------------|:---------------------------------------:|
| Ex      | Generic VADL Expression              | `X(rs1) + X(rs2) * 2                  ` |
| Lit     | Generic VADL Literal                 | `1, "ADDI"                            ` |
| Val     | Generic VADL Value Literal           | `1, 0b001                             ` |
| Bool    | Boolean Literal                      | `true, false                          ` |
| Int     | Integer Literal                      | `1, 2, 3                              ` |
| Bin     | Binary or Hexadecimal Literal        | `0b0111, 0xff                         ` |
| Str     | String Literal                       | `"ADDI"                               ` |
| CallEx  | Arbitrary Call Expression            | `MEM<2>(rs1)                          ` |
| SymEx   | Symbol Call Expression               | `rs1, MEM<2>                          ` |
| Id      | Identifier Symbol                    | `rs1, ADDI, X                         ` |
| BinOp   | Binary Operator                      | `+, -, *                              ` |
| UnOp    | Unary Operator                       | `-, !                                 ` |
| Stat    | Generic VADL Statement               | `X(rd) := X(rs)                       ` |
| Stats   | List of VADL Statements              | `X(rd) := X(rs) ...                   ` |
| Defs    | List of common VADL Definitions      | `constant b = 8, using Byte = Bits<8> ` |
| IsaDefs | List of VADL ISA Definition          | `instruction ORI : Itype = { ... } ...` |
| Encs    | Element(s) of an Encoding Definition | `opcode = 0b110’0011, ...             ` |

Figure \r{syntax_type_hierarchy} displays the subtype relation between the presented core types.
The macro type system provides an implicit up-casting of the value types.
For example, if a model expects a value of type `Val`, any subtype, i.e. `Bool`, `Int` or `Bin` will be accepted as argument.

\figure{b!}
\dot
graph example {
node  [shape=none];

top     [ label="┳"       ];
stats   [ label="Stats"   ];
stat    [ label="Stat"    ];
encs    [ label="Encs"    ];
isadefs [ label="IsaDefs" ];
defs    [ label="Defs"    ];
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

top     -- stats   ;
top     -- encs    ;
top     -- isadefs ;
top     -- ex      ;
top     -- binop   ;
top     -- unop    ;
stats   -- stat    ;
isadefs -- defs    ;
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
