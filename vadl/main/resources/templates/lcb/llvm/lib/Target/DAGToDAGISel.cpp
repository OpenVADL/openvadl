#include "[(${namespace})]DAGToDAGISel.h"
#include "[(${namespace})]RegisterInfo.h"
#include "[(${namespace})]SubTarget.h"
#include "[(${namespace})]TargetMachine.h"
#include "MCTargetDesc/[(${namespace})]ConstMatInt.h"
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
#define PASS_NAME "[(${namespace})] DAG->DAG Pattern Instruction Selection"

using namespace llvm;

void [(${namespace})]DAGToDAGISel::anchor() {}

bool [(${namespace})]DAGToDAGISel::SelectInlineAsmMemoryOperand(const SDValue &Op, InlineAsm::ConstraintCode ConstraintCode, std::vector<SDValue> &OutOps)
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

static SDNode *selectImm(SelectionDAG *CurDAG, const SDLoc &DL, int64_t Imm,
                         MVT XLenVT, const [(${namespace})]Subtarget &Subtarget) {
  auto Seq = [(${namespace})]MatInt::generateInstSeq(Imm);

  SDNode *Result;
  for ([(${namespace})]MatInt::Inst &Inst : Seq) {
    SDValue SDImm = CurDAG->getTargetConstant(Inst.getImm(), DL, XLenVT);
    Result = CurDAG->getMachineNode(Inst.getOpcode(), DL, XLenVT, SDImm);
  }

  return Result;
}

bool [(${namespace})]DAGToDAGISel::trySelect(SDNode *Node)
{
    // Instruction Selection not handled by the auto-generated tablegen selection
    // should be handled here.
    unsigned Opcode = Node->getOpcode();
    SDLoc DL(Node);
    auto XLenVT = MVT::[(${stackPointerType})];

    switch(Opcode) {
       case ISD::Constant:
       {
         auto ConstNode = cast<ConstantSDNode>(Node);
         MVT VT = Node->getSimpleValueType(0);

         [#th:block th:if="${replaceImmZeroByRegisterZero}"]
         // The idea is that when we have zero register then we can use it.
         // So we can use RR instructions and not only RI.
         if (VT == MVT::[(${stackPointerType})] && ConstNode->isZero()) {
           SDValue New = CurDAG->getCopyFromReg(CurDAG->getEntryNode(), SDLoc(Node),
                                                [(${namespace})]::[(${zeroRegister})], MVT::[(${stackPointerType})]);
           ReplaceNode(Node, New.getNode());
           return true;
         }
         [/th:block]

         // Handle rest
         int64_t Imm = ConstNode->getSExtValue();
         if (XLenVT == MVT::[(${stackPointerType})]) {
          ReplaceNode(Node, selectImm(CurDAG, SDLoc(Node), Imm, XLenVT, *Subtarget));
          return true;
         }

         return false;
       }
       default:
        return false;
    }

    return false;
}

// global function is declared in '[(${namespace})].h'
FunctionPass *llvm::create[(${namespace})]ISelDag([(${namespace})]TargetMachine &TM, CodeGenOptLevel OptLevel)
{
    return new [(${namespace})]DAGToDAGISelLegacy(TM, OptLevel);
}

[(${namespace})]DAGToDAGISelLegacy::[(${namespace})]DAGToDAGISelLegacy([(${namespace})]TargetMachine &TM,
                                                 CodeGenOptLevel OptLevel)
    : SelectionDAGISelLegacy(
          ID, std::make_unique<[(${namespace})]DAGToDAGISel>(TM, OptLevel)) {}

char [(${namespace})]DAGToDAGISelLegacy::ID = 0;

INITIALIZE_PASS([(${namespace})]DAGToDAGISelLegacy, DEBUG_TYPE, PASS_NAME, false, false)