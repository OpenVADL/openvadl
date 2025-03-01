# VADL Reference Manual {#refman}

## General Language Features

### Type System
\lbl{refman_type_system}

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
