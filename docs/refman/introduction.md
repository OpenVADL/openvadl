
# Introduction {#intro}

The \ac{VADL} is a \ac{PDL}.
The name is inspired by the \ac{VDL} which was developed 50 years ago for the formal specification of the programming language PL/I using operational semantics \cite{wegnerVDL72}.

\ac{VADL} permits the complete formal specification of a processor architecture.
Additionally it is possible to specify the behavior of generators which produce different artifacts from a processor specification.
From a single concise \ac{VADL} processor specification, the \ac{VADL} system is able to automatically generate an assembler, a compiler, linker, functional \ac{ISS}, \ac{CAS}, synthesizable specification in a \ac{HDL}, test cases and documentation.
\ac{VADL} strictly separates the \ac{ISA} specification from the \ac{MiA} specification.
The \ac{ISA} specification is needed by all generators.
The \ac{MiA} specification is used by the \ac{HDL} and \ac{CAS} generators as well as for instruction scheduling in the compiler.
An \ac{ISA} specification can be implemented by one or more \ac{MiA} specifications.
The \ac{ABI} specification defines a programming model and is used by the compiler generator.
