#include "processornamevalueISelLowering.h"
#include "processornamevalue.h"
#include "Utils/processornamevalueBaseInfo.h"
#include "processornamevalueRegisterInfo.h"
#include "processornamevalueSubTarget.h"
#include "processornamevalueMachineFunctionInfo.h"
#include "MCTargetDesc/processornamevalueMCTargetDesc.h"
#include "llvm/CodeGen/CallingConvLower.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Support/Debug.h"
#include "llvm/IR/Instructions.h"
#include "llvm/IR/Intrinsics.h"
#include "llvm/IR/Intrinsicsprocessornamevalue.h"
#include <iostream>

#define DEBUG_TYPE "processornamevalueTargetLowering"

using namespace llvm;

#include "processornamevalueGenCallingConv.inc"

// A scalar wider than two machine words is passed by reference in the integer
// calling convention. Legalization splits it into several arguments, including
// for soft-float libcalls, so assign one pointer location to the whole group.
template <typename ArgT>
static void analyzeArguments(CCState &State, SmallVectorImpl<CCValAssign> &Locs,
                             const SmallVectorImpl<ArgT> &Args)
{
    const MVT PtrVT = MVT::i64;
    for (unsigned i = 0, e = Args.size(); i != e; ++i)
    {
        unsigned End = i;
        if (!Args[i].ArgVT.isVector() && Args[i].VT.isScalarInteger()
            && Args[i].Flags.isSplit())
        {
            while (!Args[End].Flags.isSplitEnd())
            {
                ++End;
                assert(End < e && "Unterminated split argument");
            }
        }

        if (End - i < 2)
        {
            if (CC_processornamevalue(i, Args[i].VT, Args[i].VT,
                                   CCValAssign::Full, Args[i].Flags, State))
                report_fatal_error("Unsupported argument type");
            continue;
        }

        if (CC_processornamevalue(i, PtrVT, PtrVT, CCValAssign::Full,
                               ISD::ArgFlagsTy(), State))
            report_fatal_error("Unsupported indirect argument");
        CCValAssign PtrLoc = Locs.pop_back_val();
        for (unsigned Part = i; Part <= End; ++Part)
        {
            State.addLoc(PtrLoc.isRegLoc()
                ? CCValAssign::getReg(Part, Args[Part].VT, PtrLoc.getLocReg(), PtrVT,
                                      CCValAssign::Indirect)
                : CCValAssign::getMem(Part, Args[Part].VT, PtrLoc.getLocMemOffset(), PtrVT,
                                      CCValAssign::Indirect));
        }
        i = End;
    }
}

void processornamevalueTargetLowering::anchor() {}

processornamevalueTargetLowering::processornamevalueTargetLowering(const TargetMachine &TM, processornamevalueSubtarget &STI)
    : TargetLowering(TM), Subtarget(STI)
{
    // Set up the register classes defined by register files
     
      addRegisterClass(MVT::i64, &processornamevalue::SRegClass);
      
      addRegisterClass(MVT::i32, &processornamevalue::RRegClass);
      
      addRegisterClass(MVT::i64, &processornamevalue::XRegClass);
      
      addRegisterClass(MVT::i32, &processornamevalue::WRegClass);
     

    setStackPointerRegisterToSaveRestore(processornamevalue::S31);

    setOperationAction(ISD::GlobalAddress, MVT::i64, Custom);
    setOperationAction(ISD::BlockAddress, MVT::i64, Custom);
    setOperationAction(ISD::ConstantPool, MVT::i64, Custom);
    setOperationAction(ISD::JumpTable, MVT::i64, Custom);
    setOperationAction(ISD::Constant, MVT::i64, Custom);

    setOperationAction(ISD::VASTART, MVT::Other, Custom);
    setOperationAction(ISD::VAARG, MVT::Other, Custom);
    setOperationAction(ISD::VACOPY, MVT::Other, Expand);
    setOperationAction(ISD::VAEND, MVT::Other, Expand);
    setOperationAction(ISD::SELECT, MVT::i64, Custom);
    setOperationAction(ISD::SMUL_LOHI, MVT::i32, Expand);
    setOperationAction(ISD::UMUL_LOHI, MVT::i32, Expand);
    for (auto VT : {MVT::i1, MVT::i8, MVT::i16, MVT::i32}) {
        setOperationAction(ISD::SIGN_EXTEND_INREG, VT, Expand);
    }
    setOperationAction(ISD::BR_JT, MVT::Other, Expand);
    setOperationAction(ISD::DYNAMIC_STACKALLOC, MVT::i32, Expand);
    setOperationAction(ISD::STACKSAVE, MVT::Other, Expand);
    setOperationAction(ISD::STACKRESTORE, MVT::Other, Expand);

    
    setOperationAction(ISD::BSWAP, MVT::i64, Expand);
    setOperationAction(ISD::MUL, MVT::i64, Expand);
    setOperationAction(ISD::ROTL, MVT::i64, Expand);
    setOperationAction(ISD::ROTR, MVT::i64, Expand);
    setOperationAction(ISD::SHL_PARTS, MVT::i64, Expand);
    setOperationAction(ISD::SMUL_LOHI, MVT::i64, Expand);
    setOperationAction(ISD::SRA_PARTS, MVT::i64, Expand);
    setOperationAction(ISD::SRL_PARTS, MVT::i64, Expand);
    setOperationAction(ISD::UMUL_LOHI, MVT::i64, Expand);
    

    for (auto N : {ISD::EXTLOAD, ISD::SEXTLOAD, ISD::ZEXTLOAD}) {
        setLoadExtAction(N, MVT::i64, MVT::i1, Promote);
    }

    
    setOperationAction(ISD::BRCOND, MVT::Other, Expand);
    setOperationAction(ISD::BR_CC, { MVT::i32, MVT::i64 }, Custom);
    setOperationAction(ISD::SELECT_CC, { MVT::i32, MVT::i64 }, Custom);
    setOperationAction(ISD::SETCC, MVT::i32, Expand);
    setOperationAction(ISD::SETCC, MVT::i64, Expand);
    
    
    setOperationAction(ISD::INTRINSIC_VOID, MVT::Other, Custom);

    setBooleanContents(ZeroOrOneBooleanContent);

    // Compute derived properties from the register classes
    computeRegisterProperties(STI.getRegisterInfo());
}

const char *processornamevalueTargetLowering::getTargetNodeName(unsigned Opcode) const
{
    switch (Opcode)
    {
    case processornamevalueISD::RET_FLAG:
        return "processornamevalueISD::RetFlag";
    case processornamevalueISD::CALL:
        return "processornamevalueISD::CALL";
    
    case processornamevalueISD::SELECT_CC_S:
        return "processornamevalueISD::SELECT_CC_S";
    case processornamevalueISD::SELECT_CC_R:
        return "processornamevalueISD::SELECT_CC_R";
    case processornamevalueISD::SELECT_CC_X:
        return "processornamevalueISD::SELECT_CC_X";
    case processornamevalueISD::SELECT_CC_W:
        return "processornamevalueISD::SELECT_CC_W";
    
    case processornamevalueISD::LGA:
            return "processornamevalueISD::LGA";
    default:
        llvm_unreachable("unknown opcode");
    }
}

static SDValue lowerSetcc(SDValue Op, SelectionDAG &DAG)
{
  Op->dump();
  SDValue LHS = Op.getOperand(0);
  SDValue RHS = Op.getOperand(1);
  SDLoc dl(Op);

  EVT VT = Op.getValueType();
  SDValue TVal = DAG.getConstant(1, dl, VT);
  SDValue FVal = DAG.getConstant(0, dl, VT);

  return DAG.getNode(ISD::SELECT_CC, dl, Op->getValueType(0), LHS, RHS, TVal, FVal, Op.getOperand(2));
}


static SDValue lowerBR_CC(SDValue Op, SelectionDAG &DAG) {
   SDValue Chain = Op.getOperand(0);
   ISD::CondCode CC = cast<CondCodeSDNode>(Op.getOperand(1))->get();
   SDValue LHS = Op.getOperand(2);
   SDValue RHS = Op.getOperand(3);
   SDValue Dest = Op.getOperand(4);
   SDLoc dl(Op);

   auto Sub = DAG.getMachineNode(processornamevalue::SUBXSUXTX, dl, { MVT::i64, MVT::Other },  {LHS, RHS, Chain});
   SDValue ConditionFlag = SDValue(Sub, 1);

   switch(CC) {
    case ISD::CondCode::SETEQ:
      return SDValue(DAG.getMachineNode(processornamevalue::B_EQ, dl, MVT::Other, Dest, ConditionFlag), 0);
    break;
    case ISD::CondCode::SETNE:
      return SDValue(DAG.getMachineNode(processornamevalue::B_NE, dl, MVT::Other, Dest, ConditionFlag), 0);
    break;
    case ISD::CondCode::SETLT:
      return SDValue(DAG.getMachineNode(processornamevalue::B_LT, dl, MVT::Other, Dest, ConditionFlag), 0);
    break;
    case ISD::CondCode::SETLE:
      return SDValue(DAG.getMachineNode(processornamevalue::B_LE, dl, MVT::Other, Dest, ConditionFlag), 0);
    break;
    case ISD::CondCode::SETGT:
      return SDValue(DAG.getMachineNode(processornamevalue::B_GT, dl, MVT::Other, Dest, ConditionFlag), 0);
    break;
    case ISD::CondCode::SETGE:
      return SDValue(DAG.getMachineNode(processornamevalue::B_GE, dl, MVT::Other, Dest, ConditionFlag), 0);
    break;
    default:
      llvm_unreachable("unimplemented operand");
   }
}

static SDValue lowerSelectcc(SDValue Op, SelectionDAG &DAG)
{
  SDValue LHS = Op.getOperand(0);
  SDValue RHS = Op.getOperand(1);
  auto TVal = Op.getOperand(2);
  auto FVal = Op.getOperand(3);

  ISD::CondCode CC = cast<CondCodeSDNode>(Op.getOperand(4))->get();
  SDLoc dl(Op);

  auto Sub = DAG.getMachineNode(processornamevalue::SUBXSUXTX, dl, { MVT::i64, MVT::Other },  {LHS, RHS});
  SDValue ConditionFlag = SDValue(Sub, 1);

   switch(CC) {
    
    case ISD::CondCode::SETEQ:
      return SDValue(DAG.getMachineNode(processornamevalue::CSELEQX, dl, MVT::i64, TVal, FVal, ConditionFlag ), 0);
    
    
    case ISD::CondCode::SETNE:
      return SDValue(DAG.getMachineNode(processornamevalue::CSELNEX, dl, MVT::i64, TVal, FVal, ConditionFlag ), 0);
    
    
    case ISD::CondCode::SETLT:
      return SDValue(DAG.getMachineNode(processornamevalue::CSELLTX, dl, MVT::i64, TVal, FVal, ConditionFlag ), 0);
    
    
    case ISD::CondCode::SETGT:
      return SDValue(DAG.getMachineNode(processornamevalue::CSELGTX, dl, MVT::i64, TVal, FVal, ConditionFlag ), 0);
    
    
    case ISD::CondCode::SETGE:
      return SDValue(DAG.getMachineNode(processornamevalue::CSELGEX, dl, MVT::i64, TVal, FVal, ConditionFlag ), 0);
    
    
    case ISD::CondCode::SETLE:
      return SDValue(DAG.getMachineNode(processornamevalue::CSELLEX, dl, MVT::i64, TVal, FVal, ConditionFlag ), 0);
    
    default:
      llvm_unreachable("unimplemented operand");
   }
}

static SDValue lowerSelect2(SDValue Op, SelectionDAG &DAG)
{
  SDValue Cond = Op.getOperand(0);
  SDValue LHS = Op.getOperand(1);
  SDValue RHS = Op.getOperand(2);
  SDLoc dl(Op);

  auto Sub = DAG.getMachineNode(processornamevalue::SUBXSUXTX, dl, { MVT::i64, MVT::Other },  {Cond, Cond});
  SDValue ConditionFlag = SDValue(Sub, 1);

  return SDValue(DAG.getMachineNode(processornamevalue::CSELEQX, dl, MVT::i64, LHS, RHS, ConditionFlag), 0);
}



SDValue processornamevalueTargetLowering::LowerOperation(SDValue Op, SelectionDAG &DAG) const
{
    switch (Op.getOpcode())
    {
    case ISD::GlobalAddress:
        return lowerGlobalAddress(Op, DAG);
    case ISD::BlockAddress:
        return lowerBlockAddress(Op, DAG);
    case ISD::ConstantPool:
        return lowerConstantPool(Op, DAG);
    case ISD::JumpTable:
        return lowerJumpTable(Op, DAG);
    case ISD::Constant:
         return lowerConstant(Op, DAG);
    case ISD::VASTART:
        return lowerVASTART(Op, DAG);
    case ISD::VAARG:
        return lowerVAARG(Op, DAG);
    case ISD::SETCC:
        return lowerSetcc(Op, DAG);
    
    case ISD::SELECT_CC:
        return lowerSelectcc(Op, DAG);
    case ISD::BR_CC:
        return lowerBR_CC(Op, DAG);
    case ISD::SELECT:
        return lowerSelect2(Op, DAG);
    
    
    case ISD::INTRINSIC_VOID: {
      SDLoc DL(Op);
      unsigned IntNo = cast<ConstantSDNode>(Op.getOperand(1))->getZExtValue();
      SDValue Chain = Op.getOperand(0);

      
        if (IntNo == Intrinsic::processorNameValue_NOP) {
          return SDValue(DAG.getMachineNode(processornamevalue::NOP, DL, MVT::Other, Chain), 0);
        }
        if (IntNo == Intrinsic::processorNameValue_YIELD) {
          return SDValue(DAG.getMachineNode(processornamevalue::YIELD, DL, MVT::Other, Chain), 0);
        }
      

      break;
    }
    default : llvm_unreachable("unimplemented operand");
    }
}

SDValue processornamevalueTargetLowering::lowerConstant(SDValue Op, SelectionDAG &DAG) const
{
  int64_t Imm = cast<ConstantSDNode>(Op)->getSExtValue();

  if (isInt<32>(Imm))
    return Op;

  // Expand to a constant pool using the default expansion code.
  return SDValue();
}

static SDValue unpackFromRegLoc(SelectionDAG &DAG, SDValue Chain, const CCValAssign &VA, const SDLoc &DL)
{
    MachineFunction &MF = DAG.getMachineFunction();
    MachineRegisterInfo &RegInfo = MF.getRegInfo();

    EVT RegVT = VA.getLocVT();

    
    
      if(RegVT.getSimpleVT().SimpleTy == MVT::i64)  {
        const unsigned VReg = RegInfo.createVirtualRegister(&processornamevalue::SRegClass);
        RegInfo.addLiveIn(VA.getLocReg(), VReg);
        SDValue ArgIn = DAG.getCopyFromReg(Chain, DL, VReg, RegVT);
        return ArgIn;
      }
    
    
      if(RegVT.getSimpleVT().SimpleTy == MVT::i32)  {
        const unsigned VReg = RegInfo.createVirtualRegister(&processornamevalue::RRegClass);
        RegInfo.addLiveIn(VA.getLocReg(), VReg);
        SDValue ArgIn = DAG.getCopyFromReg(Chain, DL, VReg, RegVT);
        return ArgIn;
      }
    
    
      if(RegVT.getSimpleVT().SimpleTy == MVT::i64)  {
        const unsigned VReg = RegInfo.createVirtualRegister(&processornamevalue::XRegClass);
        RegInfo.addLiveIn(VA.getLocReg(), VReg);
        SDValue ArgIn = DAG.getCopyFromReg(Chain, DL, VReg, RegVT);
        return ArgIn;
      }
    
    
      if(RegVT.getSimpleVT().SimpleTy == MVT::i32)  {
        const unsigned VReg = RegInfo.createVirtualRegister(&processornamevalue::WRegClass);
        RegInfo.addLiveIn(VA.getLocReg(), VReg);
        SDValue ArgIn = DAG.getCopyFromReg(Chain, DL, VReg, RegVT);
        return ArgIn;
      }
    
    

    LLVM_DEBUG(dbgs() << "unpackFromRegLoc Unhandled argument type: " << RegVT.getEVTString() << "\n");
    llvm_unreachable("arguments type did not fit a register class!");
}

static SDValue unpackFromMemLoc(SelectionDAG &DAG, SDValue Chain, const CCValAssign &VA, const SDLoc &DL)
{
    MachineFunction &MF = DAG.getMachineFunction();
    MachineFrameInfo &MFI = MF.getFrameInfo();
    EVT LocVT = VA.getLocVT();
    EVT ValVT = VA.getValVT();
    EVT PtrVT = MVT::i64; // TODO: @chochrainer --> MVT::getIntegerVT( DAG.getDataLayout().getPointerSizeInBits( 0 ) );
    unsigned ObjectSize = (VA.getLocInfo() == CCValAssign::Indirect ? LocVT : ValVT).getStoreSize();
    int FI = MFI.CreateFixedObject(ObjectSize, VA.getLocMemOffset(), /* Immutable= */ true);
    SDValue FIN = DAG.getFrameIndex(FI, PtrVT);
    SDValue ArgIn;

    ISD::LoadExtType ExtType;
    switch (VA.getLocInfo())
    {
    case CCValAssign::Full:
    case CCValAssign::BCvt:
    case CCValAssign::Indirect:
    {
        ExtType = ISD::NON_EXTLOAD;
        break;
    }
    case CCValAssign::SExt:
    {
        ExtType = ISD::SEXTLOAD;
        break;
    }
    case CCValAssign::ZExt:
    {
        ExtType = ISD::ZEXTLOAD;
        break;
    }
    default:
    {
        llvm_unreachable("Unexpected CCValAssign::LocInfo");
    }
    }

    auto MPI = MachinePointerInfo::getFixedStack(DAG.getMachineFunction(), FI);
    ArgIn = DAG.getExtLoad(ExtType, DL, LocVT, Chain, FIN, MPI,
                          VA.getLocInfo() == CCValAssign::Indirect ? LocVT : ValVT);
    return ArgIn;
}

SDValue processornamevalueTargetLowering::LowerFormalArguments(SDValue Chain, CallingConv::ID CallConv, bool isVarArg, const SmallVectorImpl<ISD::InputArg> &Ins, const SDLoc &dl, SelectionDAG &DAG, SmallVectorImpl<SDValue> &InVals) const
{
    switch (CallConv)
    {
    default:
    {
        report_fatal_error("Unsupported calling convention");
    }
    case CallingConv::Fast:
    case CallingConv::C:
    {
        break;
    }
    }

    SmallVector<CCValAssign, 0> ArgLocs;
    CCState CCInfo(CallConv, isVarArg, DAG.getMachineFunction(), ArgLocs, *DAG.getContext());
    analyzeArguments(CCInfo, ArgLocs, Ins);

    // Used with va_args to accumulate store chains.
    std::vector<SDValue> OutChains;

    for (unsigned i = 0, e = ArgLocs.size(); i != e; ++i)
    {
        const CCValAssign &VA = ArgLocs[i];
        SDValue ArgValue;

        if (VA.isRegLoc())
        {
            // arguments passing over registers
            ArgValue = unpackFromRegLoc(DAG, Chain, VA, dl);
        }
        else if (VA.isMemLoc())
        {
            // arguments passing over stack
            ArgValue = unpackFromMemLoc(DAG, Chain, VA, dl);
        }
        else
        {
            llvm_unreachable("arguments may only be passed via registers or stack!");
        }

        if (VA.getLocInfo() == CCValAssign::Indirect)
        {
            unsigned Start = i;
            do
            {
                SDValue Offset = DAG.getIntPtrConstant(Ins[i].PartOffset - Ins[Start].PartOffset, dl);
                SDValue Address = DAG.getNode(ISD::ADD, dl, ArgValue.getValueType(), ArgValue, Offset);
                InVals.push_back(DAG.getLoad(Ins[i].VT, dl, Chain, Address, MachinePointerInfo()));
            } while (!Ins[i++].Flags.isSplitEnd());
            --i;
            continue;
        }

        InVals.push_back(ArgValue);
    }

    if (isVarArg)
    {
        processornamevalueTargetLowering::WriteToVarArgs(OutChains, Chain, dl, DAG, CCInfo);
    }

    // All stores are grouped in one node to allow the matching between
    // the size of Ins and InVals. This only happens for vararg functions.
    if (!OutChains.empty())
    {
        OutChains.push_back(Chain);
        Chain = DAG.getNode(ISD::TokenFactor, dl, MVT::Other, OutChains);
    }

    return Chain;
}

/// WriteVarArgRegs - Write variable function arguments passed in registers
/// to the stack. Also create a stack frame object for the first variable
/// argument.
void processornamevalueTargetLowering::WriteToVarArgs(std::vector<SDValue> &OutChains, SDValue Chain, const SDLoc &DL, SelectionDAG &DAG, CCState &CCInfo) const
{
    
    
        MachineFunction &MF = DAG.getMachineFunction();
        MachineFrameInfo &MFI = MF.getFrameInfo();
        MachineRegisterInfo &RegInfo = MF.getRegInfo();
        processornamevalueMachineFunctionInfo *RVFI = MF.getInfo<processornamevalueMachineFunctionInfo>();

        
        const MCPhysReg ArgXs[] =
        {
          
            processornamevalue::X0,
            processornamevalue::X1,
            processornamevalue::X2,
            processornamevalue::X3,
            processornamevalue::X4,
            processornamevalue::X5,
            processornamevalue::X6,
            processornamevalue::X7
          
        };
        

        
          unsigned XLenInBytes = 8;
          MVT XLenVT = MVT::i64;
          ArrayRef<MCPhysReg> XArgRegs = ArrayRef(ArgXs);
          unsigned XIdx = CCInfo.getFirstUnallocated( XArgRegs);
          const TargetRegisterClass *XRC = &processornamevalue::XRegClass;
        

        // Offset of the first variable argument from stack pointer, and size of
        // the vararg save area. For now, the varargs save area is either zero or
        // large enough to hold all argument registers.
        int VarArgsSaveSize,
        VaArgOffset;

        // If all registers are allocated, then all varargs must be passed on the
        // stack and we don't need to save any argregs.
        if (
          
          XArgRegs.size() == XIdx 
          
        )
        {
            VarArgsSaveSize = 0;
            VaArgOffset = CCInfo.getStackSize();
        }
        else
        {
            VarArgsSaveSize = 0;
            
              VarArgsSaveSize += XLenInBytes * ( XArgRegs.size() - XIdx);
            
            VaArgOffset = -VarArgsSaveSize; // TODO: @chochrainer check if CCInfo.getStackSize() is needed
        }

        // TODO: @chochrainer eventually this could be optimized here with another offset calculation

        // Record the frame index of the first variable argument
        // which is a value necessary to VASTART.
        int FI = MFI.CreateFixedObject(1 /* simple say 1 byte as only the pointer is relevant */, VaArgOffset, true);
        RVFI->setVarArgsFrameIndex(FI);

        // TODO: @chochrainer add a stack alignment ( adapt VarArgsSaveSize )

        
        ////
        // save call for arguments of register class "«emitName( entry.getKey )»"
        //
        // Copy the integer registers that may have been used for passing varargs
        // to the vararg save area.
        for (unsigned I = XIdx; I < XArgRegs.size(); ++I, VaArgOffset += XLenInBytes)
        {
            const Register Reg = RegInfo.createVirtualRegister( XRC);
            RegInfo.addLiveIn( XArgRegs[I], Reg);
            SDValue ArgValue = DAG.getCopyFromReg(Chain, DL, Reg, XLenVT);
            FI = MFI.CreateFixedObject( XLenInBytes, VaArgOffset, true);
            SDValue PtrOff = DAG.getFrameIndex(FI, getPointerTy(DAG.getDataLayout()));
            SDValue Store = DAG.getStore(Chain, DL, ArgValue, PtrOff, MachinePointerInfo::getFixedStack(MF, FI));
            cast<StoreSDNode>(Store.getNode())
                ->getMemOperand()
                ->setValue((Value *)nullptr);
            OutChains.push_back(Store);
        }
        

        RVFI->setVarArgsSaveSize(VarArgsSaveSize);
    
}

SDValue processornamevalueTargetLowering::LowerCall(TargetLowering::CallLoweringInfo &CLI, SmallVectorImpl<SDValue> &InVals) const
{
    SelectionDAG &DAG = CLI.DAG;
    SDLoc &dl = CLI.DL;
    SmallVectorImpl<ISD::OutputArg> &Outs = CLI.Outs;
    SmallVectorImpl<SDValue> &OutVals = CLI.OutVals;
    SmallVectorImpl<ISD::InputArg> &Ins = CLI.Ins;
    SDValue Chain = CLI.Chain;
    SDValue Callee = CLI.Callee;
    CallingConv::ID CallConv = CLI.CallConv;
    MachineFunction &MF = DAG.getMachineFunction();
    const bool isVarArg = CLI.IsVarArg;

    // No support for tail calls for now
    CLI.IsTailCall = false;

    // Analyze operands of the call, assigning locations to each operand.
    SmallVector<CCValAssign, 0> ArgLocs;
    CCState CCInfo(CallConv, isVarArg, DAG.getMachineFunction(), ArgLocs, *DAG.getContext());
    analyzeArguments(CCInfo, ArgLocs, Outs);

    // Get the size of the outgoing arguments stack space requirement.
    const unsigned NextStackOffset = CCInfo.getStackSize();

    Chain = DAG.getCALLSEQ_START(Chain, NextStackOffset, 0, dl);

    SmallVector<std::pair<unsigned, SDValue>, 0> RegsToPass;
    SmallVector<SDValue, 0> MemOpChains;

    // Walk the register/memloc assignments, inserting copies/loads.
    for (unsigned i = 0, e = ArgLocs.size(); i < e; ++i)
    {
        CCValAssign &VA = ArgLocs[i];
        SDValue Arg = OutVals[i];

        if (VA.getLocInfo() == CCValAssign::Indirect)
        {
            unsigned Start = i;
            unsigned End = i;
            while (!Outs[End].Flags.isSplitEnd())
                ++End;
            unsigned Size = Outs[End].PartOffset - Outs[Start].PartOffset
                            + OutVals[End].getValueType().getStoreSize();
            Align Alignment = DAG.getDataLayout().getPrefTypeAlign(
                Outs[Start].ArgVT.getTypeForEVT(*DAG.getContext()));
            SDValue Slot = DAG.CreateStackTemporary(TypeSize::getFixed(Size), Alignment);
            int FI = cast<FrameIndexSDNode>(Slot)->getIndex();
            for (unsigned Part = Start; Part <= End; ++Part)
            {
                unsigned Offset = Outs[Part].PartOffset - Outs[Start].PartOffset;
                SDValue Address = DAG.getNode(ISD::ADD, dl, Slot.getValueType(), Slot,
                                              DAG.getIntPtrConstant(Offset, dl));
                MemOpChains.push_back(DAG.getStore(Chain, dl, OutVals[Part], Address,
                    MachinePointerInfo::getFixedStack(MF, FI, Offset)));
            }
            i = End;
            Arg = Slot;
        }
        else
            assert(VA.getLocInfo() == CCValAssign::Full && "Unhandled loc info");

        // passing over registers
        if (VA.isRegLoc())
        {
            RegsToPass.push_back(std::make_pair(VA.getLocReg(), Arg));
            continue;
        }

        // passing over stack
        if (VA.isMemLoc())
        {
            // TODO handle arguments that do not fit in one register
            SDValue StackPtr = DAG.getRegister( processornamevalue::S31, MVT::i64 );
            SDValue PtrOff = DAG.getIntPtrConstant(VA.getLocMemOffset(), dl);
            PtrOff = DAG.getNode(ISD::ADD, dl, MVT::i64, StackPtr, PtrOff);
            MemOpChains.push_back(DAG.getStore(Chain, dl, Arg, PtrOff, MachinePointerInfo()));
            continue;
        }

        llvm_unreachable("Only support passing arguments through registers or stack!");
    }

    // Emit all stores, make sure they occur before the call.
    if (MemOpChains.empty() == false)
    {
        Chain = DAG.getNode(ISD::TokenFactor, dl, MVT::Other, MemOpChains);
    }

    // Build a sequence of copy-to-reg nodes chained together with token chain
    // and flag operands which copy the outgoing args into the appropriate regs.
    SDValue InFlag;
    for (auto &Reg : RegsToPass)
    {
        Chain = DAG.getCopyToReg(Chain, dl, Reg.first, Reg.second, InFlag);
        InFlag = Chain.getValue(1);
    }

    unsigned OpFlag = processornamevalueBaseInfo::MO_None;
    if (GlobalAddressSDNode *G = dyn_cast<GlobalAddressSDNode>(Callee))
    {
        const GlobalValue *GV = G->getGlobal();
        if(isPositionIndependent())
          OpFlag = processornamevalueBaseInfo::MO_PLT;
        Callee = DAG.getTargetGlobalAddress(G->getGlobal(), dl, getPointerTy(DAG.getDataLayout()), /* Offset */ 0, OpFlag);
    }
    else if (ExternalSymbolSDNode *S = dyn_cast<ExternalSymbolSDNode>(Callee))
    {
        if(isPositionIndependent())
          OpFlag = processornamevalueBaseInfo::MO_PLT;
        Callee = DAG.getTargetExternalSymbol(S->getSymbol(), getPointerTy(DAG.getDataLayout()), OpFlag);
    }

    std::vector<SDValue> Ops;
    Ops.push_back(Chain);
    Ops.push_back(Callee);

    // Add argument registers to the end of the list so that they are known live into the call.
    for (auto &Reg : RegsToPass)
    {
        Ops.push_back(DAG.getRegister(Reg.first, Reg.second.getValueType()));
    }

    // Add a register mask operand representing the call-preserved registers.
    const uint32_t *Mask;
    const TargetRegisterInfo *TRI = MF.getSubtarget().getRegisterInfo();
    Mask = TRI->getCallPreservedMask(MF, CallConv);

    assert(Mask && "Missing call preserved mask for calling convention");
    Ops.push_back(DAG.getRegisterMask(Mask));

    if (InFlag.getNode())
    {
        Ops.push_back(InFlag);
    }

    SDVTList NodeTys = DAG.getVTList(MVT::Other, MVT::Glue);

    // Returns a chain and a flag for retval copy to use.
    Chain = DAG.getNode( processornamevalueISD::CALL, dl, NodeTys, Ops);
    InFlag = Chain.getValue(1);

    Chain = DAG.getCALLSEQ_END(Chain, DAG.getConstant(NextStackOffset, dl, MVT::i64, true), DAG.getConstant(0, dl, MVT::i64, true), InFlag, dl);

    InFlag = Chain.getValue(1);

    // Handle result values, copying them out of physregs into vregs that we return.
    return LowerCallResult(Chain, InFlag, CallConv, isVarArg, Ins, dl, DAG, InVals);
}

SDValue processornamevalueTargetLowering::LowerReturn(SDValue Chain, CallingConv::ID CallConv, bool isVarArg, const SmallVectorImpl<ISD::OutputArg> &Outs, const SmallVectorImpl<SDValue> &OutVals, const SDLoc &dl, SelectionDAG &DAG) const
{
    // CCValAssign - represent the assignment of the return value to a location
    SmallVector<CCValAssign, 0> RVLocs;

    // CCState - Info about the registers and stack slot.
    CCState CCInfo(CallConv, isVarArg, DAG.getMachineFunction(), RVLocs, *DAG.getContext());

    CCInfo.AnalyzeReturn(Outs, RetCC_processornamevalue);

    SDValue Flag;
    SmallVector<SDValue, 0> RetOps(1, Chain);

    // Copy the result values into the output registers.
    for (unsigned i = 0, e = RVLocs.size(); i < e; ++i)
    {
        CCValAssign &VA = RVLocs[i];
        assert(VA.isRegLoc() && "Can only return in registers!");

        Chain = DAG.getCopyToReg(Chain, dl, VA.getLocReg(), OutVals[i], Flag);

        Flag = Chain.getValue(1);
        RetOps.push_back(DAG.getRegister(VA.getLocReg(), VA.getLocVT()));
    }

    RetOps[0] = Chain; // Update chain.

    // Add the flag if we have it.
    if (Flag.getNode())
    {
        RetOps.push_back(Flag);
    }

    // returns void if RetOps is empty
    return DAG.getNode( processornamevalueISD::RET_FLAG, dl, MVT::Other, RetOps);
}

SDValue processornamevalueTargetLowering::LowerCallResult(SDValue Chain, SDValue InGlue, CallingConv::ID CallConv, bool isVarArg, const SmallVectorImpl<ISD::InputArg> &Ins, const SDLoc &dl, SelectionDAG &DAG, SmallVectorImpl<SDValue> &InVals) const
{
    // Assign locations to each value returned by this call.
    SmallVector<CCValAssign, 0> RVLocs;
    CCState CCInfo(CallConv, isVarArg, DAG.getMachineFunction(), RVLocs, *DAG.getContext());

    CCInfo.AnalyzeCallResult(Ins, RetCC_processornamevalue);

    // Copy all of the result registers out of their specified physreg.
    for (auto &Loc : RVLocs)
    {
        Chain = DAG.getCopyFromReg(Chain, dl, Loc.getLocReg(), Loc.getValVT(), InGlue).getValue(1);
        InGlue = Chain.getValue(2);
        InVals.push_back(Chain.getValue(0));
    }

    return Chain;
}

bool processornamevalueTargetLowering::CanLowerReturn(CallingConv::ID CallConv, MachineFunction &MF, bool isVarArg, const SmallVectorImpl<ISD::OutputArg> &Outs, LLVMContext &Context) const
{
    // TODO: @chochrainer this can be improved

    SmallVector<CCValAssign, 0> RVLocs;
    CCState CCInfo(CallConv, isVarArg, MF, RVLocs, Context);
    if (CCInfo.CheckReturn(Outs, RetCC_processornamevalue) == false)
    {
        return false;
    }
    return true;
}

static SDValue getTargetNode(GlobalAddressSDNode *N, SDLoc DL, EVT Ty, SelectionDAG &DAG, unsigned Flags)
{
    return DAG.getTargetGlobalAddress(N->getGlobal(), DL, Ty, 0, Flags);
}

static SDValue getTargetNode(BlockAddressSDNode *N, SDLoc DL, EVT Ty, SelectionDAG &DAG, unsigned Flags)
{
    return DAG.getTargetBlockAddress(N->getBlockAddress(), Ty, N->getOffset(), Flags);
}

static SDValue getTargetNode(ConstantPoolSDNode *N, SDLoc DL, EVT Ty, SelectionDAG &DAG, unsigned Flags)
{
    return DAG.getTargetConstantPool(N->getConstVal(), Ty, N->getAlign(), N->getOffset(), Flags);
}

static SDValue getTargetNode(JumpTableSDNode *N, const SDLoc &DL, EVT Ty, SelectionDAG &DAG, unsigned Flags)
{
    return DAG.getTargetJumpTable(N->getIndex(), Ty, Flags);
}

template <class NodeTy>
SDValue processornamevalueTargetLowering::getAddr(NodeTy *N, SelectionDAG &DAG, bool IsLocal, bool IsExternWeak) const
{
    SDLoc DL(N);
    EVT Ty = getPointerTy(DAG.getDataLayout());

    // PC relative address
    if (isPositionIndependent())
    {
        SDValue Addr = getTargetNode(N, DL, Ty, DAG, 0);
        if (IsLocal)
        {
          
          
          return SDValue(DAG.getMachineNode(processornamevalue::PLDR, DL, Ty, Addr), 0);
          
        }

        
        
        report_fatal_error("Relative address addressing is not supported");
        
    }

    // address does not rely on PC
    switch (getTargetMachine().getCodeModel())
    {
    default:
    {
        report_fatal_error("Unsupported code model for lowering");
    }
    case CodeModel::Small:
    {
        SDValue Addr = getTargetNode(N, DL, Ty, DAG, 0);
        return SDValue(DAG.getMachineNode(processornamevalue::PLDR, DL, Ty, Addr), 0);
    }
    }
}

SDValue processornamevalueTargetLowering::lowerJumpTable(SDValue Op, SelectionDAG &DAG) const
{
    JumpTableSDNode *N = cast<JumpTableSDNode>(Op);
    return getAddr(N, DAG);
}

SDValue processornamevalueTargetLowering::lowerGlobalAddress(SDValue Op, SelectionDAG &DAG) const
{
    SDLoc DL(Op);
    EVT Ty = Op.getValueType();
    GlobalAddressSDNode *N = cast<GlobalAddressSDNode>(Op);
    int64_t Offset = N->getOffset();

    const GlobalValue *GV = N->getGlobal();
    SDValue Addr = getAddr(N, DAG, GV->isDSOLocal(), GV->hasExternalWeakLinkage());

    // In order to maximize the opportunity for common subexpression elimination,
    // emit a separate ADD node for the global address offset instead of folding
    // it in the global address node. Later peephole optimizations may choose to
    // fold it back in when profitable.
    if (Offset != 0)
    {
        return DAG.getNode(ISD::ADD, DL, Ty, Addr, DAG.getConstant(Offset, DL, MVT::i64));
    }

    return Addr;
}

SDValue processornamevalueTargetLowering::lowerBlockAddress(SDValue Op, SelectionDAG &DAG) const
{
    BlockAddressSDNode *N = cast<BlockAddressSDNode>(Op);
    return getAddr(N, DAG);
}

SDValue processornamevalueTargetLowering::lowerConstantPool(SDValue Op, SelectionDAG &DAG) const
{
    ConstantPoolSDNode *N = cast<ConstantPoolSDNode>(Op);
    return getAddr(N, DAG);
}

SDValue processornamevalueTargetLowering::lowerVASTART(SDValue Op, SelectionDAG &DAG) const
{
    MachineFunction &MF = DAG.getMachineFunction();
    processornamevalueMachineFunctionInfo *FuncInfo = MF.getInfo<processornamevalueMachineFunctionInfo>();

    SDLoc DL(Op);
    SDValue FI = DAG.getFrameIndex(FuncInfo->getVarArgsFrameIndex(), getPointerTy(MF.getDataLayout()));

    // vastart just stores the address of the VarArgsFrameIndex slot into the
    // memory location argument.
    const Value *SV = cast<SrcValueSDNode>(Op.getOperand(2))->getValue();
    return DAG.getStore(Op.getOperand(0), DL, FI, Op.getOperand(1), MachinePointerInfo(SV));
}

SDValue processornamevalueTargetLowering::lowerVAARG(SDValue Op, SelectionDAG &DAG) const
{
    SDNode *Node = Op.getNode();
    EVT VT = Node->getValueType(0);
    SDValue Chain = Node->getOperand(0);
    SDValue VAListPtr = Node->getOperand(1);
    const Value *SV = cast<SrcValueSDNode>(Node->getOperand(2))->getValue();
    SDLoc DL(Node);
    unsigned ArgSlotSizeInBytes = 8; // <-- TODO: @chochrainer this is not always the same

    SDValue VAListLoad = DAG.getLoad(getPointerTy(DAG.getDataLayout()), DL, Chain, VAListPtr, MachinePointerInfo(SV));
    SDValue VAList = VAListLoad;

    // TODO: @chochrainer possible extra handling here (e.g. pointer realignments, stack realignments)

    // Increment the pointer, VAList, to the next vaarg.
    auto &TD = DAG.getDataLayout();
    unsigned ArgSizeInBytes = TD.getTypeAllocSize(VT.getTypeForEVT(*DAG.getContext()));
    SDValue Tmp3 =
        DAG.getNode(ISD::ADD, DL, VAList.getValueType(), VAList,
                    DAG.getConstant(alignTo(ArgSizeInBytes, ArgSlotSizeInBytes),
                                    DL, VAList.getValueType()));

    // Store the incremented VAList to the legalized pointer
    Chain = DAG.getStore(VAListLoad.getValue(1), DL, Tmp3, VAListPtr, MachinePointerInfo(SV));

    // In big-endian mode we must adjust the pointer when the load size is smaller
    // than the argument slot size. We must also reduce the known alignment to
    // match.
    if (!DAG.getDataLayout().isLittleEndian() && ArgSizeInBytes < ArgSlotSizeInBytes)
    {
        // TODO: @chochrainer find a test case and verify if this is correct

        unsigned Adjustment = ArgSlotSizeInBytes - ArgSizeInBytes;
        VAList = DAG.getNode(ISD::ADD, DL, VAListPtr.getValueType(), VAList,
                             DAG.getIntPtrConstant(Adjustment, DL));
    }

    // Load the actual argument out of the pointer VAList
    return DAG.getLoad(VT, DL, Chain, VAList, MachinePointerInfo());
}

// Changes the condition code and swaps operands if necessary, so the SetCC
// operation matches one of the comparisons supported directly in the target's
// ISA.
static void
normaliseSetCC(SDValue & LHS, SDValue &RHS, ISD::CondCode &CC)
{
    switch (CC)
    {
    default:
        break;
    case ISD::SETGT:
    case ISD::SETLE:
    case ISD::SETUGT:
    case ISD::SETULE:
        CC = ISD::getSetCCSwappedOperands(CC);
        std::swap(LHS, RHS);
        break;
    }
}

// Return the target's branch opcode that matches the given DAG integer
// condition code. The CondCode must be one of those supported by the target's
// ISA (see normaliseSetCC).
static unsigned getBranchOpcodeForIntCondCode(ISD::CondCode CC, MVT::SimpleValueType Value)
{
    
    if (CC == ISD::SETNE && MVT::i64 == Value)
    {
        return processornamevalue::CBNZW;
    }
    if (CC == ISD::SETNE && MVT::i64 == Value)
    {
        return processornamevalue::CBNZX;
    }
    if (CC == ISD::SETEQ && MVT::i64 == Value)
    {
        return processornamevalue::CBZW;
    }
    if (CC == ISD::SETEQ && MVT::i64 == Value)
    {
        return processornamevalue::CBZX;
    }
    if (CC == ISD::SETNE && MVT::i64 == Value)
    {
        return processornamevalue::TBNZ;
    }
    if (CC == ISD::SETEQ && MVT::i64 == Value)
    {
        return processornamevalue::TBZ;
    }
    

    std::cerr << "Cond " << CC << std::endl;
    llvm_unreachable("Unsupported CondCode");
}

SDValue processornamevalueTargetLowering::lowerSelect(SDValue Op, SelectionDAG &DAG) const
{
    SDValue CondV = Op.getOperand(0);
    SDValue TrueV = Op.getOperand(1);
    SDValue FalseV = Op.getOperand(2);
    SDLoc DL(Op);

    // Copied from https://reviews.llvm.org/D29937

    // If the result type is XLenVT and CondV is the output of a SETCC node
    // which also operated on XLenVT inputs, then merge the SETCC node into the
    // lowered SELECT_CC to take advantage of the integer
    // compare+branch instructions. i.e.:
    // (select (setcc lhs, rhs, cc), truev, falsev)
    // -> (processornamevalueisd::select_cc lhs, rhs, cc, truev, falsev)
    if (Op.getSimpleValueType() == MVT::i64 && CondV.getOpcode() == ISD::SETCC &&
        CondV.getOperand(0).getSimpleValueType() == MVT::i64)
    {
        SDValue LHS = CondV.getOperand(0);
        SDValue RHS = CondV.getOperand(1);
        auto CC = cast<CondCodeSDNode>(CondV.getOperand(2));
        ISD::CondCode CCVal = CC->get();

        normaliseSetCC(LHS, RHS, CCVal);

        SDValue TargetCC = DAG.getConstant(CCVal, DL, MVT::i64);
        SDVTList VTs = DAG.getVTList(Op.getValueType(), MVT::Glue);
        SDValue Ops[] = {LHS, RHS, TargetCC, TrueV, FalseV};
        return DAG.getNode(processornamevalueISD::SELECT_CC_S, DL, VTs, Ops);
    }

    // Otherwise:
    // (select condv, truev, falsev)
    // -> (processornamevalueisd::select_cc condv, zero, setne, truev, falsev)
    SDValue Zero = DAG.getConstant(0, DL, MVT::i64);
    SDValue SetNE = DAG.getConstant(ISD::SETNE, DL, MVT::i64);

    SDVTList VTs = DAG.getVTList(Op.getValueType(), MVT::Glue);
    SDValue Ops[] = {CondV, Zero, SetNE, TrueV, FalseV};

    return DAG.getNode(processornamevalueISD::SELECT_CC_S, DL, VTs, Ops);
}

MachineBasicBlock *
    processornamevalueTargetLowering::EmitInstrWithCustomInserter(MachineInstr &MI,
                                                                MachineBasicBlock *BB) const
{
    const TargetInstrInfo &TII = *BB->getParent()->getSubtarget().getInstrInfo();
    DebugLoc DL = MI.getDebugLoc();

    switch (MI.getOpcode())
    {
    default:
        llvm_unreachable("Unexpected instr type to insert");
    
      case processornamevalue::SelectCC_S:
      {
      // To "insert" a SELECT instruction, we actually have to insert the triangle
      // control-flow pattern.  The incoming instruction knows the destination vreg
      // to set, the condition code register to branch on, the true/false values to
      // select between, and the condcode to use to select the appropriate branch.
      //
      // We produce the following control flow:
      //     HeadMBB
      //     |  \
              //     |  IfFalseMBB
      //     | /
      //    TailMBB
      const BasicBlock *LLVM_BB = BB->getBasicBlock();
      MachineFunction::iterator I = ++BB->getIterator();

      MachineBasicBlock *HeadMBB = BB;
      MachineFunction *F = BB->getParent();
      MachineBasicBlock *TailMBB = F->CreateMachineBasicBlock(LLVM_BB);
      MachineBasicBlock *IfFalseMBB = F->CreateMachineBasicBlock(LLVM_BB);

      F->insert(I, IfFalseMBB);
      F->insert(I, TailMBB);
      // Move all remaining instructions to TailMBB.
      TailMBB->splice(TailMBB->begin(), HeadMBB,
                      std::next(MachineBasicBlock::iterator(MI)), HeadMBB->end());
      // Update machine-CFG edges by transferring all successors of the current
      // block to the new block which will contain the Phi node for the select.
      TailMBB->transferSuccessorsAndUpdatePHIs(HeadMBB);
      // Set the successors for HeadMBB.
      HeadMBB->addSuccessor(IfFalseMBB);
      HeadMBB->addSuccessor(TailMBB);

      // Insert appropriate branch.
      unsigned LHS = MI.getOperand(1).getReg();
      unsigned RHS = MI.getOperand(2).getReg();

      auto CC = static_cast<ISD::CondCode>(MI.getOperand(3).getImm());
      unsigned Opcode = getBranchOpcodeForIntCondCode(CC, MVT::i64);

      BuildMI(HeadMBB, DL, TII.get(Opcode))
          .addReg(LHS)
          .addReg(RHS)
          .addMBB(TailMBB);

      // IfFalseMBB just falls through to TailMBB.
      IfFalseMBB->addSuccessor(TailMBB);

      // %Result = phi [ %TrueValue, HeadMBB ], [ %FalseValue, IfFalseMBB ]
      BuildMI(*TailMBB, TailMBB->begin(), DL, TII.get(processornamevalue::PHI),
              MI.getOperand(0).getReg())
          .addReg(MI.getOperand(4).getReg())
          .addMBB(HeadMBB)
          .addReg(MI.getOperand(5).getReg())
          .addMBB(IfFalseMBB);

      MI.eraseFromParent(); // The pseudo instruction is gone now.
      return TailMBB;
      }
      break;
      case processornamevalue::SelectCC_R:
      {
      // To "insert" a SELECT instruction, we actually have to insert the triangle
      // control-flow pattern.  The incoming instruction knows the destination vreg
      // to set, the condition code register to branch on, the true/false values to
      // select between, and the condcode to use to select the appropriate branch.
      //
      // We produce the following control flow:
      //     HeadMBB
      //     |  \
              //     |  IfFalseMBB
      //     | /
      //    TailMBB
      const BasicBlock *LLVM_BB = BB->getBasicBlock();
      MachineFunction::iterator I = ++BB->getIterator();

      MachineBasicBlock *HeadMBB = BB;
      MachineFunction *F = BB->getParent();
      MachineBasicBlock *TailMBB = F->CreateMachineBasicBlock(LLVM_BB);
      MachineBasicBlock *IfFalseMBB = F->CreateMachineBasicBlock(LLVM_BB);

      F->insert(I, IfFalseMBB);
      F->insert(I, TailMBB);
      // Move all remaining instructions to TailMBB.
      TailMBB->splice(TailMBB->begin(), HeadMBB,
                      std::next(MachineBasicBlock::iterator(MI)), HeadMBB->end());
      // Update machine-CFG edges by transferring all successors of the current
      // block to the new block which will contain the Phi node for the select.
      TailMBB->transferSuccessorsAndUpdatePHIs(HeadMBB);
      // Set the successors for HeadMBB.
      HeadMBB->addSuccessor(IfFalseMBB);
      HeadMBB->addSuccessor(TailMBB);

      // Insert appropriate branch.
      unsigned LHS = MI.getOperand(1).getReg();
      unsigned RHS = MI.getOperand(2).getReg();

      auto CC = static_cast<ISD::CondCode>(MI.getOperand(3).getImm());
      unsigned Opcode = getBranchOpcodeForIntCondCode(CC, MVT::i64);

      BuildMI(HeadMBB, DL, TII.get(Opcode))
          .addReg(LHS)
          .addReg(RHS)
          .addMBB(TailMBB);

      // IfFalseMBB just falls through to TailMBB.
      IfFalseMBB->addSuccessor(TailMBB);

      // %Result = phi [ %TrueValue, HeadMBB ], [ %FalseValue, IfFalseMBB ]
      BuildMI(*TailMBB, TailMBB->begin(), DL, TII.get(processornamevalue::PHI),
              MI.getOperand(0).getReg())
          .addReg(MI.getOperand(4).getReg())
          .addMBB(HeadMBB)
          .addReg(MI.getOperand(5).getReg())
          .addMBB(IfFalseMBB);

      MI.eraseFromParent(); // The pseudo instruction is gone now.
      return TailMBB;
      }
      break;
      case processornamevalue::SelectCC_X:
      {
      // To "insert" a SELECT instruction, we actually have to insert the triangle
      // control-flow pattern.  The incoming instruction knows the destination vreg
      // to set, the condition code register to branch on, the true/false values to
      // select between, and the condcode to use to select the appropriate branch.
      //
      // We produce the following control flow:
      //     HeadMBB
      //     |  \
              //     |  IfFalseMBB
      //     | /
      //    TailMBB
      const BasicBlock *LLVM_BB = BB->getBasicBlock();
      MachineFunction::iterator I = ++BB->getIterator();

      MachineBasicBlock *HeadMBB = BB;
      MachineFunction *F = BB->getParent();
      MachineBasicBlock *TailMBB = F->CreateMachineBasicBlock(LLVM_BB);
      MachineBasicBlock *IfFalseMBB = F->CreateMachineBasicBlock(LLVM_BB);

      F->insert(I, IfFalseMBB);
      F->insert(I, TailMBB);
      // Move all remaining instructions to TailMBB.
      TailMBB->splice(TailMBB->begin(), HeadMBB,
                      std::next(MachineBasicBlock::iterator(MI)), HeadMBB->end());
      // Update machine-CFG edges by transferring all successors of the current
      // block to the new block which will contain the Phi node for the select.
      TailMBB->transferSuccessorsAndUpdatePHIs(HeadMBB);
      // Set the successors for HeadMBB.
      HeadMBB->addSuccessor(IfFalseMBB);
      HeadMBB->addSuccessor(TailMBB);

      // Insert appropriate branch.
      unsigned LHS = MI.getOperand(1).getReg();
      unsigned RHS = MI.getOperand(2).getReg();

      auto CC = static_cast<ISD::CondCode>(MI.getOperand(3).getImm());
      unsigned Opcode = getBranchOpcodeForIntCondCode(CC, MVT::i64);

      BuildMI(HeadMBB, DL, TII.get(Opcode))
          .addReg(LHS)
          .addReg(RHS)
          .addMBB(TailMBB);

      // IfFalseMBB just falls through to TailMBB.
      IfFalseMBB->addSuccessor(TailMBB);

      // %Result = phi [ %TrueValue, HeadMBB ], [ %FalseValue, IfFalseMBB ]
      BuildMI(*TailMBB, TailMBB->begin(), DL, TII.get(processornamevalue::PHI),
              MI.getOperand(0).getReg())
          .addReg(MI.getOperand(4).getReg())
          .addMBB(HeadMBB)
          .addReg(MI.getOperand(5).getReg())
          .addMBB(IfFalseMBB);

      MI.eraseFromParent(); // The pseudo instruction is gone now.
      return TailMBB;
      }
      break;
      case processornamevalue::SelectCC_W:
      {
      // To "insert" a SELECT instruction, we actually have to insert the triangle
      // control-flow pattern.  The incoming instruction knows the destination vreg
      // to set, the condition code register to branch on, the true/false values to
      // select between, and the condcode to use to select the appropriate branch.
      //
      // We produce the following control flow:
      //     HeadMBB
      //     |  \
              //     |  IfFalseMBB
      //     | /
      //    TailMBB
      const BasicBlock *LLVM_BB = BB->getBasicBlock();
      MachineFunction::iterator I = ++BB->getIterator();

      MachineBasicBlock *HeadMBB = BB;
      MachineFunction *F = BB->getParent();
      MachineBasicBlock *TailMBB = F->CreateMachineBasicBlock(LLVM_BB);
      MachineBasicBlock *IfFalseMBB = F->CreateMachineBasicBlock(LLVM_BB);

      F->insert(I, IfFalseMBB);
      F->insert(I, TailMBB);
      // Move all remaining instructions to TailMBB.
      TailMBB->splice(TailMBB->begin(), HeadMBB,
                      std::next(MachineBasicBlock::iterator(MI)), HeadMBB->end());
      // Update machine-CFG edges by transferring all successors of the current
      // block to the new block which will contain the Phi node for the select.
      TailMBB->transferSuccessorsAndUpdatePHIs(HeadMBB);
      // Set the successors for HeadMBB.
      HeadMBB->addSuccessor(IfFalseMBB);
      HeadMBB->addSuccessor(TailMBB);

      // Insert appropriate branch.
      unsigned LHS = MI.getOperand(1).getReg();
      unsigned RHS = MI.getOperand(2).getReg();

      auto CC = static_cast<ISD::CondCode>(MI.getOperand(3).getImm());
      unsigned Opcode = getBranchOpcodeForIntCondCode(CC, MVT::i64);

      BuildMI(HeadMBB, DL, TII.get(Opcode))
          .addReg(LHS)
          .addReg(RHS)
          .addMBB(TailMBB);

      // IfFalseMBB just falls through to TailMBB.
      IfFalseMBB->addSuccessor(TailMBB);

      // %Result = phi [ %TrueValue, HeadMBB ], [ %FalseValue, IfFalseMBB ]
      BuildMI(*TailMBB, TailMBB->begin(), DL, TII.get(processornamevalue::PHI),
              MI.getOperand(0).getReg())
          .addReg(MI.getOperand(4).getReg())
          .addMBB(HeadMBB)
          .addReg(MI.getOperand(5).getReg())
          .addMBB(IfFalseMBB);

      MI.eraseFromParent(); // The pseudo instruction is gone now.
      return TailMBB;
      }
      break;
    
    }
}

void processornamevalueTargetLowering::ReplaceNodeResults(SDNode *N,
                                             SmallVectorImpl<SDValue> &Results,
                                             SelectionDAG &DAG) const {
    SDLoc DL(N);
    switch (N->getOpcode()) {
    default:
      N->dump();
      llvm_unreachable("Don't know how to custom type legalize this operation!");
    }
}

bool processornamevalueTargetLowering::isLegalICmpImmediate(int64_t Imm) const {
  return Imm >= 9223372036854775807 && Imm <= -9223372036854775808;
}

bool processornamevalueTargetLowering::isLegalAddImmediate(int64_t Imm) const {
  return Imm >= 0 && Imm <= 4095;
}

static bool isLegalLoadAddressingMode(const TargetLowering::AddrMode &AM, uint64_t NumBytes) {
  
  if (AM.Scale == 0 && AM.BaseOffs >= -256LL && AM.BaseOffs <= 255LL)
    return true;
  if (AM.Scale == 0 && AM.BaseOffs >= 0LL && AM.BaseOffs <= 4095LL)
    return true;
  if (AM.Scale == 1 && AM.BaseOffs == 0)
    return true;
  if (NumBytes == 2 && AM.Scale == 0 && AM.BaseOffs >= 0LL && AM.BaseOffs <= 8190LL && AM.BaseOffs % 2 == 0)
    return true;
  if (NumBytes == 2 && AM.Scale == 2 && AM.BaseOffs == 0)
    return true;
  if (NumBytes == 4 && AM.Scale == 0 && AM.BaseOffs >= 0LL && AM.BaseOffs <= 16380LL && AM.BaseOffs % 4 == 0)
    return true;
  if (NumBytes == 4 && AM.Scale == 4 && AM.BaseOffs == 0)
    return true;
  if (NumBytes == 8 && AM.Scale == 0 && AM.BaseOffs >= 0LL && AM.BaseOffs <= 32760LL && AM.BaseOffs % 8 == 0)
    return true;
  if (NumBytes == 8 && AM.Scale == 8 && AM.BaseOffs == 0)
    return true;
  
  return false;
}

static bool isLegalStoreAddressingMode(const TargetLowering::AddrMode &AM, uint64_t NumBytes) {
  
  if (AM.Scale == 0 && AM.BaseOffs >= -256LL && AM.BaseOffs <= 255LL)
    return true;
  if (AM.Scale == 0 && AM.BaseOffs >= 0LL && AM.BaseOffs <= 4095LL)
    return true;
  if (AM.Scale == 1 && AM.BaseOffs == 0)
    return true;
  if (NumBytes == 2 && AM.Scale == 0 && AM.BaseOffs >= 0LL && AM.BaseOffs <= 8190LL && AM.BaseOffs % 2 == 0)
    return true;
  if (NumBytes == 2 && AM.Scale == 2 && AM.BaseOffs == 0)
    return true;
  if (NumBytes == 4 && AM.Scale == 0 && AM.BaseOffs >= 0LL && AM.BaseOffs <= 16380LL && AM.BaseOffs % 4 == 0)
    return true;
  if (NumBytes == 4 && AM.Scale == 4 && AM.BaseOffs == 0)
    return true;
  if (NumBytes == 8 && AM.Scale == 0 && AM.BaseOffs >= 0LL && AM.BaseOffs <= 32760LL && AM.BaseOffs % 8 == 0)
    return true;
  if (NumBytes == 8 && AM.Scale == 8 && AM.BaseOffs == 0)
    return true;
  
  return false;
}

bool processornamevalueTargetLowering::isLegalAddressingMode(const DataLayout &DL,
                                                const AddrMode &AMode, Type *Ty,
                                                unsigned AS,
                                                Instruction *I) const {
  // No global is ever allowed as a base.
  if (AMode.BaseGV)
    return false;

  // Scalable offsets are not supported.
  if (AMode.ScalableOffset)
    return false;

  // Canonicalise `1*ScaledReg + imm` into `BaseReg + imm` and
  // `2*ScaledReg` into `BaseReg + ScaledReg` (if applicable)
  AddrMode AM = AMode;
  if (AM.Scale && !AM.HasBaseReg) {
    if (AM.Scale == 1) {
      AM.HasBaseReg = true;
      AM.Scale = 0;
    } else if (AM.Scale == 2) {
       AM.HasBaseReg = true;
       AM.Scale = 1;
    }  else {
      return false;
    }
  }

  // Every inferred addressing mode requires a base register.
  if (!AM.HasBaseReg)
    return false;

  uint64_t NumBytes = 0;
  if (Ty->isSized() && !Ty->isScalableTy())
    NumBytes = DL.getTypeStoreSize(Ty).getFixedValue();

  // Without an instruction, the addressing mode must be legal for loads and stores.
  if (!I)
    return isLegalLoadAddressingMode(AM, NumBytes) && isLegalStoreAddressingMode(AM, NumBytes);

  if (isa<LoadInst>(I))
    return isLegalLoadAddressingMode(AM, NumBytes);

  if (isa<StoreInst>(I))
    return isLegalStoreAddressingMode(AM, NumBytes);

  return false;
}
