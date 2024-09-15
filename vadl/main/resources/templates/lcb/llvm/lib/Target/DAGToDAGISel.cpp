#include "[(${namespace})]DAGToDAGISel.h"
#include "[(${namespace})]RegisterInfo.h"
#include "[(${namespace})]SubTarget.h"
#include "[(${namespace})]TargetMachine.h"
#include "llvm/CodeGen/MachineConstantPool.h"
#include "llvm/CodeGen/MachineFrameInfo.h"
#include "llvm/CodeGen/MachineFunction.h"
#include "llvm/CodeGen/MachineInstrBuilder.h"
#include "llvm/CodeGen/MachineRegisterInfo.h"
#include "llvm/CodeGen/SelectionDAGISel.h"
#include "llvm/IR/CFG.h"
#include "llvm/IR/GlobalValue.h"
#include "llvm/IR/Instructions.h"
#include "llvm/IR/Intrinsics.h"
#include "llvm/IR/Type.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Support/Debug.h"
#include "llvm/Target/TargetMachine.h"

#define DEBUG_TYPE "[(${namespace})]DAGToDAGISel"

using namespace llvm;

void [(${namespace})]DAGToDAGISel::anchor() {}

bool [(${namespace})]DAGToDAGISel::SelectInlineAsmMemoryOperand(const SDValue &Op, unsigned ConstraintCode, std::vector<SDValue> &OutOps)
{
    return false;
}

// Other targets use different names for this
// This method is used for instruction selection to test if an address is a frame index
bool [(${namespace})]DAGToDAGISel::SelectAddrFI(SDValue Addr, SDValue &Base)
{
    if (auto FIN = dyn_cast<FrameIndexSDNode>(Addr))
    {
        Base = CurDAG->getTargetFrameIndex(FIN->getIndex(), MVT::SimpleValueType::[(${stackPointerType})]);
        return true;
    }
    return false;
}

void [(${namespace})]DAGToDAGISel::Select(SDNode *Node)
{
    LLVM_DEBUG(dbgs() << "Selecting: "; Node->dump(CurDAG); dbgs() << "\n");

    // If we have a custom node, we have already selected.
    if (Node->isMachineOpcode())
    {
        LLVM_DEBUG(dbgs() << "== "; Node->dump(CurDAG); dbgs() << "\n");
        Node->setNodeId(-1);
        return;
    }

    // Instruction Selection not handled by the auto-generated tablegen selection
    // should be handled here.

    // Try to select special nodes first.
    if (trySelect(Node))
    {
        return; // success
    }

    // select default node
    SelectCode(Node);
}

bool [(${namespace})]DAGToDAGISel::trySelect(SDNode *Node)
{
    // Instruction Selection not handled by the auto-generated tablegen selection
    // should be handled here.
    unsigned Opcode = Node->getOpcode();
    SDLoc DL(Node);

    return false;
}

// global function is declared in '[(${namespace})].h'
FunctionPass *llvm::create[(${namespace})]ISelDag([(${namespace})]TargetMachine &TM, CodeGenOpt::Level OptLevel)
{
    return new [(${namespace})]DAGToDAGISel(TM, OptLevel);
}

char [(${namespace})]DAGToDAGISel::ID = 0;