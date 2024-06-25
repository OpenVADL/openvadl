//===--------------------------------------------------------------------------===//
// This is the top level entry point for the [(${namespace})] target.
//===--------------------------------------------------------------------------===//


//===--------------------------------------------------------------------------===//
// Target-independent interface
//===--------------------------------------------------------------------------===//

include "llvm/Target/Target.td"


//===--------------------------------------------------------------------------===//
// Target-dependent interface
//===--------------------------------------------------------------------------===//

include "[(${namespace})]RegisterInfo.td"
include "[(${namespace})]InstrInfo.td"
include "[(${namespace})]CallingConv.td"

def [(${namespace})]InstrInfo : InstrInfo {}

// this is important for subtarget
def : ProcessorModel<"generic-[(${namespace})]", NoSchedModel, []>;

def [(${namespace})]AsmParser : AsmParser {
}

def [(${namespace})]InstPrinter : AsmWriter
{
    string AsmWriterClassName  = "InstPrinter";
    bit isMCAsmWriter = 1;
}

def [(${namespace})] : Target
{
    let InstructionSet = [(${namespace})]InstrInfo;
    let AssemblyWriters = [[(${namespace})]InstPrinter];
    let AssemblyParsers  = [[(${namespace})]AsmParser];
}

//===--------------------------------------------------------------------------===//
// TODOs
//===--------------------------------------------------------------------------===//
//
// * finish instruction operands
// * finish instruction pattern
// * finish calling conventions
// * ...
//