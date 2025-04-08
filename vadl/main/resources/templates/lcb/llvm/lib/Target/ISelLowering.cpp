#include "[(${namespace})]ISelLowering.h"
#include "[(${namespace})].h"
#include "Utils/[(${namespace})]BaseInfo.h"
#include "[(${namespace})]RegisterInfo.h"
#include "[(${namespace})]SubTarget.h"
#include "[(${namespace})]MachineFunctionInfo.h"
#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "llvm/CodeGen/MachineValueType.h"
#include "llvm/CodeGen/CallingConvLower.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Support/Debug.h"
#include <iostream>

#define DEBUG_TYPE "[(${namespace})]TargetLowering"

using namespace llvm;

#include "[(${namespace})]GenCallingConv.inc"

void [(${namespace})]TargetLowering::anchor() {}

[(${namespace})]TargetLowering::[(${namespace})]TargetLowering(const TargetMachine &TM, [(${namespace})]Subtarget &STI)
    : TargetLowering(TM), Subtarget(STI)
{
    // Set up the register classes defined by register files
    [# th:each="rg : ${registerFiles}" ] [# th:each="ty : ${rg.regTypes}" ]
      addRegisterClass(MVT::[(${ty.getLlvmType})], &[(${namespace})]::[(${rg.name})]RegClass);
    [/] [/]

    setStackPointerRegisterToSaveRestore([(${namespace})]::[(${stackPointer})]);

    setOperationAction(ISD::GlobalAddress, MVT::[(${stackPointerType})], Custom);
    setOperationAction(ISD::BlockAddress, MVT::[(${stackPointerType})], Custom);
    setOperationAction(ISD::ConstantPool, MVT::[(${stackPointerType})], Custom);
    setOperationAction(ISD::JumpTable, MVT::[(${stackPointerType})], Custom);

    setOperationAction(ISD::VASTART, MVT::Other, Custom);
    setOperationAction(ISD::VAARG, MVT::Other, Custom);
    setOperationAction(ISD::VACOPY, MVT::Other, Expand);
    setOperationAction(ISD::VAEND, MVT::Other, Expand);
    [#th:block th:if="${!hasCMove32 && stackPointerBitWidth == 32}"]
    setOperationAction(ISD::SELECT, MVT::i32, Custom);
    [/th:block]
    [#th:block th:if="${!hasCMove64 && stackPointerBitWidth == 64}"]
    setOperationAction(ISD::SELECT, MVT::i64, Custom);
    [/th:block]
    setOperationAction(ISD::SELECT_CC, MVT::[(${stackPointerType})], Expand);
    setOperationAction(ISD::SMUL_LOHI, MVT::i32, Expand);
    setOperationAction(ISD::UMUL_LOHI, MVT::i32, Expand);
    for (auto VT : {MVT::i1, MVT::i8, MVT::i16, MVT::i32}) {
        setOperationAction(ISD::SIGN_EXTEND_INREG, VT, Expand);
    }
    setOperationAction(ISD::BR_JT, MVT::Other, Expand);
    setOperationAction(ISD::DYNAMIC_STACKALLOC, MVT::i32, Expand);
    setOperationAction(ISD::STACKSAVE, MVT::Other, Expand);
    setOperationAction(ISD::STACKRESTORE, MVT::Other, Expand);

    [# th:each="x : ${expandableDagNodes}" ]
    setOperationAction(ISD::[(${x.isdName})], MVT::[(${x.mvt.value})], Expand);
    [/]

    for (auto N : {ISD::EXTLOAD, ISD::SEXTLOAD, ISD::ZEXTLOAD}) {
        setLoadExtAction(N, MVT::[(${stackPointerType})], MVT::i1, Promote);
    }

    setBooleanContents(ZeroOrOneBooleanContent);

    // Compute derived properties from the register classes
    computeRegisterProperties(STI.getRegisterInfo());
}

const char *[(${namespace})]TargetLowering::getTargetNodeName(unsigned Opcode) const
{
    switch (Opcode)
    {
    case [(${namespace})]ISD::RET_FLAG:
        return "[(${namespace})]ISD::RetFlag";
    case [(${namespace})]ISD::CALL:
        return "[(${namespace})]ISD::CALL";
    case [(${namespace})]ISD::SELECT_CC:
        return "[(${namespace})]ISD::SELECT_CC";
    case [(${namespace})]ISD::HI:
            return "[(${namespace})]ISD::LGA";
    default:
        llvm_unreachable("unknown opcode");
    }
}

SDValue [(${namespace})]TargetLowering::LowerOperation(SDValue Op, SelectionDAG &DAG) const
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
    case ISD::VASTART:
        return lowerVASTART(Op, DAG);
    case ISD::VAARG:
        return lowerVAARG(Op, DAG);
    [#th:block th:if="${!hasConditionalMove}"]
    case ISD::SELECT:
        return lowerSelect(Op, DAG);
    [/th:block]
    default : llvm_unreachable("unimplemented operand");
    }
}

static SDValue unpackFromRegLoc(SelectionDAG &DAG, SDValue Chain, const CCValAssign &VA, const SDLoc &DL)
{
    MachineFunction &MF = DAG.getMachineFunction();
    MachineRegisterInfo &RegInfo = MF.getRegInfo();

    EVT RegVT = VA.getLocVT();

    [# th:each="rg : ${registerFiles}" ]
    [# th:each="ty : ${rg.regTypes}" ]
      if(RegVT.getSimpleVT().SimpleTy == MVT::[(${ty.llvmType})])  {
        const unsigned VReg = RegInfo.createVirtualRegister(&[(${namespace})]::[(${rg.name})]RegClass);
        RegInfo.addLiveIn(VA.getLocReg(), VReg);
        SDValue ArgIn = DAG.getCopyFromReg(Chain, DL, VReg, RegVT);
        return ArgIn;
      }
    [/]
    [/]

    LLVM_DEBUG(dbgs() << "unpackFromRegLoc Unhandled argument type: " << RegVT.getEVTString() << "\n");
    llvm_unreachable("arguments type did not fit a register class!");
}

static SDValue unpackFromMemLoc(SelectionDAG &DAG, SDValue Chain, const CCValAssign &VA, const SDLoc &DL)
{
    MachineFunction &MF = DAG.getMachineFunction();
    MachineFrameInfo &MFI = MF.getFrameInfo();
    EVT LocVT = VA.getLocVT();
    EVT ValVT = VA.getValVT();
    EVT PtrVT = MVT::[(${stackPointerType})]; // TODO: @chochrainer --> MVT::getIntegerVT( DAG.getDataLayout().getPointerSizeInBits( 0 ) );
    unsigned ObjectSize = ValVT.getSizeInBits() / 8;
    int FI = MFI.CreateFixedObject(ObjectSize, VA.getLocMemOffset(), /* Immutable= */ true);
    SDValue FIN = DAG.getFrameIndex(FI, PtrVT);
    SDValue ArgIn;

    ISD::LoadExtType ExtType;
    switch (VA.getLocInfo())
    {
    case CCValAssign::Full:
    case CCValAssign::BCvt:
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
    case CCValAssign::Indirect:
    {
        // TODO: @chochrainer implement indirect load
        llvm_unreachable("indirect assignment is not yet supported");
    }
    default:
    {
        llvm_unreachable("Unexpected CCValAssign::LocInfo");
    }
    }

    auto MPI = MachinePointerInfo::getFixedStack(DAG.getMachineFunction(), FI);
    ArgIn = DAG.getExtLoad(ExtType, DL, LocVT, Chain, FIN, MPI, ValVT);
    return ArgIn;
}

SDValue [(${namespace})]TargetLowering::LowerFormalArguments(SDValue Chain, CallingConv::ID CallConv, bool isVarArg, const SmallVectorImpl<ISD::InputArg> &Ins, const SDLoc &dl, SelectionDAG &DAG, SmallVectorImpl<SDValue> &InVals) const
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
    CCInfo.AnalyzeFormalArguments(Ins, CC_[(${namespace})]);

    // Used with va_args to accumulate store chains.
    std::vector<SDValue> OutChains;

    for (auto &VA : ArgLocs)
    {
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

        // TODO: @chochrainer implement indirect loads
        assert(VA.getLocInfo() != CCValAssign::Indirect && "indirect load of arguments is not yet supported!");

        InVals.push_back(ArgValue);
    }

    if (isVarArg)
    {
        [(${namespace})]TargetLowering::WriteToVarArgs(OutChains, Chain, dl, DAG, CCInfo);
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
void [(${namespace})]TargetLowering::WriteToVarArgs(std::vector<SDValue> &OutChains, SDValue Chain, const SDLoc &DL, SelectionDAG &DAG, CCState &CCInfo) const
{
    [#th:block th:if="${argumentRegisters.size() == 0}"]
        /* architecture has no function argument registers */

        MachineFunction &MF = DAG.getMachineFunction();
        MachineFrameInfo &MFI = MF.getFrameInfo();
        [(${namespace})]MachineFunctionInfo *RVFI = MF.getInfo<[(${namespace})]MachineFunctionInfo>();

        int VarArgsSaveSize = 0;
        int VaArgOffset = CCInfo.getStackSize();

        int FI = MFI.CreateFixedObject(1 /* simple say 1 byte as only the pointer is relevant */, VaArgOffset, true);
        RVFI->setVarArgsFrameIndex(FI);
        RVFI->setVarArgsSaveSize(VarArgsSaveSize);
    [/th:block]
    [#th:block th:if="${argumentRegisters.size() > 0}"]
        MachineFunction &MF = DAG.getMachineFunction();
        MachineFrameInfo &MFI = MF.getFrameInfo();
        MachineRegisterInfo &RegInfo = MF.getRegInfo();
        [(${namespace})]MachineFunctionInfo *RVFI = MF.getInfo<[(${namespace})]MachineFunctionInfo>();

        [#th:block th:each="cl : ${argumentRegisterClasses}" ]
        const MCPhysReg Arg[(${cl.name})]s[] =
        {
          // We only support currently one register class for all the argument registers.
          [#th:block th:each="rg, iterStat : ${argumentRegisters}" ]
            [(${namespace})]::[(${rg})][#th:block th:if="${!iterStat.last}"],[/th:block]
          [/th:block]
        };
        [/th:block]

        [#th:block th:each="cl : ${argumentRegisterClasses}" ]
          unsigned [(${cl.name})]LenInBytes = [(${cl.resultWidth / 8})];
          MVT [(${cl.name})]LenVT = MVT::[(${cl.llvmResultType})];
          ArrayRef<MCPhysReg> [(${cl.name})]ArgRegs = makeArrayRef(Arg[(${cl.name})]s);
          unsigned [(${cl.name})]Idx = CCInfo.getFirstUnallocated( [(${cl.name})]ArgRegs);
          const TargetRegisterClass *[(${cl.name})]RC = &[(${namespace})]::[(${cl.name})]RegClass;
        [/th:block]

        // Offset of the first variable argument from stack pointer, and size of
        // the vararg save area. For now, the varargs save area is either zero or
        // large enough to hold all argument registers.
        int VarArgsSaveSize,
        VaArgOffset;

        // If all registers are allocated, then all varargs must be passed on the
        // stack and we don't need to save any argregs.
        if (
          [#th:block th:each="cl, iterStat : ${argumentRegisterClasses}" ]
          [(${cl.name})]ArgRegs.size() == [(${cl.name})]Idx [#th:block th:if="${!iterStat.last}"]&&[/th:block]
          [/th:block]
        )
        {
            VarArgsSaveSize = 0;
            VaArgOffset = CCInfo.getStackSize();
        }
        else
        {
            VarArgsSaveSize = 0;
            [#th:block th:each="cl : ${argumentRegisterClasses}" ]
              VarArgsSaveSize += [(${cl.name})]LenInBytes * ( [(${cl.name})]ArgRegs.size() - [(${cl.name})]Idx);
            [/th:block]
            VaArgOffset = -VarArgsSaveSize; // TODO: @chochrainer check if CCInfo.getStackSize() is needed
        }

        // TODO: @chochrainer eventually this could be optimized here with another offset calculation

        // Record the frame index of the first variable argument
        // which is a value necessary to VASTART.
        int FI = MFI.CreateFixedObject(1 /* simple say 1 byte as only the pointer is relevant */, VaArgOffset, true);
        RVFI->setVarArgsFrameIndex(FI);

        // TODO: @chochrainer add a stack alignment ( adapt VarArgsSaveSize )

        [#th:block th:each="cl : ${argumentRegisterClasses}" ]
        ////
        // save call for arguments of register class "«emitName( entry.getKey )»"
        //
        // Copy the integer registers that may have been used for passing varargs
        // to the vararg save area.
        for (unsigned I = [(${cl.name})]Idx; I < [(${cl.name})]ArgRegs.size(); ++I, VaArgOffset += [(${cl.name})]LenInBytes)
        {
            const Register Reg = RegInfo.createVirtualRegister( [(${cl.name})]RC);
            RegInfo.addLiveIn( [(${cl.name})]ArgRegs[I], Reg);
            SDValue ArgValue = DAG.getCopyFromReg(Chain, DL, Reg, [(${cl.name})]LenVT);
            FI = MFI.CreateFixedObject( [(${cl.name})]LenInBytes, VaArgOffset, true);
            SDValue PtrOff = DAG.getFrameIndex(FI, getPointerTy(DAG.getDataLayout()));
            SDValue Store = DAG.getStore(Chain, DL, ArgValue, PtrOff, MachinePointerInfo::getFixedStack(MF, FI));
            cast<StoreSDNode>(Store.getNode())
                ->getMemOperand()
                ->setValue((Value *)nullptr);
            OutChains.push_back(Store);
        }
        [/th:block]

        RVFI->setVarArgsSaveSize(VarArgsSaveSize);
    [/th:block]
}

SDValue [(${namespace})]TargetLowering::LowerCall(TargetLowering::CallLoweringInfo &CLI, SmallVectorImpl<SDValue> &InVals) const
{
    SelectionDAG &DAG = CLI.DAG;
    SDLoc &dl = CLI.DL;
    SmallVectorImpl<ISD::OutputArg> &Outs = CLI.Outs;
    SmallVectorImpl<SDValue> &OutVals = CLI.OutVals;
    SmallVectorImpl<ISD::InputArg> &Ins = CLI.Ins;
    SDValue Chain = CLI.Chain;
    SDValue Callee = CLI.Callee;
    CallingConv::ID CallConv = CLI.CallConv;
    const bool isVarArg = CLI.IsVarArg;

    // No support for tail calls for now
    CLI.IsTailCall = false;

    // Analyze operands of the call, assigning locations to each operand.
    SmallVector<CCValAssign, 0> ArgLocs;
    CCState CCInfo(CallConv, isVarArg, DAG.getMachineFunction(), ArgLocs, *DAG.getContext());
    CCInfo.AnalyzeCallOperands(Outs, CC_[(${namespace})]);

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

        // We only handle fully promoted arguments.
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
            SDValue StackPtr = DAG.getRegister( [(${namespace})]::[(${stackPointer})], MVT::[(${stackPointerType})] );
            SDValue PtrOff = DAG.getIntPtrConstant(VA.getLocMemOffset(), dl);
            PtrOff = DAG.getNode(ISD::ADD, dl, MVT::[(${stackPointerType})], StackPtr, PtrOff);
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

    unsigned OpFlag = [(${namespace})]BaseInfo::MO_None;
    if (GlobalAddressSDNode *G = dyn_cast<GlobalAddressSDNode>(Callee))
    {
        Callee = DAG.getTargetGlobalAddress(G->getGlobal(), dl, getPointerTy(DAG.getDataLayout()), /* Offset */ 0, OpFlag);
    }
    else if (ExternalSymbolSDNode *S = dyn_cast<ExternalSymbolSDNode>(Callee))
    {
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
    MachineFunction &MF = DAG.getMachineFunction();
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
    Chain = DAG.getNode( [(${namespace})]ISD::CALL, dl, NodeTys, Ops);
    InFlag = Chain.getValue(1);

    Chain = DAG.getCALLSEQ_END(Chain, DAG.getConstant(NextStackOffset, dl, MVT::[(${stackPointerType})], true), DAG.getConstant(0, dl, MVT::[(${stackPointerType})], true), InFlag, dl);

    InFlag = Chain.getValue(1);

    // Handle result values, copying them out of physregs into vregs that we return.
    return LowerCallResult(Chain, InFlag, CallConv, isVarArg, Ins, dl, DAG, InVals);
}

SDValue [(${namespace})]TargetLowering::LowerReturn(SDValue Chain, CallingConv::ID CallConv, bool isVarArg, const SmallVectorImpl<ISD::OutputArg> &Outs, const SmallVectorImpl<SDValue> &OutVals, const SDLoc &dl, SelectionDAG &DAG) const
{
    // CCValAssign - represent the assignment of the return value to a location
    SmallVector<CCValAssign, 0> RVLocs;

    // CCState - Info about the registers and stack slot.
    CCState CCInfo(CallConv, isVarArg, DAG.getMachineFunction(), RVLocs, *DAG.getContext());

    CCInfo.AnalyzeReturn(Outs, RetCC_[(${namespace})]);

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
    return DAG.getNode( [(${namespace})]ISD::RET_FLAG, dl, MVT::Other, RetOps);
}

SDValue [(${namespace})]TargetLowering::LowerCallResult(SDValue Chain, SDValue InGlue, CallingConv::ID CallConv, bool isVarArg, const SmallVectorImpl<ISD::InputArg> &Ins, const SDLoc &dl, SelectionDAG &DAG, SmallVectorImpl<SDValue> &InVals) const
{
    // Assign locations to each value returned by this call.
    SmallVector<CCValAssign, 0> RVLocs;
    CCState CCInfo(CallConv, isVarArg, DAG.getMachineFunction(), RVLocs, *DAG.getContext());

    CCInfo.AnalyzeCallResult(Ins, RetCC_[(${namespace})]);

    // Copy all of the result registers out of their specified physreg.
    for (auto &Loc : RVLocs)
    {
        Chain = DAG.getCopyFromReg(Chain, dl, Loc.getLocReg(), Loc.getValVT(), InGlue).getValue(1);
        InGlue = Chain.getValue(2);
        InVals.push_back(Chain.getValue(0));
    }

    return Chain;
}

bool [(${namespace})]TargetLowering::CanLowerReturn(CallingConv::ID CallConv, MachineFunction &MF, bool isVarArg, const SmallVectorImpl<ISD::OutputArg> &Outs, LLVMContext &Context) const
{
    // TODO: @chochrainer this can be improved

    SmallVector<CCValAssign, 0> RVLocs;
    CCState CCInfo(CallConv, isVarArg, MF, RVLocs, Context);
    if (CCInfo.CheckReturn(Outs, RetCC_[(${namespace})]) == false)
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
SDValue [(${namespace})]TargetLowering::getAddr(NodeTy *N, SelectionDAG &DAG, bool IsLocal) const
{
    SDLoc DL(N);
    EVT Ty = getPointerTy(DAG.getDataLayout());

    // PC relative address
    if (isPositionIndependent())
    {
        SDValue Addr = getTargetNode(N, DL, Ty, DAG, 0);
        if (IsLocal)
        {
          [# th:if="${hasPicLA == false}" ]
          report_fatal_error("Unsupported position independent local address loading");
          [/]
          [# th:if="${hasPicLA == true}" ]
          return DAG.getNode([(${namespace})]::[(${picLA})], DL, Ty, Addr);
          [/]
        }

        MachineFunction &MF = DAG.getMachineFunction();
        MachineMemOperand *MemOp = MF.getMachineMemOperand(
            MachinePointerInfo::getGOT(MF),
            MachineMemOperand::MOLoad | MachineMemOperand::MODereferenceable |
                MachineMemOperand::MOInvariant,
            LLT(Ty.getSimpleVT()), Align(Ty.getFixedSizeInBits() / 8));

        MachineFunction &MF = DAG.getMachineFunction();
              MachineMemOperand *MemOp = MF.getMachineMemOperand(
                  MachinePointerInfo::getGOT(MF),
                  MachineMemOperand::MOLoad | MachineMemOperand::MODereferenceable |
                      MachineMemOperand::MOInvariant,
                  LLT(Ty.getSimpleVT()), Align(Ty.getFixedSizeInBits() / 8));

        return DAG.getMemIntrinsicNode([(${namespace})]ISD::LGA, DL, DAG.getVTList(Ty, MVT::Other),
            {DAG.getEntryNode(), Addr}, Ty, MemOp);
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
        return SDValue(DAG.getMachineNode([(${namespace})]::[(${nonPicLA})], DL, Ty, Addr), 0);
    }
    }
}

SDValue [(${namespace})]TargetLowering::lowerJumpTable(SDValue Op, SelectionDAG &DAG) const
{
    JumpTableSDNode *N = cast<JumpTableSDNode>(Op);
    return getAddr(N, DAG);
}

SDValue [(${namespace})]TargetLowering::lowerGlobalAddress(SDValue Op, SelectionDAG &DAG) const
{
    SDLoc DL(Op);
    EVT Ty = Op.getValueType();
    GlobalAddressSDNode *N = cast<GlobalAddressSDNode>(Op);
    int64_t Offset = N->getOffset();

    const GlobalValue *GV = N->getGlobal();
    bool IsLocal = getTargetMachine().shouldAssumeDSOLocal(*GV->getParent(), GV);
    SDValue Addr = getAddr(N, DAG, IsLocal);

    // In order to maximize the opportunity for common subexpression elimination,
    // emit a separate ADD node for the global address offset instead of folding
    // it in the global address node. Later peephole optimizations may choose to
    // fold it back in when profitable.
    if (Offset != 0)
    {
        return DAG.getNode(ISD::ADD, DL, Ty, Addr, DAG.getConstant(Offset, DL, MVT::[(${stackPointerType})]));
    }

    return Addr;
}

SDValue [(${namespace})]TargetLowering::lowerBlockAddress(SDValue Op, SelectionDAG &DAG) const
{
    BlockAddressSDNode *N = cast<BlockAddressSDNode>(Op);
    return getAddr(N, DAG);
}

SDValue [(${namespace})]TargetLowering::lowerConstantPool(SDValue Op, SelectionDAG &DAG) const
{
    ConstantPoolSDNode *N = cast<ConstantPoolSDNode>(Op);
    return getAddr(N, DAG);
}

SDValue [(${namespace})]TargetLowering::lowerVASTART(SDValue Op, SelectionDAG &DAG) const
{
    MachineFunction &MF = DAG.getMachineFunction();
    [(${namespace})]MachineFunctionInfo *FuncInfo = MF.getInfo<[(${namespace})]MachineFunctionInfo>();

    SDLoc DL(Op);
    SDValue FI = DAG.getFrameIndex(FuncInfo->getVarArgsFrameIndex(), getPointerTy(MF.getDataLayout()));

    // vastart just stores the address of the VarArgsFrameIndex slot into the
    // memory location argument.
    const Value *SV = cast<SrcValueSDNode>(Op.getOperand(2))->getValue();
    return DAG.getStore(Op.getOperand(0), DL, FI, Op.getOperand(1), MachinePointerInfo(SV));
}

SDValue [(${namespace})]TargetLowering::lowerVAARG(SDValue Op, SelectionDAG &DAG) const
{
    SDNode *Node = Op.getNode();
    EVT VT = Node->getValueType(0);
    SDValue Chain = Node->getOperand(0);
    SDValue VAListPtr = Node->getOperand(1);
    const Value *SV = cast<SrcValueSDNode>(Node->getOperand(2))->getValue();
    SDLoc DL(Node);
    unsigned ArgSlotSizeInBytes = [(${stackPointerByteSize})]; // <-- TODO: @chochrainer this is not always the same

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
    [# th:each="bi : ${branchInstructions}" ]
    if (CC == ISD::[(${bi.isdName})] && MVT::[(${stackPointerType})] == Value)
    {
        return [(${namespace})]::[(${bi.instructionName})];
    }
    [/]

    std::cerr << "Cond " << CC << std::endl;
    llvm_unreachable("Unsupported CondCode");
}

SDValue [(${namespace})]TargetLowering::lowerSelect(SDValue Op, SelectionDAG &DAG) const
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
    // -> (riscvisd::select_cc lhs, rhs, cc, truev, falsev)
    if (Op.getSimpleValueType() == MVT::[(${stackPointerType})] && CondV.getOpcode() == ISD::SETCC &&
        CondV.getOperand(0).getSimpleValueType() == MVT::[(${stackPointerType})])
    {
        SDValue LHS = CondV.getOperand(0);
        SDValue RHS = CondV.getOperand(1);
        auto CC = cast<CondCodeSDNode>(CondV.getOperand(2));
        ISD::CondCode CCVal = CC->get();

        normaliseSetCC(LHS, RHS, CCVal);

        SDValue TargetCC = DAG.getConstant(CCVal, DL, MVT::[(${stackPointerType})]);
        SDVTList VTs = DAG.getVTList(Op.getValueType(), MVT::Glue);
        SDValue Ops[] = {LHS, RHS, TargetCC, TrueV, FalseV};
        return DAG.getNode([(${namespace})]ISD::SELECT_CC, DL, VTs, Ops);
    }

    // Otherwise:
    // (select condv, truev, falsev)
    // -> ([(${namespace})]isd::select_cc condv, zero, setne, truev, falsev)
    SDValue Zero = DAG.getConstant(0, DL, MVT::[(${stackPointerType})]);
    SDValue SetNE = DAG.getConstant(ISD::SETNE, DL, MVT::[(${stackPointerType})]);

    SDVTList VTs = DAG.getVTList(Op.getValueType(), MVT::Glue);
    SDValue Ops[] = {CondV, Zero, SetNE, TrueV, FalseV};

    return DAG.getNode([(${namespace})]ISD::SELECT_CC, DL, VTs, Ops);
}

MachineBasicBlock *
    [(${namespace})]TargetLowering::EmitInstrWithCustomInserter(MachineInstr &MI,
                                                                MachineBasicBlock *BB) const
{
    const TargetInstrInfo &TII = *BB->getParent()->getSubtarget().getInstrInfo();
    DebugLoc DL = MI.getDebugLoc();

    switch (MI.getOpcode())
    {
    default:
        llvm_unreachable("Unexpected instr type to insert");
    [# th:each="rg : ${registerFiles}" ]
      case [(${namespace})]::SelectCC_[(${rg.registerFileRef.name})]:
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
      unsigned Opcode = getBranchOpcodeForIntCondCode(CC, MVT::[(${stackPointerType})]);

      BuildMI(HeadMBB, DL, TII.get(Opcode))
          .addReg(LHS)
          .addReg(RHS)
          .addMBB(TailMBB);

      // IfFalseMBB just falls through to TailMBB.
      IfFalseMBB->addSuccessor(TailMBB);

      // %Result = phi [ %TrueValue, HeadMBB ], [ %FalseValue, IfFalseMBB ]
      BuildMI(*TailMBB, TailMBB->begin(), DL, TII.get([(${namespace})]::PHI),
              MI.getOperand(0).getReg())
          .addReg(MI.getOperand(4).getReg())
          .addMBB(HeadMBB)
          .addReg(MI.getOperand(5).getReg())
          .addMBB(IfFalseMBB);

      MI.eraseFromParent(); // The pseudo instruction is gone now.
      return TailMBB;
      break;
    [/]
    }
}

void [(${namespace})]TargetLowering::ReplaceNodeResults(SDNode *N,
                                             SmallVectorImpl<SDValue> &Results,
                                             SelectionDAG &DAG) const {
    SDLoc DL(N);
    switch (N->getOpcode()) {
    default:
      N->dump();
      llvm_unreachable("Don't know how to custom type legalize this operation!");
    }
}

bool [(${namespace})]TargetLowering::isLegalICmpImmediate(int64_t Imm) const {
  return Imm >= [(${conditionalValueRangeLowest})] && Imm <= [(${conditionalValueRangeHighest})];
}

bool [(${namespace})]TargetLowering::isLegalAddImmediate(int64_t Imm) const {
  return Imm >= [(${addImmediateInstruction.minValue})] && Imm <= [(${addImmediateInstruction.maxValue})];
}

bool [(${namespace})]TargetLowering::isLegalAddressingMode(const DataLayout &DL,
                                                const AddrMode &AM, Type *Ty,
                                                unsigned AS,
                                                Instruction *I) const {
  if(I == nullptr) {
    return true;
  }

  // No global is ever allowed as a base.
  if (AM.BaseGV)
    return false;

  auto withInRange = false;
  switch(I->getOpcode()) {
  [# th:each="mem : ${memoryInstructions}" ]
    case [(${namespace})]::[(${mem.instructionName})]:
      withInRange = AM.BaseOffs >= [(${mem.minValue})] && AM.BaseOffs <= [(${mem.maxValue})];
      break;
  [/]
    default:
      // because not affected
      return true;
  }

  // Only return when false
  if(!withInRange)
    return false;

  switch (AM.Scale) {
  case 0: // "r+i" or just "i", depending on HasBaseReg.
    break;
  case 1:
    if (!AM.HasBaseReg) // allow "r+i".
      break;
    return false; // disallow "r+r" or "r+r+i".
  default:
    return false;
  }

  return true;
}