#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]ISELLOWERING_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]ISELLOWERING_H

#include "llvm/CodeGen/SelectionDAG.h"
#include "llvm/CodeGen/TargetLowering.h"

namespace llvm
{
    class [(${namespace})]Subtarget;

    namespace [(${namespace})]ISD
    {
        enum NodeType : unsigned
        {
            FIRST_NUMBER = ISD::BUILTIN_OP_END,
            CALL,
            RET_FLAG,
            SELECT_CC
        };
    }

    class [(${namespace})]TargetLowering : public TargetLowering
    {
        // virtual anchor method to decrease link time as the vtable
        virtual void anchor();

        public:
            [(${namespace})]TargetLowering(const TargetMachine &TM, [(${namespace})]Subtarget &STI);

            const char *getTargetNodeName(unsigned Opcode) const;

            SDValue LowerOperation(SDValue Op, SelectionDAG &DAG) const;

            SDValue LowerFormalArguments
                ( SDValue Chain
                , CallingConv::ID CallConv
                , bool isVarArg
                , const SmallVectorImpl<ISD::InputArg> &Ins
                , const SDLoc &dl
                , SelectionDAG &DAG
                , SmallVectorImpl<SDValue> &InVals
                ) const override;

            SDValue LowerCall
                ( TargetLowering::CallLoweringInfo &CLI
                , SmallVectorImpl<SDValue> &InVals
                ) const override;

            SDValue LowerReturn
                ( SDValue Chain
                , CallingConv::ID CallConv
                , bool isVarArg
                , const SmallVectorImpl<ISD::OutputArg> &Outs
                , const SmallVectorImpl<SDValue> &OutVals
                , const SDLoc &dl
                , SelectionDAG &DAG
                ) const override;

            SDValue LowerCallResult
                ( SDValue Chain
                , SDValue InGlue
                , CallingConv::ID CallConv
                , bool isVarArg
                , const SmallVectorImpl<ISD::InputArg> &Ins
                , const SDLoc &dl
                , SelectionDAG &DAG
                , SmallVectorImpl<SDValue> &InVals
                ) const;

            bool CanLowerReturn
                ( CallingConv::ID CallConv
                , MachineFunction &MF
                , bool isVarArg
                , const SmallVectorImpl<ISD::OutputArg> &Outs
                , LLVMContext &Context
                ) const;

            MachineBasicBlock*
                EmitInstrWithCustomInserter(MachineInstr &MI,
                                            MachineBasicBlock *BB) const override;

            void ReplaceNodeResults(SDNode *N, SmallVectorImpl<SDValue> &Results, SelectionDAG &DAG) const override;


        private:
            const [(${namespace})]Subtarget &Subtarget;
            template <class NodeTy> SDValue getAddr(NodeTy *N, SelectionDAG &DAG, bool IsLocal = true) const;
            SDValue lowerGlobalAddress( SDValue Op, SelectionDAG &DAG ) const;
            SDValue lowerBlockAddress( SDValue Op, SelectionDAG &DAG ) const;
            SDValue lowerConstantPool( SDValue Op, SelectionDAG &DAG ) const;
            SDValue lowerVASTART(SDValue Op, SelectionDAG &DAG) const;
            SDValue lowerVAARG(SDValue Op, SelectionDAG &DAG) const;
            SDValue lowerSelect(SDValue Op, SelectionDAG &DAG) const;
            SDValue lowerJumpTable(SDValue Op, SelectionDAG &DAG) const;

            void WriteToVarArgs
                ( std::vector<SDValue> &OutChains
                , SDValue Chain, const SDLoc &DL
                , SelectionDAG &DAG
                , CCState &State
                ) const;
    };
}

#endif // LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]ISELLOWERING_H
