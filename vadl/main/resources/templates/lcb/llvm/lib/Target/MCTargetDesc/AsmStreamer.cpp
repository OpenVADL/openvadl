#include "[(${namespace})]AsmStreamer.h"
#include "llvm/Support/FormattedStream.h"

using namespace llvm;

[(${namespace})]TargetStreamer::[(${namespace})]TargetStreamer(MCStreamer &S) : MCTargetStreamer(S) {}

// This part is for ascii assembly output
[(${namespace})]AsmStreamer::[(${namespace})]AsmStreamer(MCStreamer &S, formatted_raw_ostream &OS) : [(${namespace})]TargetStreamer(S), OS(OS)
{
}

void [(${namespace})]AsmStreamer::emitDirectiveOptionPush()
{
    // TODO: FIXME @chochrainer, this si RISCV specific
    OS << "\t.option\tpush\n";
}

void [(${namespace})]AsmStreamer::emitDirectiveOptionPop()
{
    // TODO: FIXME @chochrainer, this si RISCV specific
    OS << "\t.option\tpop\n";
}

void [(${namespace})]AsmStreamer::emitDirectiveOptionRVC()
{
    // TODO: FIXME @chochrainer, this si RISCV specific
    OS << "\t.option\trvc\n";
}

void [(${namespace})]AsmStreamer::emitDirectiveOptionNoRVC()
{
    // TODO: FIXME @chochrainer, this si RISCV specific
    OS << "\t.option\tnorvc\n";
}

void [(${namespace})]AsmStreamer::emitDirectiveOptionRelax()
{
    // TODO: FIXME @chochrainer, this si RISCV specific
    OS << "\t.option\trelax\n";
}

void [(${namespace})]AsmStreamer::emitDirectiveOptionNoRelax()
{
    // TODO: FIXME @chochrainer, this si RISCV specific
    OS << "\t.option\tnorelax\n";
}